package com.viewtrak.plugins.morphousb;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.morpho.android.usb.USBManager;
import com.morpho.morphosmart.sdk.CallbackMask;
import com.morpho.morphosmart.sdk.CallbackMessage;
import com.morpho.morphosmart.sdk.Coder;
import com.morpho.morphosmart.sdk.CompressionAlgorithm;
import com.morpho.morphosmart.sdk.DetectionMode;
import com.morpho.morphosmart.sdk.ErrorCodes;
import com.morpho.morphosmart.sdk.FieldAttribute;
import com.morpho.morphosmart.sdk.FingerNumber;
import com.morpho.morphosmart.sdk.IMsoSecu;
import com.morpho.morphosmart.sdk.MatchingStrategy;
import com.morpho.morphosmart.sdk.MorphoDatabase;
import com.morpho.morphosmart.sdk.MorphoDevice;
import com.morpho.morphosmart.sdk.MorphoField;
import com.morpho.morphosmart.sdk.MorphoImage;
import com.morpho.morphosmart.sdk.MorphoUser;
import com.morpho.morphosmart.sdk.MorphoUserList;
import com.morpho.morphosmart.sdk.MorphoWakeUpMode;
import com.morpho.morphosmart.sdk.ResultMatching;
import com.morpho.morphosmart.sdk.SecuConfig;
import com.morpho.morphosmart.sdk.TemplateFVPType;
import com.morpho.morphosmart.sdk.TemplateList;
import com.morpho.morphosmart.sdk.TemplateType;
import com.rscja.deviceapi.UsbFingerprint;
import com.viewtrak.plugins.morphousb.database.DatabaseItem;
import com.viewtrak.plugins.morphousb.info.EnrollInfo;
import com.viewtrak.plugins.morphousb.info.MorphoInfo;
import com.viewtrak.plugins.morphousb.info.ProcessInfo;
import com.viewtrak.plugins.morphousb.info.subtype.SecurityOption;
import com.viewtrak.plugins.morphousb.tools.MorphoTools;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;

import morpho.msosecu.sdk.api.MsoSecu;

public class MorphoUsb {
    private String TAG = MorphoUsb.class.getName();

    static MorphoDevice morphoDevice = null;
    static MorphoDatabase morphoDatabase = null;
    Activity activity;
    MorphoUsbPlugin morphoPlugin;
    Boolean hasUsbPermission = false;
    private String sensorName = "";
    private IMsoSecu msoSecu = new MsoSecu();
    private boolean isOfferedSecurityMode = false;
    private boolean isTunnelingMode = false;

    private Handler mHandler = new Handler();

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

    public void init(Activity activity, MorphoUsbPlugin plugin) {
        new InitTask().execute();
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        this.activity = activity;
        this.morphoPlugin = plugin;
//        if (this.hasUsbPermission) {
        morphoDevice = new MorphoDevice();
        morphoDatabase = new MorphoDatabase();

        loadDatabaseItem();
    }

    public void uninit() {
        try {
            morphoDevice.closeDevice();
        } catch (Exception e) {
            Log.e("ERROR", e.toString());
        }
        UsbFingerprint.getInstance().UsbToHost();
    }

