package com.simferopol.gta;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.SceneView;
import com.google.ar.sceneform.animation.ModelAnimator;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;

public class MainActivity extends Activity {
    private SceneView sceneView;
    private SoundManager soundManager;
    private Node playerNode;
    private ModelAnimator animator;

    // Координаты игрока
    private float playerX = 0f;
    private float playerZ = 0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        soundManager = new SoundManager(this);
        soundManager.init();

        sceneView = new SceneView(this);
        setContentView(sceneView);

        // Загрузка модели CJ
        Uri modelUri = Uri.parse("cj.glb");
        ModelRenderable.builder()
                .setSource(this, modelUri)
                .build()
                .thenAccept(renderable -> {
                    playerNode = new Node();
                    playerNode.setRenderable(renderable);
                    playerNode.setLocalPosition(new Vector3(playerX, 0f, playerZ));
                    playerNode.setLocalScale(new Vector3(1.5f, 1.5f, 1.5f)); // Размер
                    sceneView.getScene().addChild(playerNode);

                    // Запуск анимаций, если они есть в модели
                    if (renderable.getAnimationDataCount() > 0) {
                        animator = new ModelAnimator(renderable, 0);
                        animator.setLooping(true); // Зацикливаем (например, ходьба)
                        animator.start();
                    }

                    // Настраиваем камеру позади игрока
                    updateCamera();
                })
                .exceptionally(throwable -> {
                    return null; // Не крашимся, если модель не загрузилась
                });

        // Добавляем сенсорное управление
        sceneView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_MOVE || event.getAction() == MotionEvent.ACTION_DOWN) {
                    float x = event.getX();
                    // Если тапнули в левой части экрана — идем влево, в правой — вправо
                    if (x < sceneView.getWidth() / 3f) {
                        playerX -= 0.1f;
                    } else if (x > sceneView.getWidth() * 2f / 3f) {
                        playerX += 0.1f;
                    } else {
                        playerZ -= 0.1f; // Вперед (по оси Z)
                    }
                    
                    if (playerNode != null) {
                        playerNode.setLocalPosition(new Vector3(playerX, 0f, playerZ));
                        updateCamera();
                    }
                    return true;
                }
                return false;
            }
        });
    }

    // Обновление позиции камеры, чтобы она смотрела на игрока
    private void updateCamera() {
        if (sceneView.getCamera() != null) {
            // Камера в 5 метрах сзади и на 2.5 метра выше
            sceneView.getCamera().setLocalPosition(new Vector3(playerX, 2.5f, playerZ + 5f));
            // Наклоняем камеру вниз
            sceneView.getCamera().setLocalRotation(new Quaternion(new Vector3(1f, 0f, 0f), -20f));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sceneView.pause();
        soundManager.pauseAll();
        if (animator != null) animator.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        sceneView.resume();
        soundManager.resumeAll();
        if (animator != null && !animator.isRunning()) animator.resume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        soundManager.release();
    }
}
