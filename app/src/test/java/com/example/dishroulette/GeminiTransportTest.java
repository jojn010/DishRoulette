package com.example.dishroulette;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.ResponseBody;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Refactoring transport must preserve byte limits and per-request credential handling.
 */
public class GeminiTransportTest {
    @Test
    public void responseLimitIsMeasuredInBytesBeforeJsonParsing() throws Exception {
        String valid = "a".repeat(100_000);
        try (ResponseBody body = ResponseBody.create(MediaType.get("application/json"), valid)) {
            assertEquals(valid, GeminiTransport.readBody(body));
        }
        for (String invalid : new String[]{valid + "a", "أ".repeat(50_001)}) {
            try (ResponseBody body = ResponseBody.create(MediaType.get("application/json"), invalid)) {
                try {
                    GeminiTransport.readBody(body);
                    fail("Accepted oversized provider response");
                } catch (IOException expected) {
                }
            }
        }
    }

    @Test
    public void credentialsRemainInHeadersAndRedirectsStayDisabled() {
        String secret = "test-key-only-not-a-real-credential";
        okhttp3.Request request =
            GeminiTransport.generateRequest(new GeminiCredentials.Value(secret, "gemini-test"), "{}");
        assertEquals("https", request.url().scheme());
        assertEquals("generativelanguage.googleapis.com", request.url().host());
        assertFalse(request.url().toString().contains(secret));
        assertEquals(secret, request.header("x-goog-api-key"));
        assertFalse(GeminiTransport.client().followRedirects());
        assertFalse(GeminiTransport.client().followSslRedirects());
    }
}
