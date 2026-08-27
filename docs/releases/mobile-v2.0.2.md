# Hugoer Mobile v2.0.2

## Visual parity

- Restores the original five-tab mobile workflow: 站台、設定檔、主題、內容、GitHub.
- Matches the reference light surface, blue selection states, large screen titles,
  connected-site card, configuration fields, theme cards, content list, and
  GitHub deployment status layout.
- Keeps the native Markdown editor and media preview flow from v2.0.1.

## Delivery

- Android unit tests and release packaging run before publishing.
- iOS/iPadOS 17 is archived on a macOS runner and packaged as an IPA.
- The release contains preview-signed and unsigned Android APKs plus an unsigned
  IPA when Apple signing secrets are not configured.

For a manual release, enable `publish` and enter `mobile-v2.0.2` as `release_tag`.
