CREATE TABLE proposals (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    kind       VARCHAR(20)  NOT NULL CHECK (kind IN ('MAINTENANCE', 'PENALTY')),
    target_id  BIGINT       NOT NULL,
    reason     VARCHAR(500) NOT NULL,
    severity   VARCHAR(10)  NOT NULL CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH')),
    status     VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
               CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    decided_by BIGINT REFERENCES users(id),
    decided_at TIMESTAMPTZ,
    CHECK (
        (status = 'PENDING'  AND decided_by IS NULL     AND decided_at IS NULL) OR
        (status <> 'PENDING' AND decided_by IS NOT NULL AND decided_at IS NOT NULL)
    )
);

CREATE UNIQUE INDEX ux_proposals_pending_target
    ON proposals (kind, target_id) WHERE status = 'PENDING';

CREATE INDEX idx_proposals_status ON proposals (status, created_at DESC);