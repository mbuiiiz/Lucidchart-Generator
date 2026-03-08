package com.group16.aetherxmlbridge.repository; 

import java.util.Optional; 
import org.springframework.data.jpa.repository.JpaRepository; 
import com.group16.aetherxmlbridge.model.AppUser; 

// Define repository for AppUser entities.
public interface AppUserRepository extends JpaRepository<AppUser, Long> { 

  Optional<AppUser> findByEmail(String email); // Find a user by email if present.
}
