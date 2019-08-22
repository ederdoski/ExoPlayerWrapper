package com.ederdoski.exoplayerwrapper;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.ederdoski.exoplayerwrapper.components.ExoPlayerComponent;
import com.ederdoski.exoplayerwrapper.interfaces.ExoPlayerComponetInterface;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.Logger;

public class MainActivity extends AppCompatActivity implements ExoPlayerComponetInterface{

    ExoPlayerComponent exoPlayerComponent;

    @Override
    protected void onStart() {
        super.onStart();
        exoPlayerComponent.onStartPlayer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        exoPlayerComponent.onResumePlayer();
    }

    @Override
    public void onPause() {
        super.onPause();
        exoPlayerComponent.onPausePlayer();
    }

    @Override
    public void onStop() {
        super.onStop();
        exoPlayerComponent.onStopPlayer();
    }

    @Override
    public void onCustomTrackingProgress() {
        Logger.e("onCustomTrackingProgress");
    }

    @Override
    public void onTrackingTenPercent() {
        Logger.e("onTrackingTenPercent");
    }

    @Override
    public void onTrackingFirstQuartil() {
        Logger.e("onTrackingFirstQuartil");
    }

    @Override
    public void onTrackingMidPoint() {
        Logger.e("onTrackingMidPoint");
    }

    @Override
    public void onTrackingThirdQuartil() {
        Logger.e("onTrackingThirdQuartil");
    }

    @Override
    public void onTrackingComplete() {
        Logger.e("onTrackingComplete");
    }

    @Override
    public void onTrackingEnded() {
        Logger.e("onTrackingEnded");
    }

    @Override
    public void onPauseTap() {
        Logger.e("Pause tap");
    }

    @Override
    public void onPlayTap() {
        Logger.e("Play tap");
    }

    @Override
    public void onVisibilityChanged(boolean isVisible) {
        Logger.e("Visibility of Controls: " + isVisible);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_player);
        Logger.addLogAdapter(new AndroidLogAdapter());

        PlayerView playerView = findViewById(R.id.exoPlayer);

        String videoURI      = "https://storage.googleapis.com/wvmedia/cenc/h264/tears/tears.mpd";
        String drmScheme     = "widevine";
        String drmLicenseURI = "https://proxy.uat.widevine.com/proxy?video_id=d286538032258a1c&provider=widevine_test";
        String extension     = "mpd";

        //---- Initialization For content with DRM without reports

        //exoPlayerComponent = new ExoPlayerComponent(this, videoURI, drmLicenseURI, drmScheme, extension, false, playerView);

        //---- Initialization for content without DRM without reports
        //exoPlayerComponent = new ExoPlayerComponent(this, videoURI, null, null, extension, false, playerView);

        //---- Initialization for content without DRM  with Tracking events in the player
        exoPlayerComponent = new ExoPlayerComponent(this, videoURI, drmLicenseURI, drmScheme, extension, true, playerView);
        exoPlayerComponent.eventTimeListener(10);

    }

}
