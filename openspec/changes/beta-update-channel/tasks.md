## 1. Latest-Tag Selection (Pure Logic)

- [x] 1.1 Add pure max-tag selection over the releases list (helper location at implementer's discretion, beside ChangelogFormatter/VersionUtils), skipping unparseable entries
- [x] 1.2 Write unit tests for max-tag selection using real tag shapes (v5.0.3-v8-beta), ties, and empty/unparseable input — pure logic only, no network-fetch tests

## 2. Beta Gate in UpdateService

- [x] 2.1 Branch checkForUpdate on the beta flag: when enabled, skip the mod.hjson fetch, fetch releases only, and compare the max tag against the installed version
- [x] 2.2 Resolve beta-path failure, empty releases, and same-number ties to silent (log and finish, still invoking done); keep the beta-OFF path unchanged

## 3. Beta Dialog and Install

- [x] 3.1 Show the winning raw release tag as the latest version on the beta path with a prerelease-inclusive changelog
- [x] 3.2 Wire the beta dialog Update action to githubImportMod(repo, isJava, release, forceEnable) with the winning tag
- [x] 3.3 Verify build, Java 8 compatibility, Solim declarative rules, and i18n requirements (no new bundle keys expected); existing update/settings tests green
