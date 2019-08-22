package com.ederdoski.exoplayerwrapper.interfaces;

public interface ExoPlayerComponetInterface {

    // --- Report Events

    public void onCustomTrackingProgress();
    public void onTrackingTenPercent();
    public void onTrackingFirstQuartil();
    public void onTrackingMidPoint();
    public void onTrackingThirdQuartil();
    public void onTrackingComplete();
    public void onTrackingEnded();

    public void onPauseTap();
    public void onPlayTap();
    public void onVisibilityChanged(boolean isVisible);

}
