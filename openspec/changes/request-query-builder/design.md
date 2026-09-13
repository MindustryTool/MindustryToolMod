## Context

`MindustryTool.java` has a private `buildPagedUrl` helper that manually constructs query strings for paginated API calls. It's called from `searchMaps` and `searchSchematics`. The `RequestBuilder` class already handles headers, body, timeout, and auth — but has no query parameter support.

## Goals / Non-Goals

**Goals:**
- Add `.query()` overloads to `RequestBuilder` for declaring query parameters
- Accumulate params in `LinkedHashMap<String, List<String>>` (preserves order, supports repeated keys)
- Assemble query string at send time, before `resolveUrl()`
- Delete `buildPagedUrl` and migrate call sites

**Non-Goals:**
- Path parameter templating (e.g. `/maps/{id}`)
- Query parameter removal/clearing API
- Thread safety (RequestBuilder isn't thread-safe today)

## Decisions

### D1: Storage — `LinkedHashMap<String, List<String>>`

Accumulates query params. `LinkedHashMap` preserves insertion order for deterministic URLs. `List<String>` supports repeated keys (`tags=a&tags=b`).

### D2: Null key → throw NullPointerException

Null keys are programmer errors. Null/empty String values are silently skipped (common pattern: conditional params like `sort`).

### D3: Append vs Replace semantics

- `query(String, ...)` overloads → **append** to key's list
- `query(Map<String, String>)` → **replace** each key's list

This lets Map act as a "set" operation while individual overloads build incrementally.

### D4: Assemble at send time

Query params accumulate as `.query()` calls are chained. The final `?key=value&...` string is built in `sendAsync()` and prepended to the URL before `resolveUrl()` runs. This keeps `resolveUrl` unchanged and the URL path clean during chaining.

### D5: Encoding

`URLEncoder.encode(value, StandardCharsets.UTF_8)` on both keys and values. Matches existing `buildPagedUrl` behavior.

## Risks / Trade-offs

- **[Risk] URL already has `?`** → Check `url.contains("?")` and use `&` separator. Real-world HTTP behavior.
- **[Risk] Map replace surprises caller** → Documented in Javadoc. Append semantics for Map would make it indistinguishable from List overload.
