# Mirrored photo orientation

The scheduled review found that mirrored portrait photos (EXIF orientations 5 and 7) were
transformed in the wrong order. AndroidX ExifInterface defines its rotation relative to an
image that has already been flipped horizontally, but photo preparation rotated first.
See the [official getRotationDegrees contract](https://developer.android.com/reference/androidx/exifinterface/media/ExifInterface#getRotationDegrees()).

Added a synthetic four-color photo test with independently specified corner positions for
all eight EXIF orientations. It failed before the fix at EXIF 5, corner 0. Preparation now
applies the flip before rotation, correcting both the preview and the re-encoded image used
for ingredient recognition. Size sampling and metadata removal remain unchanged.

Validation: all eight orientations pass their dimension and corner-color checks. The photo
suite also covers metadata removal, cache eviction, review editing/recreation, cancellation,
empty/network-result retries and English/Arabic review rendering. Debug build, Android unit,
recipe-core/backend tests, lint, cooking and bilingual responsive-screen checks passed.
No user photos or live Gemini calls were needed. Local logs are under ignored work/heartbeat-exif/.

This focused source fix is queued under Unreleased; the published v1.7.2 APK stays intact.
