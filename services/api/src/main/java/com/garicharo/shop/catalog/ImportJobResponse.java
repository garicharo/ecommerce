package com.garicharo.shop.catalog;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ImportJobResponse(
        UUID jobId,
        String filename,
        ImportJobStatus status,
        int totalRows,
        int inserted,
        int updated,
        int failed,
        int skipped,
        int warnings,
        String headline,
        List<Map<String, Object>> errorsGrouped,
        OffsetDateTime createdAt,
        OffsetDateTime finishedAt) {

    static ImportJobResponse from(ImportJob job, List<Map<String, Object>> errorsGrouped) {
        int loaded = job.getInserted() + job.getUpdated();
        String headline;
        if (job.getStatus() == ImportJobStatus.FAILED_HEADER) {
            headline = "Header is invalid. No rows were imported.";
        } else if (job.getStatus() == ImportJobStatus.COMPLETED) {
            headline = "File with " + job.getTotalRows() + " rows: " + loaded + " loaded (" + job.getInserted()
                    + " new, " + job.getUpdated() + " already existed), " + job.getFailed() + " had errors";
            if (job.getSkipped() > 0) {
                headline += ", " + job.getSkipped() + " skipped (empty)";
            }
            headline += ".";
        } else {
            headline = "Import " + job.getStatus().name().toLowerCase() + ".";
        }
        return new ImportJobResponse(
                job.getId(),
                job.getFilename(),
                job.getStatus(),
                job.getTotalRows(),
                job.getInserted(),
                job.getUpdated(),
                job.getFailed(),
                job.getSkipped(),
                job.getWarnings(),
                headline,
                errorsGrouped,
                job.getCreatedAt(),
                job.getFinishedAt());
    }
}
