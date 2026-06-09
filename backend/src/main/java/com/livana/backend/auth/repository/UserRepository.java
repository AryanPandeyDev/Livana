package com.livana.backend.auth.repository;

import com.livana.backend.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByClerkId(String clerkId);

    Optional<User> findByWalletAddress(String walletAddress);

    Optional<User> findByEmail(String email);

    boolean existsByWalletAddress(String walletAddress);
}
