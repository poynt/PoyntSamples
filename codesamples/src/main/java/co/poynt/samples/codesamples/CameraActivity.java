package co.poynt.samples.codesamples;

import android.app.ActionBar;
import android.app.Activity;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import co.poynt.samples.codesamples.utils.CameraPreview;


public class CameraActivity extends Activity {

    private static final String TAG = CameraActivity.class.getSimpleName();
    private Button frontCamera, backCamera;
    private Camera mCamera;
    private CameraPreview mPreview;
    private FrameLayout preview;
    private int currentCameraId = -1;
    private boolean inPreview = false;
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        frontCamera = (Button) findViewById(R.id.frontCamera);
        frontCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCamera != null && currentCameraId != Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    changeCamera(Camera.CameraInfo.CAMERA_FACING_FRONT);
                } else {
                    startCamera(Camera.CameraInfo.CAMERA_FACING_FRONT);
                }
            }
        });
        backCamera = (Button) findViewById(R.id.backCamera);
        backCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCamera != null && currentCameraId != Camera.CameraInfo.CAMERA_FACING_BACK) {
                    changeCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
                } else {
                    startCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
                }
            }
        });

        // Add a listener to the Capture button
        Button captureButton = (Button) findViewById(R.id.capturePicture);
        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // get an image from the camera
                        if (mCamera != null) {
                            mCamera.takePicture(null, null, mPicture);
                        }
                    }
                }
        );

        // camera preview
        preview = (FrameLayout) findViewById(R.id.camera_preview);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void startCamera(int cameraId) {
        // Create an instance of Camera
        mCamera = getCameraInstance(cameraId);
        currentCameraId = cameraId;
        if (mCamera != null) {
            // rotate if it's front facing camera
            setCameraDisplayOrientation(currentCameraId);
            // Create our Preview view and set it as the content of our activity.
            mPreview = new CameraPreview(this, mCamera);
            preview.addView(mPreview);
            inPreview = true;
        }
    }

    private void changeCamera(int cameraId) {
        if (mCamera != null && inPreview == true) {
            releaseCamera();
            startCamera(cameraId);
        } else {
            startCamera(cameraId);
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            if (inPreview) {
                preview.removeView(mPreview);
                mCamera.stopPreview();
            }
            mCamera.release();
            mCamera = null;
        }
        inPreview = false;
    }

    /**
     * A safe way to get an instance of the Camera object.
     */
    public static Camera getCameraInstance(int cameraId) {
        Camera c = null;
        try {
            c = Camera.open(cameraId); // attempt to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
            Log.e(TAG, "Received exception opening camera");
            e.printStackTrace();
        }
        return c; // returns null if camera is unavailable
    }

    public void setCameraDisplayOrientation(int cameraId) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
            mCamera.setDisplayOrientation(result);
        } else {
            // back-facing -- do not change on Poynt Terminal
        }
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null) {
                Log.d(TAG, "Error creating media file, check storage permissions");
                return;
            }
            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            } finally {
                Toast.makeText(CameraActivity.this, "Picture saved to /sdcard/Pictures/MyCameraApp/", Toast.LENGTH_SHORT).show();
                mCamera.startPreview();
            }
        }
    };

    /**
     * Create a file Uri for saving an image or video
     */
    private static Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /**
     * Create a File for saving an image or video
     */
    private static File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }
}
