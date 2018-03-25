package com.group3.filterme;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static  String TAG = "MainActivity";
    JavaCameraView mJavaCameraView;
    Mat mRgba, imgGrey, imgCanny;
    BaseLoaderCallback mLoaderCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case BaseLoaderCallback.SUCCESS:
                    mJavaCameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind variable with layout
        mJavaCameraView = (JavaCameraView)findViewById(R.id.java_camera_view);
        // Make camera visible
        mJavaCameraView.setVisibility(SurfaceView.VISIBLE);
        // Set a listener for the camera
        mJavaCameraView.setCvCameraViewListener(this);

    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    @Override
    protected void onPause() {
        super.onPause();

        if (mJavaCameraView != null) {
            mJavaCameraView.disableView();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mJavaCameraView != null) {
            mJavaCameraView.disableView();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (OpenCVLoader.initDebug()) {
            Log.i(TAG, "OpenCV loaded successfully!");
            mLoaderCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        else {
            Log.i(TAG, "OpenCV NOT loaded successfully!");
            // Try to load OpenCV again in case of failure
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallBack);
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        imgGrey = new Mat(height, width, CvType.CV_8UC1);
        imgCanny = new Mat(height, width, CvType.CV_8UC1);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        // Some Basic Image Processing
        Imgproc.cvtColor(mRgba, imgGrey, Imgproc.COLOR_RGB2GRAY);
        Imgproc.Canny(imgGrey, imgCanny, 50, 150);

        return imgCanny;
    }
}
