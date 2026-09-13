## 1. Header Bar Alignment & Dynamic Filtering

- [ ] 1.1 Set fixed height `unit(10)` on the search card container and vertically center search field and icon in `BrowserSearchHeader.java`
- [ ] 1.2 Convert active filter chip bar from `.visible(...)` to `dynamic(...)` to eliminate residual empty layout space
- [ ] 1.3 Apply proper gaps (`gap(unit(2))`) and padding across header elements

## 2. Image-First Card Redesign

- [ ] 2.1 Refactor `SchematicCard.java` to hero image-first layout with translucent bottom title overlay using `stack()`
- [ ] 2.2 Replace redundant badges in `SchematicCard.java` with 4 compact interactive action buttons (`♡ likes`, `💬 comments`, `⬇ downloads`, `📋 copy`)
- [ ] 2.3 Refactor `MapCard.java` to matching hero image-first layout with overlaid title and interactive buttons (`♡ likes`, `💬 comments`, `⬇ downloads`, `▶ play`)

## 3. Full-Screen Detail Dialogs & Zero Layout Shift

- [ ] 3.1 Expand `SchematicDetailDialog.java` to full screen, allocating 55% width x 100% height in landscape and 45% height in portrait for the preview image
- [ ] 3.2 Expand `MapDetailDialog.java` to full screen with matching maximized preview image proportions
- [ ] 3.3 Add dark pre-allocated placeholder containers with icons (`Icon.image` / `Icon.terrain`) for `NetworkImage` in cards and dialogs to prevent layout shifts

## 4. Browser View Spacing & Dynamic States

- [ ] 4.1 Apply consistent `padding(unit(2))` and `gap(unit(2))` to root browser columns in `SchematicBrowserDialog.java` and `MapBrowserDialog.java`
- [ ] 4.2 Replace `.visible()` on loading indicator and error alert with `dynamic(...)` to remove empty table space

## 5. Verification & Testing

- [ ] 5.1 Verify cards render cleanly with hero image, title overlay, and interactive buttons
- [ ] 5.2 Verify detail dialogs fill screen with expansive preview images
- [ ] 5.3 Run `./gradlew test` to ensure all tests pass cleanly
