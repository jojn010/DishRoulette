package com.example.dishroulette;

import android.content.Context;

import com.example.dishroulette.core.PhotoIngredientRules;

import java.io.IOException;
import java.util.List;

import okhttp3.*;

/**
 * One cancellable HTTPS request using the existing personal connection. Never logs images or keys.
 */
final class PhotoIngredientApi {
    interface Callback {
        void success(List<String> ingredients);

        void failure(int message);
    }

    static Call analyze(Context context, String image, Callback callback) {
        GeminiCredentials.Value credentials = new GeminiCredentials(context).read();
        if (credentials == null) {
            callback.failure(R.string.photo_connect_first);
            return null;
        }
        OkHttpClient client = GeminiTransport.client();
        Request request = GeminiTransport.generateRequest(credentials,
            PhotoIngredientRules.payload(image, InterfaceSettings.language(context)).toString());
        Call call = client.newCall(request);
        call.enqueue(new okhttp3.Callback() {
            public void onFailure(Call c, IOException e) {
                if (!c.isCanceled())
                    callback.failure(R.string.photo_network_error);
            }

            public void onResponse(Call c, Response response) {
                try (Response close = response) {
                    if (c.isCanceled())
                        return;
                    if (response.code() == 429) {
                        callback.failure(R.string.photo_rate_limit);
                        return;
                    }
                    if (GeminiTransport.configurationError(response.code())) {
                        callback.failure(R.string.photo_model_error);
                        return;
                    }
                    if (!response.isSuccessful() || response.body() == null) {
                        callback.failure(R.string.photo_network_error);
                        return;
                    }
                    List<String> result = PhotoIngredientRules.parseResponse(GeminiTransport.readBody(response.body()));
                    if (!c.isCanceled())
                        callback.success(result);
                } catch (Exception e) {
                    if (!c.isCanceled())
                        callback.failure(R.string.photo_response_error);
                }
            }
        });
        return call;
    }
}
