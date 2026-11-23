package com.simferopol.gta;

import android.content.Context;
import android.media.MediaPlayer;

public class SoundManager {
    private MediaPlayer bgMusic, doorOpenSound, doorCloseSound, clickSound, engineSound, fireSound;
    private Context context;

    public SoundManager(Context context) { this.context = context; }

    public void init() {
        bgMusic = create(getId("music_menu"));
        doorOpenSound = create(getId("door_open", "car_door_open"));
        doorCloseSound = create(getId("door_close", "car_door_close"));
        clickSound = create(getId("click"));
        engineSound = create(getId("car_engine", "engine"));
        fireSound = create(getId("car_fire", "fire", "explosion"));
        
        if (bgMusic != null) { bgMusic.setLooping(true); bgMusic.start(); }
        if (engineSound != null) { engineSound.setLooping(true); engineSound.setVolume(0f, 0f); }
    }

    private MediaPlayer create(int resId) {
        if (resId != 0) try { return MediaPlayer.create(context, resId); } catch (Exception e) { return null; }
        return null;
    }

    private int getId(String... names) {
        for (String name : names) {
            int id = context.getResources().getIdentifier(name, "raw", context.getPackageName());
            if (id != 0) return id;
        }
        return 0;
    }

    public void playDoorOpen() { if (doorOpenSound != null) doorOpenSound.start(); }
    public void playDoorClose() { if (doorCloseSound != null) doorCloseSound.start(); }
    public void playClick() { if (clickSound != null) clickSound.start(); }
    public void playFire() { if (fireSound != null) fireSound.start(); }
    public void setEngineVolume(float v) { if (engineSound != null) engineSound.setVolume(v, v); }
    public void startEngine() { if (engineSound != null && !engineSound.isPlaying()) engineSound.start(); }
    public void stopEngine() { if (engineSound != null && engineSound.isPlaying()) engineSound.pause(); }
    public void pauseAll() {
        if (bgMusic != null && bgMusic.isPlaying()) bgMusic.pause();
        if (engineSound != null && engineSound.isPlaying()) engineSound.pause();
    }
    public void resumeAll() {
        if (bgMusic != null && !bgMusic.isPlaying()) bgMusic.start();
        if (engineSound != null && !engineSound.isPlaying()) engineSound.start();
    }
    public void release() {
        if (bgMusic != null) bgMusic.release();
        if (doorOpenSound != null) doorOpenSound.release();
        if (doorCloseSound != null) doorCloseSound.release();
        if (clickSound != null) clickSound.release();
        if (engineSound != null) engineSound.release();
        if (fireSound != null) fireSound.release();
    }
}
