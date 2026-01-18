-- Migration: Create users table
-- Matches Node.js Prisma schema exactly

CREATE TABLE users (
    id VARCHAR(30) PRIMARY KEY,
    phone VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    role VARCHAR(10) NOT NULL DEFAULT 'USER',
    passkey_hash VARCHAR(60),
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_user_phone ON users(phone);
CREATE INDEX idx_user_status ON users(status);

-- Comments
COMMENT ON TABLE users IS 'User accounts with phone-based authentication';
COMMENT ON COLUMN users.id IS 'CUID generated unique identifier';
COMMENT ON COLUMN users.phone IS 'E.164 formatted phone number';
COMMENT ON COLUMN users.passkey_hash IS 'BCrypt hashed passkey (optional)';
