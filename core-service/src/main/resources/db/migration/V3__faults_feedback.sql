CREATE TABLE faults (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    machine_id  BIGINT NOT NULL REFERENCES machines(id),
    reported_by BIGINT NOT NULL REFERENCES users(id),
    description VARCHAR(500) NOT NULL,
    reported_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_faults_machine_time ON faults(machine_id, reported_at DESC);

CREATE TABLE feedback (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id      BIGINT NOT NULL REFERENCES users(id),
    message      TEXT NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT now()
);