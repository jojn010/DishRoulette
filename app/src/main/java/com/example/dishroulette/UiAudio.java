package com.example.dishroulette;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Downloaded Kenney CC0 samples; never replay an interaction after a sample loads.
 */
final class UiAudio {
    private final Context context;
    private final SoundPool pool;
    private final Set<Integer> ready = ConcurrentHashMap.newKeySet();
    private final int click, confirm, timer;
    private boolean closed;

    UiAudio(Context context) {
        this.context = context;
        pool = new SoundPool.Builder().setMaxStreams(2).setAudioAttributes(new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();
        pool.setOnLoadCompleteListener((soundPool, id, status) -> {
            if (status == 0)
                ready.add(id);
        });
        click = pool.load(context, R.raw.ui_click, 1);
        confirm = pool.load(context, R.raw.ui_confirm, 1);
        timer = pool.load(context, R.raw.timer_complete, 1);
    }

    private void play(int id, float volume) {
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (closed || !ready.contains(id) || !InterfaceSettings.prefs(context).getBoolean("ui_sounds", true)
            || audio == null || audio.getRingerMode() != AudioManager.RINGER_MODE_NORMAL
            || audio.getStreamVolume(AudioManager.STREAM_SYSTEM) == 0)
            return;
        pool.play(id, volume, volume, 1, 0, 1f);
    }

    void click() {
        play(click, .18f);
    }

    void confirm() {
        play(confirm, .28f);
    }

    void timerComplete() {
        play(timer, .55f);
    }

    void close() {
        closed = true;
        pool.release();
        ready.clear();
    }
}
