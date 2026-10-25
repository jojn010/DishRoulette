package com.example.dishroulette;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.example.dishroulette.core.RecipeRules;
import com.google.gson.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.*;
import okio.Timeout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** HTTPS directly from the owner's phone to Gemini; no laptop, proxy, key in APK, or logging. */
final class PersonalGeminiApi implements RecipeApi {
    private static final Gson GSON=new Gson();
    private final GeminiCredentials credentials;
    private final OkHttpClient client;
    private static OkHttpClient client(){return new OkHttpClient.Builder().connectTimeout(10,TimeUnit.SECONDS)
        .readTimeout(60,TimeUnit.SECONDS).callTimeout(65,TimeUnit.SECONDS).retryOnConnectionFailure(false)
        .followRedirects(false).followSslRedirects(false).build();}
    PersonalGeminiApi(Context c){this(c,client());}
    PersonalGeminiApi(Context c,OkHttpClient client){credentials=new GeminiCredentials(c.getApplicationContext());this.client=client;}
    public Call<RecipeSuggestion> suggest(RecipeApi.Request request){return new Suggestion(request);}
    private final class Suggestion implements Call<RecipeSuggestion> {
        private final RecipeApi.Request input;
        private okhttp3.Call upstream;
        private boolean executed;
        private volatile boolean canceled;
        private final Handler main=new Handler(Looper.getMainLooper());
        Suggestion(RecipeApi.Request input){this.input=input;}
        private void error(Callback<RecipeSuggestion> cb,int status,String code){
            JsonObject error=new JsonObject();error.addProperty("code",code);
            Response<RecipeSuggestion> result=Response.error(status,ResponseBody.create(MediaType.get("application/json"),error.toString()));
            main.post(()->{if(!canceled)cb.onResponse(this,result);});
        }
        public synchronized void enqueue(Callback<RecipeSuggestion> cb){
            if(executed)throw new IllegalStateException("Already executed");executed=true;if(canceled)return;
            GeminiCredentials.Value value=credentials.read();
            if(value==null){error(cb,503,"KEY_NOT_CONFIGURED");return;}
            final JsonObject request;
            try{
                request=RecipeRules.validateRequest(GSON.toJsonTree(input).getAsJsonObject());
                okhttp3.Request http=new okhttp3.Request.Builder().url("https://generativelanguage.googleapis.com/v1beta/models/"+value.model+":generateContent")
                    .header("x-goog-api-key",value.key)
                    .post(RequestBody.create(MediaType.get("application/json; charset=utf-8"),RecipeRules.payload(request).toString())).build();
                upstream=client.newCall(http);
            }catch(RecipeRules.ApiFailure e){error(cb,e.status,e.code);return;}
            upstream.enqueue(new okhttp3.Callback(){
                public void onFailure(okhttp3.Call call,IOException e){main.post(()->{if(!canceled)cb.onFailure(Suggestion.this,new IOException("Gemini connection failed"));});}
                public void onResponse(okhttp3.Call call,okhttp3.Response response){
                    try(okhttp3.Response close=response){
                        int status=response.code();
                        if(status==429){error(cb,429,"RATE_LIMITED");return;}
                        if(status==400||status==401||status==403||status==404){error(cb,503,"PROVIDER_CONFIGURATION");return;}
                        if(status!=200){error(cb,502,"PROVIDER_UNAVAILABLE");return;}
                        if(response.body()==null){error(cb,502,"INVALID_RECIPE");return;}
                        // Bound untrusted output before constructing the JSON document.
                        okio.BufferedSource source=response.body().source();source.request(100_001);
                        if(source.getBuffer().size()>100_000){error(cb,502,"INVALID_RECIPE");return;}
                        JsonObject result=RecipeRules.parseResponse(source.readUtf8(),request);
                        RecipeSuggestion recipe=GSON.fromJson(result,RecipeSuggestion.class);
                        main.post(()->{if(!canceled)cb.onResponse(Suggestion.this,Response.success(recipe));});
                    }catch(RecipeRules.ApiFailure e){error(cb,e.status,e.code);}
                    catch(Exception e){error(cb,502,"INVALID_RECIPE");}
                }
            });
        }
        public Response<RecipeSuggestion> execute(){throw new UnsupportedOperationException("Use enqueue");}
        public synchronized void cancel(){canceled=true;if(upstream!=null)upstream.cancel();}
        public boolean isCanceled(){return canceled;}
        public synchronized boolean isExecuted(){return executed;}
        public Call<RecipeSuggestion> clone(){return new Suggestion(input);}
        public okhttp3.Request request(){return new okhttp3.Request.Builder().url("https://generativelanguage.googleapis.com/").build();}
        public Timeout timeout(){return Timeout.NONE;}
    }
}
