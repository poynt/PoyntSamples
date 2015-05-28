package co.poynt.location.test;

import android.content.Context;
import android.location.LocationManager;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by sathyaiyer on 5/27/15.
 */
@Module
public class ApplicationModule {

    private final LocationTestApplication application;

    /**
     * Initialize application module
     * and provide necessary object on injections.
     * @param application
     */
    public ApplicationModule(LocationTestApplication application) {
        this.application = application;
    }

    @Provides @Singleton
    Context provideApplicationContext() {
        return this.application;
    }

    /**
     * Provide location manager as a singleton.
     * @return
     */
    @Provides @Singleton
    LocationManager provideLocationManager() {
        return (LocationManager) application.getSystemService(Context.LOCATION_SERVICE);
    }
}