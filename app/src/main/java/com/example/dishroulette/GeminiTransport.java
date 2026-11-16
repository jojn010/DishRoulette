package com.example.dishroulette;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

/**
 * Shared connections and response bounds for recipe, translation and photo requests.
 */
final class GeminiTransport {
    static final String BASE_URL = "https://generativelanguage.googleapis.com/";
    private static final int MAX_RESPONSE_BYTES = 100_000;
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(65, TimeUnit.SECONDS).retryOnConnectionFailure(false)
        .followRedirects(false).followSslRedirects(false).build();

    private GeminiTransport() {
    }

    static OkHttpClient client() {
        return CLIENT;
    }

    static Request generateRequest(GeminiCredentials.Value credentials, String payload) {
        return new Request.Builder().url(BASE_URL + "v1beta/models/" + credentials.model + ":generateContent")
            .header("x-goog-api-key", credentials.key)
            .post(RequestBody.create(MediaType.get("application/json; charset=utf-8"), payload)).build();
    }

    static boolean configurationError(int status) {
        return status == 400 || status == 401 || status == 403 || status == 404;
    }

    /**
     * Bound bytes before UTF-8 decoding or JSON parsing. Callers close the enclosing response.
     */
    static String readBody(ResponseBody body) throws IOException {
        if (body == null)
            throw new IOException("Missing provider response");
        okio.BufferedSource source = body.source();
        source.request(MAX_RESPONSE_BYTES + 1L);
        if (source.getBuffer().size() > MAX_RESPONSE_BYTES)
            throw new IOException("Provider response too large");
        return source.readUtf8();
    }
}
