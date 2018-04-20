package com.group3.filterme;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import static org.opencv.android.CameraBridgeViewBase.CAMERA_ID_FRONT;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static String TAG = "MainActivity";
    private static String successMessage = "Photo Taken ;)";
    private static String errorMessage = "Error saving photo :(";
    JavaCameraView mJavaCameraView;
    Mat mRgba, mask;
    CascadeClassifier faceRec;
    ImageButton cameraButton, galleryButton;
    Boolean success = false;
    double minFaceSize = 20.0;
    double maxFaceSize = 200.0;
    private BaseLoaderCallback mLoaderCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                /*case BaseLoaderCallback.SUCCESS:
                    mJavaCameraView.enableView();
                    loadCascade();
                    break;*/
                case LoaderCallbackInterface.SUCCESS:
                    mJavaCameraView.enableView();
                    mask = new Mat();
                    loadCascade();
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
        // Set Frontal Camera
        mJavaCameraView.setCameraIndex(CAMERA_ID_FRONT);
        // Set a listener for the camera
        mJavaCameraView.setCvCameraViewListener(this);

        // Set Up Camera Button
        cameraButton = (ImageButton)findViewById(R.id.cameraButton);
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Take a picture :)
                Mat mIntermediateMat = new Mat();
                Imgproc.cvtColor(mRgba, mIntermediateMat, Imgproc.COLOR_RGBA2BGR, 3);

                // Get current Date and Time
                String currentDateTimeString = new SimpleDateFormat("YYYYMMDDHHmmss").format(new Date());

                // Create Path + Filename
                String filename = Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DCIM + "/Camera/";
                filename += currentDateTimeString + ".jpg";

                // Save Image
                success = Imgcodecs.imwrite(filename, mIntermediateMat);

                if (success == true) {
                    Toast.makeText(getApplicationContext(), successMessage, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "SUCCESS writing image to external storage");
                }
                else {
                    Log.d(TAG, "Fail writing image to external storage");
                    Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_SHORT).show();
                }
            }
        });
        // Set Up Gallery Button
        galleryButton = (ImageButton)findViewById(R.id.galleryButton);
        galleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Go to gallery :)
                Intent openGallery = new Intent(Intent.ACTION_VIEW, Uri.parse("content://media/internal/images/media"));
                startActivity(openGallery);
            }
        });
    }

    private void loadMask() {
        InputStream stream = null;
        Uri uri = Uri.parse("android.resource://com.group3.filterme/drawable/the_mask");
        try {
            stream = getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
        bmpFactoryOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;

        Bitmap bmp = BitmapFactory.decodeStream(stream, null, bmpFactoryOptions);
        Utils.bitmapToMat(bmp, mask);
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
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        findFaces();
        return mRgba;
    }

    public void loadCascade() {
        try {
            InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_alt2);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "haarcascade_frontalface_alt2.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            faceRec = new CascadeClassifier(mCascadeFile.getAbsolutePath());
            if (faceRec.empty()) {
                Log.d(TAG, "Error loading FaceCascade");
                return;
            }
            else {
                Log.d(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "Failed to load cascade. Exception thrown: " + e);
        }
    }

    // Function used to detect faces
    public void findFaces() {
        // Load Mask
        loadMask();

        // Find Faces
        MatOfRect faces = new MatOfRect();
        Mat grey = new Mat();
        Imgproc.cvtColor(mRgba, grey, Imgproc.COLOR_RGB2BGR);
        faceRec.detectMultiScale(grey, faces, 1.2, 2, 0|2, new Size(0, 0), new Size(400, 400));

        List<Rect> faces_ =  faces.toList();

        // draw circles on the detected faces
        if (!faces_.isEmpty()) {
            for (int i = 0; i < faces_.size(); i++) {
                minFaceSize = faces_.get(i).width * 0.7;
                maxFaceSize = faces_.get(i).height * 1.5;
                Point centre = new Point(faces_.get(i).x + faces_.get(i).width * 0.5, faces_.get(i).y + faces_.get(i).height * 0.55);

//              // START JUNK
                Imgproc.rectangle(mRgba, new Point(centre.x - faces_.get(i).width / 2, centre.y - faces_.get(i).width / 2),
                       new Point(centre.x + faces_.get(i).width / 2, centre.y + faces_.get(i).width / 2), new Scalar(0,0,0));
//                // Create Path + Filename
                String filename = Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DCIM + "/Camera/file.jpg";
//
//                // Save Image
//                success = Imgcodecs.imwrite(filename, mRgba);
//                // END JUNK

                //filterMe(centre, new Size(faces_.get(i).width, faces_.get(i).height));
            }
        }
    }

    public void filterMe(Point centre, Size faceSize) {
        Mat mask1 = new Mat();
        Mat mRgba_ = new Mat();
        Imgproc.resize(mask, mask1, faceSize);

        // ROI selection
        Rect roi = new Rect((int)(centre.x - faceSize.width / 2), (int)(centre.y - faceSize.width / 2), (int)(faceSize.width), (int)(faceSize.height));
        // Copy sub-rectangle to new mat
        mRgba.submat(roi).copyTo(mRgba_);

        // make white region of mask transparent
        Mat mask2 = new Mat();
        Mat m1 = new Mat();
        Mat m2 = new Mat();
        Imgproc.cvtColor(mask1, mask2, Imgproc.COLOR_BGR2GRAY);
        Imgproc.threshold(mask2, mask2, 230, 255, Imgproc.THRESH_BINARY_INV);

        //START JUNK
        // Create Path + Filename
        //String filename = Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DCIM + "/Camera/file.jpg";

        // Save Image
        //success = Imgcodecs.imwrite(filename, mask2);
        // END JUNK

        Vector<Mat> maskChannels = new Vector<>(3);
        Vector<Mat> resultMask = new Vector<>(3);
        Core.split(mask1, maskChannels);
        for (int i = 0; i < 3; i++) {
            Mat tmp = new Mat();
            Core.bitwise_and(maskChannels.get(i), mask2, tmp);
            resultMask.add(tmp);
        }
        Core.merge(resultMask, m1);

        Mat whites = new Mat(mask2.rows(), mask2.cols(), mask2.type(), new Scalar(255, 255, 255));
        Core.subtract(whites, mask2, mask2);

        Vector<Mat> srcChannels = new Vector<>(3);
        Core.split(mRgba_, srcChannels);
        for (int i = 0; i < 3; i++) {
            Core.bitwise_and(srcChannels.get(i), mask2, resultMask.get(i));
        }
        Core.merge(resultMask, m2);

        Core.addWeighted(m1, 1, m2, 1, 0, m2);

        int startX = (int)(centre.x - (faceSize.width / 2));
        int startY = (int)(centre.y - (faceSize.width / 2));
        //byte[] pixel = new byte[3];

        // Copy sub-rectangle to new mat
        /*for (int row = 0; row < m2.rows(); row++) {
            for (int col = 0; row < m2.rows(); row++) {
                m2.get(row, col,pixel);
                mRgba.put(startX + row, startY + col, pixel);
            }
        }*/
        //m2.copyTo(mRgba.submat(roi));
        //.copyTo(mRgba.submat(roi));

        //START JUNK
        // Create Path + Filename
        //String filename = Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DCIM + "/Camera/file.jpg";

        // Save Image
        //success = Imgcodecs.imwrite(filename, m2);
        // END JUNK
    }
}