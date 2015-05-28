package co.poynt.location.test;

/**
 * Created by sathyaiyer on 5/27/15.
 */

import android.app.Application;
import android.location.LocationManager;

import javax.inject.Inject;

/**
 * Android Main test Application that will initialize
 * component for injection
 */
public class LocationTestApplication extends Application {

    private ApplicationComponent applicationComponent;
    @Inject
    LocationManager locationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        this.initializeInjector();
        applicationComponent.injectApplication(this);
    }

    private void initializeInjector() {
        this.applicationComponent = DaggerApplicationComponent.builder()
                .applicationModule(new ApplicationModule(this))
                .build();
    }

    public ApplicationComponent getApplicationComponent() {
        return this.applicationComponent;
    }


}