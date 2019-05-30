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
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;

public class MainActivity extends AppCompatActivity implements PlayerControlView.VisibilityListener {

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
    public void onVisibilityChange(int visibility) {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_player);

        PlayerView playerView = findViewById(R.id.exoPlayer);

        String videoURI      = "https://storage.googleapis.com/wvmedia/cenc/h264/tears/tears.mpd";
        String drmScheme     = "widevine";
        String drmLicenseURI = "https://proxy.uat.widevine.com/proxy?video_id=d286538032258a1c&provider=widevine_test";
        String extension     = "mpd";

        //---- Initialization For content with DRM

        exoPlayerComponent = new ExoPlayerComponent(this, videoURI, drmLicenseURI, drmScheme, extension, playerView, this);

        //---- Initialization for content without DRM
        //exoPlayerComponent = new ExoPlayerComponent(this, videoURI, null, null, extension, playerView, this);

    }

}
