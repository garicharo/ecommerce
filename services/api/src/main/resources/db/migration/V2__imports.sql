CREATE TABLE import_jobs (
    id UUID PRIMARY KEY DEFAULT GEN_RANDOM_UUID(),
    created_by UUID NOT NULL REFERENCES users (id),
    filename VARCHAR(255) NOT NULL,
    byte_size INT NOT NULL CHECK (byte_size >= 0),
    file_content BYTEA NOT NULL,
    status VARCHAR(32) NOT NULL CHECK (status IN ('QUEUED', 'RUNNING', 'COMPLETED', 'FAILED_HEADER')),
    header_ok BOOLEAN NOT NULL DEFAULT FALSE,
    total_rows INT NOT NULL DEFAULT 0,
    inserted INT NOT NULL DEFAULT 0,
    updated INT NOT NULL DEFAULT 0,
    failed INT NOT NULL DEFAULT 0,
    skipped INT NOT NULL DEFAULT 0,
    warnings INT NOT NULL DEFAULT 0,
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE import_row_results (
    id UUID PRIMARY KEY DEFAULT GEN_RANDOM_UUID(),
    job_id UUID NOT NULL REFERENCES import_jobs (id),
    line_number INT NOT NULL,
    sku VARCHAR(255),
    outcome VARCHAR(32) NOT NULL CHECK (outcome IN ('INSERTED', 'UPDATED', 'FAILED', 'SKIPPED')),
    code VARCHAR(64),
    message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_import_row_results_job_outcome ON import_row_results (job_id, outcome);
