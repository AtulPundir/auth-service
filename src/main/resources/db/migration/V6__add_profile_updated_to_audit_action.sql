-- Migration: Add PROFILE_UPDATED to AuditAction enum
-- Required for profile update functionality

ALTER TYPE "AuditAction" ADD VALUE IF NOT EXISTS 'PROFILE_UPDATED';
