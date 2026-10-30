# Missing cooking instructions

After safely fast-forwarding to v1.7.1, the scheduled review reproduced a crash opening a
recipe whose method is empty or consists only of a step marker such as `1.` or `١.`.
The step parser correctly removes the marker, leaving zero instructions, but the cooking
screen previously indexed step zero. The regression failed with IndexOutOfBoundsException
before the fix.

The screen now shows a missing-instructions message in English or Arabic with its existing
Back control. It does not display fabricated instructions, start a timer, or keep the screen
awake. The recipe remains available to inspect or remove from the archive.

Validation uses the empty/English-marker/Arabic-marker emulator regression, existing cooking
timer coverage, bilingual responsive layout checks and saved-card regressions,
plus debug build, unit tests, backend/recipe-core tests and Android lint. Three additional
sharing/people UI tests could not execute their interactions: Espresso 3.5.1 calls the removed
InputManager.getInstance method on API 37. This is a test-runtime limitation, not a passing
result for those flows. Local test logs and
rendered screens stay under ignored work/heartbeat-2026-10-30. Recorded under Unreleased;
the published v1.7.1 APK is unchanged.

Follow-up: the API 37 test-runtime limitation was resolved by upgrading Espresso and AndroidX
Test JUnit. All ten tests, including the three previously blocked interactions, now pass.
See [UI test runtime verification](ui-test-runtime-verification.md).
