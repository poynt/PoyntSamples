package co.poynt.location.test;

import android.app.Activity;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MinimapOverlay;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Main activity that shows how to get location and
 * display the location on the map.
 * MapView being used in provided by osmdroid which depends
 * on openStreetMapView.
 * <p>
 * More information about osmdroid can be found on this
 * <a href="https://github.com/osmdroid/osmdroid">link</a>
 * </p>
 */
public class MainActivity extends Activity {
    private static final String TAG = "LocationActivity";

    @Inject
    LocationManager mLocationManager;

    @InjectView(R.id.providers)
    TextView mProvidersText;

    @InjectView(R.id.last_location)
    TextView mLastLocationText;

    @InjectView(R.id.last_location_time)
    TextView mLastLocationTimeText;

    @InjectView(R.id.location)
    TextView mLocationText;

    @InjectView(R.id.location_time)
    TextView mLocationTimeText;

    @InjectView(R.id.mapview)
    MapView mMapView;


    // Location related variables
    private Location mLastLocation;
    private MyLocationNewOverlay mLocationOverlay;
    private CompassOverlay mCompassOverlay;
    private final int MAP_ZOOM_VALUE = 18;


    //we are going to use a handler to be able to run in our TimerTask
    private final Handler handler = new Handler();
    private Timer mTimer;
    private TimerTask mTimerTask;
    private long mStartTime;

    /**
     * Initialize view via injections, display initial location and
     * start location acquisition
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Perform injection so that when this call returns
        // all dependencies will be available for use.
        ((LocationTestApplication) getApplication())
                .getApplicationComponent().injectActivity(this);
        ButterKnife.inject(this);

        // display available providers.
        displayProviders();

        // display last know location.
        displayLastKnownLocation();

        // Initialize mapView
        initMapView();

    }


    /**
     * Location listener to get status of location updates.
     */
    private LocationListener mLocationListener = new LocationListener() {

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
            Toast.makeText(MainActivity.this,
                    "Provider enabled: " + provider, Toast.LENGTH_SHORT)
                    .show();
        }

        @Override
        public void onProviderDisabled(String provider) {
            Toast.makeText(MainActivity.this,
                    "Provider disabled: " + provider, Toast.LENGTH_SHORT)
                    .show();
        }

        @Override
        public void onLocationChanged(Location location) {
            updateCurrentLocation(location.getLongitude(), location.getLatitude());
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Start acquiring new location.
        startLocationAcquisition();
        // start the timer for ui updates.
        startTimerTask();

        mStartTime = System.currentTimeMillis();

        // Initialize map view.
        startMapView();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // start the timer for ui updates.
        stopTimertask();
        stopLocationAcquisition();
        stopMapView();
    }

    /**
     * Update current location and stop acquisition and timer.
     *
     * @param latitude
     * @param longitude
     */
    private void updateCurrentLocation(double latitude, double longitude) {
        // Do work with new location. Implementation of this method will be covered later.
        Log.d(TAG, " Location : lat:" + latitude + " lon:" + longitude);
        String loc = latitude + ", " + longitude;
        Log.d(TAG, " Removed location");
        mLocationText.setText(loc);
        stopLocationAcquisition();
        stopTimertask();
    }

    /**
     * Display last know location.
     */
    private void displayLastKnownLocation() {
        Location lastLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (lastLocation != null) {
            String loc = lastLocation.getLongitude() + ", " + lastLocation.getLatitude();
            mLastLocationText.setText(loc);

            Date lastTime = new Date(lastLocation.getTime());
            String format = "yyyy/MM/dd HH:mm:ss";
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            mLastLocationTimeText.setText(getTimeDifference(lastLocation.getTime()));
        } else {
            mLastLocationText.setText("Not found");
        }
        mLastLocation = lastLocation;
    }

