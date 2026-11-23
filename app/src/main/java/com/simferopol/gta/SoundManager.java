package com.simferopol.gta;

import android.content.Context;
import android.media.MediaPlayer;

public class SoundManager {
    private MediaPlayer bgMusic;
    private MediaPlayer doorOpenSound;
    private MediaPlayer doorCloseSound;
    private MediaPlayer clickSound;
    private MediaPlayer engineSound;
    private MediaPlayer fireSound;
    private Context context;

    public SoundManager(Context context) {
        this.context = context;
    }

    public void init() {
        // Ищем звуки с разными возможными названиями
        bgMusic = createMediaPlayer(getSoundId("music_menu"));
        doorOpenSound = createMediaPlayer(getSoundId("door_open", "car_door_open"));
        doorCloseSound = createMediaPlayer(getSoundId("door_close", "car_door_close"));
        clickSound = createMediaPlayer(getSoundId("click"));
        engineSound = createMediaPlayer(getSoundId("car_engine", "engine"));
        fireSound = createMediaPlayer(getSoundId("car_fire", "fire"));
        
        if (bgMusic != null) {
            bgMusic.setLooping(true);
            bgMusic.setVolume(0.5f, 0.5f);
            bgMusic.start();
        }
        if (engineSound != null) {
            engineSound.setLooping(true);
            engineSound.setVolume(0f, 0f);
        }
    }

    // Хелпер для безопасного создания плеера
    private MediaPlayer createMediaPlayer(int resId) {
        if (resId != 0) {
            try {
                return MediaPlayer.create(context, resId);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    // Ищет ресурс по нескольким именам. Если нет — возвращает 0.
    private int getSoundId(String... names) {
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

    public void setEngineVolume(float volume) {
        if (engineSound != null) engineSound.setVolume(volume, volume);
    }

    public void startEngine() {
        if (engineSound != null && !engineSound.isPlaying()) engineSound.start();
    }

    public void stopEngine() {
        if (engineSound != null && engineSound.isPlaying()) engineSound.pause();
    }

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
