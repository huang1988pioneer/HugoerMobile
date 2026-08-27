# Hugoer Mobile design system

Hugoer Mobile is a native companion for the desktop Hugo workbench. Its primary surface follows the original five-tab workflow: 站台、設定檔、主題、內容、GitHub. Each tab keeps one focused task visible without turning the phone into a scaled desktop sidebar.

## Visual language

- **Surface:** a quiet light grouped background with white title bars and soft gray content cards; dark mode maps the same roles to graphite surfaces.
- **Signal:** Hugoer blue identifies selected tabs, connected sites, links, healthy Pages state, and primary actions.
- **Decision:** destructive actions remain red and deployment actions use the platform's prominent button treatment.
- **Type:** platform system typography carries Dynamic Type / font scaling. Large screen titles make each tab immediately identifiable.
- **Shape:** generous rounded cards and pill-shaped segmented controls group decisions while preserving the original mobile rhythm.

## Interaction contract

1. 站台 answers which repository is connected and which site is active.
2. 設定檔 exposes the basic Hugo fields, Params 表單, and raw TOML tabs.
3. 主題 keeps the active theme and one-tap theme choices visible.
4. 內容 keeps `content/post` browsing, search, refresh, add, delete, and Markdown/Preview editing together.
5. GitHub makes Pages health, repository permission, deployment, and Actions history observable.

## Platform translation

Android uses edge-to-edge Compose Material 3, a five-destination bottom navigation bar on compact windows, a navigation rail on expanded windows, a Sites FAB, modal sheets, and snackbars.

iOS uses SwiftUI `TabView`, `NavigationStack`, sheets, confirmation dialogs, SF Symbols, semantic `Color`, safe-area behavior, and Dynamic Type. iPad uses the same content model while allowing the system navigation container to widen naturally.

## Accessibility and resilience

- Every primary action has a text label and a system icon; status is not communicated by color alone.
- Controls are given Material/SwiftUI minimum touch targets and remain usable with larger text.
- Empty, pending, and desktop-only states explain what happens next rather than leaving a blank card.
- The initial store is deterministic demo data behind a replaceable repository seam; credentials and network writes are not hidden in the UI layer.
