## v1.8.0

- Remove the abandoned Advanced interface: renderer, gestures, mechanical audio, hidden views, duplicate tablet XML and no-op adapters. Remove the OpenGL device requirement and the unused ConstraintLayout dependency.
- Remove over 120 unused resources, obsolete sounds/icons/text, redundant legacy colors, pre-API24 launcher raster fallbacks and placeholder tests. Keep the native Google icons, Kenney UI sounds, bilingual recipe library and owner setup tools.
- Give preferences their own meal controls. Apply saves all changes together; Cancel no longer changes the selected meal.
- Preserve the ingredient being edited across rotation so an update cannot become an accidental new ingredient. Ignore late generation callbacks after cancellation or replacement.
- Integrate the preserved local cooking workspace: clear step card, readable progress, circular timer and expandable nutrition below cooking controls. Preserve current language, theme and timer fixes.
- Correct outdated connection guidance and count labels in English/Arabic; reuse drawing objects in the animated timer.
- Replace retired Advanced tests with native cancellation, rotation and preferences regressions. Update bilingual saved fixtures and stabilize import taps after keyboard dismissal.

## v1.7.9

- Add Light, Dark and Use device theme choices in Settings, saved across restarts and applied throughout the app. System mode follows device changes, while explicit Light/Dark choices override them.
- Add a dark aubergine palette with warm apricot controls and high-contrast text. Keep the ivory/apricot Light palette and adapt system bar icons, dialogs, recipe actions, navigation, saved recipes and cooking screens.
- Update Settings descriptions for themes and bilingual recipes. Verify English/Arabic rendering, palette contrast, persisted choices and live system-theme transitions.

## v1.7.8

- Show the official filled Google bookmark icon for a saved recipe and the outline icon when unsaved. Update immediately when saving or removing a favorite and when reopening a recipe.

## v1.7.7

- Make Try another, Edit preferences and Share recipe clear full-width secondary buttons with peach backgrounds, visible borders, rounded corners, readable bold labels, pressed feedback and consistent spacing.

## v1.7.6

- Use the theme’s dark ink color for preferences switch labels, including “Inspired by my favorites”, so they remain readable on the peach dialog in English and Arabic.

## v1.7.5

- Center each bottom navigation label directly beneath its own Google icon, including after repeated Arabic/English switches. Use separate centered icon and label views instead of MaterialButton compound-drawable positioning.
- Preserve the selected Home appearance, full-size accessible button targets, RTL order and compact/large-text stacked navigation. Add a geometric regression check for all three icon/label centers through repeated language switches.

## v1.7.4

- Generate and store full English and Arabic recipe text together. Imported recipes, saved cards, recipe overviews and guided cooking use the selected app language while keeping servings, nutrition, IDs and cooking progress.
- Share both translations in one offline code with a canonical identity independent of the sender’s selected language. Validate both languages, matching step/ingredient counts and exclusions; keep personal explanations and credentials outside shared codes.
- Add reviewed Arabic text for all eight bundled recipes. Upgrade historical single-language recipes through a one-time text-only Gemini translation, preserving the original and offering a clear retry message when unavailable.

## v1.7.3

- Center Home headings, navigation icon/label groups, recipe titles and screen headings; retain natural reading alignment for cooking instructions and ingredient entry in English and Arabic.
- Replace in-app symbols with official Google Material Icons Round vectors and add downloaded Kenney CC0 button, completion and foreground timer sounds. Sound effects can be disabled in Settings and respect silent/vibrate mode and system volume. Asset sources and licenses are included in third_party.

- Correct mirrored photo orientation for EXIF transpose/transverse images by applying the horizontal flip before rotation. Ingredient previews and the JPEG sent to Gemini now match the camera orientation; all eight EXIF orientations have pixel-position regression coverage.

## v1.7.2

- Recover the photo chooser when Android evicts a cached photo preview before screen recreation. A missing image no longer leaves Scan ingredients unresponsive; outstanding camera and picker results are retained.
- Upgrade the Android UI test framework to Espresso 3.7.0 and AndroidX Test JUnit 1.3.0, restoring API 37 interaction tests for English/Arabic recipe sharing and the people slider. All ten focused emulator tests now pass.
- Prevent a cooking-screen crash when a saved/shared recipe has blank instructions or only an English/Arabic step number. Show a localized missing-instructions message and keep Back available without starting timers or keeping the screen awake.

## v1.7.1

- Adapt Home to the measured window and text size: keep tablet pantry/preferences columns, stack ingredient entry/navigation/branding when narrow, and let header/actions scroll in short windows and with the keyboard.
- Collapse introductory artwork in short windows to prioritize ingredient entry. Keep the complete ingredient field visible while typing; allow native input/action heights to grow with text.
- Center and bound recipe, Saved, Settings and cooking page widths inside actual available space. Size the cooking dial from its parent and use RTL-aware cooking navigation spacing.
- Let import and photo-progress dialogs scroll, with window-filling dialogs and a compact code field when space or large text requires them. Explicitly support resizable/multiwindow activity layouts. Preserve recipes, sharing codes, servings, timers and personal connection/data.

## v1.7.0

- Add offline recipe sharing: copy a self-contained Tabkha code or open Android’s share sheet from results and saved cards. Include the full dish, quantities, serving count, original language and available nutrition, without credentials or personal preference/taste data.
- Add Saved → Import recipe with paste, review and explicit save, rotation-safe import state, duplicate detection, shared-recipe labelling and existing exclusion checks. Reject damaged, oversized and unsupported codes; no database or Gemini connection is needed for import.
- Replace the people counter with a discrete 1–12 slider flanked by − and + buttons. Keep native accessibility/RTL behavior, the persistent count and scaled generation requests.

