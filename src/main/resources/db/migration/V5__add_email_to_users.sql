-- Migration: Add email column to users table

ALTER TABLE users ADD COLUMN email VARCHAR(100);

-- Index for email lookups (optional, add if you plan to search by email)
CREATE INDEX idx_user_email ON users(email);

-- Comment
COMMENT ON COLUMN users.email IS 'User email address (optional)';
