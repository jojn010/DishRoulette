package com.example.dishroulette;

import android.content.Context;
import com.example.dishroulette.core.PhotoIngredientRules;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.*;

/** One cancellable HTTPS request using the existing personal connection. Never logs images or keys. */
final class PhotoIngredientApi {
    interface Callback { void success(List<String> ingredients); void failure(int message); }
    static Call analyze(Context context, String image, Callback callback) {
        GeminiCredentials.Value credentials = new GeminiCredentials(context).read();
        if (credentials == null) { callback.failure(R.string.photo_connect_first); return null; }
        OkHttpClient client = new OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS).callTimeout(65, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false).build();
        Request request = new Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/" + credentials.model + ":generateContent")
                .header("x-goog-api-key", credentials.key)
                .post(RequestBody.create(MediaType.get("application/json; charset=utf-8"),
                        PhotoIngredientRules.payload(image, InterfaceSettings.language(context)).toString())).build();
        Call call = client.newCall(request);
        call.enqueue(new okhttp3.Callback() {
            public void onFailure(Call c, IOException e) { if (!c.isCanceled()) callback.failure(R.string.photo_network_error); }
            public void onResponse(Call c, Response response) {
                try (Response close = response) {
                    if (c.isCanceled()) return;
                    if (response.code() == 429) { callback.failure(R.string.photo_rate_limit); return; }
                    if (response.code() == 400 || response.code() == 401 || response.code() == 403 || response.code() == 404) {
                        callback.failure(R.string.photo_model_error); return;
                    }
                    if (!response.isSuccessful() || response.body() == null) { callback.failure(R.string.photo_network_error); return; }
                    okio.BufferedSource source = response.body().source(); source.request(100_001);
                    if (source.getBuffer().size() > 100_000) throw new IOException();
                    List<String> result = PhotoIngredientRules.parseResponse(source.readUtf8());
                    if (!c.isCanceled()) callback.success(result);
                } catch (Exception e) { if (!c.isCanceled()) callback.failure(R.string.photo_response_error); }
            }
        });
        return call;
    }
}
