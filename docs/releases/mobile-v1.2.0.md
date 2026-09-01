# Hugoer Mobile v1.2.0

Quality release after the Grok 4.6 refactor. Product behaviour stays the same: manage Hugo sites through the GitHub REST API on Android and iOS.

## Highlights

- Split the Android UI into feature screens (sites, config, themes, content, GitHub, settings) so the main activity is no longer a single 500-line file.
- Stop silently dropping user actions while a previous GitHub request is in flight; work is serialized instead of cancelled or ignored.
- Debounce content search, persist the active site across launches, and upsert a site when the same owner/repo is linked again.
- Load GitHub repo, Pages, and Actions data in parallel.
- Show the real app version, open Actions runs in the browser, and map GitHub 401/403/409/429 into clearer Chinese error messages.
- Send a `User-Agent: HugoerMobile` header on every GitHub request (required by the API).
- Drive iOS `CFBundleShortVersionString` from `MARKETING_VERSION` so GitHub Actions can stamp `v1.2.0` onto the IPA.

## Tests

- Repository URL parsing (`owner/repo`, HTTPS, `.git` suffix, user Pages sites).
- Post slug generation and front-matter round-trip.
- Existing Hugo shortcode / media preview parser tests.

## Packages

- Android: signed APK when Android signing secrets are configured; otherwise a QA preview-signed APK plus an unsigned APK.
- iOS: signed IPA when Apple signing secrets are configured; otherwise an unsigned IPA.
