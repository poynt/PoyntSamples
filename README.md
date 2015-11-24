Poynt Sample
============
The purpose of this sample app is to demonstrate a simple integration with PoyntOS SDK to build apps that run on PoyntOS.

Couple of things to note here:

1. Permissions in AndroidManifest.xml - these are required to make sure your application can invoke
the Poynt Services and access Data from the Poynt Content Providers.
2. Binding/Unbinding to the PoyntServices is required as with any AIDL service on android.
3. Poynt Authenticator is used to support Poynt Accounts (Business users 'aka' employees) logins
 through the Android Account Manager interface.


Building & Running
============
The sample uses gradle wrapper to build. Just use the gradlew commands to build or you can open the
project in Android Studio and run it directly from there.

    ./gradlew assembleDebug

