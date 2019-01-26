package android.example.com.cameraapp;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaActionSound;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static android.content.ContentValues.TAG;
import static android.example.com.cameraapp.R.id.gridView;
import static android.media.MediaRecorder.VideoSource.CAMERA;


public class CameraActivity extends AppCompatActivity {

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 1;
    private static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 2;
    private static final int MY_PERMISSIONS_REQUEST_STORAGE = 3;

    private boolean permission_accept = false;
    private  boolean isZoomed = false;

    private int nextFlashMode = 0;

    private float mDist = 0;


    private ImageView captureButton;
    private ImageView recordButton;
    private ImageView galleryButton;
    private ImageView flashButton;
    private ImageView filterButton;
    private GridView filterGridView;
    private FrameLayout preview;


    private Camera mCamera;
    private CameraPreview mPreview;
    private MediaRecorder mMediaRecorder;
    private boolean isRecording = false;
    private boolean inPreview = true;
    private int currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;




    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preview = (FrameLayout) findViewById(R.id.camera_preview);

        // Hide the status bar.
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        //Toast.makeText(getApplicationContext(),"OnCreate",Toast.LENGTH_SHORT).show();

        ActivityCompat.requestPermissions(CameraActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_STORAGE);

        ActivityCompat.requestPermissions(CameraActivity.this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                MY_PERMISSIONS_REQUEST_RECORD_AUDIO);

        ActivityCompat.requestPermissions(CameraActivity.this,
                new String[]{Manifest.permission.CAMERA},
                MY_PERMISSIONS_REQUEST_CAMERA);

        filterGridView =(GridView) findViewById(gridView);
        filterGridView.setVisibility(View.GONE);
        
            //bottom panel..............................................


