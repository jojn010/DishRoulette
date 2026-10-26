## v1.2.8

- Expand per-serving nutrition estimates with carbohydrates, fat, fiber, sugar (grams) and sodium (milligrams).
- Mark every nutrition estimate with `~~~` and explain ingredient size, brand, portion and preparation differences in English and Arabic.
- Preserve older recipes and their existing estimates without fabricating newly added nutrient values.
- Validate and persist new nutrients, with regression and on-device localization coverage.
- Constrain generated cuisine to actual supported values, preventing empty cuisine fields from invalidating otherwise complete recipes.

## v1.2.7

- Add estimated calories (kcal) and protein (grams) per serving to newly generated recipes.
- Show localized estimates in recipe results, saved archive details, and cooking metadata; retain estimates when saving.
- Keep old saved recipes readable with an explicit unavailable state rather than invented nutrition values.
- Validate estimate types and ranges; add persistence, compatibility and invalid-value coverage.

## v1.2.6

- Accept dotted Gemini keys in both encrypted storage and manual setup, fixing automatic private-build import for the existing owner connection.
- Share key-format checks and test dotted keys, legacy keys, length bounds, and invalid header characters.

## v1.2.5

- Add an opt-in owner-only APK build that imports the existing Gemini connection on first launch, with no API entry or laptop tether needed.
- Preserve an existing saved connection. Keep ordinary/public APKs free of bootstrap credentials.
- Add a private build helper that verifies its embedded setup, keeps the personal APK in ignored output, and rebuilds ordinary output without the key.
- The private APK contains an extractable key and must never be publicly distributed; public APKs continue to use personal setup.

## v1.2.4

- Tell recipe generation to choose a coherent subset of available pantry ingredients instead of trying to use everything.
- Explain optional pantry usage in English and Arabic ingredient helper text.
- Keep accepting both subsets and complete pantry usage when appropriate; no new ingredients beyond allowed staples.

## v1.2.3

- Reject pasted multiline ingredient lists before whitespace normalization can hide line breaks.
- Preserve rejected cartridge text and selection so it can be corrected without losing input.
- Add regression coverage for LF, CRLF, CR, English/Arabic separators, and valid spaces/tabs.

## v1.2.2

- Correct cooking timers for fractional minutes: round up the full duration instead of reading only the decimal suffix.
- Support Persian digits and Arabic decimal separators, explicit duration ranges, and safe clamping of long durations.
- Add regression tests for localized durations and embedded-number rejection.

## v1.2.1

- Reject unsupported generated cuisines and repeated recipe titles when requesting another dish.
- Apply exclusion screening to descriptions, taste explanations, and goal explanations.
- Add regression coverage for these shared Android/backend validation rules.

## v1.2.0

- Publish the existing personal Gemini connection and interface improvements.
- Store owner-supplied credentials encrypted with Android Keystore, outside backups.
- Share recipe generation rules between Android and the optional Java backend.
- Correct setup documentation to describe direct HTTPS generation without a laptop.

# Changelog

## v1.1.0 — Two ways to cook

- New cream/green Simple interface with pantry chips, recipe goals, preferences, recipe details, saved archive and guided cooking. Simple is the default for new installations.
- Advanced retains the 3D experience and initializes its renderer/audio only when selected. Both interfaces share all recipe logic and persisted state.
- Independent, persistent interface and language settings, including legacy installation handling.
- Complete English/Arabic native resources, RTL controls, bundled Cairo font, correctly shaped cartridge labels and mixed-script ingredient input.
- Selected-language Gemini content with stable JSON schema and canonical filters, bilingual common-food matching and expanded Arabic exclusion screening. Saved recipe content is preserved.
- Arabic minute timers, responsive phone/wide layouts, accessible headings and reduced-motion support.
- Version name 1.1.0, version code 3. Existing v1.0.0 and v1.0.1 checkpoints remain available.

Android only; recipe generation still requires the local backend. Cooking timers have no background alarm. Ingredient screening remains heuristic.


## v1.0.1 — The Midnight Kitchen

- Original charcoal/olive, tungsten/amber visual system with readable native typography, squared metal controls, scene instrumentation and an unmistakable firing action.
- Phone loadout layout and a separate wide Android layout with ingredient and preference side consoles.
- Visible Pantry/Surprise mode selection and an accessible ingredient editor, Load button, scrollable cartridge tray, selected/empty/full feedback, individual removal and persistent ammunition.
- Warm material lighting, three-quarter framing, worn workbench details, machined weapon details, selective cylinder cutaway and distinct revolver/rifle/shotgun models. Existing direct cylinder drag, orbit, zoom and mechanical loading remain functional.
- Oven visibility remains tied to firing. The cooking wait presentation has themed messages and cancellation while preserving the real request/error handling.
- Full recipe reveal, saved archive with descriptions/metadata/removal, and cooking workspace with collapsible ingredient checklist, numbered steps, completion progress, navigation and contextual in-app minute timers.
- Checklist, step and timer state persist locally; existing saved recipes, preferences, API credentials and favorite-based personalization remain compatible.
- Snack/Dessert meal types supported by both app and backend. New recipes retain their generation goal.
- Reduced-motion and sound controls; system animator-scale support. Idle 3D rendering no longer runs continuously.
- Android version name 1.0.1, version code 2. No new dependencies or production mock recipes.

The Gemini service remains a local development backend; this release does not add cloud hosting, a web/desktop port, food photography, background timer alarms or a production signing key.

## v1.0.0

First preserved source snapshot of the DishRoulette Android application and Gemini backend.
