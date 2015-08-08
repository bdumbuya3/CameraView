package com.business.cameraview;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.ErrorCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.os.Environment;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;
//import android.R.drawable;
//import android.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

//import static com.business.cameraview.R.drawable.ic_launcher;



public class CameraView extends Activity implements SurfaceHolder.Callback,View.OnClickListener {
    private SurfaceView surfaceView;
    private SurfaceHolder holder;
    private Camera camera;
    private Button snap;
    private Button flash;
    private Button flip;
    private int cameraId;
    private boolean flashMode = false;
    private int rotation;
    /*private String[] idList;
    private CameraDevice cameraDevice;
    private CaptureRequest.Builder previewBuilder;
    private CameraCaptureSession previewSession;

    private final static String TAG = "Camera2 testing";
    private Button shot;*/

    /*private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_view);
        cameraId = CameraInfo.CAMERA_FACING_BACK;
        snap = (Button) findViewById(R.id.bSnap);
        flash = (Button) findViewById(R.id.bFlash);
        flip = (Button) findViewById(R.id.bFlip);
        surfaceView = (SurfaceView) findViewById(R.id.svCamView);

        holder = surfaceView.getHolder();
        holder.addCallback(this);
        flip.setOnClickListener(this);
        snap.setOnClickListener(this);
        flash.setOnClickListener(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if(Camera.getNumberOfCameras()>1) {
            flip.setVisibility(View.VISIBLE);
        }
        if(!getBaseContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            flash.setVisibility(View.GONE);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if(!openCamera(android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK)){
            alertCameraDialog();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }


    private void alertCameraDialog() {
        AlertDialog.Builder dialog = createAlert(CameraView.this, "Camera info", "Failure to open");
        dialog.setNegativeButton("OK", new DialogInterface.OnClickListener() {
            @Override
        public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        dialog.show();
    }

    private boolean openCamera(int id) {
        boolean result = false;
        cameraId = id;
        releaseCamera();
        try {
            camera = Camera.open(cameraId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (camera != null) {
            try {
                setUpCamera(camera);
                camera.setErrorCallback(new ErrorCallback() {
                    @Override
                    public void onError(int error, Camera camera) {

                    }
                });
                camera.setPreviewDisplay(holder);
                camera.startPreview();
                result = true;
                } catch (IOException e) {
                result = false;
                releaseCamera();
            }
        }
        return result;
    }

    private void releaseCamera() {
        try {
            if (camera != null) {
                camera.setPreviewCallback(null);
                camera.setErrorCallback(null);
                camera.stopPreview();
                camera.release();
                camera = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("error", e.toString());
            camera = null;
        }
    }

    private Builder createAlert(Context context, String title, String message) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(new ContextThemeWrapper(
                    context, android.R.style.Theme_Holo_Light_Dialog));
        dialog.setIcon(android.R.drawable.btn_dialog);
        if(title != null) {
            dialog.setTitle(title);
        } else {
            dialog.setTitle("Information");
        }
            dialog.setMessage(message);
            dialog.setCancelable(false);
            return dialog;
    }
    private void setUpCamera(Camera c) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degree = 0;
        switch(rotation) {
            case Surface.ROTATION_0:
                degree = 0;
                break;
            case  Surface.ROTATION_90:
                degree = 90;
                break;
            case Surface.ROTATION_180:
                degree = 180;
                break;
            case Surface.ROTATION_270:
                degree = 270;
                break;
            default:
                break;
        }
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            rotation = (info.orientation - degree) % 330;
            rotation = (360 - rotation) % 360;
        } else {
            rotation = (info.orientation - degree + 360) % 360;
        }
        c.setDisplayOrientation(rotation);
        Parameters params = c.getParameters();

        showFlashButton(params);

        List<String> focusModes = params.getSupportedFlashModes();
        if (focusModes != null) {
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                params.setFlashMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }
        }
        params.setRotation(rotation);
    }

    private void showFlashButton(Parameters params) {
        boolean showFlash = (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
            && params.getSupportedFlashModes() != null && params.getSupportedFocusModes().size() >1);
        flash.setVisibility(showFlash ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bFlash:
                flashOnButton();
                break;
            case R.id.bFlip:
                flipCamera();
                break;
            case R.id.bSnap:
                takeImage();
                break;
            default:
                break;
        }
    }
    private void flashOnButton() {
        if(camera != null) {
            try {
                Parameters param = camera.getParameters();
                param.setFlashMode(!flashMode ? Parameters.FLASH_MODE_TORCH: Parameters.FLASH_MODE_OFF);
                camera.setParameters(param);
                flashMode = !flashMode;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    private void flipCamera() {
        int id = (cameraId == CameraInfo.CAMERA_FACING_BACK ? CameraInfo.CAMERA_FACING_FRONT:
            CameraInfo.CAMERA_FACING_BACK);
        if (!openCamera(id)) {
            alertCameraDialog();
        }
    }
    private void takeImage() {
        camera.takePicture(null, null, new PictureCallback() {
            private File imageFile;

            @Override
                    public void onPictureTaken( byte[] data, Camera camera) {
                try {
                    Bitmap loadedImage = BitmapFactory.decodeByteArray(data, 0, data.length);
                    Matrix rotateMatrix = new Matrix();
                    rotateMatrix.postRotate(rotation);
                    Bitmap rotatedBitmap = Bitmap.createBitmap(loadedImage, 0, 0, loadedImage.getWidth(),
                            loadedImage.getHeight(),rotateMatrix,false);
                    String state = Environment.getExternalStorageState();
                    File folder = null;
                    if (state.contains(Environment.MEDIA_MOUNTED)) {
                        folder = new File(Environment.getExternalStorageDirectory() + "/Demo");
                    } else {
                        folder = new File(Environment.getExternalStorageDirectory() + "/Demo");
                    }
                    boolean success = true;
                    if (!folder.exists()) {
                        success = folder.mkdirs();
                    }
                    if (success) {
                        java.util.Date date = new Date();
                        imageFile = new File(folder.getAbsolutePath() + File.separator +
                        new Timestamp(date.getTime()).toString() + "Image.jpg");
                        imageFile.createNewFile();
                    } else {
                        Toast.makeText(getBaseContext(), "Image not saved", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    ByteArrayOutputStream ostream = new ByteArrayOutputStream();
                    rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, ostream);
                    FileOutputStream fout = new FileOutputStream(imageFile);
                    fout.write(ostream.toByteArray());
                    fout.close();
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                    values.put(MediaStore.MediaColumns.DATA, imageFile.getAbsolutePath());
                    CameraView.this.getContentResolver().insert(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
