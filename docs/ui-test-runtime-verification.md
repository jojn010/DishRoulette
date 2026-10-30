# Restored Android API 37 interaction tests

The previous scheduled run recorded three UI tests that could not interact with the app because
Espresso 3.5.1 reflectively called the removed `InputManager.getInstance` method. Updated only
instrumentation dependencies to stable Espresso 3.7.0 and AndroidX Test JUnit 1.3.0.
Android's [official release notes](https://developer.android.com/jetpack/androidx/releases/test)
document replacing that reflective call with `getSystemService` in Espresso 3.7.0.

All ten focused tests passed on the same API 37 emulator:

- English/Arabic people-count controls, slider gestures, persistence and recipe request fields.
- English/Arabic recipe copy/import/review, recreation, deduplication and offline cooking.
- Invalid import handling, exclusion blocking and opening the Android share sheet without sending.
- Cooking timers, reduced motion, recreation and missing-instructions fallback.
- Bilingual responsive screens and saved recipe cards.

Android unit, backend and recipe-core tests passed. The test APK built successfully and Android
lint passed. An initial incremental lint run emitted stale quick-fix diagnostics for the replaced
version strings; rerunning lint tasks produced a clean successful task log without those exceptions.
English and Arabic import-review screenshots were visually inspected. Local logs and images stay
under ignored `work/heartbeat-espresso/`.

This changes test infrastructure only. Production app dependencies, signing and the published APK
are unchanged. No new release is needed for this test-runtime correction.
