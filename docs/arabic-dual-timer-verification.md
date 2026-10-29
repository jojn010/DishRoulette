# Arabic dual minute cooking timers

The two-hour review reproduced a missed timer: `اترك الخليط لمدة دقيقتين ثم قلّب.`
returned no suggested timer because the duration parser required a separate number. Arabic
dual nouns already encode the value two. Both `دقيقتين` and `دقيقتان` now produce a two-minute
timer, while numeric durations, fractions, ranges, whole-word matching and first-duration
selection continue to use the existing behavior.

Validation:

- Added unit regression failed before the implementation and passed afterward.
- Debug build, all Android unit tests, recipe-core/backend tests and Android lint passed.
- API 37 emulator cooking-screen regression passed: Arabic step displays the timer and
  starting it saves a deadline approximately 120 seconds in the future. Existing pause,
  resume, reset, reopening, completion and untimed-step assertions also passed.

No UI layout, credentials or connection settings were changed. This focused source fix is
recorded under Unreleased for the next coherent APK release; the v1.5.0 release stays intact.
