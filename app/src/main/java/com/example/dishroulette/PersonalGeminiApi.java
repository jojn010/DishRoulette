package com.example.dishroulette;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.dishroulette.core.RecipeRules;
import com.google.gson.*;

import java.io.IOException;

import okhttp3.*;
import okio.Timeout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * HTTPS directly from the owner's phone to Gemini; no laptop, proxy, or credential logging.
 */
final class PersonalGeminiApi implements RecipeApi {
    private static final Gson GSON = new Gson();
    private final GeminiCredentials credentials;
    private final OkHttpClient client;

    PersonalGeminiApi(Context c) {
        this(c, GeminiTransport.client());
    }

    PersonalGeminiApi(Context c, OkHttpClient client) {
        credentials = new GeminiCredentials(c.getApplicationContext());
        this.client = client;
    }

    public Call<RecipeSuggestion> suggest(RecipeApi.Request request) {
        return new Suggestion(request);
    }

    private final class Suggestion implements Call<RecipeSuggestion> {
        private final RecipeApi.Request input;
        private okhttp3.Call upstream;
        private boolean executed;
        private volatile boolean canceled;
        private final Handler main = new Handler(Looper.getMainLooper());

        Suggestion(RecipeApi.Request input) {
            this.input = input;
        }

        private void error(Callback<RecipeSuggestion> cb, int status, String code) {
            JsonObject error = new JsonObject();
            error.addProperty("code", code);
            Response<RecipeSuggestion> result =
                Response.error(status, ResponseBody.create(MediaType.get("application/json"), error.toString()));
            main.post(() -> {
                if (!canceled)
                    cb.onResponse(this, result);
            });
        }

        public synchronized void enqueue(Callback<RecipeSuggestion> cb) {
            if (executed)
                throw new IllegalStateException("Already executed");
            executed = true;
            if (canceled)
                return;
            GeminiCredentials.Value value = credentials.read();
            if (value == null) {
                error(cb, 503, "KEY_NOT_CONFIGURED");
                return;
            }
            final JsonObject request;
            try {
                request = RecipeRules.validateRequest(GSON.toJsonTree(input).getAsJsonObject());
                okhttp3.Request http = GeminiTransport.generateRequest(value, RecipeRules.payload(request).toString());
                upstream = client.newCall(http);
            } catch (RecipeRules.ApiFailure e) {
                error(cb, e.status, e.code);
                return;
            }
            upstream.enqueue(new okhttp3.Callback() {
                public void onFailure(okhttp3.Call call, IOException e) {
                    main.post(() -> {
                        if (!canceled)
                            cb.onFailure(Suggestion.this, new IOException("Gemini connection failed"));
                    });
                }

                public void onResponse(okhttp3.Call call, okhttp3.Response response) {
                    handleProviderResponse(response, request, cb);
                }
            });
        }

        private void handleProviderResponse(okhttp3.Response response, JsonObject request,
                                            Callback<RecipeSuggestion> cb) {
            try (okhttp3.Response close = response) {
                int status = response.code();
                if (status == 429) {
                    error(cb, 429, "RATE_LIMITED");
                    return;
                }
                if (GeminiTransport.configurationError(status)) {
                    error(cb, 503, "PROVIDER_CONFIGURATION");
                    return;
                }
                if (status != 200) {
                    error(cb, 502, "PROVIDER_UNAVAILABLE");
                    return;
                }
                if (response.body() == null) {
                    error(cb, 502, "INVALID_RECIPE");
                    return;
                }
                JsonObject result = RecipeRules.parseResponse(GeminiTransport.readBody(response.body()), request);
                RecipeSuggestion recipe = GSON.fromJson(result, RecipeSuggestion.class);
                main.post(() -> {
                    if (!canceled)
                        cb.onResponse(Suggestion.this, Response.success(recipe));
                });
            } catch (RecipeRules.ApiFailure e) {
                error(cb, e.status, e.code);
            } catch (Exception e) {
                error(cb, 502, "INVALID_RECIPE");
            }
        }

        public Response<RecipeSuggestion> execute() {
            throw new UnsupportedOperationException("Use enqueue");
        }

        public synchronized void cancel() {
            canceled = true;
            if (upstream != null)
                upstream.cancel();
        }

        public boolean isCanceled() {
            return canceled;
        }

        public synchronized boolean isExecuted() {
            return executed;
        }

        public Call<RecipeSuggestion> clone() {
            return new Suggestion(input);
        }

        public okhttp3.Request request() {
            return new okhttp3.Request.Builder().url(GeminiTransport.BASE_URL).build();
        }

        public Timeout timeout() {
            return Timeout.NONE;
        }
    }
}
