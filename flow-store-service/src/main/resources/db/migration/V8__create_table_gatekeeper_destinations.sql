CREATE TABLE gatekeeper_destinations (
    id bigint NOT NULL PRIMARY KEY,
    submitternumber text NOT NULL,
    destination text NOT NULL,
    packaging text NOT NULL,
    format text NOT NULL,
    CONSTRAINT unique_constraint_gatekeeper_destinations UNIQUE (submitternumber, destination, packaging, format)
);