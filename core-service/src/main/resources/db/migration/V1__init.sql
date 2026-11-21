CREATE TABLE users (
    id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name  VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    role  VARCHAR(20)  NOT NULL CHECK (role IN ('STUDENT', 'ADMIN'))
);

CREATE TABLE machines (
    id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name     VARCHAR(100) NOT NULL,
    type     VARCHAR(20)  NOT NULL CHECK (type IN ('WASHER', 'DRYER')),
    building VARCHAR(50)  NOT NULL,
    status   VARCHAR(20)  NOT NULL DEFAULT 'AVAILABLE'
             CHECK (status IN ('AVAILABLE', 'MAINTENANCE'))
);

INSERT INTO machines (name, type, building) VALUES
    ('Washer 01', 'WASHER', 'A'),
    ('Washer 02', 'WASHER', 'A'),
    ('Dryer 01',  'DRYER',  'A'),
    ('Washer 03', 'WASHER', 'B');