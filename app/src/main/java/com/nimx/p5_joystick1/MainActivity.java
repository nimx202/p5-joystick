package com.nimx.p5_joystick1;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private ImageView joystick;
    private float centerX;
    private float centerY;
    private boolean isTouched = false;

    private float initialX, maxRadius, initialY;
    private TextView degreeText;
    private TextView xCoordinateText;
    private TextView yCoordinateText;

    private Handler sendDataHandler;
    private int intervalMillis = 500;

    private static final float INTERPOLATION_FACTOR = 0.2f;
    private float targetX = centerX;
    private float targetY = centerY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        joystick = findViewById(R.id.joystick);
        degreeText = findViewById(R.id.degreeText);
        xCoordinateText = findViewById(R.id.xCoordinateText);
        yCoordinateText = findViewById(R.id.yCoordinateText);

        joystick.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                centerX = joystick.getX() + joystick.getWidth() / 2;
                centerY = joystick.getY() + joystick.getHeight() / 2;

                initialX = centerX - joystick.getWidth() / 2;
                initialY = centerY - joystick.getHeight() / 2;

                maxRadius = joystick.getWidth() / 2;

                joystick.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        sendDataHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                float currentDegree = getCurrentDegree();
                float currentXCoordinate = joystick.getX() - centerX + joystick.getWidth() / 2;
                float currentYCoordinate = joystick.getY() - centerY + joystick.getHeight() / 2;
                sendDegreeAndCoordinatesDataToServer(currentDegree, currentXCoordinate, currentYCoordinate);

                sendDataHandler.sendEmptyMessageDelayed(0, intervalMillis);
            }
        };
        sendDataHandler.sendEmptyMessage(0);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void handleJoystickTouch(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isTouched = true;
                break;

            case MotionEvent.ACTION_MOVE:
                if (isTouched) {
                    float targetDx = event.getX() - centerX;
                    float targetDy = event.getY() - centerY;

                    float dx = targetDx * INTERPOLATION_FACTOR;
                    float dy = targetDy * INTERPOLATION_FACTOR;

                    targetX = centerX + dx;
                    targetY = centerY + dy;

                    joystick.setX(targetX - joystick.getWidth() / 2);
                    joystick.setY(targetY - joystick.getHeight() / 2);

                    float angle = (float) Math.atan2(dy, dx);
                    float degree = (float) (angle * 180 / Math.PI);
                    degreeText.setText(String.format("%.2f", degree));

                    xCoordinateText.setText("X: " + String.format("%.2f", targetDx));
                    yCoordinateText.setText("Y: " + String.format("%.2f", targetDy));
                }
                break;

            case MotionEvent.ACTION_UP:
                isTouched = false;
                targetX = centerX;
                targetY = centerY;

                degreeText.setText("0.00");
                xCoordinateText.setText("X: 0.00");
                yCoordinateText.setText("Y: 0.00");
                break;
        }

        joystick.setX(targetX - joystick.getWidth() / 2);
        joystick.setY(targetY - joystick.getHeight() / 2);
    }
    private float getCurrentDegree() {
        float dx = joystick.getX() - centerX + joystick.getWidth() / 2;
        float dy = joystick.getY() - centerY + joystick.getHeight() / 2;
        double angle = Math.atan2(dy, dx);

        float degree = (float) (angle * 180 / Math.PI);

        while (degree < 0) {
            degree += 360;
        }
        while (degree > 360) {
            degree -= 360;
        }

        return degree;
    }

    private void sendDegreeAndCoordinatesDataToServer(float degree, float xCoordinate, float yCoordinate) {
        new SendDegreeAndCoordinatesDataTask().execute(degree, xCoordinate, yCoordinate);
    }

    private class SendDegreeAndCoordinatesDataTask extends AsyncTask<Float, Void, Void> {

        @Override
        protected Void doInBackground(Float... params) {
            try {
                String serverUrl = "https://p1.nimx.me:5000/";
                float degree = params[0];
                float xCoordinate = params[1];
                float yCoordinate = params[2];

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("degree", degree);
                jsonObject.put("xCoordinate", xCoordinate);
                jsonObject.put("yCoordinate", yCoordinate);

                String postData = jsonObject.toString();

                URL url = new URL(serverUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                OutputStream os = connection.getOutputStream();
                os.write(postData.getBytes());
                os.flush();
                os.close();

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d("HTTP POST", "Data sent successfully");
                } else {
                    Log.e("HTTP POST", "Error sending data. Response code: " + responseCode);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        handleJoystickTouch(event);
        return true;
    }
}
