# Admin User Configuration

This document explains how admin users are currently configured in aetherxmlbridge, how admin-only access works, and the limitations of the current setup.

## Overview

The application currently supports two roles:

- ROLE_ADMIN  
- ROLE_USER  

At the moment, admin assignment is implemented using a simple hardcoded email rule during registration. This was done to keep role-based access control simple during development.

## How Admin Role Assignment Works

Admin role assignment happens inside:

src/main/java/com/group16/aetherxmlbridge/service/AppUserService.java

In the registerUser(...) method, the application checks the email used during registration.

If the email matches the predefined admin email, the new account is assigned ROLE_ADMIN. Otherwise, the account is assigned ROLE_USER.

Current logic:

String assignedRole = email.equalsIgnoreCase("admin@gmail.com")
        ? "ROLE_ADMIN"
        : "ROLE_USER";

Result:

- admin@gmail.com → ROLE_ADMIN  
- any other email → ROLE_USER  

## Where Roles Are Stored

Roles are stored in the role field of the AppUser entity:

src/main/java/com/group16/aetherxmlbridge/model/AppUser.java

The role is passed to Spring Security through:

@Override
public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority(role));
}

This allows Spring Security to enforce route protection and UI visibility based on roles.

## Route Protection

Admin-only access is enforced in:

src/main/java/com/group16/aetherxmlbridge/config/ProdSecurityConfig.java

Admin routes are protected with:

.requestMatchers("/admin/**").hasRole("ADMIN")

This means:

- ROLE_ADMIN users can access /admin/**
- ROLE_USER users cannot access admin routes

## Admin Users Page

The admin-only page is:

/admin/users

Handled by:

src/main/java/com/group16/aetherxmlbridge/controller/AdminController.java

This page displays all registered users from the database, including:

- id  
- full name  
- email  
- role  
- created date  

It is used as a simple admin overview page.

## Admin-Only UI Elements

Some UI elements are only shown to admins (for example, the Manage Users button on the dashboard).

UI visibility is controlled by checking the current user's role before rendering elements.

Backend security remains the main protection layer.

## Current Limitations

The current setup is intentionally simple and has some limitations:

1. Admin assignment is hardcoded to one email  
2. Changing the admin requires changing code  
3. There is no admin management UI yet  
4. Additional admins cannot be assigned dynamically  

This approach works for development but is not ideal for long-term production use.

## Future Improvements

Possible improvements:

- Allow admins to manage roles from an admin panel  
- Move the hardcoded email to an environment variable  

Example:

DEV_ADMIN_EMAIL=admin@gmail.com

## Summary

Current implementation:

- two roles: ROLE_ADMIN and ROLE_USER  
- admin role assigned using hardcoded email  
- admin routes protected by Spring Security  
- admin UI conditionally rendered  

This works well for development and can be extended later if needed.
