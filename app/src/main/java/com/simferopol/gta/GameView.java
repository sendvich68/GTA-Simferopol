package com.simferopol.gta;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.SceneView;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;

public class GameView extends SceneView implements SurfaceHolder.Callback, Runnable {
    private Thread thread;
    private volatile boolean running;
    private SoundManager soundManager;

    private Node playerNode;
    private Node copNode;

    private float playerX = 0f;
    private float playerZ = 0f;

    private boolean movingForward = false;
    private boolean movingBackward = false;
    private boolean movingLeft = false;
    private boolean movingRight = false;
    private boolean shooting = false;

    private Paint textPaint = new Paint();
    private Paint buttonPaint = new Paint();

    public GameView(Context context, SoundManager soundManager) {
        super(context);
        this.soundManager = soundManager;
        initPaints();
        initWorld();
    }

    private void initPaints() {
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(50);
        textPaint.setAntiAlias(true);
        buttonPaint.setColor(Color.argb(120, 255, 255, 255));
    }

    private void initWorld() {
        // Игрок (куб, если нет модели)
        Node player = new Node();
        player.setRenderable(ShapeFactory.makeCube(new Vector3(1f, 1.8f, 1f), new Vector3(0, 0.9f, 0), null));
        player.setLocalPosition(new Vector3(0, 0, 0));
        getScene().addChild(player);
        playerNode = player;

        // Враг (красный куб)
        Node cop = new Node();
        cop.setRenderable(ShapeFactory.makeCube(new Vector3(1f, 1.8f, 1f), new Vector3(0, 0.9f, 0), null));
        cop.setLocalPosition(new Vector3(5f, 0, -5f));
        getScene().addChild(cop);
        copNode = cop;

        // Дом (большой серый куб)
        Node house = new Node();
        house.setRenderable(ShapeFactory.makeCube(new Vector3(5f, 5f, 5f), new Vector3(0, 2.5f, 0), null));
        house.setLocalPosition(new Vector3(-10f, 0, -10f));
        getScene().addChild(house);

        updateCamera();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        running = true;
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int f, int w, int h) {}

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
            postInvalidate();
            long elapsed = System.currentTimeMillis() - start;
            long sleep = 16 - elapsed;
            if (sleep > 0) {
                try { Thread.sleep(sleep); } catch (InterruptedException ignored) {}
            }
        }
    }

    private void update() {
        float speed = 0.1f;
        if (movingForward) playerZ -= speed;
        if (movingBackward) playerZ += speed;
        if (movingLeft) playerX -= speed;
        if (movingRight) playerX += speed;

        if (playerNode != null) {
            playerNode.setLocalPosition(new Vector3(playerX, 0f, playerZ));
            updateCamera();
        }

        if (shooting) {
            shooting = false;
            if (copNode != null) {
                Vector3 copPos = copNode.getWorldPosition();
                float dist = Vector3.subtract(copPos, new Vector3(playerX, 0f, playerZ)).length();
                if (dist < 30f) {
                    soundManager.playFire();
                }
            }
        }
    }

    private void updateCamera() {
        Camera camera = getScene().getCamera();
        if (camera != null) {
            camera.setLocalPosition(new Vector3(playerX, 3f, playerZ + 6f));
            camera.setLocalRotation(new Quaternion(new Vector3(1f, 0f, 0f), -20f));
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawButton(canvas, 150, getHeight() - 200, 100, "◀");
        drawButton(canvas, 350, getHeight() - 200, 100, "▶");
        drawButton(canvas, getWidth() - 350, getHeight() - 200, 100, "▲");
        drawButton(canvas, getWidth() - 150, getHeight() - 200, 100, "▼");
        drawButton(canvas, getWidth() - 150, getHeight() - 450, 100, "🔥");
        canvas.drawText("Патроны: 30", 50, 80, textPaint);
    }

    private void drawButton(Canvas c, float cx, float cy, float r, String label) {
        c.drawCircle(cx, cy, r, buttonPaint);
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
            if (dist(x, y, 150, getHeight() - 200) < 120) movingLeft = true;
            if (dist(x, y, 350, getHeight() - 200) < 120) movingRight = true;
            if (dist(x, y, getWidth() - 350, getHeight() - 200) < 120) movingForward = true;
            if (dist(x, y, getWidth() - 150, getHeight() - 200) < 120) movingBackward = true;
            if (dist(x, y, getWidth() - 150, getHeight() - 450) < 150) shooting = true;
        } else if (e.getAction() == MotionEvent.ACTION_UP) {
            movingLeft = movingRight = movingForward = movingBackward = false;
        }
        return true;
    }

    private float dist(float x1, float y1, float x2, float y2) {
        return (float) Math.hypot(x1 - x2, y1 - y2);
    }
}
