# Android ExoPlayer Wrapper


## Introduction

Currently playing content using exoPlayer for newbies can be quite problematic since there is no suitable guide to perform the process and the example project is quite dense. If you want to skip all the technical part of ExoPlayer and go straight to the point this wrapper will be helpful.

Currently with this wrapper you can play the following contents:

* HLS (Without DRM)
* SmoothStreaming (Without DRM)
* DASH (Without DRM)
* DASH (With DRM)

Add the following in your app's build.gradle file:

```
defaultConfig {
  minSdkVersion 21
  ...
}

compileOptions {
  sourceCompatibility JavaVersion.VERSION_1_8
  targetCompatibility JavaVersion.VERSION_1_8
}

dependencies {
  implementation 'com.google.android.exoplayer:exoplayer:2.9.6'
}
```
## AndroidManifest

1) You need to request permissions to access bluetooth and gps
```
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

## Usage

1) Declare a variable ExoPlayerComponent

```
ExoPlayerComponent exoPlayerComponent;
```
2) Instance the following methods in your activity, these method are used to maintain the memory and protect the current instance of ExoPlayer.

```
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
```

Actually There are two ways to reproduce content.

* If your content have DRM 
```
exoPlayerComponent = new ExoPlayerComponent(this, videoURI, drmLicenseURI, drmScheme, extension, false, playerView);
```

* If your content dont have DRM
```
exoPlayerComponent = new ExoPlayerComponent(this, videoURI, null, null, extension, false, playerView);
```

3) Enjoy.

## Player events report

For some users it is necessary to track the position of the player, to make reports, Here are some events to make reports.
	
* when the player have reproduce 10% of video
```
@Override
public void onTrackingTenPercent() {}
```

* when the player have reproduce 25% of video
```
@Override
public void onTrackingFirstQuartil() {}
```

* when the player have reproduce 50% of video
```
@Override
public void onTrackingMidPoint() {}
```

* when the player have reproduce 75% of video
```
@Override
public void onTrackingThirdQuartil() {}
```

* when the player have reproduce 95% of video
```
@Override
public void onTrackingComplete() {}
```

* when the player have reproduce 100% of video
```
@Override
public void onTrackingEnded() {}
```

* Custom event for report in a indicate time
```
@Override
public void onCustomTrackingProgress() {}
```

* when the pause button is tap
```
@Override
public void onPauseTap() {}
```

* when the play button is tap
```
@Override
public void onPlayTap() {}
```

* Indicate if the bar of controls of the player is visible  
```
@Override
public void onVisibilityChanged(boolean isVisible) {}
```
## Report events Usage

for usage the report events you need implements ExoPlayerComponetInterface, once this is done, only import the interfaces to your activity. 

```
public class MainActivity extends AppCompatActivity implements ExoPlayerComponetInterface {
```

in your void onCreate call to the next method: 

```
exoPlayerComponent.eventTimeListener(10);
```

and when instantiating ExoPlayerComponent you must enable the reportEvents parameter
```
exoPlayerComponent = new ExoPlayerComponent(this, videoURI, drmLicenseURI, drmScheme, extension, true, playerView);
```

## URL for testing 

The ExoPlayer example include a serie of url with DRM and without DRM for testing.

* Dash without DRM 
```
{
    "name": "WV: Clear SD & HD (MP4,H264)",
    "uri": "https://storage.googleapis.com/wvmedia/clear/h264/tears/tears.mpd"
    "extension": "mpd"
}
```

* Dash with DRM 
```
{
    "name": "WV: HDCP not specified",
    "uri": "https://storage.googleapis.com/wvmedia/cenc/h264/tears/tears.mpd",
    "drm_scheme": "widevine",
    "drm_license_url": "https://proxy.uat.widevine.com/proxy?video_id=d286538032258a1c&provider=widevine_test"
    "extension": "mpd"
}
```

* HLS without DRM
```
{
    "name": "Apple 4x3 basic stream",
    "uri": "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_4x3/bipbop_4x3_variant.m3u8"
    "extension": "meu8"
}
```

## Aditional

 * String videoURI  --> Is the url that includes the manifest to play the video either ".m3u8" or ".mpd"
 * String drmScheme --> Indicates the type of encryption only for DRM content "widevine", "playready", "clearkey".
 * String extension --> Indicates the extension of content to play.
 * String drmLicenseURI --> Indicates the URL where the license to play the content is.


## References

* [ExoPlayer](https://github.com/google/ExoPlayer) - ExoPlayer repositorie GitHub
* [ExoPlayer Components](https://android.jlelse.eu/exoplayer-components-explained-9937e3a5d2f5) - Introduction to Exoplayer Components

## License

This code is open-sourced software licensed under the [MIT license.](https://opensource.org/licenses/MIT)
