# Tabkha / طبخة

**v1.7.0 — Share a dish, slide to your group size**

A native Android recipe app with a warm, lightweight interface, ingredient entry, recipe preferences, saved recipes, guided cooking and a personal Gemini connection. Choose English or Arabic in Settings.

## Run locally

1. Open this folder in Android Studio and let Gradle sync. Use the project's configured Android SDK/JDK toolchain.
2. Run the **app / debug** configuration in Android Studio, or install the debug APK from the GitHub release.
3. Configure the owner connection using USB provisioning (`tools/provision-personal.py --device SERIAL`) or a private owner build. Settings contains language and recipe preferences; the Gemini connection entry has been removed.

The Android app calls Gemini directly over HTTPS. Once configured, recipe generation does not require a running computer or local backend. The owner's key is encrypted using Android Keystore and stored outside Android backups; it is never packaged in the APK. Gemini usage is charged against the owner's provider account.

The `backend` module remains available as an optional local development service; see `backend/README.md`. It is not used by the current Android client. For USB setup of a debug installation, `tools/provision-personal.py --device SERIAL` imports the owner's existing local configuration without including it in the APK.

New generated recipes include estimated calories (kcal), protein, carbs, fat, fiber and sugar (grams), and sodium (milligrams) **per serving**, shown in recipe details, saved recipe listings and cooking views. Every estimate is marked `~~~`. Actual amounts vary with ingredient size, brand, portions and preparation; these are model estimates, not measured nutrition. Older recipes with only calories/protein keep those estimates without invented additional values. Older saved recipes remain available and show that nutrition is unavailable.

## Private preconfigured APK

On Windows, run `tools/build-private-apk.ps1` with `JAVA_HOME` and `ANDROID_HOME` configured.
It reads the owner's ignored `backend/local.properties` (or Gemini environment variables), builds
`work/private/Tabkha-personal.apk`, then restores credential-free public build output. The private
APK imports the connection on first launch and keeps it encrypted across restarts and compatible
updates. Home no longer displays a Connect Gemini button. Settings now contains only language and recipe preferences; USB provisioning provides connection management
for recovery or replacing a revoked key. A key cannot remain usable if the provider revokes it.
Only the credential-free APK may be uploaded to GitHub; the auto-connected APK stays local.

For the owner only, `python3 tools/build-private-apk.py` builds `work/private/DishRoulette-personal.apk` with the existing local Gemini key. Install this APK to connect automatically on first launch without typing an API key. The phone calls Gemini directly over the internet and needs no laptop connection. Existing saved credentials take precedence.

**This private APK contains an extractable API key. Keep it private; never upload it to GitHub or share it.** The script keeps the APK in ignored local output, removes the temporary setup file, and rebuilds ordinary output without credentials. Public release APKs still require personal connection setup. On the phone, the imported key is saved encrypted with Android Keystore; the bundled key remains extractable from the private APK. USB provisioning can replace the saved connection.

## Interface and languages

### Share and import recipes

Tap **Share recipe** on a recipe result or saved card, then **Copy recipe code** or use the Android share sheet. Your friend opens **Saved → Import recipe**, pastes the complete `TABKHA1:` code, reviews it, and saves it. Both people need Tabkha v1.7.0 or later. Importing works offline without Gemini, a database, an account or a hosted service.

The self-contained code includes the dish title, description, original language, ingredients, method, time, servings, cuisine and available per-serving nutrition. It excludes Gemini credentials, local recipe IDs, pantry contents, allergies, preferences and personal taste explanations. Because it contains the recipe itself, it is longer than a short database ID. Imported recipes keep the sender’s quantities and original language; your Home people count stays unchanged. Re-importing the same snapshot does not create another copy or overwrite an unrelated local recipe.

Imports are labelled as shared recipes and reviewed before saving; the recipient’s exclusions still apply. Codes represent recipe content, not verified authorship. Malformed, damaged, oversized or unsupported codes are rejected. `TABKHA1:` uses a versioned, compressed JSON snapshot with bounded decoding and an explicit field allowlist; there are no web requests during import.

### People and portions

On Home, under **Recipe preferences**, drag the **How many are eating?** slider or use its − / + buttons to choose 1–12 people, including yourself. The app remembers your choice (default: 2), and both pantry and Surprise recipes request that many servings with ingredient quantities scaled for the group. Nutrition remains an estimate per serving. A provider response with a different yield is rejected. Existing saved recipes keep their original quantities and serving count; generate a new recipe to use a different group size.

