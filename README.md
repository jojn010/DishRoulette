# DishRoulette

**v1.0.1 — The Midnight Kitchen**

A native Android cooking roulette: load ingredients into an interactive 3D weapon, choose your preferences, and fire to discover a Gemini-generated recipe.

## Run locally

1. Open this folder in Android Studio and let Gradle sync. Use the project's configured Android SDK/JDK toolchain.
2. Keep your existing Gemini key in `backend/local.properties` (see `backend/local.properties.example`). This local file is excluded from Git and APKs.
3. Run `./backend/start-local.sh`. It starts the local recipe service and forwards port 8080 to connected emulators/USB phones.
4. Run the **app / debug** configuration in Android Studio, or install the debug APK from the private GitHub release.

The computer and backend must remain running. The release build deliberately has no public recipe endpoint; a standalone cloud deployment is separate work.

## Using the app

- **From my pantry:** type one ingredient and press Next or **+ LOAD**. Tap a cartridge in the readable ingredient tray or the 3D scene to edit it. Remove affects only the selected cartridge; Clear all empties the loadout. Load navigation supports more than six ingredients, up to the service's 20-ingredient limit.
- **Surprise me:** generates a dish and shopping ingredients without sending your pantry. Your pantry loadout is retained when switching back.
- **Balanced / Cut / Bulk:** switches the revolver, precision rifle, and heavy pump-action silhouette, along with the recipe style.
- **Setup:** meal, cuisine, allergies, exclusions, favorites inspiration, reduced motion, and mechanical sound. Snack and Dessert are supported end to end.
- **Shoot:** reveals the oven during the firing sequence. A themed waiting scene continues while the real request runs. Cancel returns to the loadout; failures retain ingredients for another attempt.
- **Let's cook:** ingredient checklists, numbered steps, progress, previous/next navigation, and contextual minute timers. Progress survives recreation/reopening. Timers are visual in-app timers with no background alarm.
- **Saved:** full recipe archive with descriptions, timing, cuisine/goal, opening and removal. Existing favorites remain compatible and continue to inspire future requests.

## Presentation and accessibility

Original procedural steel/wood meshes and warm lighting use OpenGL ES 2.0; all recipe and ingredient text uses native accessible controls. Touch dragging, direct cylinder dragging, pinch zoom, mouse orbit/scroll and double-tap camera reset are supported. Reduced motion respects both the app setting and the system animator scale. Sound can be disabled separately. The scene stops continuous rendering when idle.

Phones use a vertical loadout. Narrow/short phones and open keyboards use a compact layout: tap **Manage** to edit/remove loaded ingredients, and use **Setup** for goal selection. Android windows at least 840dp wide use side consoles around the central weapon. This is an Android app, not a separate desktop/web port.

No fabricated dish images, image service, or production mock recipes are included. Gemini calls still go through the existing backend and shared exclusion validation; ingredient screening cannot guarantee allergy safety.

## Verification

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :backend:test :recipe-core:test
./gradlew :app:connectedDebugAndroidTest
```

Instrumentation tests use deterministic replies only inside the test APK. Live Gemini tests are opt-in and may use API quota:

```sh
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.liveRecipe=true
```

For emulator graphics compatibility, software ANGLE is available with `-gpu swangle`. Verification artifacts and emulator logs are local, ignored files under `work/v1.0.1/`.

## Version checkpoints

- **v1.0.0** preserves the original application.
- **v1.0.1** preserves this visual overhaul and cooking interface.

Keep developing on `main`. To inspect an older version without discarding current work, first commit or stash pending changes, then create a separate branch:

```sh
git switch -c revisit-v1 v1.0.0
```

Switch back with `git switch main`. Tags and private GitHub releases retain both snapshots.

When installing an older **debug** APK over a newer one, Android may require `adb install -r -d path/to/older.apk`. Keep `-r` to retain app data; avoid uninstalling to downgrade.
