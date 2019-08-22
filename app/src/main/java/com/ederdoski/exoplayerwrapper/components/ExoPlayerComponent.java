package com.ederdoski.exoplayerwrapper.components;

import android.app.Activity;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import com.ederdoski.exoplayerwrapper.Constants;
import com.ederdoski.exoplayerwrapper.R;
import com.ederdoski.exoplayerwrapper.interfaces.ExoPlayerComponetInterface;
import com.google.android.exoplayer2.BuildConfig;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.drm.FrameworkMediaDrm;
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback;
import com.google.android.exoplayer2.drm.UnsupportedDrmException;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.EventLogger;
import com.google.android.exoplayer2.Player.EventListener;
import com.google.android.exoplayer2.util.Util;
import com.orhanobut.logger.Logger;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import static com.google.android.exoplayer2.Player.STATE_READY;

public class ExoPlayerComponent implements PlayerControlView.VisibilityListener, EventListener {

    private static final CookieManager DEFAULT_COOKIE_MANAGER;

    private int lastReport;
    private int customEventTrigger;

    private boolean isVideoPause;
    private ExoPlayerComponetInterface exoplayerInterface;

    static {
        DEFAULT_COOKIE_MANAGER = new CookieManager();
        DEFAULT_COOKIE_MANAGER.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
    }
    private Activity act;
    private String videoExtension;
    private String videoURl;
    private String extension;
    private String drmScheme;
    private String drmLicenseURL;
    private SimpleExoPlayer player;
    private PlayerView playerView;
    private FrameworkMediaDrm mediaDrm;
    private DefaultTrackSelector trackSelector;
    private PlayerControlView.VisibilityListener uiControllersListener;

    public ExoPlayerComponent(Activity act, String videoURl, @Nullable String drmLicenseURL, @Nullable String drmScheme, String extension, boolean reportEvents, PlayerView playerView){
        this.act           = act;
        this.playerView    = playerView;
        this.videoURl      = videoURl;
        this.extension     = extension;
        this.drmScheme     = drmScheme;
        this.drmLicenseURL = drmLicenseURL;
        this.uiControllersListener = this;

        if(reportEvents) {
            exoplayerInterface = (ExoPlayerComponetInterface) act;
        }

        if (CookieHandler.getDefault() != DEFAULT_COOKIE_MANAGER) {
            CookieHandler.setDefault(DEFAULT_COOKIE_MANAGER);
        }
    }

    /**
     *
     * Este metodo se encarga de construir una session cuando el contenido tiene DRM
     * en caso de haber un error en el proceso devolvera NULL lo que hara imposible
     * la reproduccion del contenido.
     *
     */
    private DefaultDrmSessionManager<FrameworkMediaCrypto> setDRMSession(){

        DefaultDrmSessionManager<FrameworkMediaCrypto> drmSessionManager;
        String[] keyRequestPropertiesArray = null;

        try {
            UUID drmSchemeUuid = Util.getDrmUuid(drmScheme);

            if (drmSchemeUuid == null) {
                Logger.e("This device does not support the required DRM scheme");
                return null;
            } else {
                drmSessionManager = buildDrmSessionManagerV18(drmSchemeUuid, drmLicenseURL, keyRequestPropertiesArray, false);
            }
        } catch (UnsupportedDrmException e) {

            if(e.reason == UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME){
                Logger.e("This device does not support the required DRM scheme");
            }else{
                Logger.e("An unknown DRM error occurred");
            }

            return null;
        }

        return drmSessionManager;
    }

    private DefaultDrmSessionManager<FrameworkMediaCrypto> buildDrmSessionManagerV18(UUID uuid, String licenseUrl, String[] keyRequestPropertiesArray, boolean multiSession)
            throws UnsupportedDrmException {

        HttpDataSource.Factory licenseDataSourceFactory = new DefaultHttpDataSourceFactory(Util.getUserAgent(act, act.getString(R.string.app_name)));
        HttpMediaDrmCallback drmCallback = new HttpMediaDrmCallback(licenseUrl, licenseDataSourceFactory);

        if (keyRequestPropertiesArray != null) {
            for (int i = 0; i < keyRequestPropertiesArray.length - 1; i += 2) {
                drmCallback.setKeyRequestProperty(keyRequestPropertiesArray[i], keyRequestPropertiesArray[i + 1]);
            }
        }

        releaseMediaDrm();
        mediaDrm = FrameworkMediaDrm.newInstance(uuid);
        return new DefaultDrmSessionManager<>(uuid, mediaDrm, drmCallback, null, multiSession);
    }

