package com.example.mediacodecplayer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.IOException;

public class MediaCodecPlayerActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_STORAGE = 100;
    String TAG = "SampleActivity";
    private Mp4Player player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_media_codec_player);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        requestStoragePermission();
        SurfaceView surfaceView = findViewById(R.id.surfaceView);
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                new Thread(() -> {
                    AssetFileDescriptor afd = getResources().openRawResourceFd(R.raw.sample);

                    try {
                        player = new Mp4Player("/sdcard/sample.mp4", holder.getSurface());
                        player.play(afd);
                        afd.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            }

            @Override
            public void surfaceChanged(SurfaceHolder h, int f, int w, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder h) {
            }
        });
        findViewById(R.id.btnPlay).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (player.isPaused()) {
                    player.resume();
                } else {
                    player.pause();
                }

            }
        });
        findViewById(R.id.forward).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        findViewById(R.id.backward).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ — use granular media permissions
            String[] permissions = {
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.READ_MEDIA_IMAGES
            };
            if (!hasPermissions(permissions)) {
                ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_STORAGE);
            } else {
                onPermissionGranted();
            }

        } else {
            // Android 12 and below
            String[] permissions = {
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
            if (!hasPermissions(permissions)) {
                ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_STORAGE);
            } else {
                onPermissionGranted();
            }
        }
    }

    private boolean hasPermissions(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_STORAGE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                onPermissionGranted();
            } else {
                onPermissionDenied();
            }
        }
    }

    private void onPermissionGranted() {
        // Start your Mp4Player here
        Log.d(TAG, "onPermissionGranted: ");
    }

    private void onPermissionDenied() {
        // Show rationale or disable features
        Log.d(TAG, "onPermissionGranted: ");

    }
}