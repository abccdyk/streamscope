package com.streamscope.backend.auth;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);
}