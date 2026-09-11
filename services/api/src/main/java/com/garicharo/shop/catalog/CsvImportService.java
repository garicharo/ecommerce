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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.garicharo.shop.identity.User;
import com.garicharo.shop.shared.ApiException;

@Service
public class CsvImportService {

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

        try {
            byte[] fileContent = file.getBytes();
            try (CSVParser csvParser = CSVFormat.RFC4180.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .build()
                    .parse(new InputStreamReader(new ByteArrayInputStream(fileContent), StandardCharsets.UTF_8))) {

                List<String> headers = csvParser.getHeaderNames();
                OffsetDateTime now = OffsetDateTime.now();

                if (headers.isEmpty()
                        || !headers.containsAll(CsvRowValidator.requiredColumns())
                        || CsvRowValidator.requiredColumns().stream()
                                .anyMatch(required -> headers.stream().filter(required::equals).count() > 1)) {
                    return saveFailedHeader(user, filename, fileContent, now);
                }

                ImportJob importJob = new ImportJob();
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
                    String name = csvRecord.get("name");
                    String sku = csvRecord.get("sku");
                    String description = csvRecord.get("description");
                    String category = csvRecord.get("category");
                    String price = csvRecord.get("price");
                    String stock = csvRecord.get("stock");
                    String weightKg = csvRecord.get("weight_kg");
                    int lineNumber = (int) csvParser.getCurrentLineNumber();

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
                        ImportRowResult row = new ImportRowResult();
                        row.setJob(importJob);
                        row.setLineNumber(lineNumber);
                        row.setOutcome(ImportRowOutcome.FAILED);
                        row.setSku(sku != null && !sku.isBlank() ? sku.trim() : null);
                        row.setCode(failure.code());
                        row.setMessage(failure.message());
                        row.setCreatedAt(now);
                        importRowResultRepository.save(row);
                        failed++;
                        continue;
                    }

                    CsvRowValidator.ValidRow validRow = CsvRowValidator.parse(
                            name, sku, description, category, price, stock, weightKg);
                    ImportRowOutcome outcome = productCsvWriter.upsert(validRow);
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
            throw new ApiException("CSV_IMPORT_FAILED", "Failed to import CSV", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ImportJobResponse getJob(UUID jobId) {
        ImportJob job = importJobRepository.findById(jobId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Import job not found", HttpStatus.NOT_FOUND));
        return ImportJobResponse.from(job, errorsGrouped(jobId));
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

    private List<Map<String, Object>> errorsGrouped(UUID jobId) {
        return importRowResultRepository
                .findByJobIdAndOutcome(jobId, ImportRowOutcome.FAILED, org.springframework.data.domain.Pageable.unpaged())
                .getContent()
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        row -> row.getCode() == null ? "UNKNOWN" : row.getCode(),
                        java.util.stream.Collectors.counting()))
                .entrySet()
                .stream()
                .map(entry -> Map.<String, Object>of("code", entry.getKey(), "count", entry.getValue()))
                .toList();
    }
}
