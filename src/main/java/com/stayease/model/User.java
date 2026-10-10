package com.stayease.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User
{
    @Id
    private String userId;

    private String userName;

    private String userEmail;

    private String userPassword;

    private UserStatus userStatus = UserStatus.ACTIVE;

    private UserRole userRole = UserRole.CUSTOMER;

    public enum UserStatus
    {
        ACTIVE,
        INACTIVE
    }

    public enum UserRole
    {
        CUSTOMER,
        MANAGER,
        ADMIN
    }
}