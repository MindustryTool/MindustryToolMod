## Context

MindustryToolMod is a Java 8 (via Jabel) mod for Mindustry v154 with 100+ Java files across ~25 feature groups. No systematic code review has been performed. The codebase includes concurrency-sensitive UI code, network interactions, reflection, file I/O, and AI task systems that all warrant inspection.

## Goals / Non-Goals

**Goals:**
- Create a feature inventory document (`docs/FEATURE.md`) mapping every feature to its files
- For each feature group, produce an audit document (`docs/<feature>.md`) with categorized findings
- Identify: security issues, data loss risks, concurrency bugs, anti-patterns, dead code, tech debt, performance bottlenecks, architectural violations
- Each finding must include the exact file path and relevant line references
- Cover all feature groups as listed in the proposal

**Non-Goals:**
- Fixing identified issues (audit-only)
- Modifying any source code
- Writing tests
- Making architectural changes

## Decisions

1. **Per-feature audit files** rather than one monolithic report — easier to review, maintain, and assign remediation tasks
2. **docs/ directory** for outputs — standard location, git-tracked, separate from source
3. **Standardized finding format** — each finding has: severity, category, file:line, description, and recommendation
4. **Processing order** — ordered by risk profile: infrastructure/auth first (security-sensitive), then gameplay features, then display/UI
5. **Multi-pass approach** per feature: first inventory files, then scan for common patterns (reflection, thread access, raw JSON parsing, unchecked casts, etc.), then read each file for deeper issues

## Risks / Trade-offs

- [Large scope] → Process features one by one, do not multithread the audit
- [False positives] → Flag findings with confidence level (high/medium/low)
- [Time investment] → Focus on high-severity findings first; minor style issues can be deferred
