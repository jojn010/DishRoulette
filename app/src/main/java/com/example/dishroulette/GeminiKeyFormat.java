package com.example.dishroulette;

/**
 * Local input checks; the provider determines whether a key is valid.
 */
final class GeminiKeyFormat {
    private GeminiKeyFormat() {
    }

    static boolean accepts(String key) {
        return key != null && key.length() >= 20 && key.length() <= 256
            && key.matches("[A-Za-z0-9_.-]+");
    }
}