    /**
     * Initialize map view.
     */
    private void initMapView() {
        final DisplayMetrics dm = getResources().getDisplayMetrics();
        MinimapOverlay minimapOverlay;
        ScaleBarOverlay scaleBarOverlay;

        // Create loation overlay to track location.
        mLocationOverlay = new MyLocationNewOverlay(this, new GpsMyLocationProvider(this),
                mMapView);
        mLocationOverlay.setDrawAccuracyEnabled(true);
        mLocationOverlay.enableMyLocation();

        // Show mini map on bottom right corner.
        minimapOverlay = new MinimapOverlay(this, mMapView.getTileRequestCompleteHandler());
        minimapOverlay.setWidth(dm.widthPixels / 5);
        minimapOverlay.setHeight(dm.heightPixels / 5);

        // Show zoom in/out bar for changing zoom.
        scaleBarOverlay = new ScaleBarOverlay(this);
        scaleBarOverlay.setCentred(true);
        scaleBarOverlay.setScaleBarOffset(dm.widthPixels / 2, 10);

        // Show a N/S compass
        mCompassOverlay = new CompassOverlay(this, new InternalCompassOrientationProvider(this),
                mMapView);
        mCompassOverlay.enableCompass();

        mMapView.setBuiltInZoomControls(true);
        mMapView.setMultiTouchControls(true);

        // Add all the overlays to mapview.
        mMapView.getOverlays().add(mLocationOverlay);
        mMapView.getOverlays().add(mCompassOverlay);
        mMapView.getOverlays().add(minimapOverlay);
        mMapView.getOverlays().add(scaleBarOverlay);

        mMapView.getController().setZoom(MAP_ZOOM_VALUE);
        mMapView.scrollTo(0, 0);


        // Select a map source, by default MAPNIK is selected.
        // Check TileSourceFactory to see list of map source provided.
        final String tileSourceName =
                TileSourceFactory.DEFAULT_TILE_SOURCE.name();
        try {
            final ITileSource tileSource = TileSourceFactory.getTileSource(tileSourceName);
            mMapView.setTileSource(tileSource);
        } catch (final IllegalArgumentException e) {
            mMapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
        }

        // Move the map to last know location.
        if (mLastLocation != null) {
            GeoPoint geoPoint = new GeoPoint(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            mMapView.getController().setCenter(geoPoint);
            mMapView.getController().animateTo(geoPoint);
        }

        // Register for getting first location lock on a location overlay.
        mLocationOverlay.runOnFirstFix(new Runnable() {
            public void run() {
                // Give some time for sensor to settle down.
                // calling getMyLocation immediately known to return null
                // Adjust the sleep time if you see geopoint as null.
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                final GeoPoint point = mLocationOverlay.getMyLocation();
                if (point != null) {
                    // Do work with new location. Implementation of this method will be covered later.
                    Log.d(TAG, " Location : lat:" + point.getLatitude() + " lon:" + point.getLongitude());

                    // Update view on UI thread.
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateCurrentLocation(point.getLatitude(), point.getLongitude());
                            mMapView.getController().animateTo(mLocationOverlay
                                    .getMyLocation());
                        }
                    });
                }
            }
        });
    }

    /**
     * unInitialize map view.
     */
    private void startMapView() {
        mLocationOverlay.enableMyLocation();
        mCompassOverlay.enableCompass();
    }

    /**
     * unInitialize map view.
     */
    private void stopMapView() {
        mLocationOverlay.disableMyLocation();
        mCompassOverlay.disableCompass();
    }

        /**
         * Display list of providers.
         */
    private void displayProviders() {
        Log.d(TAG, " List of Provider");
        List<String> providers = mLocationManager.getAllProviders();
        if (providers != null) {
            Log.d(TAG, " Provider:" + providers.toString());

        }
        mProvidersText.setText(providers.toString());
    }

    /**
     * Start getting location upates.
     */
    private void startLocationAcquisition() {
        // request new location.
        // Minimum time interval for update in seconds, i.e. 5 seconds.
        long minTime = 1000;
        // Minimum distance change for update in meters, i.e. 10 meters.
        // Increase the distance if location lock doesn't happen within
        // your desired time.
        long minDistance = 10;
        String bestProvider = getBestProviderName();
        if (bestProvider == null) {
            // default to gps provider.
            bestProvider = LocationManager.GPS_PROVIDER;
        }
        mLocationManager.requestLocationUpdates(bestProvider, minTime,
                minDistance, mLocationListener);
    }

    /**
     * Start getting location upates.
     */
    private void stopLocationAcquisition() {
        mLocationManager.removeUpdates(mLocationListener);
    }

    /**
     * Start the timer.
     */
    private void startTimerTask() {
        //set a new Timer
        mTimer = new Timer();

        //initialize the TimerTask's job
        initializeTimerTask();

        //schedule the timer, after the first 5000ms the TimerTask will run every 10000ms
        mTimer.schedule(mTimerTask, 0, 1000); //
    }

    /**
     * Stop the timer.
     */
    private void stopTimertask() {
        //stop the timer, if it's not already null
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    /**
     * Get formatted time difference string from the input time in UTC.
     *
     * @param pastTime
     * @return
     */
    private String getTimeDifference(long pastTime) {
        long time = System.currentTimeMillis() - pastTime;
        Date date = new Date(time);
        DateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        return formatter.format(date);
    }

    /**
     * Timer task that tick every second to update UI.
     */
    public void initializeTimerTask() {

        mTimerTask = new TimerTask() {
            boolean colorflip = false;

            public void run() {

                //use a handler to run a toast that shows the current timestamp
                handler.post(new Runnable() {
                    public void run() {
                        getTimeDifference(mStartTime);
                        int color = colorflip ? R.color.white : R.color.red;
                        mLocationText.setTextColor(getResources().getColor(color));
                        colorflip = !colorflip;
                        //get the current timeStamp
                        mLocationTimeText.setText(getTimeDifference(mStartTime));
                    }
                });
            }
        };
    }


    /**
     * Get provider name.
     *
     * @return Name of best suiting provider.
     */
    String getBestProviderName() {
        Criteria criteria = new Criteria();
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setSpeedRequired(true);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(false);
        return mLocationManager.getBestProvider(criteria, true);
    }
}
