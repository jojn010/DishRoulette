package com.example.dishroulette;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

/** Short original synthesized mechanical samples; SoundPool keeps playback off the UI thread. */
public final class MechanicalAudio {
    private final SoundPool pool;
    private final int revolver,rifle,shotgun,shot;
    public MechanicalAudio(Context context) {
        pool=new SoundPool.Builder().setMaxStreams(3).setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();
        revolver=pool.load(context,R.raw.cylinder_click,1);rifle=pool.load(context,R.raw.bolt,1);
        shotgun=pool.load(context,R.raw.shell_load,1);shot=pool.load(context,R.raw.shot,1);
    }
    public void load(String goal){pool.play(goal.equals("Cutting")?rifle:goal.equals("Bulking")?shotgun:revolver,.5f,.5f,1,0,1);}
    public void detent(){pool.play(revolver,.17f,.17f,1,0,1.6f);}
    public void fire(){pool.play(shot,.45f,.45f,1,0,1);}
    public void close(){pool.release();}
}
