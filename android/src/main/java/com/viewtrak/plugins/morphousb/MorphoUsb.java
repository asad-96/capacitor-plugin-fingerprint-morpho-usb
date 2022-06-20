package com.viewtrak.plugins.morphousb;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.StrictMode;
import android.util.Log;
import android.widget.Button;

import com.getcapacitor.PluginCall;
import com.morpho.android.usb.USBManager;
import com.morpho.morphosmart.sdk.MorphoDevice;
import com.rscja.deviceapi.UsbFingerprint;

public class MorphoUsb {
    private String TAG = MorphoUsb.class.getName();

    MorphoDevice morphoDevice;
    Activity activity;
    Boolean hasUsbPermission = false;

    static {
        try {
            System.loadLibrary("MSO_Secu");
        } catch (UnsatisfiedLinkError e) {
            Log.e("MorphoSample", "Exception in loadLibrary: " + e);
            e.printStackTrace();
        }
    }

    public String echo(String value) {
        Log.i("Echo", value);
        return value;
    }

    public void init(Activity activity) {
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        this.activity = activity;
        if (this.hasUsbPermission) {
            morphoDevice = new MorphoDevice();
            checkReadWritePermission(activity);
        } else {
            Log.e(TAG, "Device doesn't have USB permission");
        }
    }

    public void openDevice(Activity activity, PluginCall call) {

    }

    public class InitTask extends AsyncTask<String, Integer, Boolean> {
        ProgressDialog mypDialog;

        @Override
        protected Boolean doInBackground(String... params) {

            UsbFingerprint.getInstance().FingerprintSwitchUsb();

            UsbFingerprint.getInstance().UsbToFingerprint();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            USBManager.getInstance().initialize(MorphoUsb.this.activity, "com.morpho.morphosample.USB_ACTION", true);

            if(USBManager.getInstance().isDevicesHasPermission() == true)
            {
                return  false;
            }
            return  true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if(!result) {
                MorphoUsb.this.hasUsbPermission = true;
            }

        }

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();
            MorphoUsb.this.hasUsbPermission = false;
        }

    }
    private void checkReadWritePermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            }
            if (activity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
        }
    }
}