### Photo ingredients

Tap **Scan ingredients** on Home, then take a photo or choose one from the system photo picker.
Review the preview and tap **Identify ingredients** to send it to your configured Gemini model.
The model must support image input. Edit the suggested list (one ingredient per line), remove
incorrect items, and tap **Add to pantry**. Existing ingredients are retained and duplicates are
skipped. Tap **Find my recipe** to generate a dish using your usual preferences and exclusions.

Images are sampled to at most 1600 pixels on the longest edge, rotated to match camera orientation,
and re-encoded as JPEG without original metadata before transmission. Temporary photos stay in
the app cache and are deleted when the flow is completed or cancelled. No camera or broad storage
permission is required: capture is delegated to the camera app and selection uses the system picker.
Only the selected photo is sent, after tapping **Identify ingredients**; photos are not attached to
the later recipe request. Recognition can miss or misidentify ingredients, especially hidden ingredients
in prepared foods. Review suggestions before generating a dish. Empty results and connection errors
allow retry; rotating during analysis returns to the preview without automatically repeating the request.

### App interface

All installations now use the Simple interface. Advanced mode and the interface selector have been removed. Upgrading migrates the old interface choice while preserving ingredients, recipe preferences, saved recipes, cooking progress and Gemini settings.

Saved recipes use apricot cards with bold titles and ivory time/serving summaries. Open recipe stays prominent; expand Nutrition estimates to read all available values and their approximation note. Screen-reader action labels include the recipe title.

Home groups ingredient entry and photo scanning in an apricot workspace with ivory controls. The bottom navigation marks Home with a selected pill and allows larger text to grow. Empty pantries hide edit/remove guidance and Clear all.

Add or edit ingredient chips, choose Pantry or Surprise, select a goal and generate a recipe. Open Settings for language and recipe preferences. Recipe details support saving and guided cooking with checklists, progress and contextual timers. Timers recognize English and Arabic hours, minutes and adjacent combinations (such as “1 hour 30 minutes” or “ساعة و٣٠ دقيقة”), including numeric ranges and fractions. Durations round up to whole minutes and retain the existing four-hour limit. Timers are visual in-app timers with no background alarm.

English and Arabic are supported, including bundled Cairo typography and right-to-left layout. Gemini receives the selected language. Saved recipes retain their original content and language.

The app uses native Android controls and does not initialize the legacy OpenGL scene or mechanical audio.

No fabricated dish images, image service, or production mock recipes are included. Gemini calls use the personal HTTPS connection and shared exclusion validation; ingredient screening cannot guarantee allergy safety.

## Verification

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :backend:test :recipe-core:test
./gradlew :app:connectedDebugAndroidTest
```

Instrumentation tests use deterministic replies only inside the test APK. Live Gemini tests are opt-in and may use API quota:

```sh
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.liveRecipe=true
```

For emulator graphics compatibility, software ANGLE is available with `-gpu swangle`. Verification artifacts and emulator logs are local, ignored files under `work/v1.1.0/`.

## Version checkpoints

- **v1.4.0** adds camera/gallery ingredient recognition with editable English/Arabic results.
- **v1.3.0** removes Advanced mode and migrates all users to Simple.
- **v1.0.0** preserves the original application.
- **v1.0.1** preserves the Midnight Kitchen visual overhaul and cooking interface.
- **v1.1.0** adds Simple/Advanced interfaces and English/Arabic support.

Keep developing on `main`. To inspect an older version without discarding current work, first commit or stash pending changes, then create a separate branch:

```sh
git switch -c revisit-v1 v1.0.0
```

Switch back with `git switch main`. Tags and private GitHub releases retain all snapshots.

When installing an older **debug** APK over a newer one, Android may require `adb install -r -d path/to/older.apk`. Keep `-r` to retain app data; avoid uninstalling to downgrade.

Arabic font: Cairo by Mohamed Gaber and contributors, SIL Open Font License 1.1; bundled license in `app/src/main/assets/licenses/Cairo-OFL.txt`. This release targets Android phones, tablets and resizable Android windows; it does not create an iPhone or browser application.
