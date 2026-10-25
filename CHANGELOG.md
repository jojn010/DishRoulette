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
