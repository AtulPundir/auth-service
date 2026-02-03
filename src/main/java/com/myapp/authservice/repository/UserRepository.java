package com.myapp.authservice.repository;

import com.myapp.authservice.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * User repository - matches Node.js Prisma client query methods
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * Find user by phone number
     * Matches: prisma.user.findUnique({ where: { phone } })
     */
    Optional<User> findByPhone(String phone);

    /**
     * Check if user exists by phone
     * Matches: prisma.user.findUnique({ where: { phone } }) != null
     */
    boolean existsByPhone(String phone);

    /**
     * Find user by email (case-insensitive)
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Find user by last N digits of phone number.
     * Used when identity-service sends phone without country code.
     */
    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE u.phone LIKE %:suffix")
    Optional<User> findByPhoneSuffix(@org.springframework.data.repository.query.Param("suffix") String suffix);
}
