package com.simferopol.gta;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private Thread thread;
    private volatile boolean running;

    private float carX = 400, carY = 300;
    private float carSpeed = 0;
    private float carAngle = 0;
    private final float MAX_SPEED = 6f;

    private boolean gas = false, brake = false, left = false, right = false;

    private final Paint roadPaint = new Paint();
    private final Paint linePaint = new Paint();
    private final Paint carPaint = new Paint();
    private final Paint buildingPaint = new Paint();
    private final Paint btnPaint = new Paint();
    private final Paint textPaint = new Paint();

    private final RectF[] buildings = {
        new RectF(100, 100, 250, 200),
        new RectF(700, 80, 900, 220),
        new RectF(150, 500, 300, 620),
        new RectF(800, 500, 950, 650),
        new RectF(450, 150, 600, 250)
    };

    private int width, height;

    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);
        initPaints();
    }

    private void initPaints() {
        roadPaint.setColor(Color.rgb(60, 60, 60));
        linePaint.setColor(Color.YELLOW);
        linePaint.setStrokeWidth(4);
        carPaint.setColor(Color.RED);
        buildingPaint.setColor(Color.rgb(120, 120, 130));
        btnPaint.setColor(Color.argb(150, 255, 255, 255));
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(36);
        textPaint.setAntiAlias(true);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        width = getWidth();
        height = getHeight();
        carX = width / 2f;
        carY = height / 2f;
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
        if (gas) carSpeed = Math.min(carSpeed + 0.15f, MAX_SPEED);
        else if (brake) carSpeed = Math.max(carSpeed - 0.2f, -MAX_SPEED / 2);
        else carSpeed *= 0.96f;

        float turn = 0.06f * (carSpeed / MAX_SPEED);
        if (left) carAngle -= turn;
        if (right) carAngle += turn;

        carX += Math.sin(carAngle) * carSpeed;
        carY -= Math.cos(carAngle) * carSpeed;

        if (carX < 40) carX = 40;
        if (carX > width - 40) carX = width - 40;
        if (carY < 40) carY = 40;
        if (carY > height - 40) carY = height - 40;
    }

    private void draw() {
        Canvas c = null;
        try {
            c = getHolder().lockCanvas();
            if (c == null) return;
            c.drawColor(Color.rgb(90, 140, 90));

            // Дороги (под портретный экран)
            c.drawRect(0, height / 2f - 150, width, height / 2f + 150, roadPaint);
            c.drawRect(width / 2f - 150, 0, width / 2f + 150, height, roadPaint);

            // Разметка
            for (float x = 0; x < width; x += 80)
                c.drawLine(x, height / 2f, x + 40, height / 2f, linePaint);
            for (float y = 0; y < height; y += 80)
                c.drawLine(width / 2f, y, width / 2f, y + 40, linePaint);

            // Здания
            for (RectF b : buildings) c.drawRect(b, buildingPaint);

            // Машина
            c.save();
            c.translate(carX, carY);
            c.rotate((float) Math.toDegrees(carAngle));
            RectF car = new RectF(-25, -40, 25, 40);
            c.drawRect(car, carPaint);
            Paint glass = new Paint();
            glass.setColor(Color.CYAN);
            c.drawRect(new RectF(-18, -25, 18, -5), glass);
            c.restore();

            // Кнопки управления (адаптация под вертикальный экран)
            float btnY = height - 200;
            drawButton(c, 80, btnY, 60, "◀");
            drawButton(c, 220, btnY, 60, "▶");
            drawButton(c, width - 220, btnY, 60, "▲");
            drawButton(c, width - 80, btnY, 60, "■");

            // HUD
            c.drawText("GTA Simferopol 1", 30, 60, textPaint);
            textPaint.setTextSize(28);
            c.drawText("Speed: " + (int)(carSpeed * 10) + " km/h", 30, 100, textPaint);
            textPaint.setTextSize(36);

        } finally {
            if (c != null) getHolder().unlockCanvasAndPost(c);
        }
    }

    private void drawButton(Canvas c, float cx, float cy, float r, String label) {
        c.drawCircle(cx, cy, r, btnPaint);
        textPaint.setTextSize(48);
        textPaint.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float ty = cy - (fm.ascent + fm.descent) / 2;
        c.drawText(label, cx, ty, textPaint);
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTextSize(36);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX(), y = e.getY();
        float btnY = height - 200;

        float leftCx = 80, rightCx = 220;
        float gasCx = width - 220, brakeCx = width - 80;

        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            if (dist(x, y, leftCx, btnY) < 80) left = true;
            if (dist(x, y, rightCx, btnY) < 80) right = true;
            if (dist(x, y, gasCx, btnY) < 80) gas = true;
            if (dist(x, y, brakeCx, btnY) < 80) brake = true;
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
