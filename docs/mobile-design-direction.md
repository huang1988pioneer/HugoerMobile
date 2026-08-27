# Hugoer Mobile — direction contract

<!-- impeccable:direction-contract -->
<!-- THESIS: Preserve the original Hugoer Mobile five-tab workflow so a returning author can move between site, configuration, theme, content, and GitHub without learning a replacement dashboard. -->
<!-- OWN-WORLD: White title bars, a quiet light grouped background, soft gray cards, and a single confident blue signal. Native Material 3 / SwiftUI components carry structure while the five-tab rhythm carries continuity. -->
<!-- STORY: A returning author sees the connected site first, edits Hugo settings or a theme, opens content/post, and then checks or triggers GitHub Pages deployment. -->
<!-- FIRST VIEWPORT: Large screen title with settings affordance, a connected-site card, explanatory copy, and a single add-site FAB above the five-tab bar. -->
<!-- FORM: Original five-tab mobile workflow restored from the supplied reference screenshots and the native implementation in the repository history. -->
<!-- FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance -->

## Grounded directions (ordered before the roll)

1. **Connected site** — make the active repository immediately legible and keep local-link management close at hand.
2. **Configuration** — keep basic Hugo fields, Params, and raw TOML as adjacent tabs.
3. **Theme catalog** — show the active theme and one-tap choices with their source URLs.
4. **Content browser** — keep `content/post`, search, refresh, add, and delete in one predictable list.
5. **GitHub status** — show permissions, Pages health, deployment, and Actions history together.

The supplied screenshots are the visual source of truth for this release; the native screens now follow that information architecture instead of the interim dispatch-board design.

## Visual checks

The acceptance check is direct comparison with the supplied reference screens: the
five tab labels, large title bars, light gray surfaces, blue selected states, card
spacing, and the visible content/configuration values must remain recognizable.

## Platform expression

- Android uses a compact `NavigationBar`, expanded `NavigationRail`, Material 3 top bars, a Sites FAB, snackbars for transient status, and edge-to-edge insets.
- iOS uses `TabView` for the five top-level sections, `NavigationStack`, sheets for article editing, SF Symbols, semantic colors, safe areas, and Dynamic Type.
- Both platforms share the same demo repository contract and copy; platform controls are not visually transplanted across OSes.