    public void openDevice(Activity activity, PluginCall call) {
        JSObject object = new JSObject();
        Integer nbUsbDevice = new Integer(0);
        int ret = morphoDevice.initUsbDevicesNameEnum(nbUsbDevice);
        if (ret == ErrorCodes.MORPHO_OK) {
            if (nbUsbDevice > 0) {
                sensorName = morphoDevice.getUsbDeviceName(0);
            } else {
                object.put("success", false);
                object.put("error", "The device is not detected, or you have not asked USB permissions.");
                call.resolve(object);
                return;
            }
        } else {
            String error = ErrorCodes.getError(ret, morphoDevice.getInternalError());
            object.put("success", false);
            object.put("error", error);
            call.resolve(object);
            return;
        }
//        ret = ErrorCodes.MORPHO_OK;
//        final SecuConfig secuConfig = new SecuConfig();
        ret = morphoDevice.openUsbDevice(sensorName, 0);
        ArrayList<SecurityOption> securityOptions = ProcessInfo.getInstance().getSecurityOptions();
        for (SecurityOption so : securityOptions) {
            if (so.title.equals("Mode Offered Security")) {
                isOfferedSecurityMode = so.activated;
            } else if (so.title.equals("Mode Tunneling")) {
                isTunnelingMode = so.activated;
            }
        }

        if (isOfferedSecurityMode) {
            // Set OpenSSL path
            msoSecu.setOpenSSLPath(AppContext.RootPath + "Keys/");
            // Open Offered security mode
            ret = morphoDevice.offeredSecuOpen(msoSecu);
            Log.i("MORPHO_USB", "Opening device in Offered Security mode returns " + ret);
            if (ret != ErrorCodes.MORPHO_OK) {
                object.put("success", false);
                object.put("error", ErrorCodes.getError(ret, morphoDevice.getInternalError()));
                call.resolve(object);
                return;
            }
        }

        if (isTunnelingMode) {
            // Set OpenSSL path
            msoSecu.setOpenSSLPath(AppContext.RootPath + "Keys/");
            // Get host certificate
            ArrayList<Byte> hostCertificate = new ArrayList<Byte>();
            msoSecu.getHostCertif(hostCertificate);
            // Open Tunneling mode
            ret = morphoDevice.tunnelingOpen(msoSecu, MorphoTools.toByteArray(hostCertificate));
            Log.i("MORPHO_USB", "Opening device in Tunneling mode returns " + ret);
            if (ret != ErrorCodes.MORPHO_OK) {
                object.put("success", false);
                object.put("error", ErrorCodes.getError(ret, morphoDevice.getInternalError()));
                call.resolve(object);
                return;
            }
        }

        if (ret == ErrorCodes.MORPHO_OK) {
            ret = morphoDevice.getDatabase(0, morphoDatabase);
            Log.i("MORPHO_USB", "morphoDevice.getDatabase = " + ret);
            if (ret != ErrorCodes.MORPHO_OK) {
                String error = ErrorCodes.getError(ret, morphoDevice.getInternalError());
                object.put("success", false);
                object.put("error", error);
                call.resolve(object);
                return;
            }
            ProcessInfo.getInstance().setMorphoDevice(morphoDevice);
            ProcessInfo.getInstance().setMorphoDatabase(morphoDatabase);
            object.put("success", true);
            object.put("error", "Opened the device");
        } else {
            String error = ErrorCodes.getError(ret, morphoDevice.getInternalError());
            object.put("success", false);
            object.put("error", error);
        }
        call.resolve(object);
    }

