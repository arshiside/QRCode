package com.example.scanner;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.hardware.Camera;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.List;

public class Scanner extends Activity implements View.OnClickListener {

    private SurfaceView preview = null;
    private SurfaceHolder previewHolder = null;
    static private Camera camera = null;
    private boolean inPreview = false;
    ImageView image;
    Bitmap bmp, itembmp;
    static Bitmap mutableBitmap;
    PointF start = new PointF();
    PointF mid = new PointF();
    float oldDist = 1f;
    File imageFileName = null;
    File imageFileFolder = null;
    private MediaScannerConnection msConn;
    Display d;
    int screenhgt, screenwdh;
    //ProgressDialog dialog;
    //
    private static final String LOG_TAG = "Barcode Scanner API";
    private static final int PHOTO_REQUEST = 10;
    //private TextView scanResults;
    private BarcodeDetector detector;
    private Uri imageUri;
    private static final int REQUEST_WRITE_PERMISSION = 20;
    private static final String SAVED_INSTANCE_URI = "uri";
    private static final String SAVED_INSTANCE_RESULT = "result";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        image = (ImageView) findViewById(R.id.imageView);
        preview = (SurfaceView) findViewById(R.id.surfaceView);

        previewHolder = preview.getHolder();
        previewHolder.addCallback(surfaceCallback);
        previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        previewHolder.setFixedSize(getWindow().getWindowManager()
                .getDefaultDisplay().getWidth(), getWindow().getWindowManager()
                .getDefaultDisplay().getHeight());

