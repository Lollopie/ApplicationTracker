CREATE TABLE application (
    application_id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    company text NOT NULL,
    position text NOT NULL,
    status text NOT NULL,
    notes text,
    links text,
    version int
);