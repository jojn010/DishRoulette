# DishRoulette

**v1.3.0 — Simple interface only**

A native Android recipe app with a warm, lightweight interface, ingredient entry, recipe preferences, saved recipes, guided cooking and a personal Gemini connection. Choose English or Arabic in Settings.

## Run locally

1. Open this folder in Android Studio and let Gradle sync. Use the project's configured Android SDK/JDK toolchain.
2. Run the **app / debug** configuration in Android Studio, or install the debug APK from the GitHub release.
3. Open **Settings → Gemini connection**, enter your own Gemini API key and model, and connect.

The Android app calls Gemini directly over HTTPS. Once configured, recipe generation does not require a running computer or local backend. The owner's key is encrypted using Android Keystore and stored outside Android backups; it is never packaged in the APK. Gemini usage is charged against the owner's provider account.

The `backend` module remains available as an optional local development service; see `backend/README.md`. It is not used by the current Android client. For USB setup of a debug installation, `tools/provision-personal.py --device SERIAL` imports the owner's existing local configuration without including it in the APK.

New generated recipes include estimated calories (kcal), protein, carbs, fat, fiber and sugar (grams), and sodium (milligrams) **per serving**, shown in recipe details, saved recipe listings and cooking views. Every estimate is marked `~~~`. Actual amounts vary with ingredient size, brand, portions and preparation; these are model estimates, not measured nutrition. Older recipes with only calories/protein keep those estimates without invented additional values. Older saved recipes remain available and show that nutrition is unavailable.

## Private preconfigured APK

For the owner only, `python3 tools/build-private-apk.py` builds `work/private/DishRoulette-personal.apk` with the existing local Gemini key. Install this APK to connect automatically on first launch without typing an API key. The phone calls Gemini directly over the internet and needs no laptop connection. Existing saved credentials take precedence.

**This private APK contains an extractable API key. Keep it private; never upload it to GitHub or share it.** The script keeps the APK in ignored local output, removes the temporary setup file, and rebuilds ordinary output without credentials. Public release APKs still require personal connection setup. On the phone, the imported key is saved encrypted with Android Keystore; the bundled key remains extractable from the private APK. Settings can replace the saved connection.

## Interface and languages

All installations now use the Simple interface. Advanced mode and the interface selector have been removed. Upgrading migrates the old interface choice while preserving ingredients, recipe preferences, saved recipes, cooking progress and Gemini settings.

Add or edit ingredient chips, choose Pantry or Surprise, select a goal and generate a recipe. Open Settings for language, Gemini connection and recipe preferences. Recipe details support saving and guided cooking with checklists, progress and contextual timers. Timers are visual in-app timers with no background alarm.

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
