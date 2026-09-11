package com.garicharo.shop.catalog;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.garicharo.shop.identity.User;
import com.garicharo.shop.identity.UserRepository;
import com.garicharo.shop.shared.ApiException;

@RestController
@RequestMapping("/api/admin/imports")
public class ImportController {

    private final CsvImportService csvImportService;
    private final ImportJobRepository importJobRepository;
    private final ImportRowResultRepository importRowResultRepository;
    private final UserRepository userRepository;

    public ImportController(
            CsvImportService csvImportService,
            ImportJobRepository importJobRepository,
            ImportRowResultRepository importRowResultRepository,
            UserRepository userRepository) {
        this.csvImportService = csvImportService;
        this.importJobRepository = importJobRepository;
        this.importRowResultRepository = importRowResultRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<Map<String, UUID>> create(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ApiException("NOT_FOUND", "User not found", HttpStatus.UNAUTHORIZED));
        UUID jobId = csvImportService.importCsv(file, user);
        return ResponseEntity.accepted().body(Map.of("jobId", jobId));
    }

    @GetMapping
    public Page<ImportJobResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return importJobRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(job -> ImportJobResponse.from(job, List.of()));
    }

    @GetMapping("/{jobId}")
    public ImportJobResponse get(@PathVariable UUID jobId) {
        return csvImportService.getJob(jobId);
    }

    @GetMapping("/{jobId}/rows")
    public Page<ImportRowResult> rows(
            @PathVariable UUID jobId,
            @RequestParam(required = false) ImportRowOutcome outcome,
            @PageableDefault(size = 20) Pageable pageable) {
        if (!importJobRepository.existsById(jobId)) {
            throw new ApiException("NOT_FOUND", "Import job not found", HttpStatus.NOT_FOUND);
        }
        if (outcome == null) {
            return importRowResultRepository.findByJobId(jobId, pageable);
        }
        return importRowResultRepository.findByJobIdAndOutcome(jobId, outcome, pageable);
    }
}
