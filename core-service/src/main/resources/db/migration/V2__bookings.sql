CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE bookings (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users(id),
    machine_id BIGINT NOT NULL REFERENCES machines(id),
    start_time TIMESTAMPTZ NOT NULL,
    end_time   TIMESTAMPTZ NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'CREATED'
               CHECK (status IN ('CREATED', 'STARTED', 'CANCELLED', 'NO_SHOW')),
    started_at TIMESTAMPTZ,
    CHECK (end_time > start_time),
    CONSTRAINT bookings_no_overlap EXCLUDE USING gist (
        machine_id WITH =,
        tstzrange(start_time, end_time) WITH &&
    ) WHERE (status IN ('CREATED', 'STARTED'))
);

CREATE INDEX idx_bookings_user ON bookings(user_id);

INSERT INTO users (name, email, role) VALUES
    ('Admin Test',   'admin@upt.ro',   'ADMIN'),
    ('Student Test', 'student@upt.ro', 'STUDENT');