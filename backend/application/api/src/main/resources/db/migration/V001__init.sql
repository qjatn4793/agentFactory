CREATE TABLE jobs (
    id              CHAR(36)     NOT NULL,
    idea            TEXT         NOT NULL,
    status          VARCHAR(32)  NOT NULL,
    workspace_path  VARCHAR(512) NULL,
    error_message   TEXT         NULL,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_jobs_status (status),
    KEY idx_jobs_created_at (created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE artifacts (
    id          CHAR(36)     NOT NULL,
    job_id      CHAR(36)     NOT NULL,
    kind        VARCHAR(32)  NOT NULL,
    path        VARCHAR(1024) NOT NULL,
    size_bytes  BIGINT       NULL,
    checksum    VARCHAR(128) NULL,
    created_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_artifacts_job_id (job_id),
    CONSTRAINT fk_artifacts_job FOREIGN KEY (job_id) REFERENCES jobs (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE agent_executions (
    id             CHAR(36)     NOT NULL,
    job_id         CHAR(36)     NOT NULL,
    stage          VARCHAR(32)  NOT NULL,
    status         VARCHAR(32)  NOT NULL,
    attempt        INT          NOT NULL DEFAULT 1,
    started_at     DATETIME(6)  NULL,
    finished_at    DATETIME(6)  NULL,
    error_message  TEXT         NULL,
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_agent_executions_job_id (job_id),
    KEY idx_agent_executions_stage_status (stage, status),
    CONSTRAINT fk_agent_executions_job FOREIGN KEY (job_id) REFERENCES jobs (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
