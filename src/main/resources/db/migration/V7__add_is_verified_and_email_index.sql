-- Add is_verified column to users table
-- Placeholder users created via resolve-or-create will have is_verified = false
-- When user logs in via OTP, is_verified is set to true
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_verified BOOLEAN NOT NULL DEFAULT true;

-- Existing users (who logged in via OTP) are verified
UPDATE users SET is_verified = true WHERE is_verified IS NULL;

-- Add index on email for lookups
CREATE INDEX IF NOT EXISTS idx_user_email ON users(email);

-- Add index on is_verified for filtering
CREATE INDEX IF NOT EXISTS idx_user_is_verified ON users(is_verified);
