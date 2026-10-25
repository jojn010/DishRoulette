# DishRoulette

**v1.2.5 — Private preconfigured installation**

A native Android recipe app with a warm, lightweight Simple interface and the immersive 3D Advanced interface. Both share ingredients, preferences, saved recipes, cooking progress and the Gemini connection. Choose English or Arabic independently of the interface.

## Run locally

1. Open this folder in Android Studio and let Gradle sync. Use the project's configured Android SDK/JDK toolchain.
2. Run the **app / debug** configuration in Android Studio, or install the debug APK from the GitHub release.
3. Open **Settings → Gemini connection**, enter your own Gemini API key and model, and connect.

The Android app calls Gemini directly over HTTPS. Once configured, recipe generation does not require a running computer or local backend. The owner's key is encrypted using Android Keystore and stored outside Android backups; it is never packaged in the APK. Gemini usage is charged against the owner's provider account.

The `backend` module remains available as an optional local development service; see `backend/README.md`. It is not used by the current Android client. For USB setup of a debug installation, `tools/provision-personal.py --device SERIAL` imports the owner's existing local configuration without including it in the APK.

## Private preconfigured APK

For the owner only, `python3 tools/build-private-apk.py` builds `work/private/DishRoulette-personal.apk` with the existing local Gemini key. Install this APK to connect automatically on first launch without typing an API key. The phone calls Gemini directly over the internet and needs no laptop connection. Existing saved credentials take precedence.

**This private APK contains an extractable API key. Keep it private; never upload it to GitHub or share it.** The script keeps the APK in ignored local output, removes the temporary setup file, and rebuilds ordinary output without credentials. Public release APKs still require personal connection setup. On the phone, the imported key is saved encrypted with Android Keystore; the bundled key remains extractable from the private APK. Settings can replace the saved connection.

## Interfaces and languages

New installs start in **Simple** mode. Existing installations keep their saved choice or the familiar Advanced interface. Open **Settings** from either interface to select Simple/Advanced and English/العربية independently. Choices persist across restarts.

Pantry ingredients describe availability: a recipe may use one, some, or all when appropriate, without forcing unrelated ingredients together.

Simple mode offers editable ingredient chips, Pantry/Surprise selection, goal cards, shared preferences, full recipe details, saved recipes and guided cooking. It does not instantiate the OpenGL renderer or mechanical audio. Advanced mode retains the 3D weapon, camera and firing experience.

Arabic uses bundled Cairo typography and native Android RTL resources. Only the HUD is mirrored; the 3D scene and gestures retain their orientation. Gemini receives the selected language while schema keys and internal filter values remain stable. Saved recipes retain their original content and language. Common English/Arabic food aliases support pantry matching and exclusion checks; this bounded vocabulary is not a general translation service or an allergy guarantee.

## Advanced controls

- **From my pantry:** type one ingredient and press Next or **+ LOAD**. Tap a cartridge in the readable ingredient tray or the 3D scene to edit it. Remove affects only the selected cartridge; Clear all empties the loadout. Load navigation supports more than six ingredients, up to the service's 20-ingredient limit.
- **Surprise me:** generates a dish and shopping ingredients without sending your pantry. Your pantry loadout is retained when switching back.
- **Balanced / Cut / Bulk:** switches the revolver, precision rifle, and heavy pump-action silhouette, along with the recipe style.
- **Settings → Recipe preferences:** meal, cuisine, allergies, exclusions, favorites inspiration, reduced motion, and mechanical sound. Snack and Dessert are supported end to end.
- **Shoot:** reveals the oven during the firing sequence. A themed waiting scene continues while the real request runs. Cancel returns to the loadout; failures retain ingredients for another attempt.
- **Let's cook:** ingredient checklists, numbered steps, progress, previous/next navigation, and contextual minute timers. Progress survives recreation/reopening. Timers are visual in-app timers with no background alarm.
- **Saved:** full recipe archive with descriptions, timing, cuisine/goal, opening and removal. Existing favorites remain compatible and continue to inspire future requests.

## Presentation and accessibility

Original procedural steel/wood meshes and warm lighting use OpenGL ES 2.0; all recipe and ingredient text uses native accessible controls. Touch dragging, direct cylinder dragging, pinch zoom, mouse orbit/scroll and double-tap camera reset are supported. Reduced motion respects both the app setting and the system animator scale. Sound can be disabled separately. The scene stops continuous rendering when idle.

Phones use a vertical loadout. Narrow/short phones and open keyboards use a compact layout: tap **Manage** to edit/remove loaded ingredients, and use **Setup** for goal selection. Android windows at least 840dp wide use side consoles around the central weapon. This is an Android app, not a separate desktop/web port.

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