    public void captureFingerprint(Activity activity, PluginCall call) {
        String userId = call.getString("userId", "");
        String firstName = call.getString("firstName", "");
        String lastName = call.getString("lastName", "");

        if ((userId == null || userId.isEmpty())) {
            JSObject object = new JSObject();
            object.put("success", false);
            object.put("message", "Invalid userId");
            object.put("rawBitmap", null);
            call.resolve(object);
            return;
        }
        if ((firstName == null || firstName.isEmpty())) {
            JSObject object = new JSObject();
            object.put("success", false);
            object.put("message", "Invalid firstName");
            object.put("rawBitmap", null);
            call.resolve(object);
            return;
        }
        if ((lastName == null || lastName.isEmpty())) {
            JSObject object = new JSObject();
            object.put("success", false);
            object.put("message", "Invalid lastName");
            object.put("rawBitmap", null);
            call.resolve(object);
            return;
        }
        try {
            ProcessInfo.getInstance().setCommandBioStart(true);
            ProcessInfo.getInstance().setStarted(true);
        } catch (Exception e) {
            JSObject object = new JSObject();
            object.put("success", false);
            object.put("message", "Process Failed");
            object.put("rawBitmap", null);
            call.resolve(object);
            return;
        }

        Thread commandThread = (new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                JSObject jsObject = new JSObject();
                MorphoUser morphoUser = new MorphoUser();
                int ret = 0;

                ret = morphoDatabase.getUser(userId, morphoUser);
                if (ErrorCodes.MORPHO_OK == ret) {
                    ret = morphoUser.putField(1, MorphoTools.checkfield(firstName, false));
                    if (ErrorCodes.MORPHO_OK == ret) {
                        ret = morphoUser.putField(2, MorphoTools.checkfield(lastName, false));
                    }
                }
//                }
                if (ret == ErrorCodes.MORPHO_OK) {
                    ProcessInfo processInfo = ProcessInfo.getInstance();
                    if (processInfo.isNoCheck()) {
                        morphoUser.setNoCheckOnTemplateForDBStore(true);
                    }
                    int timeout = processInfo.getTimeout();

                    int acquisitionThreshold = 0;
                    int advancedSecurityLevelsRequired = 0;
                    final CompressionAlgorithm compressAlgo = CompressionAlgorithm.MORPHO_NO_COMPRESS;//((EnrollInfo) morphoInfo).getCompressionAlgorithm();
                    if (processInfo.isFingerprintQualityThreshold()) {
                        acquisitionThreshold = processInfo.getFingerprintQualityThresholdvalue();
                    }
                    int compressRate = 0;
                    TemplateList templateList = new TemplateList();
                    if (!compressAlgo.equals(CompressionAlgorithm.NO_IMAGE)) {
                        templateList.setActivateFullImageRetrieving(true);
                        if (compressAlgo.equals(CompressionAlgorithm.MORPHO_COMPRESS_WSQ)) {
                            compressRate = 15;
                        }
                    }
                    int exportMinutiae = 1;
                    final TemplateType templateType = TemplateType.MORPHO_PK_COMP;//((EnrollInfo) morphoInfo).getTemplateType();
                    TemplateFVPType templateFVPType = TemplateFVPType.MORPHO_NO_PK_FVP;//((EnrollInfo) morphoInfo).getFVPTemplateType();
                    final int fingerNumber = 1;//((EnrollInfo) morphoInfo).getFingerNumber();

                    boolean saveRecord = true;//((EnrollInfo) morphoInfo).isSavePKinDatabase();
                    Coder coder = processInfo.getCoder();
                    int detectModeChoice;
                    detectModeChoice = DetectionMode.MORPHO_ENROLL_DETECT_MODE.getValue();
                    if (processInfo.isForceFingerPlacementOnTop()) {
                        detectModeChoice |= DetectionMode.MORPHO_FORCE_FINGER_ON_TOP_DETECT_MODE.getValue();
                    }
                    if (processInfo.isWakeUpWithLedOff()) {
                        detectModeChoice |= MorphoWakeUpMode.MORPHO_WAKEUP_LED_OFF.getCode();
                    }
                    int callbackCmd = ProcessInfo.getInstance().getCallbackCmd();

                    if (ErrorCodes.MORPHO_OK == ret) {
                        ret = morphoDevice.setStrategyAcquisitionMode(ProcessInfo.getInstance().getStrategyAcquisitionMode());
                    }
                    if (ErrorCodes.MORPHO_OK == ret) {
                        ret = morphoUser.enroll(
                                timeout,
                                acquisitionThreshold,
                                advancedSecurityLevelsRequired,
                                compressAlgo,
                                compressRate,
                                exportMinutiae,
                                fingerNumber,
                                templateType,
                                templateFVPType,
                                saveRecord,
                                coder,
                                detectModeChoice,
                                templateList,
                                callbackCmd,
                                morphoPlugin
                        );

                    } else {
                        jsObject.put("success", false);
                        jsObject.put("message", "Enroll: "+ ErrorCodes.getError(ret, morphoDevice.getInternalError()));
                        jsObject.put("rawBitmap", null);
                    }
                    ProcessInfo.getInstance().setCommandBioStart(false);

                    if (ErrorCodes.MORPHO_OK == ret && saveRecord) {
                        DatabaseItem databaseItemsItem = new DatabaseItem(userId, firstName, lastName);
                        List<DatabaseItem> databaseItems = processInfo.getDatabaseItems();
                        databaseItems.add(databaseItemsItem);
                        processInfo.setDatabaseItems(databaseItems);
                    }

                    jsObject.put("success", ret == ErrorCodes.MORPHO_OK);
                    if (ret == ErrorCodes.MORPHO_OK) {
                        jsObject.put("message", "Compare success");
                    } else {
                        jsObject.put("message", "Save Record: "+ ErrorCodes.getError(ret, morphoDevice.getInternalError()));
                    }
                    jsObject.put("rawBitmap", null);
                } else {
                    jsObject.put("success", false);
                    jsObject.put("message", "Check Field: "+ ErrorCodes.getError(ret, morphoDevice.getInternalError()));
                    jsObject.put("rawBitmap", null);
                }
                notifyEndProcess();
                call.resolve(jsObject);
                Looper.loop();

//                getAndWriteFFDLogs();
            }
        }));
        commandThread.start();
    }

    public void compareFingerprint(Activity activity, PluginCall call) {
        String userId = call.getString("userId", "");

        if ((userId == null || userId.isEmpty())) {
            JSObject object = new JSObject();
            object.put("success", false);
            object.put("message", "Invalid userId");
            object.put("rawBitmap", null);
            call.resolve(object);
            return;
        }
        Thread commandThread = (new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                try {
                    JSObject jsObject = new JSObject();
                    if (ProcessInfo.getInstance().getDatabaseSelectedIndex() != -1) {
                        int i = ProcessInfo.getInstance().getDatabaseSelectedIndex();

                        List<DatabaseItem> databaseItems = ProcessInfo.getInstance().getDatabaseItems();

                        String userID = databaseItems.get(i).getId();

                        final MorphoUser morphoUser = new MorphoUser();

                        int ret = 0;
                        final byte[] privacyKey;


                        ret = morphoDatabase.getUser(userID, morphoUser);

                        if (ret == 0) {
                            ProcessInfo processInfo = ProcessInfo.getInstance();
                            int timeout = processInfo.getTimeout();
                            int far = processInfo.getMatchingThreshold();
                            Coder coder = processInfo.getCoder();
                            int detectModeChoice;
                            MatchingStrategy matchingStrategy = processInfo.getMatchingStrategy();

                            int callbackCmd = ProcessInfo.getInstance().getCallbackCmd();

                            callbackCmd &= ~CallbackMask.MORPHO_CALLBACK_ENROLLMENT_CMD.getValue();

                            ResultMatching resultMatching = null;
                            // Check if running under security mode
                            boolean isTunnelingMode = false;
                            boolean isOfferedSecurityMode = false;
                            ArrayList<SecurityOption> securityOptions = ProcessInfo.getInstance().getSecurityOptions();
                            for (SecurityOption so : securityOptions) {
                                if (so.title.equals("Mode Tunneling")) {
                                    isTunnelingMode = so.activated;
                                } else if (so.title.equals("Mode Offered Security")) {
                                    isOfferedSecurityMode = so.activated;
                                }
                            }
                            if (!isTunnelingMode && !isOfferedSecurityMode) {
                                resultMatching = new ResultMatching();
                            }

                            detectModeChoice = DetectionMode.MORPHO_ENROLL_DETECT_MODE.getValue();

                            if (processInfo.isForceFingerPlacementOnTop())	{
                                detectModeChoice |= DetectionMode.MORPHO_FORCE_FINGER_ON_TOP_DETECT_MODE.getValue();
                            }

                            if (processInfo.isWakeUpWithLedOff()) {
                                detectModeChoice |= MorphoWakeUpMode.MORPHO_WAKEUP_LED_OFF.getCode();
                            }

                            ret = morphoDevice.setStrategyAcquisitionMode(ProcessInfo.getInstance().getStrategyAcquisitionMode());

                            if(ret == 0) {
                                ret = morphoUser.verify(timeout, far, coder, detectModeChoice, matchingStrategy, callbackCmd, morphoPlugin, resultMatching);
                            }

                            final String [] message = {""};
                            if (ret == ErrorCodes.MORPHO_OK) {
                                String user_authenticated = "";
                                for (int j = 0; j <= 2; j++) {
                                    String mem = morphoUser.getField(j);
                                    user_authenticated = user_authenticated + " " + mem;
                                }

                                message[0] = "User authenticated :\n";
                                message[0] += "\t" + "User ID : " + morphoUser.getField(0) + "\n";
                                message[0] += "\t" + "First Name : " + morphoUser.getField(1) + "\n";
                                message[0] += "\t" + "Last Name : " + morphoUser.getField(2) + "\n";

                                if (resultMatching != null) {
                                    message[0] += "\tMatching Score = " + resultMatching.getMatchingScore() + "\n";
                                    message[0] += "\tPK Number = " + resultMatching.getMatchingPKNumber();
                                }
                            }
                            final String msg = message[0];
//                            final int l_ret = ret;
                            final int internalError = morphoDevice.getInternalError();

                            jsObject.put("success", ret == ErrorCodes.MORPHO_OK);
                            if (ret == ErrorCodes.MORPHO_OK) {
                                jsObject.put("message", msg);
                            } else {
                                jsObject.put("message", ErrorCodes.getError(ret, internalError));
                            }
                            jsObject.put("rawBitmap", null);

                            /*mHandler.post(new Runnable()
                            {
                                @Override
                                public synchronized void run()
                                {
                                    alert(l_ret, internalError, "Verify", msg, true);
                                }
                            });*/
                        }

                    } else
                    {

                    }
                } catch (Exception e) {

                }
                ProcessInfo.getInstance().setCommandBioStart(false);
                Looper.loop();
            }
        }));
        commandThread.start();
    }

    private void notifyEndProcess()
    {
        mHandler.post(new Runnable()
        {
            @Override
            public synchronized void run()
            {
                try
                {
                    MorphoUsb.morphoDevice.cancelLiveAcquisition();// cancelLiveAcquisition();
                }
                catch (Exception e)
                {
                    Log.d("notifyEndProcess", e.getMessage());
                }
            }
        });

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

            USBManager.getInstance().initialize(MorphoUsb.this.activity, "io.ionic.starter.USB_ACTION", true);

            if (USBManager.getInstance().isDevicesHasPermission() == true) {
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (!result) {
                USBManager.getInstance().initialize(MorphoUsb.this.activity, "io.ionic.starter.USB_ACTION", true);
                if (USBManager.getInstance().isDevicesHasPermission() == true) {
                    MorphoUsb.this.hasUsbPermission = true;
                }
            }

        }

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();
            MorphoUsb.this.hasUsbPermission = false;
        }

    }
    static List<DatabaseItem>	databaseItems	= null;
    public int loadDatabaseItem()
    {
        int ret = 0;
        databaseItems = new ArrayList<DatabaseItem>();
        int[] indexDescriptor = new int[3];
        indexDescriptor[0] = 0;
        indexDescriptor[1] = 1;
        indexDescriptor[2] = 2;

        MorphoUserList morphoUserList = new MorphoUserList();
        ret = morphoDatabase.readPublicFields(indexDescriptor, morphoUserList);

        int l_nb_user = morphoUserList.getNbUser();
        for (int i = 0; i < l_nb_user; i++)
        {
            MorphoUser morphoUser = morphoUserList.getUser(i);

            String userID = morphoUser.getField(0);
            String firstName = morphoUser.getField(1);
            String lastName = morphoUser.getField(2);
            databaseItems.add(new DatabaseItem(userID, firstName, lastName));
        }

        ProcessInfo.getInstance().setDatabaseItems(databaseItems);
        ProcessInfo.getInstance().setCurrentNumberOfRecordValue(databaseItems.size());

        return ret;
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

    protected byte[] encryptPrivacyData(byte[] clear_data, byte[] key, byte[] random) {
        // Privacy formatted and ciphered data = AES-128.CBC.ENC( Kprivacy, IV, CRC32( RND32 || Plain Data ) || RND32 || Plain Data || Padding )
        // CRC32 computation parameters : Polynomial = 0x04C11DB7, input and output reflected, initial value = 0xFFFFFFFF and final XOR = 0xFFFFFFFF
        byte[] crypted_bio_data = null;
        try {
            // Compute CRC32
            byte[] iv = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
            ArrayList<Byte> aesEncryptedData = new ArrayList<Byte>();
            byte[] datainput = new byte[clear_data.length + 4]; //+ size of random

            System.arraycopy(random, 0, datainput, 0, 4);
            System.arraycopy(clear_data, 0, datainput, 4, clear_data.length);

            long[] computedCRC = {0};
            int ret = msoSecu.computeCRC32(datainput, 0x04C11DB7, 0xFFFFFFFF, true, true, 0xFFFFFFFF, computedCRC);
            if (0 != ret) {
//                    alert("An error occured while computing CRC32");
                return null;
            }

            // Build data to crypt
            byte[] CRC32Buffer = MorphoTools.longToFourByteBuffer(computedCRC[0], false);
            byte[] datainputF = new byte[clear_data.length + 4 + 4]; //+ size of crc & random

            System.arraycopy(CRC32Buffer, 0, datainputF, 0, 4);
            System.arraycopy(random, 0, datainputF, 4, 4);
            System.arraycopy(clear_data, 0, datainputF, 8, clear_data.length);

            // Crypt data
            ret = msoSecu.encryptAes128Cbc(key, datainputF, iv, true, aesEncryptedData);
            if (ret != 0) {
//                alert("An error occured while encrypting data");
                return null;
            } else {
                crypted_bio_data = MorphoTools.toByteArray(aesEncryptedData);
            }
        } catch (Exception e) {
//            alert(e.getMessage());
        }

        return crypted_bio_data;
    }

}
