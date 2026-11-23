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

    // Игрок
    private float playerX = 0; // Смещение по дороге (-1.0 влево, 1.0 вправо)
    private float playerZ = 0; // Глубина (движение вперед)
    
    // Управление
    private boolean movingLeft = false;
    private boolean movingRight = false;
    private boolean movingForward = false;
    private boolean movingBackward = false;
    private boolean shooting = false;

    // NPC (Полицейская машина)
    private float copZ = 1500; // Расстояние до машины
    private float copX = 0; // Позиция машины
    private boolean copExploded = false;

    // Оружие
    private int ammo = 30;

    // Отрисовка
    private final Paint paint = new Paint();
    private final Paint skyPaint = new Paint();
    private final Paint roadPaint = new Paint();
    private final Paint gunPaint = new Paint();
    private final Paint btnPaint = new Paint();
    private final Paint textPaint = new Paint();
    
    private int width, height;
    private float horizonY;
    private float fov = 800f; // Фокусное расстояние

    public GameView(Context context, SoundManager soundManager) {
        super(context);
        this.soundManager = soundManager;
        getHolder().addCallback(this);
        initPaints();
    }

    private void initPaints() {
        skyPaint.setColor(Color.rgb(135, 206, 235));
        roadPaint.setColor(Color.rgb(80, 80, 80));
        gunPaint.setColor(Color.rgb(40, 40, 40)); // Темно-серый ствол
        btnPaint.setColor(Color.argb(150, 255, 255, 255));
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(50);
        textPaint.setAntiAlias(true);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        width = getWidth();
        height = getHeight();
        horizonY = height * 0.4f;
        running = true;
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int f, int w, int h) {
        width = w;
        height = h;
        horizonY = height * 0.4f;
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
        float speed = 15f;
        if (movingForward) playerZ += speed;
        if (movingBackward) playerZ -= speed;
        if (movingLeft) playerX -= 0.05f;
        if (movingRight) playerX += 0.05f;

        // Ограничение движения
        if (playerX < -1f) playerX = -1f;
        if (playerX > 1f) playerX = 1f;

        // Полиция едет на нас, если не взорвана
        if (!copExploded) {
            copZ -= 5f; 
            if (copZ < 50) copZ = 2000; // Респавн
        }

        if (shooting) {
            shooting = false; // Стреляем по одному выстрелу
            if (ammo > 0) {
                ammo--;
                soundManager.playClick(); // Звук выстрела (используем клик)
                // Проверка попадания
                if (!copExploded && copZ > 0 && copZ < 1500 && Math.abs(copX - playerX) < 0.3f) {
                    copExploded = true;
                    soundManager.playFire(); // Звук взрыва
                }
            }
        }
    }

    private void draw() {
        Canvas c = null;
        try {
            c = getHolder().lockCanvas();
            if (c == null) return;
            
            // Небо и земля
            c.drawRect(0, 0, width, height, skyPaint);
            c.drawRect(0, horizonY, width, height, new Paint(Color.rgb(50, 120, 50))); // Трава

            // Дорога (в перспективе)
            drawRoad(c);

            // Машина полиции (3D-куб)
            if (!copExploded) {
                draw3DCube(c, copX, copZ, Color.BLUE, 200, 100, 150);
            }

            // Отрисовка оружия (от первого лица)
            drawWeapon(c);

            // UI
            textPaint.setTextSize(50);
            c.drawText("Патроны: " + ammo, 50, 80, textPaint);
            c.drawText("Позиция X: " + String.format("%.2f", playerX), 50, 140, textPaint);

            // Кнопки управления
            drawButton(c, 150, height - 200, 100, "◀"); // Влево
            drawButton(c, 350, height - 200, 100, "▶"); // Вправо
            drawButton(c, width - 350, height - 200, 100, "▲"); // Вперед
            drawButton(c, width - 150, height - 200, 100, "▼"); // Назад
            
            // Кнопка выстрела
            drawButton(c, width - 150, height - 450, 120, "🔥 ОГОНЬ");

        } finally {
            if (c != null) getHolder().unlockCanvasAndPost(c);
        }
    }

    // Рисуем дорогу, сужающуюся к горизонту
    private void drawRoad(Canvas c) {
        Path road = new Path();
        float roadWBottom = width * 1.5f;
        float roadWTop = width * 0.1f;
        
        road.moveTo(width / 2f - roadWTop / 2, horizonY);
        road.lineTo(width / 2f + roadWTop / 2, horizonY);
        road.lineTo(width / 2f + roadWBottom / 2, height);
        road.lineTo(width / 2f - roadWBottom / 2, height);
        road.close();
        c.drawPath(road, roadPaint);

        // Разметка
        Paint line = new Paint();
        line.setColor(Color.YELLOW);
        for (float z = 0; z < 2000; z += 200) {
            float scale = fov / (z + fov);
            float y = horizonY + (height - horizonY) * scale;
            float w = width * 0.02f * scale;
            c.drawRect(width / 2f - w / 2, y, width / 2f + w / 2, y + 20 * scale, line);
        }
    }

    // Рисуем 3D-куб (объект)
    private void draw3DCube(Canvas c, float objX, float objZ, int color, float sizeW, float sizeH, float sizeD) {
        if (objZ <= 0 || objZ > 3000) return; // За пределами видимости
        
        float scale = fov / objZ; // Чем дальше, тем меньше
        float screenX = width / 2f + (objX * width * 0.5f * scale) - (width/2f); // Учет смещения игрока
        float screenY = horizonY + (height / 2f) * scale; 
        
        float w = sizeW * scale;
        float h = sizeH * scale;

        Paint p = new Paint();
        p.setColor(color);

        // Задняя стенка (чуть темнее)
        Paint back = new Paint();
        back.setColor(Color.rgb(
            Math.max(0, Color.red(color) - 30),
            Math.max(0, Color.green(color) - 30),
            Math.max(0, Color.blue(color) - 30)
        ));

        // Рисуем как прямоугольник
        c.drawRect(screenX - w / 2, screenY - h, screenX + w / 2, screenY, p);
        
        // Мигалка (если полиция)
        if (color == Color.BLUE) {
            Paint light = new Paint();
            light.setColor(Color.RED);
            c.drawRect(screenX - w / 2 + 10, screenY - h + 10, screenX + w / 2 - 10, screenY - h + 30, light);
        }
    }

    // Оружие (рука с пистолетом)
    private void drawWeapon(Canvas c) {
        Path gun = new Path();
        gun.moveTo(width - 200, height);
        gun.lineTo(width - 100, height - 300);
        gun.lineTo(width - 50, height - 300);
        gun.lineTo(width, height);
        gun.close();
        c.drawPath(gun, gunPaint);
        
        // Ствол
        Paint barrel = new Paint();
        barrel.setColor(Color.BLACK);
        c.drawRect(width - 150, height - 300, width - 70, height - 250, barrel);
    }

    private void drawButton(Canvas c, float cx, float cy, float r, String label) {
        c.drawCircle(cx, cy, r, btnPaint);
        textPaint.setTextSize(60);
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
            if (dist(x, y, 150, height - 200) < 120) movingLeft = true;
            if (dist(x, y, 350, height - 200) < 120) movingRight = true;
            if (dist(x, y, width - 350, height - 200) < 120) movingForward = true;
            if (dist(x, y, width - 150, height - 200) < 120) movingBackward = true;
            if (dist(x, y, width - 150, height - 450) < 150) shooting = true;
        } else if (e.getAction() == MotionEvent.ACTION_UP) {
            movingLeft = movingRight = movingForward = movingBackward = false;
        }
        return true;
    }

    private float dist(float x1, float y1, float x2, float y2) {
        return (float) Math.hypot(x1 - x2, y1 - y2);
    }
}
