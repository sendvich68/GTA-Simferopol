package com.simferopol.gta;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import com.google.ar.sceneform.SceneView;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.Node;

public class MainActivity extends Activity {
    private SceneView sceneView;
    private SoundManager soundManager;
    private Node playerNode;

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

        // Настройка камеры от 3-го лица
        sceneView.getCamera().setLocalPosition(new Vector3(0f, 2.5f, 5f));
        sceneView.getCamera().setLocalRotation(new com.google.ar.sceneform.math.Quaternion(
            new Vector3(1f, 0f, 0f), -20f
        ));

        // Загрузка модели CJ
        Uri modelUri = Uri.parse("cj.glb");
        ModelRenderable.builder()
                .setSource(this, modelUri)
                .build()
                .thenAccept(renderable -> {
                    playerNode = new Node();
                    playerNode.setRenderable(renderable);
                    playerNode.setLocalPosition(new Vector3(0f, 0f, 0f));
                    playerNode.setLocalScale(new Vector3(1.5f, 1.5f, 1.5f)); // Размер персонажа
                    sceneView.getScene().addChild(playerNode);
                })
                .exceptionally(throwable -> {
                    return null;
                });
    }

    @Override
    protected void onPause() {
        super.onPause();
        sceneView.pause();
        soundManager.pauseAll();
    }

    @Override
    protected void onResume() {
        super.onResume();
        sceneView.resume();
        soundManager.resumeAll();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        soundManager.release();
    }
}
