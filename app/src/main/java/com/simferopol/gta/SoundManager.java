package com.simferopol.gta;

import android.content.Context;
import android.media.MediaPlayer;
import com.simferopol.gta.R;

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
        // Фоновая музыка (зацикленная)
        bgMusic = MediaPlayer.create(context, R.raw.music_menu);
        if (bgMusic != null) {
            bgMusic.setLooping(true);
            bgMusic.setVolume(0.5f, 0.5f);
            bgMusic.start();
        }

        // Короткие звуки
        doorOpenSound = MediaPlayer.create(context, R.raw.door_open);
        doorCloseSound = MediaPlayer.create(context, R.raw.door_close);
        clickSound = MediaPlayer.create(context, R.raw.click);
        fireSound = MediaPlayer.create(context, R.raw.car_fire);
        
        // Звук двигателя (цикличный)
        engineSound = MediaPlayer.create(context, R.raw.car_engine);
        if (engineSound != null) {
            engineSound.setLooping(true);
            engineSound.setVolume(0f, 0f); // Изначально тихо
        }
    }

    public void playDoorOpen() {
        if (doorOpenSound != null) doorOpenSound.start();
    }

    public void playDoorClose() {
        if (doorCloseSound != null) doorCloseSound.start();
    }

    public void playClick() {
        if (clickSound != null) clickSound.start();
    }

    public void playFire() {
        if (fireSound != null) fireSound.start();
    }

    public void setEngineVolume(float volume) {
        if (engineSound != null) {
            engineSound.setVolume(volume, volume);
        }
    }

    public void startEngine() {
        if (engineSound != null && !engineSound.isPlaying()) {
            engineSound.start();
        }
    }

    public void stopEngine() {
        if (engineSound != null && engineSound.isPlaying()) {
            engineSound.pause();
        }
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