            // Add a listener to the Capture button for video
            recordButton = (ImageView) findViewById(R.id.button_record);
            recordButton.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (isRecording) {
                                // stop recording and release camera
                                mMediaRecorder.stop();  // stop the recording
                                releaseMediaRecorder(); // release the MediaRecorder object
                                mCamera.lock();         // take camera access back from MediaRecorder

                                // inform the user that recording has stopped
                                recordButton.setImageResource(R.drawable.icons_video);
                                isRecording = false;
                            } else {
                                // initialize video camera
                                if (prepareVideoRecorder()) {
                                    // Camera is available and unlocked, MediaRecorder is prepared,
                                    // now you can start recording
                                    mMediaRecorder.start();
                                    mCamera.setDisplayOrientation(90);

                                    // inform the user that recording has started
                                    recordButton.setImageResource(R.drawable.icons_video_start);
                                    isRecording = true;
                                } else {
                                    // prepare didn't work, release the camera
                                    releaseMediaRecorder();
                                    // inform user
                                }
                                updateGallery();
                            }
                        }
                    }
            );


        // Add a listener to the Capture button for image
        captureButton = (ImageView) findViewById(R.id.button_capture);
        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // get an image from the camera

                        mCamera.takePicture(null, null, mPicture);
                        MediaActionSound sound = new MediaActionSound();
                        sound.play(MediaActionSound.SHUTTER_CLICK);
                        updateGallery();

                    }
                }


        );

        galleryButton = (ImageView) findViewById(R.id.button_gallery);

        galleryButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        Intent galleryIntent = new Intent(Intent.ACTION_VIEW,
                                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        startActivity(galleryIntent);

                    }
                }
        );

        //top panel.................................................


            ImageView switchCamera = (ImageView) findViewById(R.id.switch_camera);
            switchCamera.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            if (inPreview) {
                                mPreview.surfaceDestroyed(mPreview.getHolder());
                                mPreview.getHolder().removeCallback(mPreview);
                                mPreview.destroyDrawingCache();
                                preview.removeView(mPreview);
                                mCamera.stopPreview();
                                mCamera.stopPreview();
                                mCamera.setPreviewCallback(null);
                                mCamera.stopPreview();
                            }
                            //NB: if you don't release the current camera before switching, you app will crash
                            mCamera.release();

                            //swap the id of the camera to be used
                            if(currentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK){
                                currentCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
                            }
                            else {
                                currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
                            }
                            mCamera = null;
                            mCamera = Camera.open(currentCameraId);

                            setCameraDisplayOrientation(CameraActivity.this, currentCameraId, mCamera);
                            try {

                                mPreview = new CameraPreview(CameraActivity.this, mCamera);
                                preview.addView(mPreview);
                                mCamera.setPreviewDisplay(mPreview.getHolder());
                                mCamera.startPreview();

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            mCamera.startPreview();
                        }
                    }



            );






    }


    public void updateGallery(){

        MediaScannerConnection.scanFile(CameraActivity.this,
                new String[] { Environment.getExternalStorageDirectory().toString()
                }, null, new MediaScannerConnection.OnScanCompletedListener() {

                    public void onScanCompleted(String path, Uri uri)
                    {
                        Log.i("ExternalStorage", "Scanned " + path + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
                    }
                });
    }




    public static void setCameraDisplayOrientation(Activity activity,
                                                   int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null){
                Log.d(TAG, "Error creating media file, check storage permissions ");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();

                mCamera.startPreview();

            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
        }

    };

    private boolean prepareVideoRecorder(){


        mMediaRecorder = new MediaRecorder();

        // Step 1: Unlock and set camera to MediaRecorder
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        // Step 2: Set sources
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(CAMERA);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        if(currentCameraId== Camera.CameraInfo.CAMERA_FACING_BACK) {
            mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
        }
        else mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_QVGA));

        // Step 4: Set output file
        mMediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());

        // Step 5: Set the preview output
        mMediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());

        // Step 6: Prepare configured MediaRecorder
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }


    /** Create a file Uri for saving an image or video */
    private static Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), "DrimCam");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("DrimCam", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    public void restoreCameraPreview(){

        //destroying camera preview
        mPreview.surfaceDestroyed(mPreview.getHolder());
        mPreview.getHolder().removeCallback(mPreview);
        mPreview.destroyDrawingCache();

        preview.removeView(mPreview);
        // recreating camera preview
        mCamera = Camera.open(currentCameraId);

       final Camera.Parameters params = mCamera.getParameters();
        if (params.getSupportedFocusModes().contains(
                Camera.Parameters.FOCUS_MODE_AUTO)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }
        if(params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }
        if(params.getSupportedFlashModes().contains(Camera.Parameters.FLASH_MODE_AUTO)) {
            params.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
        }


        mCamera.setParameters(params);

        flashButton = (ImageView) findViewById(R.id.flash_light);

        flashButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {


                        ++nextFlashMode;
                        if(nextFlashMode > 3) nextFlashMode = 0;

                        switch(nextFlashMode){

                            case 0: {

                                if (params.getSupportedFlashModes().contains(Camera.Parameters.FLASH_MODE_AUTO)) {
                                    params.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                                    flashButton.setBackgroundResource(R.drawable.icons_auto_flash);
                                    mCamera.setParameters(params);

                                }

                                break;
                            }


                            case 1: {

                                if(params.getSupportedFlashModes().contains(Camera.Parameters.FLASH_MODE_ON)) {
                                    params.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                                    flashButton.setBackgroundResource(R.drawable.icons_on_flash);
                                    mCamera.setParameters(params);
                                }

                                break;
                            }

                            case 2: {

                                if(params.getSupportedFlashModes().contains(Camera.Parameters.FLASH_MODE_OFF)) {
                                    params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                                    flashButton.setBackgroundResource(R.drawable.icons_off_flash);
                                    mCamera.setParameters(params);
                                }

                                break;
                            }

                            case 3: {

                                if(params.getSupportedFlashModes().contains(Camera.Parameters.FLASH_MODE_TORCH)) {
                                    params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                                    flashButton.setBackgroundResource(R.drawable.icons_torch_flash);
                                    mCamera.setParameters(params);
                                }

                                break;
                            }

                        }


                    }
                }
        );

        filterButton = (ImageView) findViewById(R.id.button_filter);
        filterButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int[] imageId = {
                                R.drawable.none,
                                R.drawable.mono,
                                R.drawable.sepia,
                                R.drawable.negative,
                                R.drawable.posterize,
                                R.drawable.aqua,
                                R.drawable.blackboard,
                                R.drawable.whiteboard,

                        };

                        filterGridView.setVisibility(View.VISIBLE);

                        GridAdapter gridAdapter = new GridAdapter(CameraActivity.this, imageId);

                        filterGridView.setAdapter(gridAdapter);

                        filterGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                            @Override
                            public void onItemClick(AdapterView<?> parent, View view,
                                                    int position, long id) {

                                switch (position) {

                                    case 0:

                                        if(params.getSupportedColorEffects().contains(Camera.Parameters.EFFECT_NONE)) {
                                            params.setColorEffect(Camera.Parameters.EFFECT_NONE);
                                            mCamera.setParameters(params);
                                            filterGridView.setVisibility(View.GONE);
                                            break;
                                        }


                                    case 1:
                                        if(params.getSupportedColorEffects().contains(Camera.Parameters.EFFECT_MONO)) {
                                            params.setColorEffect(Camera.Parameters.EFFECT_MONO);
                                            mCamera.setParameters(params);
                                            filterGridView.setVisibility(View.GONE);
                                            break;
                                        }

                                    case 2:
                                        if(params.getSupportedColorEffects().contains(Camera.Parameters.EFFECT_SEPIA)) {
                                            params.setColorEffect(Camera.Parameters.EFFECT_SEPIA);
                                            mCamera.setParameters(params);
                                            filterGridView.setVisibility(View.GONE);
                                            break;
                                        }

                                    case 3:
                                        if(params.getSupportedColorEffects().contains(Camera.Parameters.EFFECT_NEGATIVE)) {
                                            params.setColorEffect(Camera.Parameters.EFFECT_NEGATIVE);
                                            mCamera.setParameters(params);
                                            filterGridView.setVisibility(View.GONE);
                                            break;
                                        }
                                    case 4:
                                        if(params.getSupportedColorEffects().contains(Camera.Parameters.EFFECT_POSTERIZE)) {
                                            params.setColorEffect(Camera.Parameters.EFFECT_POSTERIZE);
                                            mCamera.setParameters(params);
                                            filterGridView.setVisibility(View.GONE);
                                            break;
                                        }

                                    case 5:

                                        if(params.getSupportedColorEffects().contains(Camera.Parameters.EFFECT_AQUA)) {
                                            params.setColorEffect(Camera.Parameters.EFFECT_AQUA);
                                            mCamera.setParameters(params);
                                            filterGridView.setVisibility(View.GONE);
                                            break;
                                        }

                                    case 6:

                                        if(params.getSupportedColorEffects().contains(Camera.Parameters.EFFECT_BLACKBOARD)) {
                                            params.setColorEffect(Camera.Parameters.EFFECT_BLACKBOARD);
                                            mCamera.setParameters(params);
                                            filterGridView.setVisibility(View.GONE);
                                            break;
                                        }

                                    case 7:

                                        if(params.getSupportedColorEffects().contains(Camera.Parameters.EFFECT_WHITEBOARD)) {
                                            params.setColorEffect(Camera.Parameters.EFFECT_WHITEBOARD);
                                            mCamera.setParameters(params);
                                            filterGridView.setVisibility(View.GONE);
                                            break;
                                        }

                                    default:
                                        break;
                                }

                            }
                        });

                    }
                }


        );

        setCameraDisplayOrientation(CameraActivity.this, currentCameraId, mCamera);
        try {

            mPreview = new CameraPreview(CameraActivity.this, mCamera);
            preview.addView(mPreview);
            mCamera.setPreviewDisplay(mPreview.getHolder());
            mCamera.startPreview();


        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.startPreview();

    }


    @Override
    public void onResume(){
        super.onResume();

        View decorView = getWindow().getDecorView();
        // Hide the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        if(permission_accept) restoreCameraPreview();
        //Toast.makeText(getApplicationContext(),"OnResume",Toast.LENGTH_SHORT).show();

    }
    @Override
    protected void onPause() {
        super.onPause();
        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
        releaseCamera();              // release the camera immediately on pause event
    }

    private void releaseMediaRecorder(){
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            mCamera.lock();           // lock camera for later use
        }
    }

    private void releaseCamera(){

        if (mCamera != null){
            mCamera.release();   // release the camera for other applications
            mCamera = null;
        }
    }

