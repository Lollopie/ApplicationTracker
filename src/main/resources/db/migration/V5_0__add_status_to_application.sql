ALTER TABLE application ADD COLUMN current_status text;

UPDATE application a
SET current_status = (
    SELECT s.status
    FROM status_change s
    WHERE s.application_id = a.application_id
    ORDER BY s.id DESC
    LIMIT 1
);

ALTER TABLE application ALTER COLUMN current_status SET NOT NULL;

ALTER TABLE application ADD CONSTRAINT fk_application_current_status FOREIGN KEY (current_status) REFERENCES status(status) ON UPDATE CASCADE;