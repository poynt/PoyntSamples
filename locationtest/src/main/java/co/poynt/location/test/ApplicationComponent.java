package co.poynt.location.test;

import javax.inject.Singleton;

import dagger.Component;
import co.poynt.location.test.ApplicationModule;
import co.poynt.location.test.LocationTestApplication;
import co.poynt.location.test.MainActivity;

/**
 * Created by sathyaiyer on 5/27/15.
 */

@Singleton @Component(
        modules = ApplicationModule.class
)
/**
 * Dagger component to enable dependency injection.
 */
interface ApplicationComponent {
    LocationTestApplication injectApplication(LocationTestApplication application);
    MainActivity injectActivity(MainActivity activity);
}
