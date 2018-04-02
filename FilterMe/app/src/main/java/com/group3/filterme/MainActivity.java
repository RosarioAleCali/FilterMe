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
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Core;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;
import org.opencv.core.Size;
import org.opencv.core.Scalar;
import java.util.Vector;


import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.equalizeHist;

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

        Mat gray = mRgba, frame = mRgba;
        cvtColor(mRgba, gray, Imgproc.COLOR_BGR2GRAY);

        Vector<Rect> faces = new Vector<>();

        cvtColor( frame, gray, Imgproc.COLOR_BGR2GRAY);
        equalizeHist( gray, gray);

        CascadeClassifier face_cascade = new CascadeClassifier(), eyes_cascade = new CascadeClassifier();

        MatOfRect face = new MatOfRect(faces.get(0));

        //7 args
        //CV_HAAR_SCALE IMAGE is 2 (https://docs.opencv.org/3.4.0/d9/d31/group__objdetect__c.html#ga812f46d031349fa2ee78a5e7240f5016)
        face_cascade.detectMultiScale( gray, face, 1.1, 2, 2, new Size(30, 30), new Size() );

        for( Integer i = 0; i < faces.size(); i++ )
        {

            Point center = new Point( faces.get(i).x + faces.get(i).width*0.5, faces.get(i).y + faces.get(i).height*0.5 );
            Imgproc.ellipse( frame, center, new Size( faces.get(i).width*0.5, faces.get(i).height*0.5), 0, 0, 360, new Scalar( 255, 0, 255 ), 4, 8, 0 );

            Mat faceROI = gray(faces.get(i));
            Vector<Rect> eyes = new Vector<>();

            //detect eyes
            eyes_cascade.detectMultiScale( faceROI, eyes, 1.1, 2, 2, new Size(30, 30), new Size() );

            for( Integer j = 0; j < eyes.size(); j++ )
            {
                //draw eyes and face
                center = new Point ( faces.get(i).x + eyes.get(j).x + eyes.get(j).width*0.5, faces.get(i).y + eyes.get(j).y + eyes.get(j).height*0.5 );
                long rad =  Math.round((eyes.get(j).width + eyes.get(j).height)*0.25 );
                int radius = (int) rad;
                Imgproc.circle( frame, center, radius, new Scalar( 255, 0, 0 ), 4, 8, 0 );
            }
        }

        // Some Basic Image Processing
        cvtColor(mRgba, imgGrey, Imgproc.COLOR_RGB2GRAY);
        Imgproc.Canny(imgGrey, imgCanny, 50, 150);

        return imgCanny;

    }
}
