package com.stayease.repository;

import com.stayease.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String>
{
    Optional<User> findByUserEmail(String userEmail);

    boolean existsByUserEmail(String userEmail);

    long countByUserRole(User.UserRole userRole);
}