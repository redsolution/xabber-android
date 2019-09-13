package com.xabber.android.ui.activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NavUtils;

import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.camera.CameraSettings;
import com.xabber.android.R;



public class QRCodeScannerActivity extends AppCompatActivity implements DecoratedBarcodeView.TorchListener, Toolbar.OnMenuItemClickListener {

    private CaptureManager capture;
    private DecoratedBarcodeView qrScannerView;
    private String accountName;
    private String accountAddress;
    private Toolbar toolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_custom_qr_code_scanner);

        qrScannerView = (DecoratedBarcodeView)findViewById(R.id.custom_qr_scanner);
        qrScannerView.setTorchListener(this);

        toolbar = (Toolbar) findViewById(R.id.scanner_toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        toolbar.setTitle("Scan QR Code");
        toolbar.setTitleTextColor(Color.WHITE);
        //toolbar.setBackgroundColor(Color.GRAY);
        toolbar.inflateMenu(R.menu.toolbar_qrscanner);
        //setSupportActionBar(toolbar);
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setOnMenuItemClickListener(this);
        //getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_left_white_24dp);


        Button testButton = findViewById(R.id.button2);
        TextView msg = findViewById(R.id.addMessage);
        /*testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(QRCodeScannerActivity.this, QRCodeActivity.class);
                intent.putExtra("account_name", accountName);
                intent.putExtra("account_address", accountAddress);
                startActivity(intent);
            }
        });

        Intent intent = getIntent();
        if(intent.hasExtra("account_name")&&intent.hasExtra("account_address")) {
            testButton.setVisibility(View.VISIBLE);
            Bundle bundle = intent.getExtras();
            accountName = bundle.get("account_name").toString();
            accountAddress = bundle.get("account_address").toString();
        }else {
            testButton.setVisibility(View.GONE);
        }*/
        testButton.setVisibility(View.GONE);
        Intent intent = getIntent();
        if(intent.hasExtra("caller")){
            Bundle bundle = intent.getExtras();
            if(bundle.get("caller").toString().equals("AccountAddFragment"))
                msg.setText("Place the Account QR code in the center of the highlighted area.");
            else
                msg.setText("Place the Contact QR code in the center of the highlighted area.");
        }

        if(Camera.getNumberOfCameras() == 1){
            toolbar.getMenu().findItem(R.id.switch_camera).setVisible(false);
        }

        capture = new CaptureManager(this, qrScannerView);
        capture.initializeFromIntent(getIntent(),savedInstanceState);
        capture.decode();
    }

    @Override
    protected void onResume() {
        super.onResume();
        capture.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        capture.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        capture.onDestroy();
    }

    @Override
    public boolean onSupportNavigateUp(){
        onBackPressed();
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        capture.onSaveInstanceState(outState);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        capture.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return qrScannerView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }

    private boolean hasFlash() {
        return getApplicationContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    public void switchFlashlight(MenuItem item) {
        if (!item.isChecked()) {
            qrScannerView.setTorchOn();
            item.setChecked(true);
            item.setIcon(R.drawable.ic_flashlight_off);
        } else {
            qrScannerView.setTorchOff();
            item.setChecked(false);
            item.setIcon(R.drawable.ic_flashlight_on);
        }
    }

    @Override
    public void onTorchOn() {

    }

    @Override
    public void onTorchOff() {
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater=getMenuInflater();
        inflater.inflate(R.menu.toolbar_qrscanner, menu);
        toolbar.getMenu().findItem(R.id.switch_flashlight).setCheckable(true);
        if(!hasFlash()) return true;
            //toolbar.getMenu().findItem(R.id.switch_flashlight).setVisible(false);
        return true;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch(item.getItemId()){
            case R.id.switch_camera:
                CameraSettings settings = qrScannerView.getBarcodeView().getCameraSettings();


                if(qrScannerView.getBarcodeView().isPreviewActive()) {
                    qrScannerView.pause();
                }

                if (settings.getRequestedCameraId() == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    settings.setRequestedCameraId(Camera.CameraInfo.CAMERA_FACING_FRONT);
                } else {
                    settings.setRequestedCameraId(Camera.CameraInfo.CAMERA_FACING_BACK);
                }
                qrScannerView.getBarcodeView().setCameraSettings(settings);

                qrScannerView.resume();
                return true;
            case R.id.switch_flashlight:
                switchFlashlight(item);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

