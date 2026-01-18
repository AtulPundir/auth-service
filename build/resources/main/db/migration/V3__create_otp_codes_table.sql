-- Migration: Create otp_codes table
-- Matches Node.js Prisma schema exactly

CREATE TABLE otp_codes (
    id VARCHAR(30) PRIMARY KEY,
    phone VARCHAR(20) NOT NULL,
    code VARCHAR(10) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id VARCHAR(30),
    CONSTRAINT fk_otp_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX idx_otp_phone ON otp_codes(phone);
CREATE INDEX idx_otp_code ON otp_codes(code);
CREATE INDEX idx_otp_expires_at ON otp_codes(expires_at);

-- Comments
COMMENT ON TABLE otp_codes IS 'OTP codes for phone-based authentication';
COMMENT ON COLUMN otp_codes.code IS '6-digit OTP code';
COMMENT ON COLUMN otp_codes.expires_at IS 'OTP expiration timestamp (5 minutes)';
COMMENT ON COLUMN otp_codes.used IS 'Whether OTP has been used';
