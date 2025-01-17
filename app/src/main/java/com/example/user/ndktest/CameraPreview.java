package com.example.user.ndktest;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.IOException;

/**
 * Created by Naing on 2015-07-06.
 * Camera & Preview Control view
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    String TAG = "CAMERA_CONTROL";

    private SurfaceHolder mHolder;
    private Camera mCamera;
    private Activity mActivity;
    private FaceDetectorView mDetectedView;
    private RelativeLayout mRelativeBottom;
    private RelativeLayout mShutterChange;

    int FRONT = Camera.CameraInfo.CAMERA_FACING_FRONT;
    int BACK = Camera.CameraInfo.CAMERA_FACING_BACK;
    int mOrientation = FRONT;

    public CameraPreview(Context context){
        super(context);
    }

    public CameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);

        mActivity = (Activity)context;
        if(checkCameraHardware(context)){
            mCamera = getCameraInstance(mOrientation);
        }

        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void releaseCamera(){
        if(mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // set surface view ratio to 3:4
        int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        int tmp_height=0;

        Log.d("sizeconfirm", "before : surface view's width " + width + " height " + height);
        tmp_height=height;
        if(width * 4/3 < height) {
            height = (int) ((double) width * (double) 4 / 3);
        }else if(height * 3/4 < width){
            width = (int) ((double) width * (double) 4 / 3);
        }
        Log.d("sizeconfirm1", "after : surface view's width " + width + " height " + height);

        // layout size setting
        tmp_height=tmp_height-height;
        RelativeLayout.LayoutParams Bottom = (RelativeLayout.LayoutParams) mRelativeBottom.getLayoutParams();
        Bottom.height = tmp_height;
        mRelativeBottom.setLayoutParams(Bottom);

        // layout size setting
        RelativeLayout.LayoutParams Shutter = (RelativeLayout.LayoutParams) mShutterChange.getLayoutParams();
        Shutter.height = tmp_height;
        mShutterChange.setLayoutParams(Shutter);

        setMeasuredDimension(width, height);
    }

    public void setDetectedView(FaceDetectorView view){
        mDetectedView = view;
        mDetectedView.setCamera(mCamera); // pass camera to detector view
    }

    public void setRelativeBottom(RelativeLayout view){
        mRelativeBottom=view;
    }

    public void setShutterChange(RelativeLayout view){
        mShutterChange=view;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            setCameraDisplayOrientation(FRONT);

            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();

            startFaceDetection();

        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCamera != null) {
            mCamera.stopFaceDetection();
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            Log.i(TAG, "Number of available camera : "+Camera.getNumberOfCameras());
            return true;
        } else {
            Toast.makeText(context, "No camera found!", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private static int findCamera(int orientation){
        int cameraId = -1;
        int numberOfCameras = Camera.getNumberOfCameras();
        for(int i = 0;i<numberOfCameras;i++){
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i,info);
            if(info.facing == orientation){
                cameraId = i;
                break;
            }
        }
        return cameraId;
    }

    public static Camera getCameraInstance(int orientation){
        Camera c = null;
        try {
            c = Camera.open(findCamera(orientation));
        }
        catch (Exception e){
            // using or disable
        }
        return c;
    }

    private void setCameraDisplayOrientation(int orientation){
        Camera.CameraInfo info = new Camera.CameraInfo();
        // default camera id is 0 (default : two cameras. front face 0, back face 1)
        Camera.getCameraInfo(orientation, info);
        int mode = 90; // 90 : portrait mode
        int result;
        if (info.facing == FRONT) { // front
            result = mode;
        } else {  // back
            result = (360-mode);
        }

        mCamera.setDisplayOrientation(result);
    }

    public void startFaceDetection(){
        // Try starting Face Detection
        Camera.Parameters params = mCamera.getParameters();
        Log.d(TAG,"# of Faces is " +params.getMaxNumDetectedFaces());
        // start face detection only *after* preview has started
        if (params.getMaxNumDetectedFaces() > 0){
            // camera supports face detection, so can start it:
            mCamera.setFaceDetectionListener(faceDetectionListener);
            mCamera.startFaceDetection();
        }
    }

    private Camera.FaceDetectionListener faceDetectionListener = new Camera.FaceDetectionListener() {
        @Override
        public void onFaceDetection(Camera.Face[] faces, Camera camera) {
            if (faces.length > 0){
                //Log.d(TAG, "face detected: " + faces.length);
                mDetectedView.setFaces(faces);
            }else {
                //Log.d(TAG, "No faces detected");
            }
            mDetectedView.invalidate();
        }
    };
}

