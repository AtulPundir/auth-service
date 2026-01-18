package com.myapp.authservice.repository;

import com.myapp.authservice.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * RefreshToken repository - matches Node.js Prisma client query methods
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    /**
     * Find refresh token by token string
     * Matches: prisma.refreshToken.findUnique({ where: { token }, include: { user: true } })
     */
    @Query("SELECT rt FROM RefreshToken rt JOIN FETCH rt.user WHERE rt.token = :token")
    Optional<RefreshToken> findByTokenWithUser(@Param("token") String token);

    /**
     * Find refresh token by token string (without user)
     * Matches: prisma.refreshToken.findUnique({ where: { token } })
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Delete all refresh tokens for a user
     * Matches: prisma.refreshToken.deleteMany({ where: { userId } })
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.user.id = :userId")
    void deleteAllByUserId(@Param("userId") String userId);

    /**
     * Delete specific refresh token for a user
     * Matches: prisma.refreshToken.deleteMany({ where: { userId, token } })
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.user.id = :userId AND rt.token = :token")
    void deleteByUserIdAndToken(@Param("userId") String userId, @Param("token") String token);

    /**
     * Delete expired refresh tokens (cleanup job)
     * Matches: prisma.refreshToken.deleteMany({ where: { expiresAt: { lt: new Date() } } })
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);
}
