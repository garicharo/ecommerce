package com.garicharo.shop.catalog;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.garicharo.shop.identity.User;
import com.garicharo.shop.shared.ApiException;

@Service
public class CsvImportService {

    private static final Logger log = LoggerFactory.getLogger(CsvImportService.class);
    private static final long MAX_BYTES = 2 * 1024 * 1024;

    private final ImportJobRepository importJobRepository;
    private final ImportRowResultRepository importRowResultRepository;
    private final ProductCsvWriter productCsvWriter;

    public CsvImportService(
            ImportJobRepository importJobRepository,
            ImportRowResultRepository importRowResultRepository,
            ProductCsvWriter productCsvWriter) {
        this.importJobRepository = importJobRepository;
        this.importRowResultRepository = importRowResultRepository;
        this.productCsvWriter = productCsvWriter;
    }

    public UUID importCsv(MultipartFile file, User user) {
        if (file == null || file.isEmpty()) {
            throw new ApiException("INVALID_HEADER", "CSV file is empty", HttpStatus.BAD_REQUEST);
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ApiException("FILE_TOO_LARGE", "CSV must be 2 MB or smaller", HttpStatus.PAYLOAD_TOO_LARGE);
        }
        String filename = file.getOriginalFilename() == null ? "upload.csv" : file.getOriginalFilename();
        String contentType = file.getContentType() == null ? "" : file.getContentType();
        if (!filename.toLowerCase().endsWith(".csv")
                && !contentType.contains("csv")
                && !contentType.equals("application/vnd.ms-excel")) {
            throw new ApiException("INVALID_HEADER", "File must be a CSV", HttpStatus.BAD_REQUEST);
        }

        ImportJob importJob = null;
        try {
            // Kept in memory for the job audit copy (cap 2 MB). Parse from the same bytes with BOM stripped.
            byte[] fileContent = file.getBytes();
            try (CSVParser csvParser = CSVFormat.RFC4180.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .build()
                    .parse(utf8ReaderWithoutBom(fileContent))) {

                List<String> headers = csvParser.getHeaderNames();
                OffsetDateTime now = OffsetDateTime.now();

                if (headers.isEmpty()
                        || !headers.containsAll(CsvRowValidator.requiredColumns())
                        || CsvRowValidator.requiredColumns().stream()
                                .anyMatch(required -> headers.stream().filter(required::equals).count() > 1)) {
                    return saveFailedHeader(user, filename, fileContent, now);
                }

                List<String> ignoredColumns = headers.stream()
                        .filter(header -> !CsvRowValidator.requiredColumns().contains(header))
                        .toList();
                if (!ignoredColumns.isEmpty()) {
                    log.info("ignoredColumns={}", ignoredColumns);
                }

                importJob = new ImportJob();
                importJob.setCreatedBy(user);
                importJob.setStatus(ImportJobStatus.RUNNING);
                importJob.setFilename(filename);
                importJob.setFileContent(fileContent);
                importJob.setByteSize(fileContent.length);
                importJob.setHeaderOk(true);
                importJob.setStartedAt(now);
                importJob.setCreatedAt(now);
                importJob = importJobRepository.save(importJob);

                int inserted = 0;
                int updated = 0;
                int failed = 0;
                int skipped = 0;
                int warnings = 0;
                int rows = 0;
                Map<String, ImportRowResult> firstBySku = new HashMap<>();

                for (CSVRecord csvRecord : csvParser) {
                    rows++;
                    int lineNumber = (int) csvParser.getCurrentLineNumber();
                    String name;
                    String sku;
                    String description;
                    String category;
                    String price;
                    String stock;
                    String weightKg;
                    try {
                        name = csvRecord.get("name");
                        sku = csvRecord.get("sku");
                        description = csvRecord.get("description");
                        category = csvRecord.get("category");
                        price = csvRecord.get("price");
                        stock = csvRecord.get("stock");
                        weightKg = csvRecord.get("weight_kg");
                    } catch (IllegalArgumentException ex) {
                        // Truncated / uneven rows (fewer cells than the header).
                        if (csvRecord.size() == 0 || (csvRecord.size() == 1 && blankCell(csvRecord.get(0)))) {
                            ImportRowResult row = new ImportRowResult();
                            row.setJob(importJob);
                            row.setLineNumber(lineNumber);
                            row.setOutcome(ImportRowOutcome.SKIPPED);
                            row.setCreatedAt(now);
                            importRowResultRepository.save(row);
                            skipped++;
                            continue;
                        }
                        saveFailedRow(
                                importJob,
                                lineNumber,
                                null,
                                "MALFORMED_ROW",
                                "Row has fewer columns than the header",
                                now);
                        failed++;
                        continue;
                    }

                    if (CsvRowValidator.isEmptyRow(name, sku, description, category, price, stock, weightKg)) {
                        ImportRowResult row = new ImportRowResult();
                        row.setJob(importJob);
                        row.setLineNumber(lineNumber);
                        row.setOutcome(ImportRowOutcome.SKIPPED);
                        row.setCreatedAt(now);
                        importRowResultRepository.save(row);
                        skipped++;
                        continue;
                    }

                    CsvRowValidator.Failure failure = CsvRowValidator.validate(
                            name, sku, description, category, price, stock, weightKg);
                    if (failure != null) {
                        saveFailedRow(importJob, lineNumber, sku, failure.code(), failure.message(), now);
                        failed++;
                        continue;
                    }

                    CsvRowValidator.ValidRow validRow = CsvRowValidator.parse(
                            name, sku, description, category, price, stock, weightKg);
                    ImportRowOutcome outcome;
                    try {
                        outcome = productCsvWriter.upsert(validRow);
                    } catch (RuntimeException ex) {
                        log.warn("CSV upsert failed jobId={} line={} sku={}: {}",
                                importJob.getId(), lineNumber, validRow.sku(), ex.getMessage());
                        saveFailedRow(
                                importJob,
                                lineNumber,
                                validRow.sku(),
                                "UPSERT_FAILED",
                                "Could not save this row; other rows continue",
                                now);
                        failed++;
                        continue;
                    }

                    ImportRowResult importRowResult = new ImportRowResult();
                    importRowResult.setSku(validRow.sku());
                    importRowResult.setJob(importJob);
                    importRowResult.setLineNumber(lineNumber);
                    importRowResult.setOutcome(outcome);
                    importRowResult.setCreatedAt(now);
                    importRowResult = importRowResultRepository.save(importRowResult);

                    ImportRowResult earlier = firstBySku.putIfAbsent(validRow.sku(), importRowResult);
                    if (earlier != null) {
                        importRowResult.setCode("DUPLICATE_SKU_UPSERT");
                        importRowResult.setMessage(
                                "SKU already appeared on CSV line " + earlier.getLineNumber()
                                        + "; last row wins");
                        importRowResultRepository.save(importRowResult);
                        warnings++;
                    }

                    if (outcome == ImportRowOutcome.INSERTED) {
                        inserted++;
                    } else if (outcome == ImportRowOutcome.UPDATED) {
                        updated++;
                    }
                }

                importJob.setFinishedAt(OffsetDateTime.now());
                importJob.setStatus(ImportJobStatus.COMPLETED);
                importJob.setTotalRows(rows);
                importJob.setInserted(inserted);
                importJob.setUpdated(updated);
                importJob.setFailed(failed);
                importJob.setSkipped(skipped);
                importJob.setWarnings(warnings);
                importJobRepository.save(importJob);
                return importJob.getId();
            }
        } catch (IOException e) {
            markJobFailed(importJob);
            throw new ApiException("CSV_IMPORT_FAILED", "Failed to import CSV", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (RuntimeException e) {
            markJobFailed(importJob);
            throw e;
        }
    }

    public ImportJobResponse getJob(UUID jobId) {
        ImportJob job = importJobRepository.findById(jobId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Import job not found", HttpStatus.NOT_FOUND));
        return ImportJobResponse.from(job, errorsGrouped(jobId));
    }

    private void saveFailedRow(
            ImportJob importJob,
            int lineNumber,
            String sku,
            String code,
            String message,
            OffsetDateTime now) {
        ImportRowResult row = new ImportRowResult();
        row.setJob(importJob);
        row.setLineNumber(lineNumber);
        row.setOutcome(ImportRowOutcome.FAILED);
        row.setSku(sku != null && !sku.isBlank() ? sku.trim() : null);
        row.setCode(code);
        row.setMessage(message);
        row.setCreatedAt(now);
        importRowResultRepository.save(row);
    }

    private void markJobFailed(ImportJob importJob) {
        if (importJob == null || importJob.getId() == null) {
            return;
        }
        importJobRepository.findById(importJob.getId()).ifPresent(job -> {
            if (job.getStatus() == ImportJobStatus.RUNNING) {
                job.setStatus(ImportJobStatus.FAILED);
                job.setFinishedAt(OffsetDateTime.now());
                importJobRepository.save(job);
            }
        });
    }

    private UUID saveFailedHeader(User user, String filename, byte[] fileContent, OffsetDateTime now) {
        ImportJob importJob = new ImportJob();
        importJob.setCreatedBy(user);
        importJob.setFilename(filename);
        importJob.setFileContent(fileContent);
        importJob.setByteSize(fileContent.length);
        importJob.setStatus(ImportJobStatus.FAILED_HEADER);
        importJob.setHeaderOk(false);
        importJob.setStartedAt(now);
        importJob.setFinishedAt(now);
        importJob.setCreatedAt(now);
        return importJobRepository.save(importJob).getId();
    }

    private List<ErrorGroup> errorsGrouped(UUID jobId) {
        return importRowResultRepository
                .findByJobIdAndOutcome(jobId, ImportRowOutcome.FAILED, org.springframework.data.domain.Pageable.unpaged())
                .getContent()
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        row -> row.getCode() == null ? "UNKNOWN" : row.getCode(),
                        java.util.stream.Collectors.counting()))
                .entrySet()
                .stream()
                .map(entry -> new ErrorGroup(entry.getKey(), entry.getValue()))
                .toList();
    }

    static InputStreamReader utf8ReaderWithoutBom(byte[] content) {
        int offset = 0;
        if (content.length >= 3
                && (content[0] & 0xFF) == 0xEF
                && (content[1] & 0xFF) == 0xBB
                && (content[2] & 0xFF) == 0xBF) {
            offset = 3;
        }
        return new InputStreamReader(
                new ByteArrayInputStream(content, offset, content.length - offset),
                StandardCharsets.UTF_8);
    }

    private static boolean blankCell(String value) {
        return value == null || value.trim().isEmpty();
    }
}
