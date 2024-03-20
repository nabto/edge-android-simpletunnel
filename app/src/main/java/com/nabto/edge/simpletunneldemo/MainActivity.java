package com.nabto.edge.simpletunneldemo;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;

import android.os.Bundle;

import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.LoadControl;
import androidx.media3.exoplayer.rtsp.RtspMediaSource;
import androidx.media3.ui.PlayerView;

import com.nabto.edge.client.Connection;
import com.nabto.edge.client.ConnectionEventsCallback;
import com.nabto.edge.client.NabtoClient;
import com.nabto.edge.client.TcpTunnel;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private NabtoClient nabtoClient;
    private Connection deviceConnection;
    private TcpTunnel deviceTunnel;

    // FILL IN YOUR DEVICE SETTINGS HERE!!
    // You can get the info from the Nabto Cloud Console and then fill out these strings.
    private String productId = "pr-xxxxxxxx";
    private String deviceId = "de-xxxxxxxx";
    private String serverConnectToken = "demosct";

    // We will open a tunnel service to access an RTSP stream.
    private String tunnelService = "rtsp";

    // This LiveData will be updated with a url that our video player can use to display the livestream.
    private MutableLiveData<String> rtspUrl = new MutableLiveData<>();

    private ExoPlayer exoPlayer;

    // This listener will be called when the connection to your device is connected or is closed.
    private ConnectionEventsCallback deviceConnectionListener = new ConnectionEventsCallback() {
        @Override
        public void onEvent(int event) {
            if (event == CONNECTED) {
                onDeviceConnected();
            } else if (event == CLOSED) {
                // do something if the connection has closed...
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Let's first set up ExoPlayer to play a video, this is _NOT_ a part of Nabto!
        setupExoPlayer();

        // Create a NabtoClient using our activity as context.
        // You should create one NabtoClient and keep it throughout the application's lifetime.
        nabtoClient = NabtoClient.create(this);

        // Now we make a private key for this client app.
        // You should save this private key in for example shared preferences or a database
        // A device will need this key to recognize the client.
        String clientPrivateKey = nabtoClient.createPrivateKey();

        // The settings for the connection are passed as JSON
        JSONObject connectionOptions = new JSONObject();
        try {
            connectionOptions.put("ProductId", productId);
            connectionOptions.put("DeviceId", deviceId);
            connectionOptions.put("PrivateKey", clientPrivateKey);
            connectionOptions.put("ServerConnectToken", serverConnectToken);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Now we create a connection object.
        deviceConnection = nabtoClient.createConnection();

        // Fill it with our settings and set our listener
        deviceConnection.updateOptions(connectionOptions.toString());
        deviceConnection.addConnectionEventsListener(deviceConnectionListener);

        // Now we're finally ready to connect to our device.
        // Note that doing this in onCreate of an activity is _VERY BAD_ as it is blocking
        // the main thread. We're doing it here to keep everything simple and easy to understand.
        // You should put this on a separate thread, or use connectCallback().
        // In Kotlin you can use coroutines and awaitConnect() which is the preferred method.
        deviceConnection.connect();
    }

    @OptIn(markerClass = UnstableApi.class) @Override
    protected void onStart() {
        super.onStart();

        PlayerView videoPlayerView = findViewById(R.id.video_player);
        videoPlayerView.setPlayer(exoPlayer);
        videoPlayerView.setUseController(false);

        rtspUrl.observe(this, url -> {
            MediaItem.LiveConfiguration config = new MediaItem.LiveConfiguration.Builder()
                    .setTargetOffsetMs(300)
                    .setMaxPlaybackSpeed(1.04f)
                    .build();

            MediaItem mediaItem = new MediaItem.Builder()
                    .setUri(url)
                    .setLiveConfiguration(config)
                    .build();

            RtspMediaSource mediaSource = new RtspMediaSource.Factory()
                    .setForceUseRtpTcp(true)
                    .createMediaSource(mediaItem);

            exoPlayer.setMediaSource(mediaSource);
            exoPlayer.setPlayWhenReady(true);
            exoPlayer.prepare();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Shut down the connection as we're done with it now.
        deviceConnection.close();
    }

    @OptIn(markerClass = UnstableApi.class) private void setupExoPlayer() {
        LoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(1000, 2000, 1000, 1000)
                .build();

        exoPlayer = new ExoPlayer.Builder(this)
                .setLoadControl(loadControl)
                .build();
    }

    private void onDeviceConnected() {
        // Our device is connected, let's make a TCP tunnel
        deviceTunnel = deviceConnection.createTcpTunnel();
        deviceTunnel.open(tunnelService, 0);

        // The tunnel will expose RTSP on local host, so our url for the rtsp stream is
        // rtsp://127.0.0.1:xxxx where xxxx is the tunnel's port
        // We add /video at the end because that's where our endpoint is, this may not be
        // the case for your camera, in which case you need to change this part.
        String url = "rtsp://127.0.0.1:" + deviceTunnel.getLocalPort() + "/video";
        rtspUrl.postValue(url);
    }
}