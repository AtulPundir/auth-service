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
}
