package com.example.dishroulette;

import org.junit.Test;

import static org.junit.Assert.*;

public class GeminiKeyFormatTest {
    @Test
    public void acceptsLegacyAndDottedProviderKeys() {
        assertTrue(GeminiKeyFormat.accepts("example_legacy-key_123456789"));
        assertTrue(GeminiKeyFormat.accepts("example.prefix_token-part_123456789"));
        assertTrue(GeminiKeyFormat.accepts("a".repeat(20)));
        assertTrue(GeminiKeyFormat.accepts("a".repeat(256)));
    }

    @Test
    public void rejectsInvalidLengthsAndHeaderCharacters() {
        assertFalse(GeminiKeyFormat.accepts(null));
        assertFalse(GeminiKeyFormat.accepts("a".repeat(19)));
        assertFalse(GeminiKeyFormat.accepts("a".repeat(257)));
        for (String suffix : new String[]{"\r", "\n", " ", "\t", "é", "\""}) {
            assertFalse(GeminiKeyFormat.accepts("example_key_1234567890" + suffix));
        }
    }
}
