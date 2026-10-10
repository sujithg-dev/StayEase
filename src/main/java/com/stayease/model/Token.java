package com.stayease.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Token
{
    @Id
    private String id;

    private String userId;

    private String tokenHash;

    private LocalDateTime expiryTime;

    private boolean used = false;
}