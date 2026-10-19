# DishRoulette Gemini development service

The Java Android app sends ingredients and meal choice to this local Java service.
The service calls Gemini, validates the structured response, and returns a recipe.
Generated recipes can be saved locally in the app. The API key is never packaged in the APK.

## Start and connect a phone or emulator

From the project directory:

```sh
./backend/start-local.sh
```

This builds and starts the backend as a Linux user service, then runs `adb reverse`
for each connected, authorized Android device. The debug APK uses
`http://127.0.0.1:8080/` through `adb reverse` on both phones and emulators.
This avoids relying on direct emulator access to the computer's network address.
On Linux with systemd, the script also starts `dishroulette-devices`, which restores
port forwarding within a few seconds after an authorized phone or emulator reconnects or reboots.
On systems without that helper, rerun the script after reconnecting. Then run the
`app` configuration in Android Studio (or install the debug APK).

The computer must remain on. This setup does not provide a standalone cloud service.
The server binds only to the computer's loopback interface. The release app does
not enable this development endpoint; public hosting, authentication, and HTTPS
are a separate deployment step.

## Key and model

`backend/local.properties` is ignored by Git and should be readable only by its owner:

```properties
GEMINI_API_KEY=your-key-here
GEMINI_MODEL=gemini-3.5-flash-lite
```

Environment variables with the same names take precedence. Do not paste a real
key into example files, Android resources, Gradle build fields, or source code.
After changing the backend configuration, stop and restart the backend.

Stop with `systemctl --user stop dishroulette-backend`. Start again with the script.
Stop automatic device reconnection with `systemctl --user stop dishroulette-devices`.
The service survives closing the terminal; it is not enabled to start at login.
After a computer reboot, rerun the script. On systems without systemd, the script
prints a command to run in a terminal that must remain open.
Read logs with `journalctl --user -u dishroulette-backend`. Ingredients and keys
are not logged. `/health` confirms that the service is running, not that Google quota
or credentials are valid.

## Behavior and limits

- At most 20 ingredients and 300 ingredient characters per request.
- Uses supplied ingredients plus explicitly listed water, salt, pepper, or cooking oil.
- Google responses are schema constrained and validated again by the backend and app.
  These structural checks cannot guarantee a model's recipe is semantically correct.
- 50-second Google timeout, two simultaneous generations, and 12 requests per minute
  for this local development service. Google account quotas also apply.
- No automatic generation retries, to avoid duplicate usage charges.
- In-flight requests are cancelled when the Activity is destroyed, including rotation;
  the user can try again. Previously displayed recipes survive rotation.
- Existing sample favorites remain accessible; new favorites retain the generated recipe.

## Verify

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :backend:test
```

Backend tests use a mock Google endpoint; real requests require a working key
and available Google quota. The contract is `POST /recipes/suggest` with JSON:

```json
{"ingredients":["eggs","tomato","bread"],"meal":"Breakfast","avoidTitle":""}
```

Reference: https://ai.google.dev/api/generate-content

## Personalization, cuisine, and exclusions

The app's **Taste, goals & filters** dialog stores preferences locally. It sends
up to eight eligible, distinct liked dishes (title and main ingredients) with each
request when **Inspired by my favorites** is enabled. Removing a like removes it
from future taste examples. Exclusions override likes; conflicting saved dishes
cannot be opened for cooking and are excluded from taste examples.

- `mode=pantry` uses the listed pantry ingredients and explicitly listed staples.
- `mode=discover` requires an empty ingredient list and produces a shopping-list recipe.
- `goal` accepts Balanced, Cutting, and Bulking. These are qualitative recipe styles,
  not prescribed calorie targets, nutritional measurements, or personal diet plans.
- `cuisine` chooses from the app's cuisine selector (or Any cuisine).
- `allergies` chooses supported allergen groups; `customExclusions` accepts up to
  ten additional food names. `likedDishes` supplies bounded taste examples.

Shared name screening runs in Android and the backend. It recognizes common
allergen aliases and checks recipe ingredient lines, steps, and garnishes. With
exclusions active, generic prepared foods whose composition cannot be verified
are conservatively rejected. These lexical and model checks are not comprehensive
allergen detection and do not establish safety: labels, derivatives not covered by
the alias list, translations, substitutions, and cross-contact still require review.

Allergy background: https://www.fda.gov/food/buy-store-serve-safe-food/food-allergies-what-you-need-know

Run all tests with `./gradlew :app:testDebugUnitTest :backend:test :recipe-core:test`.

The instrumented `LiveRecipeConnectionTest` checks the Android client through the
local backend and Gemini, including response conversion. It is skipped by default;
enable it explicitly with instrumentation argument `liveRecipe=true` when the
backend is running. Each device makes two real generation requests.