## v1.6.0

- Add an English/Arabic people selector to Home recipe preferences, from 1 to 12 including yourself, with accessible increase/decrease buttons and a saved count across restarts.
- Send the selected count in pantry and Surprise requests; ask Gemini to scale all ingredient quantities and adjust batch instructions, cookware and timing where needed. Keep nutrition per serving.
- Constrain generated servings in the response schema and reject mismatched yields. Preserve existing saved recipe quantities, dietary preferences and encrypted personal connections.

## v1.5.5

- Make Saved easier to browse with distinct apricot recipe cards, bolder titles and ivory time/serving summaries.
- Keep Open recipe prominent and use quieter, accessible nutrition and removal actions. Each action names its recipe for screen readers.
- Expand nutrition on demand while preserving all available estimates, the `~~~` approximation explanation and historical unavailable values. Recipe details and cooking still show their full metadata.
- Preserve bilingual RTL layouts, saved recipe data and encrypted personal connections; no new animation or sound.

## v1.5.4

- Give Home ingredient entry a cohesive apricot workspace with a stronger count heading, ivory input/chips and an integrated photo-scanning action.
- Simplify the bottom navigation with a selected Home pill and quieter Saved/Settings actions, keeping the aubergine recipe action prominent.
- Let the recipe action and navigation grow with text size; improve Arabic navigation spacing at larger fonts. Hide edit/remove guidance and Clear all when the pantry is empty.
- Preserve English/Arabic RTL, ingredient editing, photo review, saved recipes, cooking timers and reduced-motion behavior.

## v1.5.3

- Fix hour-based cooking instructions: “1 hour 30 minutes” now starts a 90-minute timer instead of 30 minutes, and hour-only instructions now offer a timer.
- Recognize English hour/hr forms and Arabic ساعة / ساعتين / ساعتان / ساعات, including Arabic/Persian digits, fractions and ranges. Combine only adjacent hour/minute durations; later separate instructions retain their own meaning.
- Preserve timer pause/reset state, minute rounding, the existing four-hour limit, bilingual accessibility labels and reduced-motion behavior.

## v1.5.2

- Remove the Gemini connection entry from Settings completely.
- Update English and Arabic Settings descriptions to show the available language and recipe-preference controls.
- Preserve existing encrypted connections and private USB provisioning.

## v1.5.1

- Shorten the Arabic pantry selector to «من مكوناتي» to avoid an awkward two-line label on phones.
- Match the pot-and-spoon logo with ivory backgrounds, apricot surfaces and aubergine text, buttons and countdown rings. Photo captions now follow the theme.

- Smooth the circular cooking timer ring between second ticks while running. Pausing and resetting stop the sweep immediately. Respect reduced-motion settings and the Android animation scale; cancel animation when leaving the timer.

- Recognize Arabic dual minute forms (دقيقتين / دقيقتان) in cooking steps, so instructions such as "اترك الخليط لمدة دقيقتين" offer the correct two-minute timer without needing a separate numeral. Preserve the first duration when a step contains multiple durations.
- Verified with a regression that failed before the fix, the full unit/build/lint checks, and the API 37 cooking-screen test including starting the Arabic two-minute countdown.

## v1.5.0

- Remove the Connect Gemini call to action from Home.
- Add a Windows owner-build script that reads ignored local configuration and builds an automatically connected personal APK.
- Restore credential-free public build output after the private build; private APKs and API keys remain local.
- Make the existing Python build/provisioning tools select Windows Gradle and Android SDK executables correctly.
- Keep encrypted connections across restarts and compatible updates, with Settings available for key replacement and recovery.

## v1.4.0

- Add Scan ingredients: take a photo or choose one from the system picker, preview it, and ask Gemini to identify visible foods.
- Review and edit detected ingredients in English or Arabic before adding them to the pantry and generating a dish.
- Preserve existing pantry ingredients, skip duplicates, and retain recipe preferences and exclusion validation.
- Resize and orient images locally, strip original metadata, and delete temporary images after completion or cancellation.
- Handle unreadable photos, empty results, network/model/quota errors, cancellation and screen recreation without automatically repeating paid requests.
- This workstation uses a different debug signing key from earlier builds. Existing installations with a different signature require a backed-up reinstall and Gemini reconnection.

## v1.3.1

- Rename the app to Tabkha in English and طبخة in Arabic, including the home screen and launcher.
- Add the standalone cooking pot and spoon app icon.
- Show timed cooking steps with a large circular countdown dial, clock marks and a shrinking remaining-time ring.
- Preserve timer pause, resume, reset, completion and saved state.

## v1.3.0

- Remove Advanced mode and the interface selector from Settings.
- Always open the native Simple interface, including after upgrading from Advanced.
- Preserve ingredients, saved recipes, preferences, cooking progress, Gemini connection and English/Arabic support.

## v1.2.11

- Keep the screen awake while cooking instructions are open so you can follow a recipe hands-free.
- Restore normal screen timeout when leaving cooking; backgrounding the app still allows the screen to sleep.

## v1.2.10

- Give calories and protein separate nutrition rows in English and Arabic.
- Keep each protein estimate and its grams unit together when text wraps; retain `~~~` and the ingredient-variation explanation.

## v1.2.9

- Split Arabic and Persian numbered cooking instructions into individual steps, including indented lines and Windows line endings.
- Preserve wrapped instruction text and decimal ingredient quantities; English numbered instructions continue to work.

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
