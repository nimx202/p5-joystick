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

    private Handler sendDataHandler; // Handler to send data
    private int intervalMillis = 500; // Interval in milliseconds

    // Smoother joystick movement
    private static final float INTERPOLATION_FACTOR = 0.2f; // Adjust this value to control smoothness
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

        // Add a layout listener to the joystick ImageView
        joystick.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // The view has been laid out, and its dimensions are available here
                // Initialize joystick center, initial position, and maximum radius here
                centerX = joystick.getX() + joystick.getWidth() / 2;
                centerY = joystick.getY() + joystick.getHeight() / 2;

                initialX = centerX - joystick.getWidth() / 2;
                initialY = centerY - joystick.getHeight() / 2;

                maxRadius = joystick.getWidth() / 2;

                // Remove the listener to avoid repeated calls
                joystick.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        // Initialize the sendDataHandler
        sendDataHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                // Get the current degree, x coordinate, and y coordinate and send them to the server
                float currentDegree = getCurrentDegree();
                float currentXCoordinate = joystick.getX() - centerX + joystick.getWidth() / 2;
                float currentYCoordinate = joystick.getY() - centerY + joystick.getHeight() / 2;
                sendDegreeAndCoordinatesDataToServer(currentDegree, currentXCoordinate, currentYCoordinate);

                // Schedule the next execution
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
                    // Calculate the target position based on touch coordinates
                    float targetDx = event.getX() - centerX;
                    float targetDy = event.getY() - centerY;

                    // Apply interpolation to gradually move towards the target
                    float dx = targetDx * INTERPOLATION_FACTOR;
                    float dy = targetDy * INTERPOLATION_FACTOR;

                    // Update the joystick's target position
                    targetX = centerX + dx;
                    targetY = centerY + dy;

                    // Update the joystick position
                    joystick.setX(targetX - joystick.getWidth() / 2);
                    joystick.setY(targetY - joystick.getHeight() / 2);

                    // Update the degree text
                    float angle = (float) Math.atan2(dy, dx);
                    float degree = (float) (angle * 180 / Math.PI);
                    degreeText.setText(String.format("%.2f", degree));

                    // Update the x and y coordinate text
                    xCoordinateText.setText("X: " + String.format("%.2f", targetDx));
                    yCoordinateText.setText("Y: " + String.format("%.2f", targetDy));
                }
                break;

            case MotionEvent.ACTION_UP:
                isTouched = false;
                // Gradually reset the joystick to its center position
                targetX = centerX;
                targetY = centerY;

                // Reset the degree and coordinates texts
                degreeText.setText("0.00");
                xCoordinateText.setText("X: 0.00");
                yCoordinateText.setText("Y: 0.00");
                break;
        }

        // Update the joystick position even when not touched to allow it to return to center smoothly
        joystick.setX(targetX - joystick.getWidth() / 2);
        joystick.setY(targetY - joystick.getHeight() / 2);
    }
    private float getCurrentDegree() {
        // Calculate and return the current degree based on the joystick position
        float dx = joystick.getX() - centerX + joystick.getWidth() / 2;
        float dy = joystick.getY() - centerY + joystick.getHeight() / 2;
        double angle = Math.atan2(dy, dx);

        // Convert the angle to degrees
        float degree = (float) (angle * 180 / Math.PI);

        // Ensure the degree is within 0 to 360 degrees
        while (degree < 0) {
            degree += 360;
        }
        while (degree > 360) {
            degree -= 360;
        }

        return degree;
    }

    private void sendDegreeAndCoordinatesDataToServer(float degree, float xCoordinate, float yCoordinate) {
        // Code to send the degree, x coordinate, and y coordinate data to the server
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

                // Create a JSON object with degree, x coordinate, and y coordinate
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

                // Send the JSON data
                OutputStream os = connection.getOutputStream();
                os.write(postData.getBytes());
                os.flush();
                os.close();

                // Check the response code if needed
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Data sent successfully
                    Log.d("HTTP POST", "Data sent successfully");
                } else {
                    // Handle the error
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
