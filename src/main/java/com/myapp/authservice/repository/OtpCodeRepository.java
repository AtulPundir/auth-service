package com.myapp.authservice.repository;

import com.myapp.authservice.domain.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * OtpCode repository - matches Node.js Prisma client query methods
 */
@Repository
public interface OtpCodeRepository extends JpaRepository<OtpCode, String> {

    /**
     * Find valid OTP for verification
     * Matches: prisma.otpCode.findFirst({
     *   where: { phone, code, used: false, expiresAt: { gte: new Date() } },
     *   orderBy: { createdAt: 'desc' }
     * })
     */
    @Query("SELECT o FROM OtpCode o WHERE o.phone = :phone AND o.code = :code " +
           "AND o.used = false AND o.expiresAt > :now ORDER BY o.createdAt DESC")
    Optional<OtpCode> findValidOtp(@Param("phone") String phone,
                                    @Param("code") String code,
                                    @Param("now") LocalDateTime now);

    /**
     * Mark all OTPs for a phone as used
     * Matches: prisma.otpCode.updateMany({ where: { phone, used: false }, data: { used: true } })
     */
    @Modifying
    @Query("UPDATE OtpCode o SET o.used = true WHERE o.phone = :phone AND o.used = false")
    void markAllAsUsedForPhone(@Param("phone") String phone);

    /**
     * Delete expired and used OTPs (cleanup job)
     * Matches: prisma.otpCode.deleteMany({
     *   where: { OR: [{ expiresAt: { lt: new Date() } }, { used: true }] }
     * })
     */
    @Modifying
    @Query("DELETE FROM OtpCode o WHERE o.expiresAt < :now OR o.used = true")
    int deleteExpiredAndUsedOtps(@Param("now") LocalDateTime now);
}
