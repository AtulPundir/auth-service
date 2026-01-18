-- Migration: Create audit_logs table
-- Matches Node.js Prisma schema exactly

CREATE TABLE audit_logs (
    id VARCHAR(30) PRIMARY KEY,
    user_id VARCHAR(30),
    action VARCHAR(30) NOT NULL,
    phone VARCHAR(20),
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    metadata JSONB,
    success BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE SET NULL
);

-- Indexes for performance
CREATE INDEX idx_audit_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_action ON audit_logs(action);
CREATE INDEX idx_audit_created_at ON audit_logs(created_at);
CREATE INDEX idx_audit_phone ON audit_logs(phone);

-- Comments
COMMENT ON TABLE audit_logs IS 'Comprehensive audit trail for all authentication events';
COMMENT ON COLUMN audit_logs.action IS 'Audit action type (USER_SIGNUP, USER_LOGIN_OTP, etc.)';
COMMENT ON COLUMN audit_logs.metadata IS 'Additional context as JSON';
COMMENT ON COLUMN audit_logs.success IS 'Whether the action was successful';