        detector = new BarcodeDetector.Builder(getApplicationContext())
                .setBarcodeFormats(Barcode.DATA_MATRIX | Barcode.QR_CODE)
                .build();
        if (!detector.isOperational()) {
            Toast.makeText(this, "Could not set up the detector!", Toast.LENGTH_SHORT).show();

            return;
        }
        preview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (camera != null) {
              //  camera.setParameters(Camera.);
                    camera.takePicture(null,null,photoCallback);

                }
                return false;
            }
        });
    }



    @Override
    public void onResume() {
        super.onResume();
        camera = Camera.open();
        camera.setDisplayOrientation(90);
    }

    @Override
    public void onPause() {
        if (inPreview) {
            camera.stopPreview();
        }

        camera.release();
        camera = null;
        inPreview = false;
        super.onPause();
    }

    private Camera.Size getBestPreviewSize(int width, int height, Camera.Parameters parameters) {
        Camera.Size result = null;
        for (Camera.Size size: parameters.getSupportedPreviewSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = size;
                } else {
                    int resultArea = result.width * result.height;
                    int newArea = size.width * size.height;
                    if (newArea > resultArea) {
                        result = size;
                    }
                }
            }
        }
        return (result);
    }

    SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                camera.setPreviewDisplay(previewHolder);
            } catch (Throwable t) {
                Log.e("CAMERA",
                        "Exception in setPreviewDisplay()", t);
                Toast.makeText(Scanner.this, t.getMessage(), Toast.LENGTH_LONG)
                        .show();
            }
        }

        public void surfaceChanged(SurfaceHolder holder,
                                   int format, int width,
                                   int height) {

            Camera.Parameters parameters = camera.getParameters();
            Camera.Size size = getBestPreviewSize(width, height,
                    parameters);

            if (size != null) {
             /*   parameters.setPreviewSize(size.width, size.height);
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                parameters.setExposureCompensation(parameters.getMaxExposureCompensation());

                if(parameters.isAutoExposureLockSupported()) {
                    parameters.setAutoExposureLock(false);
                }

                camera.setParameters(parameters);
                camera.startPreview();*/

                List<Camera.Size> sizes = parameters.getSupportedPictureSizes();
                Camera.Size size1 = sizes.get(0);
                for(int i=0;i<sizes.size();i++)
                {
                    if(sizes.get(i).width > size.width)
                        size1 = sizes.get(i);
                }
                parameters.setPictureSize(size1.width, size1.height);
camera.startPreview();
                // camera.setAutoFocusMoveCallback(focusCallback);
//camera.autoFocus(autoCallBack);
                inPreview = true;
            }
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            // no-op
        }
    };
    Camera.AutoFocusCallback autoCallBack=new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {

        }
    };
    private Bitmap decodeBitmapUri(Context ctx, Uri uri) throws FileNotFoundException {
        int targetW = 600;
        int targetH = 600;
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(ctx.getContentResolver().openInputStream(uri), null, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        return BitmapFactory.decodeStream(ctx.getContentResolver()
                .openInputStream(uri), null, bmOptions);
    }
Camera.AutoFocusMoveCallback focusCallback=new Camera.AutoFocusMoveCallback() {
    @Override
    public void onAutoFocusMoving(boolean start, Camera camera) {
      //  camera.takePicture(null, null, photoCallback);


    }
};


    Frame frame;
    SparseArray<Barcode> barcodes;
    Camera.PictureCallback photoCallback = new Camera.PictureCallback() {
        public void onPictureTaken(final byte[] data, final Camera camera) {


            try {
                bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
              //  mutableBitmap = bmp.copy(Bitmap.Config.ARGB_8888, true);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    // recreate the new Bitmap


                    savePhoto(bmp);
                    imageUri = Uri.fromFile(imageFileName);
                    Bitmap bmp1=null;
                    try
                    {
                        bmp1=decodeBitmapUri(Scanner.this,imageUri);
                        Log.i("CAMERA","file path="+imageUri.getPath());
                    }catch (Exception e){
                        Log.i("CAMERA","error"+e.getLocalizedMessage());
                    }


if(bmp1!=null){
    frame = new Frame.Builder().setBitmap(bmp1).build();
    barcodes = detector.detect(frame);
    Log.i("CAMERA","no values found"+barcodes);

    if(barcodes!=null){
        String text=null;
        for (int index = 0; index < barcodes.size(); index++) {
            Barcode code = barcodes.valueAt(index);
             text = code.displayValue;

            Toast.makeText(Scanner.this, "Value Found: " +code.displayValue , Toast.LENGTH_SHORT).show();

        }
        Intent data = new Intent();

//---set the data to pass back---
        data.putExtra("value",text);

        setResult(RESULT_OK, data);
//---close the activity---
        finish();



    }else{
        Log.i("CAMERA","barcode is null"+barcodes);
    }
}else{
    Log.i("CAMERA","bmp null");
}


                    savePhoto(bmp1);
                }
            });
                camera.startPreview();
            }catch (Exception e){
                Log.i("CAMERA","error="+e.getLocalizedMessage());
                // Toast.makeText(Scanner.this, "error", Toast.LENGTH_SHORT).show();
                camera.startPreview();
            }
            // MyScanner scanner=new MyScanner();
            //scanner.execute(mutableBitmap);
        }
    };
    private void launchMediaScanIntent() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(imageUri);
        this.sendBroadcast(mediaScanIntent);
    }

    public void onPictureTake(byte[] data, Camera camera) {

        bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
        mutableBitmap = bmp.copy(Bitmap.Config.ARGB_8888, true);

       // savePhoto(mutableBitmap);
       // dialog.dismiss();
    }


    class SavePhotoTask extends AsyncTask<byte[], String, String> {
        @Override
        protected String doInBackground(byte[]... jpeg) {
            File photo = new File(Environment.getExternalStorageDirectory(), "photo.jpg");
            if (photo.exists()) {
                photo.delete();
            }
            try {
                FileOutputStream fos = new FileOutputStream(photo.getPath());
                fos.write(jpeg[0]);
                fos.close();
            } catch (java.io.IOException e) {
                Log.e("CAMERA", "Exception in photoCallback", e);
            }
            return (null);
        }
    }


    public void savePhoto(Bitmap bmp) {
        imageFileFolder = new File(Environment.getExternalStorageDirectory(), "Rotate");
        imageFileFolder.mkdir();
        FileOutputStream out = null;
        Calendar c = Calendar.getInstance();
        String date = fromInt(c.get(Calendar.MONTH)) + fromInt(c.get(Calendar.DAY_OF_MONTH)) + fromInt(c.get(Calendar.YEAR)) + fromInt(c.get(Calendar.HOUR_OF_DAY)) + fromInt(c.get(Calendar.MINUTE)) + fromInt(c.get(Calendar.SECOND));
        imageFileName = new File(imageFileFolder, date.toString() + ".jpg");
        try {
            out = new FileOutputStream(imageFileName);
            Matrix matrix=new Matrix();
            matrix.setRotate(90);
            // recreate the new Bitmap

            int x = bmp.getWidth();
            int y = bmp.getHeight();
            Bitmap resizedBitmap = Bitmap.createBitmap(bmp, 0, 0,
                    x, y, matrix, true);
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);


            out.flush();
            out.close();

            out = null;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
public  class MyScanner extends AsyncTask<Bitmap,Void,SparseArray<Barcode>>{


    @Override
    protected SparseArray<Barcode> doInBackground(Bitmap... params) {
        SparseArray<Barcode> barcodes=null;
        try {


            //if (detector.isOperational() && params[0] != null) {
                Frame frame = new Frame.Builder().setBitmap(params[0]).build();
               barcodes = detector.detect(frame);

              /*  for (int index = 0; index < barcodes.size(); index++) {
                    Barcode code = barcodes.valueAt(index);
                 //   Toast.makeText(Scanner.this, "" + code.displayValue, Toast.LENGTH_SHORT).show();


                    //Required only if you need to extract the type of barcode
                    int type = barcodes.valueAt(index).valueFormat;
                    switch (type) {
                        case Barcode.CONTACT_INFO:
                            Log.i(LOG_TAG, code.contactInfo.title);
                            break;
                        case Barcode.EMAIL:
                            Log.i(LOG_TAG, code.email.address);
                            break;
                        case Barcode.ISBN:
                            Log.i(LOG_TAG, code.rawValue);
                            break;
                        case Barcode.PHONE:
                            Log.i(LOG_TAG, code.phone.number);
                            break;
                        case Barcode.PRODUCT:
                            Log.i(LOG_TAG, code.rawValue);
                            break;
                        case Barcode.SMS:
                            Log.i(LOG_TAG, code.sms.message);
                            break;
                        case Barcode.TEXT:
                            Log.i(LOG_TAG, code.rawValue);
                            break;
                        case Barcode.URL:
                            Log.i(LOG_TAG, "url: " + code.url.url);
                            break;
                        case Barcode.WIFI:
                            Log.i(LOG_TAG, code.wifi.ssid);
                            break;
                        case Barcode.GEO:
                            Log.i(LOG_TAG, code.geoPoint.lat + ":" + code.geoPoint.lng);
                            break;
                        case Barcode.CALENDAR_EVENT:
                            Log.i(LOG_TAG, code.calendarEvent.description);
                            break;
                        case Barcode.DRIVER_LICENSE:
                            Log.i(LOG_TAG, code.driverLicense.licenseNumber);
                            break;
                        default:
                            Log.i(LOG_TAG, code.rawValue);
                            break;
                    }
                }*/


           /* } else {
               // Toast.makeText(Scanner.this, "Could not set up the detector!", Toast.LENGTH_SHORT);

            }*/
        } catch (Exception e) {
           // Toast.makeText(Scanner.this, "Failed to load Image", Toast.LENGTH_SHORT)
              //     .show();
            Log.e(LOG_TAG, e.toString());
        }

        return barcodes;
    }

    @Override
    protected void onPostExecute(final SparseArray <Barcode>sparseArray) {
        super.onPostExecute(sparseArray);

        if(sparseArray!=null){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Barcode code = sparseArray.valueAt(0);
                        Intent data = new Intent();
                        String text = code.displayValue;
//---set the data to pass back---
                        data.putExtra("value",text);

                        setResult(RESULT_OK, data);
//---close the activity---
                        finish();

                        Toast.makeText(Scanner.this, "Value Found11: "+code.displayValue, Toast
                                .LENGTH_SHORT).show();
                    }catch (Exception e){
                        Log.i("CAMERA","error="+e.getLocalizedMessage());
                       // Toast.makeText(Scanner.this, "error", Toast.LENGTH_SHORT).show();
                    }

                }
            });
        }
    }
}



    public String fromInt(int val) {
        return String.valueOf(val);
    }



    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU && event.getRepeatCount() == 0) {
            onBack();
        }
        return super.onKeyDown(keyCode, event);
    }

    public void onBack() {
        Log.e("onBack :", "yes");
        camera.takePicture(null, null, photoCallback);
        inPreview = false;

    }

    @Override
    public void onClick(View v) {

    }

}
