package edu.delta.aspin.rpptglass;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;

import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;

import im.delight.android.ddp.MeteorCallback;
import im.delight.android.ddp.MeteorSingleton;
import im.delight.android.ddp.ResultListener;

public class MainActivity extends Activity implements MeteorCallback {

    private static final String TAG = "RPPT MainActivity";
    private static final String METEOR_URL = "ws://rppt.meteorapp.com/websocket";
    private static final Integer QR_CODE_MODE = 0;

    private GestureDetector mGestureDetector;

    //region Activity Creation Lifecyle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupGestureDetector();
        setupMeteorClient();
    }

    private void setupMeteorClient() {
        if (!MeteorSingleton.hasInstance()) {
            MeteorSingleton.createInstance(this, METEOR_URL);
            MeteorSingleton.getInstance().setCallback(this);
        } else {
            MeteorSingleton.getInstance().reconnect();
        }
    }

    private void setupGestureDetector() {
        mGestureDetector = new GestureDetector(this);
        mGestureDetector.setBaseListener(new GestureDetector.BaseListener() {
            @Override
            public boolean onGesture(Gesture gesture) {
                switch (gesture) {
                    case TAP:
                        openOptionsMenu();
                        return true;
                    default:
                        return false;
                }
            }
        });
        mGestureDetector.setFingerListener(new GestureDetector.FingerListener() {
            @Override
            public void onFingerCountChanged(int previousCount, int currentCount) {
            }
        });
        mGestureDetector.setScrollListener(new GestureDetector.ScrollListener() {
            @Override
            public boolean onScroll(float displacement, float delta, float velocity) {
                return true;
            }
        });
    }
    //endregion

    //region Menu Management
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.scan:
                Intent intent = new Intent("com.google.zxing.client.android.SCAN");
                intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
                startActivityForResult(intent, QR_CODE_MODE);
                return true;
            case R.id.quit:
                cleanUp();
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void cleanUp() {
        if (MeteorSingleton.hasInstance()) {
            MeteorSingleton.getInstance().disconnect();
        }
    }
    //endregion

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        return mGestureDetector != null && mGestureDetector.onMotionEvent(event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == QR_CODE_MODE) {
            String syncCode = data.getStringExtra("SCAN_RESULT");
            Log.v(TAG, String.format("Scan result: %s", syncCode));
            Intent intent = new Intent(this, StreamActivity.class);
            intent.putExtra("syncCode", syncCode);
            startActivity(intent);
        }
    }

    //region MeteorCallback Interface Methods
    @Override
    public void onConnect(boolean signedInAutomatically) {
        Log.v(TAG, String.format("Connected to Meteor server at: %s", METEOR_URL));
    }

    @Override
    public void onDisconnect(int code, String reason) {
        Log.v(TAG, String.format("Disconnected from Meteor server at: %s. Reason: %s", METEOR_URL, reason));
    }

    @Override
    public void onDataAdded(String collectionName, String documentID, String newValuesJson) {

    }

    @Override
    public void onDataChanged(String collectionName, String documentID, String updatedValuesJson, String removedValuesJson) {

    }

    @Override
    public void onDataRemoved(String collectionName, String documentID) {

    }

    @Override
    public void onException(Exception e) {
        Log.e(TAG, "Exception thrown: ", e);
    }
    //endregion
}
