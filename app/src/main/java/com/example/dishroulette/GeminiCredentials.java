package com.example.dishroulette;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.AtomicFile;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;

/** Owner-supplied credentials, encrypted with a non-exportable Android Keystore key. */
final class GeminiCredentials {
    static final String DEFAULT_MODEL="gemini-3.5-flash-lite";
    private static final String ALIAS="dishroulette.personal.gemini.v1";
    private final AtomicFile file;
    GeminiCredentials(Context c){file=new AtomicFile(new File(c.getNoBackupFilesDir(),"gemini-credentials.bin"));}
    static final class Value {
        final String key,model;
        Value(String key,String model){this.key=key;this.model=model;}
    }
    private javax.crypto.SecretKey secret() throws Exception {
        KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
        if(!store.containsAlias(ALIAS)){
            KeyGenerator gen=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
            gen.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());gen.generateKey();
        }
        return (javax.crypto.SecretKey)store.getKey(ALIAS,null);
    }
    synchronized void save(String key,String model) throws Exception {
        key=key.trim();model=model.trim();
        if(key.length()<20||key.length()>256||!key.matches("[A-Za-z0-9_-]+")||!model.matches("gemini-[A-Za-z0-9.-]{1,70}"))throw new IllegalArgumentException();
        JsonObject data=new JsonObject();data.addProperty("key",key);data.addProperty("model",model);
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,secret());
        byte[] encrypted=cipher.doFinal(data.toString().getBytes(StandardCharsets.UTF_8));
        FileOutputStream stream=null;
        try{stream=file.startWrite();DataOutputStream out=new DataOutputStream(stream);out.writeInt(1);out.writeInt(cipher.getIV().length);out.write(cipher.getIV());out.writeInt(encrypted.length);out.write(encrypted);out.flush();file.finishWrite(stream);}
        catch(Exception e){if(stream!=null)file.failWrite(stream);throw e;}
    }
    synchronized Value read(){
        try(DataInputStream in=new DataInputStream(file.openRead())){
            if(in.readInt()!=1)return null;int ivSize=in.readInt();if(ivSize!=12)return null;byte[] iv=new byte[ivSize];in.readFully(iv);
            int size=in.readInt();if(size<16||size>2048)return null;byte[] bytes=new byte[size];in.readFully(bytes);
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,secret(),new GCMParameterSpec(128,iv));
            JsonObject data=JsonParser.parseString(new String(cipher.doFinal(bytes),StandardCharsets.UTF_8)).getAsJsonObject();
            return new Value(data.get("key").getAsString(),data.get("model").getAsString());
        }catch(Exception e){return null;}
    }
    boolean configured(){return read()!=null;}
    synchronized void clear(){file.delete();}
    /** Optional private owner APK. Standard builds contain no bootstrap asset. */
    private static void importPrivateApkSetup(Context c) {
        GeminiCredentials credentials = new GeminiCredentials(c);
        // An existing user connection always wins; never silently replace it on upgrade.
        if (credentials.configured()) return;
        try (InputStream in = c.getAssets().open("private-gemini-setup.json")) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[512];
            int count;
            while ((count = in.read(buffer)) != -1) {
                if (bytes.size() + count > 2048) return;
                bytes.write(buffer, 0, count);
            }
            JsonObject data = JsonParser.parseString(bytes.toString("UTF-8")).getAsJsonObject();
            credentials.save(data.get("key").getAsString(),
                    data.has("model") ? data.get("model").getAsString() : DEFAULT_MODEL);
        } catch (Exception ignored) { /* Ordinary APKs have no asset; keep manual setup available. */ }
    }
    /** Development-only USB provisioning in the app's private directory; never packaged in an APK. */
    static void importSetup(Context c){
        if(!BuildConfig.DEBUG)return;
        importPrivateApkSetup(c);
        File setup=new File(c.getFilesDir(),"personal-gemini-setup.json");if(!setup.isFile())return;
        try(FileInputStream in=new FileInputStream(setup)){
            if(setup.length()>2048)return;byte[] bytes=new byte[(int)setup.length()];new DataInputStream(in).readFully(bytes);
            JsonObject data=JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject();
            new GeminiCredentials(c).save(data.get("key").getAsString(),data.has("model")?data.get("model").getAsString():DEFAULT_MODEL);
        }catch(Exception ignored){/* Settings lets the owner retry without logging sensitive input. */}
        finally{if(!setup.delete())setup.deleteOnExit();}
    }
}
