package com.doowzs.tank;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Locale;
import java.util.UUID;

public class GameActivity extends AppCompatActivity implements SensorEventListener {
    protected String addr;
    protected Integer port;
    protected String username;
    protected UUID uuid;

    protected Socket socket = null;
    protected BufferedReader in = null;
    protected PrintWriter out = null;
    protected Vibrator vibrator = null;

    protected TextView uuidLabel;
    protected TextView remoteLabel;
    protected TextView localLabel;
    protected Button backButton;
    protected Button shootButton;
    protected Button accelButton;

    private InitTask initTask;
    private SendTask sendTask;
    private ReadTask readTask;

    private SensorManager manager;
    private Sensor accelerometer;
    private Sensor magnetometer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        int flag = WindowManager.LayoutParams.FLAG_FULLSCREEN;
        getWindow().setFlags(flag, flag);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_game);

        Intent intent = getIntent();
        addr = intent.getStringExtra("addr");
        port = intent.getIntExtra("port", 10086);
        username = intent.getStringExtra("username");

        uuidLabel = findViewById(R.id.UUIDLabel);
        remoteLabel = findViewById(R.id.RemoteStatusLabel);
        localLabel = findViewById(R.id.LocalStatusLabel);
        backButton = findViewById(R.id.BackButton);
        shootButton = findViewById(R.id.ShootButton);
        accelButton = findViewById(R.id.AccelButton);

        manager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
    }

    @Override
    protected void onStart() {
        super.onStart();

        initTask = new InitTask();
        initTask.execute();
    }

    @Override
    protected void onResume() {
        super.onResume();
        manager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        manager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        manager.unregisterListener(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (readTask != null) readTask.cancel(true);
        if (sendTask != null) sendTask.cancel(true);
        if (initTask != null) initTask.cancel(true);
        try {
            if (socket != null) {
                in.close();
                in = null;
                out.close();
                out = null;
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    protected float[] orientation = new float[3];
    protected float[] gravity;
    protected float[] magnetic;
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            gravity = event.values;
        }
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            magnetic = event.values;
        }
        if (gravity != null && magnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            if (SensorManager.getRotationMatrix(R, I, gravity, magnetic)) {
                SensorManager.getOrientation(R, orientation);
            }
        }
    }

    private class InitTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                socket = new Socket(addr, port);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                out.println("CON<" + username);
                System.out.println("Connection established, waiting for UUID.");
                uuid = UUID.fromString(in.readLine().split("<")[1]);
                System.out.println("UUID is " + uuid.toString());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        uuidLabel.setText(uuid.toString());
                    }
                });

                sendTask = new SendTask();
                readTask = new ReadTask();
                sendTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                readTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                System.out.println("Send task and read task created.");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private class SendTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                while (out != null) {
                    Thread.sleep(20);

                    final String cmd = getCommand();
                    System.out.println("Command: " + cmd);
                    out.println(cmd);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            localLabel.setText(cmd);
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        private String getCommand() {
            StringBuilder sb = new StringBuilder("ACT");
            if (backButton.isPressed()) {
                sb.append("<BAK");
            } else if (accelButton.isPressed()) {
                sb.append("<ACC");
            } else if (shootButton.isPressed()) {
                sb.append("<SHT");
            } else {
                sb.append("<NUL");
            }

            double deg = Math.toDegrees(orientation[1]);
            if (deg < 0) {
                sb.append("<NEG<");
                sb.append(String.format(Locale.ENGLISH, "%03d", ((int) (-deg))));
            } else {
                sb.append("<POS<");
                sb.append(String.format(Locale.ENGLISH, "%03d", ((int) (deg))));
            }
            return sb.toString();
        }
    }

    private class ReadTask extends AsyncTask<Void, Void, Void> {
        int x, y;

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                while (in != null) {
                    final String res = in.readLine();
                    final String[] parts = res.split("<");
                    if (parts.length > 3) {
                        int nx = Integer.valueOf(parts[2]);
                        int ny = Integer.valueOf(parts[3]);
                        if (Math.abs(nx - x) > 5 || Math.abs(ny - y) > 5) {
                            // get hit by a bullet and reset position
                            System.out.println("Got hit!");
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vibrator.vibrate(VibrationEffect.createOneShot(200, 50));
                            } else {
                                vibrator.vibrate(200);
                            }
                        }
                        x = nx;
                        y = ny;
                    }
                    System.out.println("Response: " + res);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            remoteLabel.setText(res);
                        }
                    });
                }
            } catch (Exception e) {
                System.out.println(e.toString());
            }
            return null;
        }
    }
}
