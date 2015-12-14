package edu.delta.aspin.rpptglass;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.opentok.android.BaseVideoRenderer;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;
import com.opentok.android.SubscriberKit;

import java.lang.reflect.Type;
import java.util.Map;

import im.delight.android.ddp.MeteorSingleton;
import im.delight.android.ddp.ResultListener;

public class StreamActivity extends Activity implements Session.SessionListener, Publisher.PublisherListener, Subscriber.SubscriberListener {

    private static final String TAG = "RPPT StreamActivity";

    private String SYNC_CODE = "";

    private Session mSession;
    private Publisher mPublisher;
    private RelativeLayout mPublisherViewContainer;
    private Boolean mSessionIsConnected = false;

    private GestureDetector mGestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stream);
        setupGestureDetector();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Intent intent = getIntent();
        SYNC_CODE = intent.getStringExtra("syncCode");
        connectAndStartStream();
    }

    private void connectAndStartStream() {
        Log.v(TAG, String.format("Searching for stream %s", SYNC_CODE));
        MeteorSingleton.getInstance().call(
                "getStreamData",
                new Object[]{ SYNC_CODE, "publisher" },
                new ResultListener() {
                    @Override
                    public void onSuccess(String result) {
                        Log.v(TAG, String.format("Got result: %S", result));
                        Type type = new TypeToken<Map<String, String>>() {
                        }.getType();
                        Gson gson = new Gson();
                        Map<String, String> credentials = gson.fromJson(result, type);
                        startStream(credentials);
                    }

                    @Override
                    public void onError(String error, String reason, String details) {
                        // TODO: Handle this, usually results from no matching key
                        Log.w(TAG, String.format("Error: %s", error));
                        Log.w(TAG, String.format("Reason: %s", reason));
                        Log.w(TAG, String.format("Details: %s", details));
                    }
                }
        );
    }

    private void startStream(Map<String, String> credentials) {
        mPublisherViewContainer = (RelativeLayout) findViewById(R.id.publisher_view);
        if (mSession == null) {
            mSession = new Session(this, credentials.get("key"), credentials.get("session"));
            mSession.setSessionListener(this);
        }
        mSession.connect(credentials.get("token"));
    }


    @Override
    protected void onPause() {
        super.onPause();
        mSession.disconnect();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.stream, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
        if (mSessionIsConnected) {
            menu.getItem(0).setEnabled(false);
            menu.getItem(1).setEnabled(true);
        } else {
            menu.getItem(1).setEnabled(false);
            menu.getItem(0).setEnabled(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.resume:
                connectAndStartStream();
                return true;
            case R.id.stop:
                mSession.disconnect();
                return true;
            case R.id.quit:
                mSession.disconnect();
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        return mGestureDetector != null && mGestureDetector.onMotionEvent(event);
    }

    //region OpenTok Interface Methods
    @Override
    public void onStreamCreated(PublisherKit publisherKit, Stream stream) { }

    @Override
    public void onStreamDestroyed(PublisherKit publisherKit, Stream stream) { }

    @Override
    public void onError(PublisherKit publisherKit, OpentokError opentokError) { }

    @Override
    public void onConnected(Session session) {
        mSessionIsConnected = true;
        if (mPublisher == null) {
            mPublisher = new Publisher(this, "publisher");
            mPublisher.setPublisherListener(this);

            mPublisher.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(480, 320);
            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
            mPublisherViewContainer.addView(mPublisher.getView(), layoutParams);

            mSession.publish(mPublisher);
        }
    }

    @Override
    public void onDisconnected(Session session) {
        mSessionIsConnected = false;
        if (mPublisher != null) {
            mPublisherViewContainer.removeView(mPublisher.getView());
        }
        mPublisher = null;
    }

    @Override
    public void onStreamReceived(Session session, Stream stream) { }

    @Override
    public void onStreamDropped(Session session, Stream stream) { }

    @Override
    public void onError(Session session, OpentokError opentokError) { }

    @Override
    public void onConnected(SubscriberKit subscriberKit) { }

    @Override
    public void onDisconnected(SubscriberKit subscriberKit) { }

    @Override
    public void onError(SubscriberKit subscriberKit, OpentokError opentokError) { }
    //endregion
}
