package com.stayease.repository;

import com.stayease.model.Token;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TokenRepository extends MongoRepository<Token, String>
{
    Optional<Token> findByTokenHash(String tokenHash);
}