    private RenderersFactory buildRenderersFactory(boolean preferExtensionDecoders) {
        @DefaultRenderersFactory.ExtensionRendererMode

        int extensionRendererMode = useExtensionRenderers()
                ? (preferExtensionDecoders
                ? DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                : DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                : DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF;

        return new DefaultRenderersFactory(act).setExtensionRendererMode(extensionRendererMode);
    }

    /**
     * Este metodo define y proporciona los medios para ser reproducidos por ExoPlayer
     * Tenga en cuenta que las instancias de MediaSource no deben reutilizarse,
     * lo que significa que deben pasarse solo una vez.
     *
     * @Important: Este metodo solo se usa cuando el contenido posee DRM.
     */

    private MediaSource buildMediaSource(){
        Uri[] uris;
        String[] extensions;

        uris       = new Uri[]{Uri.parse(videoURl)};
        extensions = new String[]{extension};

        MediaSource[] mediaSources = new MediaSource[uris.length];

        for (int i = 0; i < uris.length; i++) {
            mediaSources[i] = buildMediaSource(uris[i], extensions[i]);
        }

        return mediaSources.length == 1 ? mediaSources[0] : new ConcatenatingMediaSource(mediaSources);
    }

    /**
     *
     * Este metodo se encarga de analizar una URL y detectar si es de tipo DASH (MPD),
     * HLS (m3u8) o SmoothStreaming (Aun no usado) es importante porque se encargara de construir un objeto MediaSource
     * adecuado para poder reproducir sin problemas.
     *
     * @Important: Este metodo solo se usa cuando el contenido no posee DRM.
     *
     **/

    private MediaSource buildMediaSource(Uri uri, @Nullable String overrideExtension) {
        @C.ContentType int type = Util.inferContentType(uri, overrideExtension);

        switch (type) {
            case C.TYPE_DASH:
                return new DashMediaSource.Factory(dataSourceFactory()).createMediaSource(uri);
            case C.TYPE_HLS:
                return new HlsMediaSource.Factory(dataSourceFactory()).createMediaSource(uri);
            case C.TYPE_SS:
                return new SsMediaSource.Factory(dataSourceFactory()).createMediaSource(uri);
            case C.TYPE_OTHER:
                throw new IllegalStateException("Unsupported type: " + type);
            default:
                throw new IllegalStateException("Unsupported type: " + type);
        }
    }

    /**
     *
     * Este es el ultimo paso necesario para reproducir, en este punto se instancian los controles
     * del player y se inyectan los objetos creados anteriormente en caso de que tenga DRM, en caso
     * de que no tenga DRM nos podemos saltar la parte de crear los objetos y demas y crear una
     * instancia basica con MediaSource por defecto.
     *
     */
    private void preparePlayer(MediaSource mediaSource, RenderersFactory renderersFactory,
                               DefaultDrmSessionManager<FrameworkMediaCrypto> drmSessionManager, boolean haveDRM){

        if(haveDRM) {
            setPlayer(ExoPlayerFactory.newSimpleInstance(act, renderersFactory, trackSelector, drmSessionManager));
        }else {
            setPlayer(ExoPlayerFactory.newSimpleInstance(act));
        }

        getPlayer().setPlayWhenReady(true);
        getPlayer().addAnalyticsListener(new EventLogger(trackSelector));

        playerView.setControllerVisibilityListener(uiControllersListener);
        playerView.requestFocus();
        playerView.setPlayer(getPlayer());
        getPlayer().addListener(this);

        if(haveDRM) {
            getPlayer().prepare(mediaSource, false, false);
        }else {
            getPlayer().prepare(mediaSource);
        }
    }


    public void initializePlayer() {

        if(drmLicenseURL != null) {

            DefaultDrmSessionManager<FrameworkMediaCrypto> drmSessionManager = setDRMSession();

            RenderersFactory renderersFactory = buildRenderersFactory(false);

            MediaSource mediaSource = buildMediaSource();

            TrackSelection.Factory trackSelectionFactory;
            trackSelectionFactory = new AdaptiveTrackSelection.Factory();

            trackSelector = new DefaultTrackSelector(trackSelectionFactory);
            trackSelector.setParameters(trackSelector.getParameters().buildUpon().setPreferredAudioLanguage("es").build());
            // trackSelector.setParameters(trackSelectorParameters);

            preparePlayer(mediaSource, renderersFactory, drmSessionManager, true);
        }else{
            MediaSource mediaSource = buildMediaSource(Uri.parse(videoURl), null);
            preparePlayer(mediaSource, null,null, false);
        }
    }

    /**
     * Es importante liberar el Player cuando no se esta usando esto para
     * asegurar que siempre tenemos una sola instancia del player activa.
     */

    public void releasePlayer() {
        if (getPlayer() != null) {
            getPlayer().release();
            setPlayer(null);
            trackSelector = null;
        }
    }

    private void releaseMediaDrm() {
        if (mediaDrm != null) {
            mediaDrm.release();
            mediaDrm = null;
        }
    }

    /**
     *
     * Por temas de memoria, optimizacion y que solo puede haber una instancia de ExoPlayer abierta,
     * se deben aplicar estos eventos en los siguientes listenes.
     *
     * @onPause  :  onPausePlayer();
     * @onStop   :  onStopPlayer();
     * @onStart  :  onStartPlayer();
     * @OnResume :  onResumePlayer();
     *
     */

    public void onStartPlayer(){
        if (Util.SDK_INT > 23) {
            if (getPlayer() == null) {

                initializePlayer();

                if (playerView != null) {
                    playerView.onResume();
                }
            }
        }
    }

    public void onResumePlayer(){
        if (Util.SDK_INT <= 23 || player == null) {
            if (getPlayer() == null) {

                initializePlayer();

                if (playerView != null) {
                    playerView.onResume();
                }
            }
        }
    }


    public void onPausePlayer(){
        if (Util.SDK_INT <= 23) {
            if (playerView != null) {
                playerView.onPause();
            }
            releasePlayer();
        }
    }

    public void onStopPlayer(){
        if (Util.SDK_INT > 23) {
            if (playerView != null) {
                playerView.onPause();
            }
            releasePlayer();
        }
    }

    /**
     * Metodo para reportar eventos en este caso detectar si los controles
     * del player estan visibles o no
     */
   @Override
    public void onVisibilityChange(int visibility) {
        if(exoplayerInterface != null) {
            if(visibility == Constants.CONTROLS_HIDE) {
                exoplayerInterface.onVisibilityChanged(false);
            }else{
                exoplayerInterface.onVisibilityChanged(true);
            }
        }
    }

    /**
     * Metodo para reportar eventos en este caso detectar cuando presionan el boton
     * de play o le boton de pause.
     */
    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        if(exoplayerInterface != null) {
            if (playWhenReady) {
                if (playbackState == STATE_READY) {
                    setVideoPause(false);
                    exoplayerInterface.onPlayTap();
                }
            } else {
                setVideoPause(true);
                exoplayerInterface.onPauseTap();
            }
        }
    }

