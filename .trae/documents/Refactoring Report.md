Refactoring completed successfully.

**1. Created Services Directory**
- Created `src/mindustrytool/services/`.

**2. Split and Renamed APIs to Services**
- **BrowserApi** was split into:
  - `SchematicService.java`: Handles schematic downloads and lookups.
  - `MapService.java`: Handles map downloads and lookups.
  - `UserService.java`: Handles user lookups (extracted `findUserById`).
- **PlayerConnectApi** was renamed to:
  - `PlayerConnectService.java`: Handles player connect rooms and providers.

**3. Moved Existing Services**
- Moved `ModService.java` to `src/mindustrytool/services/`.
- Moved `TagService.java` to `src/mindustrytool/services/`.

**4. Refactored Data**
- Updated `TagCategory.java` to use `private` fields with Lombok `@Data` and fluent accessors, consistent with `ModData` and other DTOs.

**5. Updated References**
- Updated `SchematicDialog.java` to use `SchematicService` and `TagService`.
- Updated `MapDialog.java` to use `MapService` and `TagService`.
- Updated `UserCard.java` to use `UserService`.
- Updated `FilterDialog.java` to import `ModService` and `TagService` from the new package.
- Updated `PlayerConnectRoomsDialog.java` and `PlayerConnectProviders.java` to use `PlayerConnectService`.

**6. Cleanup**
- Deleted the old `BrowserApi.java`, `PlayerConnectApi.java`, and the old service files in `features/browser`.
