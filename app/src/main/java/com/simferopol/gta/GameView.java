package com.simferopol.gta;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private Thread thread;
    private volatile boolean running;
    private SoundManager soundManager;

    // Игрок (машина)
    private float carX = 0.5f; // 0.0 до 1.0 (относительно ширины экрана)
    private float carSpeed = 0;
    private final float MAX_SPEED = 1.0f;
    
    // Состояние
    private boolean inCar = false; // Открыта ли дверь/в машине ли игрок
    private boolean gas = false, brake = false, left = false, right = false;

    private final Paint roadPaint = new Paint();
    private final Paint linePaint = new Paint();
    private final Paint carPaint = new Paint();
    private final Paint carShadowPaint = new Paint();
    private final Paint btnPaint = new Paint();
    private final Paint textPaint = new Paint();

    private int width, height;
    
    // Для 2.5D дороги
    private float horizonY;
    private float roadCenterX;

    public GameView(Context context, SoundManager soundManager) {
        super(context);
        this.soundManager = soundManager;
        getHolder().addCallback(this);
        initPaints();
    }

    private void initPaints() {
        roadPaint.setColor(Color.rgb(60, 60, 60));
        
        linePaint.setColor(Color.YELLOW);
        linePaint.setStrokeWidth(10);
        linePaint.setStyle(Paint.Style.FILL);
        
        carPaint.setColor(Color.RED);
        
        carShadowPaint.setColor(Color.argb(100, 0, 0, 0));
        
        btnPaint.setColor(Color.argb(150, 255, 255, 255));
        
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(60);
        textPaint.setAntiAlias(true);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        width = getWidth();
        height = getHeight();
        horizonY = height * 0.3f; // Горизонт на 30% высоты
        roadCenterX = width / 2f;
        running = true;
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int f, int w, int h) {
        width = w;
        height = h;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        running = false;
        try {
            if (thread != null) thread.join();
        } catch (InterruptedException ignored) {}
    }

    public void pause() { running = false; }
    public void resume() {
        if (!running && thread != null && thread.isAlive()) {
            running = true;
        } else if (thread != null && !thread.isAlive()) {
            running = true;
            thread = new Thread(this);
            thread.start();
        }
    }

    @Override
    public void run() {
        while (running) {
            long start = System.currentTimeMillis();
            update();
            draw();
            long elapsed = System.currentTimeMillis() - start;
            long sleep = 16 - elapsed;
            if (sleep > 0) {
                try { Thread.sleep(sleep); } catch (InterruptedException ignored) {}
            }
        }
    }

    private void update() {
        if (inCar) {
            // Физика движения
            if (gas) carSpeed = Math.min(carSpeed + 0.02f, MAX_SPEED);
            else if (brake) carSpeed = Math.max(carSpeed - 0.03f, 0);
            else carSpeed *= 0.98f; // Трение

            // Движение машины влево/вправо по дороге (2.5D)
            if (left && carSpeed > 0) carX -= 0.02f;
            if (right && carSpeed > 0) carX += 0.02f;

            // Ограничение по краям дороги
            if (carX < 0.2f) carX = 0.2f;
            if (carX > 0.8f) carX = 0.8f;

            // Управление звуком двигателя
            soundManager.setEngineVolume(carSpeed);
            if (carSpeed > 0.05f) soundManager.startEngine();
            else soundManager.stopEngine();
        } else {
            soundManager.stopEngine();
        }
    }

    private void draw() {
        Canvas c = null;
        try {
            c = getHolder().lockCanvas();
            if (c == null) return;
            
            // Небо и земля
            c.drawColor(Color.rgb(135, 206, 235)); // Небо
            c.drawRect(0, horizonY, width, height, new Paint(Color.rgb(90, 140, 90))); // Трава

            // 2.5D Дорога (трапеция)
            Path roadPath = new Path();
            float roadBottomWidth = width * 0.8f;
            float roadTopWidth = width * 0.1f;
            roadPath.moveTo(roadCenterX - roadTopWidth / 2, horizonY);
            roadPath.lineTo(roadCenterX + roadTopWidth / 2, horizonY);
            roadPath.lineTo(roadCenterX + roadBottomWidth / 2, height);
            roadPath.lineTo(roadCenterX - roadBottomWidth / 2, height);
            roadPath.close();
            c.drawPath(roadPath, roadPaint);

            // 2.5D Разметка (пунктир)
            for (float i = 0; i < 1.0f; i += 0.1f) {
                float y1 = horizonY + (height - horizonY) * i;
                float y2 = horizonY + (height - horizonY) * (i + 0.05f);
                float w1 = roadTopWidth + (roadBottomWidth - roadTopWidth) * i;
                float w2 = roadTopWidth + (roadBottomWidth - roadTopWidth) * (i + 0.05f);
                
                Path linePath = new Path();
                linePath.moveTo(roadCenterX - w1 * 0.05f, y1);
                linePath.lineTo(roadCenterX + w1 * 0.05f, y1);
                linePath.lineTo(roadCenterX + w2 * 0.05f, y2);
                linePath.lineTo(roadCenterX - w2 * 0.05f, y2);
                linePath.close();
                c.drawPath(linePath, linePaint);
            }

            // Машина (2.5D вид сзади)
            if (inCar) {
                float carW = width * 0.2f;
                float carH = carW * 1.5f;
                float carCX = roadCenterX - roadBottomWidth / 2 + roadBottomWidth * carX;
                float carCY = height * 0.8f;
                
                // Тень
                c.drawRect(carCX - carW/2, carCY, carCX + carW/2, carCY + 20, carShadowPaint);
                
                // Кузов (сзади)
                c.drawRect(carCX - carW/2, carCY - carH, carCX + carW/2, carCY, carPaint);
                
                // Заднее стекло
                Paint glassPaint = new Paint();
                glassPaint.setColor(Color.CYAN);
                c.drawRect(carCX - carW/3, carCY - carH * 0.8f, carCX + carW/3, carCY - carH * 0.5f, glassPaint);
                
                // Фары
                Paint lightPaint = new Paint();
                lightPaint.setColor(Color.YELLOW);
                c.drawRect(carCX - carW/2 + 5, carCY - 10, carCX - carW/4, carCY, lightPaint);
                c.drawRect(carCX + carW/4, carCY - 10, carCX + carW/2 - 5, carCY, lightPaint);
            }

            // Кнопки управления (внизу)
            if (inCar) {
                float btnY = height - 200;
                drawButton(c, 120, btnY, 100, "◀");
                drawButton(c, 350, btnY, 100, "▶");
                drawButton(c, width - 350, btnY, 100, "▲");
                drawButton(c, width - 120, btnY, 100, "■");
            } else {
                // Кнопка "Сесть в машину" (Дверь)
                drawButton(c, width/2f, height - 200, 150, "Открыть дверь");
            }

            // HUD
            textPaint.setTextSize(60);
            c.drawText("GTA Simferopol 1.1 (2.5D)", 30, 80, textPaint);
            textPaint.setTextSize(45);
            c.drawText("Скорость: " + (int)(carSpeed * 100) + " км/ч", 30, 140, textPaint);

        } finally {
            if (c != null) getHolder().unlockCanvasAndPost(c);
        }
    }

    private void drawButton(Canvas c, float cx, float cy, float r, String label) {
        c.drawCircle(cx, cy, r, btnPaint);
        textPaint.setTextSize(70);
        textPaint.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float ty = cy - (fm.ascent + fm.descent) / 2;
        c.drawText(label, cx, ty, textPaint);
        textPaint.setTextAlign(Paint.Align.LEFT);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX(), y = e.getY();
        
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            if (!inCar) {
                // Нажатие на кнопку двери
                if (dist(x, y, width/2f, height - 200) < 150) {
                    inCar = true;
                    soundManager.playDoorOpen();
                    soundManager.playClick();
                }
            } else {
                // Управление машиной
                float btnY = height - 200;
                float leftCx = 120, rightCx = 350;
                float gasCx = width - 350, brakeCx = width - 120;

                if (dist(x, y, leftCx, btnY) < 120) left = true;
                if (dist(x, y, rightCx, btnY) < 120) right = true;
                if (dist(x, y, gasCx, btnY) < 120) {
                    gas = true;
                    if (carSpeed < 0.1f) soundManager.playClick(); // Клик при старте
                }
                if (dist(x, y, brakeCx, btnY) < 120) brake = true;
                
                // Двойной тап для выхода из машины (закрыть дверь)
                if (e.getPointerCount() == 2) {
                    inCar = false;
                    soundManager.playDoorClose();
                }
            }
        } else if (e.getAction() == MotionEvent.ACTION_UP ||
                   e.getAction() == MotionEvent.ACTION_CANCEL) {
            left = right = gas = brake = false;
        }
        return true;
    }

    private float dist(float x1, float y1, float x2, float y2) {
        return (float) Math.hypot(x1 - x2, y1 - y2);
    }
}