    /**
     * Este metodo es una interfaz encargada de verificar los eventos de tiempo que puede tener el player
     * para hacer mas facil el reporte de datos, actualmente: 10%, 25%, 50%, 75%, 95%, 100%
     * y un evento customizable que reportara cada {customEventReportInSeconds} mientras
     * el player este reproduciendo video.
     */
    public void eventTimeListener(int customEventReportInSeconds) {
        if(exoplayerInterface != null) {

            final Timer timer = new Timer();
            final TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    act.runOnUiThread(() -> {

                        if (getPlayer() != null) {

                            exoplayerInterface = (ExoPlayerComponetInterface) act;
                            long currentPosition = getCurrentPosition() * 100;
                            Long percent = currentPosition / getDuration();

                            if (percent.intValue() == Constants.TEN_PORCENT && lastReport != Constants.TEN_PORCENT) {
                                exoplayerInterface.onTrackingTenPercent();
                                lastReport = Constants.TEN_PORCENT;
                            }

                            if (percent.intValue() == Constants.FIRST_QUARTIL && lastReport != Constants.FIRST_QUARTIL) {
                                exoplayerInterface.onTrackingFirstQuartil();
                                lastReport = Constants.FIRST_QUARTIL;
                            }

                            if (percent.intValue() == Constants.MIDPOINT && lastReport != Constants.MIDPOINT) {
                                exoplayerInterface.onTrackingMidPoint();
                                lastReport = Constants.MIDPOINT;
                            }

                            if (percent.intValue() == Constants.THIRD_QUARTIL && lastReport != Constants.THIRD_QUARTIL) {
                                exoplayerInterface.onTrackingThirdQuartil();
                                lastReport = Constants.THIRD_QUARTIL;
                            }

                            if (percent.intValue() == Constants.COMPLETE && lastReport != Constants.COMPLETE) {
                                exoplayerInterface.onTrackingComplete();
                                lastReport = Constants.COMPLETE;
                            }

                            if (percent.intValue() == Constants.ENDED && lastReport != Constants.ENDED) {
                                exoplayerInterface.onTrackingEnded();
                                lastReport = Constants.ENDED;
                            }

                            if (customEventTrigger == customEventReportInSeconds) {
                                exoplayerInterface.onCustomTrackingProgress();
                                customEventTrigger = 0;
                            } else {
                                if (!isVideoPause()) {
                                    customEventTrigger++;
                                }
                            }
                            eventTimeListener(customEventReportInSeconds);
                        }

                    });
                }
            };
            timer.schedule(task, 1000);

        }
    }

    /**
     * Este metodo crea un objeto a partir del userAgent que simplemente funciona como clave valor
     * es necesario para registar el MediaSource.
     */
    private DataSource.Factory dataSourceFactory(){
        return new DefaultDataSourceFactory(act, Util.getUserAgent(act, act.getString(R.string.app_name)));
    }

    private boolean useExtensionRenderers() {
        return "withExtensions".equals(BuildConfig.FLAVOR);
    }


    public String getVideoExtension() {
        return videoExtension;
    }

    public void setVideoExtension(String videoExtension) {
        this.videoExtension = videoExtension;
    }

    public SimpleExoPlayer getPlayer() {
        return player;
    }

    public void setPlayer(SimpleExoPlayer player) {
        this.player = player;
    }

    public long getCurrentPosition() {
        return player.getCurrentPosition();
    }

    public long getDuration() {
        return player.getDuration();
    }

    public boolean isVideoPause() {
        return isVideoPause;
    }

    public void setVideoPause(boolean videoPause) {
        isVideoPause = videoPause;
    }
}
