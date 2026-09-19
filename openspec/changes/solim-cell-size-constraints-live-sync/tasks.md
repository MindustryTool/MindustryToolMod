## 1. Implement Live Cell Size Synchronization in CellConfig

- [x] 1.1 Update `minWidth(float)` and `minWidth(Readable<Float>)` in `CellConfig.java` to invoke `applySizeToParentCell` on attached elements
- [x] 1.2 Update `minHeight(float)` and `minHeight(Readable<Float>)` in `CellConfig.java` to invoke `applySizeToParentCell` on attached elements
- [x] 1.3 Update `maxWidth(float)` and `maxWidth(Readable<Float>)` in `CellConfig.java` to invoke `applySizeToParentCell` on attached elements
- [x] 1.4 Update `maxHeight(float)` and `maxHeight(Readable<Float>)` in `CellConfig.java` to invoke `applySizeToParentCell` on attached elements

## 2. Verification & Testing

- [x] 2.1 Add unit tests in `solim-core` verifying that `minWidth`, `minHeight`, `maxWidth`, and `maxHeight` update parent Table Cells for already-attached components
- [x] 2.2 Run `./gradlew test` across `:solim-core` and `:mod` to ensure all tests pass and layout constraints work as expected
