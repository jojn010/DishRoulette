package com.example.dishroulette;

import android.content.Context;
import android.os.Looper;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.gson.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import okhttp3.*;
import org.junit.*;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import static org.junit.Assert.*;

public class PersonalConnectionTest {
    private static final String TEST_KEY="test_owner_key_abcdefghijklmnopqrstuvwxyz";
    private final Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
    private final GeminiCredentials store=new GeminiCredentials(context);
    private byte[] backup;
    private File file(){return new File(context.getNoBackupFilesDir(),"gemini-credentials.bin");}
    @Before public void backup() throws Exception {if(file().isFile()){try(FileInputStream in=new FileInputStream(file())){backup=in.readAllBytes();}}store.clear();}
    @After public void restore() throws Exception {store.clear();if(backup!=null)try(FileOutputStream out=new FileOutputStream(file())){out.write(backup);}}
    @Test public void keystoreEncryptsPersistsAndRejectsCorruption() throws Exception {
        store.save(TEST_KEY,GeminiCredentials.DEFAULT_MODEL);assertTrue(new GeminiCredentials(context).configured());
        assertEquals(TEST_KEY,store.read().key);try(FileInputStream in=new FileInputStream(file())){assertFalse(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).contains(TEST_KEY));}
        try(FileOutputStream out=new FileOutputStream(file())){out.write(new byte[]{1,2,3});}assertNull(store.read());store.clear();assertFalse(store.configured());
    }
    @Test public void directHttpsContractSupportsBothLanguagesAndMainThreadCallbacks() throws Exception {
        store.save(TEST_KEY,GeminiCredentials.DEFAULT_MODEL);
        for(String language:Arrays.asList("en","ar")){
            AtomicReference<String> title=new AtomicReference<>();AtomicReference<Throwable> error=new AtomicReference<>();CountDownLatch latch=new CountDownLatch(1);
            OkHttpClient client=new OkHttpClient.Builder().addInterceptor(chain->{
                okhttp3.Request r=chain.request();assertEquals("https",r.url().scheme());assertEquals("generativelanguage.googleapis.com",r.url().host());assertEquals(TEST_KEY,r.header("x-goog-api-key"));
                okio.Buffer body=new okio.Buffer();r.body().writeTo(body);String json=body.readUtf8();assertFalse(json.contains(TEST_KEY));assertTrue(json.contains("\\\"language\\\":\\\""+language));
                String recipe="{\"possible\":true,\"title\":\"Rice\",\"description\":\"Warm rice\",\"meal\":\"Dinner\",\"cuisine\":\"Italian\",\"whyThisDish\":\"Your rice\",\"goalFit\":\"Balanced\",\"minutes\":20,\"servings\":2,\"ingredients\":[\"1 cup rice\"],\"steps\":[\"Cook rice for 15 minutes.\"],\"pantryUsed\":[\"rice\"],\"additionalIngredients\":[]}";
                JsonObject bilingual=com.google.gson.JsonParser.parseString(recipe).getAsJsonObject();
                JsonObject en=new JsonObject();for(String field:Arrays.asList("title","description","ingredients","steps"))en.add(field,bilingual.get(field));
                JsonObject ar=com.google.gson.JsonParser.parseString("{\"title\":\"أرز\",\"description\":\"أرز دافئ\",\"ingredients\":[\"1 كوب أرز\"],\"steps\":[\"اطبخ الأرز لمدة 15 دقيقة.\"]}").getAsJsonObject();
                JsonObject translations=new JsonObject();translations.add("en",en);translations.add("ar",ar);bilingual.add("translations",translations);
                if(language.equals("ar"))for(String field:Arrays.asList("title","description","ingredients","steps"))bilingual.add(field,ar.get(field));
                recipe=bilingual.toString();
                JsonObject part=new JsonObject();part.addProperty("text",recipe);JsonObject candidate=new JsonObject();candidate.addProperty("finishReason","STOP");JsonArray parts=new JsonArray();parts.add(part);JsonObject content=new JsonObject();content.add("parts",parts);candidate.add("content",content);JsonArray candidates=new JsonArray();candidates.add(candidate);JsonObject result=new JsonObject();result.add("candidates",candidates);
                return new okhttp3.Response.Builder().request(r).protocol(Protocol.HTTP_1_1).code(200).message("OK").body(ResponseBody.create(MediaType.get("application/json"),result.toString())).build();
            }).build();
            new PersonalGeminiApi(context,client).suggest(request(language)).enqueue(new Callback<RecipeSuggestion>(){
                public void onResponse(Call<RecipeSuggestion> c,Response<RecipeSuggestion> r){try{assertSame(Looper.getMainLooper(),Looper.myLooper());assertTrue(r.isSuccessful());title.set(r.body().title);}catch(Throwable t){error.set(t);}finally{latch.countDown();}}
                public void onFailure(Call<RecipeSuggestion> c,Throwable t){error.set(t);latch.countDown();}
            });assertTrue(latch.await(10,TimeUnit.SECONDS));if(error.get()!=null)throw new AssertionError(error.get());assertEquals(language.equals("ar")?"أرز":"Rice",title.get());
        }
    }
    @Test public void missingKeyReturnsSetupCodeWithoutNetworking() throws Exception {
        AtomicBoolean network=new AtomicBoolean();OkHttpClient client=new OkHttpClient.Builder().addInterceptor(chain->{network.set(true);throw new IOException();}).build();
        CountDownLatch done=new CountDownLatch(1);AtomicReference<String> code=new AtomicReference<>();
        new PersonalGeminiApi(context,client).suggest(request("en")).enqueue(new Callback<RecipeSuggestion>(){
            public void onResponse(Call<RecipeSuggestion> c,Response<RecipeSuggestion> r){try{code.set(JsonParser.parseString(r.errorBody().string()).getAsJsonObject().get("code").getAsString());}catch(Exception ignored){}done.countDown();}
            public void onFailure(Call<RecipeSuggestion> c,Throwable t){done.countDown();}
        });assertTrue(done.await(5,TimeUnit.SECONDS));assertEquals("KEY_NOT_CONFIGURED",code.get());assertFalse(network.get());
    }
    @Test public void canceledResponseCannotReachTheUi() throws Exception {
        store.save(TEST_KEY,GeminiCredentials.DEFAULT_MODEL);CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);AtomicBoolean callback=new AtomicBoolean();
        OkHttpClient client=new OkHttpClient.Builder().addInterceptor(chain->{entered.countDown();try{release.await(5,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}return new okhttp3.Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(429).message("rate limit").body(ResponseBody.create(MediaType.get("application/json"),"{}")).build();}).build();
        Call<RecipeSuggestion> call=new PersonalGeminiApi(context,client).suggest(request("en"));call.enqueue(new Callback<RecipeSuggestion>(){public void onResponse(Call<RecipeSuggestion> c,Response<RecipeSuggestion> r){callback.set(true);}public void onFailure(Call<RecipeSuggestion> c,Throwable t){callback.set(true);}});
        assertTrue(entered.await(5,TimeUnit.SECONDS));call.cancel();release.countDown();Thread.sleep(350);assertFalse(callback.get());assertTrue(call.isCanceled());
    }
    private RecipeApi.Request request(String language){return new RecipeApi.Request(new HashSet<>(Collections.singletonList("rice")),"Any meal","",new SpinProfile(),Collections.emptyList(),language);}
}
