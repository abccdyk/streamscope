package com.streamscope.backend.auth;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("users")
public record AppUser(
        @Id Long id,
        String username,
        @Column("password_hash") String passwordHash
) {
}