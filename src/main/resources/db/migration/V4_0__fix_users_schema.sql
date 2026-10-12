ALTER TABLE users ADD CONSTRAINT uq_user_username UNIQUE (username);

ALTER TABLE users DROP COLUMN email;

ALTER TABLE users RENAME COLUMN password TO password_hash;

ALTER TABLE users ADD COLUMN created_at timestamptz NOT NULL DEFAULT NOW();