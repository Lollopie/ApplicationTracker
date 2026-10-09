CREATE TABLE status (
    status text NOT NULL PRIMARY KEY
);

INSERT INTO status(status) VALUES ('SAVED');
INSERT INTO status(status) VALUES ('APPLIED');
INSERT INTO status(status) VALUES ('INTERVIEW');
INSERT INTO status(status) VALUES ('OFFER');
INSERT INTO status(status) VALUES ('REJECTED');

CREATE TABLE status_cause (
    cause text NOT NULL PRIMARY KEY
);

INSERT INTO status_cause(cause) VALUES ('MANUAL');

CREATE TABLE status_change (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    application_id uuid NOT NULL,
    status text NOT NULL,
    cause text NOT NULL DEFAULT 'MANUAL',
    detail text,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_status_change_application FOREIGN KEY (application_id) REFERENCES "application"(application_id) ON DELETE CASCADE,
    CONSTRAINT fk_status_change_status FOREIGN KEY (status) REFERENCES "status"(status) ON UPDATE CASCADE,
    CONSTRAINT fk_status_change_cause FOREIGN KEY (cause) REFERENCES "status_cause"(cause) ON UPDATE CASCADE
);
CREATE INDEX idx_application_id_id ON status_change (application_id, id);
INSERT INTO status_change (application_id, status) SELECT application.application_id, application.status from application;

ALTER TABLE application DROP COLUMN status;