CREATE EXTENSION citext;
CREATE TABLE users (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    email citext NOT NULL,
    username citext NOT NULL,
    password text NOT NULL
);

INSERT INTO users(email, username, password) VALUES('admin@example.org', 'admin', '${admin_password_hash}');

ALTER TABLE application ADD COLUMN user_id uuid;

UPDATE application SET user_id = (SELECT id FROM users WHERE username='admin');

ALTER TABLE application ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE application ADD CONSTRAINT fk_application_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

CREATE INDEX idx_application_user_id ON application (user_id, application_id);