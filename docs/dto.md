# DTO Audit

## Findings

### Anti-Patterns

- **MEDIUM** `dto/` package — Mix of DTOs for different API endpoints (maps, schematics, auth, savesync, chat) in a flat package; no sub-package organization per domain
- **MEDIUM** `UserData.java` — Used by `UserService` for user info display; but `features/auth/dto/UserSession.java` has similar fields (id, name, imageUrl) — two DTOs for the same concept
- **LOW** `TaskData.java` / `TaskResponse.java` — Generic naming; unclear what API domain they belong to

### Dead Code / Tech Debt

- **LOW** Several DTOs use `lombok.Data` but may have unused fields from API responses (inherited from server-side schema)
