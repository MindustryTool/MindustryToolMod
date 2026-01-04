# Authentication Feature (Social Domain)

## Overview
Handles user login, registration, and token management for MindustryTool services.

## API (`api/`)
- `AuthService`: Static methods for `login()`, `logout()`, `isLoggedIn()`.
- `AuthFeature`: UI integration (the login button/dialog).

## Events
- `mindustrytool.events.LoginStateChangeEvent`: Fired when login status changes.

## Usage
```java
// Check login status
if (AuthService.isLoggedIn()) { ... }

// Listen for changes
Events.on(LoginStateChangeEvent.class, e -> {
    Log.info("Logged in: " + e.isLoggedIn);
});
```