public void startPreview(){

    // Create an instance of Camera
    mCamera = null;
    mCamera = Camera.open(currentCameraId);

    // Create our Preview view and set it as the content of our activity.
    mPreview = new CameraPreview(this, mCamera);
    preview.addView(mPreview);

    Camera.Parameters params = mCamera.getParameters();
    if(params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
    }
    if(params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
    }
    if(params.getSupportedFlashModes().contains(Camera.Parameters.FLASH_MODE_AUTO)) {
        params.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
    }
    mCamera.setParameters(params);


}

        //handle zoom.........................

    private void handleZoom(MotionEvent event, Camera.Parameters params) {
        int maxZoom = params.getMaxZoom();
        int zoom = params.getZoom();
        float newDist = getFingerSpacing(event);
        if (newDist > mDist) {
            //zoom in
            if (zoom < maxZoom)
                zoom++;
        } else if (newDist < mDist) {
            //zoom out
            if (zoom > 0)
                zoom--;
        }
        mDist = newDist;
        params.setZoom(zoom);
        mCamera.setParameters(params);
    }

    public void handleFocus(MotionEvent event, final Camera.Parameters params) {
        int pointerId = event.getPointerId(0);
        int pointerIndex = event.findPointerIndex(pointerId);
        // Get the pointer's current position
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);

        List<String> supportedFocusModes = params.getSupportedFocusModes();
        if (supportedFocusModes != null && supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO) &&
                supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_MACRO) &&
                supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
            mCamera.setParameters(params);
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            mCamera.setParameters(params);
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean b, Camera camera) {
                    // currently set to auto-focus on single touch

                }
            });
        }
    }

    /** Determine the space between the first two fingers */
    private float getFingerSpacing(MotionEvent event) {
        // ...
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float)Math.sqrt(x * x + y * y);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Get the pointer ID
        Camera.Parameters params = mCamera.getParameters();
        int action = event.getAction();


        if (event.getPointerCount() > 1) {
            // handle multi-touch events
            if (action == MotionEvent.ACTION_POINTER_DOWN) {
                mDist = getFingerSpacing(event);

            } else if (action == MotionEvent.ACTION_MOVE && params.isZoomSupported()) {
                mCamera.cancelAutoFocus();
                handleZoom(event, params);
                isZoomed = true;
            }
        } else {
            // handle single touch events
            if (action == MotionEvent.ACTION_UP) {
                handleFocus(event, params);
                if(!isZoomed){
                    MediaActionSound sound = new MediaActionSound();
                    sound.play(MediaActionSound.FOCUS_COMPLETE);

                }
                isZoomed = false;
            }


        }
        return true;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    startPreview();

                    permission_accept =true;

                } else {

                    Toast.makeText(getApplicationContext(),"Permission denied! You can change them in Setting > App",Toast.LENGTH_LONG).show();
                    this.finish();

                }
                return;
            }

            case MY_PERMISSIONS_REQUEST_RECORD_AUDIO: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do
                } else {
                    Toast.makeText(getApplicationContext(),"Permission denied! You can change them in Setting > App",Toast.LENGTH_LONG).show();
                    this.finish();

                }
                return;
            }

            case MY_PERMISSIONS_REQUEST_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted

                } else {

                    Toast.makeText(getApplicationContext(),"Permission denied! You can change them in Setting > App",Toast.LENGTH_LONG).show();
                    this.finish();
                }
                return;
            }

        }

    }

}


