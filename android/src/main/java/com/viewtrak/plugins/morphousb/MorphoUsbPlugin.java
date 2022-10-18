package com.viewtrak.plugins.morphousb;

import android.graphics.Bitmap;
import android.os.Handler;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.morpho.morphosmart.sdk.CallbackMessage;
import com.morpho.morphosmart.sdk.MorphoImage;

import java.nio.ByteBuffer;
import java.util.Observable;
import java.util.Observer;

@CapacitorPlugin(name = "MorphoUsb")
public class MorphoUsbPlugin extends Plugin implements Observer {

    private MorphoUsb implementation = new MorphoUsb();
    private Handler mHandler = new Handler();
    String strMessage = new String();

    @PluginMethod
    public void echo(PluginCall call) {
        String value = call.getString("value");

        JSObject ret = new JSObject();
        ret.put("value", implementation.echo(value));
        call.resolve(ret);
    }

    @Override
    public void load() {
        super.load();
        implementation.init(this.getActivity(), this);
    }

    @PluginMethod
    public void openDevice(PluginCall call) {
        implementation.openDevice(this.getActivity(), call);
    }

    @PluginMethod
    public void captureFingerprint(PluginCall call) {
        implementation.captureFingerprint(this.getActivity(), call);
    }

    @PluginMethod
    public void compareFingerprint(PluginCall call) {
        implementation.compareFingerprint(this.getActivity(), call);
    }

    @Override
    public void handleOnDestroy() {
        super.handleOnDestroy();
        implementation.uninit();
    }

    @Override
    public synchronized void update(Observable observable, Object arg) {

/*        Boolean isOpenOK = (Boolean) data;

//        MorphoSample.isRebootSoft = false;

        if (isOpenOK == true) {
            mHandler.post(new Runnable() {
                @Override
                public synchronized void run() {
//                    enableDisableBoutton(true);
                    // L.B the sample must reload the database after connection resumed
                    int ret = morphoDevice.getDatabase(0, morphoDatabase);
                    if (ErrorCodes.MORPHO_OK == ret) {
//                        loadDatabaseItem();
                    } else if (ErrorCodes.MORPHOERR_BASE_NOT_FOUND == ret) {
//                        activateButton(false, false);
                    } else {
//                        alert(ErrorCodes.MORPHOERR_RESUME_CONNEXION, 0, "Resume Connection", "Failed to reload database.");
                    }
                }
            });
        } else {
            mHandler.post(new Runnable() {
                @Override
                public synchronized void run() {
//                    Button btn = (Button) findViewById(R.id.btn_closeandquit);
//                    btn.setEnabled(true);
//                    alert(ErrorCodes.MORPHOERR_RESUME_CONNEXION, 0, "Resume Connection", "");
                }
            });
        }*/
        notifyListeners("MorphoEvent", new JSObject().put("info", "capture fingerprint"));
        try {
            // convert the object to a callback back message.
            CallbackMessage message = (CallbackMessage) arg;

            int type = message.getMessageType();
            notifyListeners("MorphoEvent", new JSObject().put("info", type));

            switch (type) {

                case 1:
                    // message is a command.
                    Integer command = (Integer) message.getMessage();

                    // Analyze the command.
                    switch (command) {
                        case 0:
                            strMessage = "move-no-finger";
                            break;
                        case 1:
                            strMessage = "move-finger-up";
                            break;
                        case 2:
                            strMessage = "move-finger-down";
                            break;
                        case 3:
                            strMessage = "move-finger-left";
                            break;
                        case 4:
                            strMessage = "move-finger-right";
                            break;
                        case 5:
                            strMessage = "press-harder";
                            break;
                        case 6:
                            strMessage = "move-latent";
                            break;
                        case 7:
                            strMessage = "remove-finger";
                            break;
                        case 8:
                            strMessage = "finger-ok";
                            // switch live acquisition ImageView
                            /*if (isCaptureVerif) {
                                isCaptureVerif = false;
                                index = 4; //R.id.imageView5;
                            } else {
                                index++;
                            }

                            switch (index) {
                                case 1:
                                    currentCaptureBitmapId = R.id.imageView2;
                                    break;
                                case 2:
                                    currentCaptureBitmapId = R.id.imageView3;
                                    break;
                                case 3:
                                    currentCaptureBitmapId = R.id.imageView4;
                                    break;
                                case 4:
                                    currentCaptureBitmapId = R.id.imageView5;
                                    break;
                                case 5:
                                    currentCaptureBitmapId = R.id.imageView6;
                                    break;
                                default:
                                case 0:
                                    currentCaptureBitmapId = R.id.imageView1;
                                    break;
                            }*/
                            break;
                    }

                    mHandler.post(new Runnable() {
                        @Override
                        public synchronized void run() {
//                            updateSensorMessage(strMessage);
                            notifyListeners("MorphoEvent", new JSObject().put("info", strMessage));
                        }
                    });

                    break;
                case 2:
                    // message is a low resolution image, display it.
                    byte[] image = (byte[]) message.getMessage();

                    MorphoImage morphoImage = MorphoImage.getMorphoImageFromLive(image);
                    int imageRowNumber = morphoImage.getMorphoImageHeader().getNbRow();
                    int imageColumnNumber = morphoImage.getMorphoImageHeader().getNbColumn();
                    final Bitmap imageBmp = Bitmap.createBitmap(imageColumnNumber, imageRowNumber, Bitmap.Config.ALPHA_8);

                    imageBmp.copyPixelsFromBuffer(ByteBuffer.wrap(morphoImage.getImage(), 0, morphoImage.getImage().length));
                    mHandler.post(new Runnable() {
                        @Override
                        public synchronized void run() {
                            notifyListeners("MorphoEvent", new JSObject().put("info", imageBmp.toString()));
                        }
                    });
                    break;
                case 3:
                    // message is the coded image quality.
                    final Integer quality = (Integer) message.getMessage();
                    mHandler.post(new Runnable() {
                        @Override
                        public synchronized void run() {
//                            updateSensorProgressBar(quality);
//                            updateImageBackground(currentCaptureBitmapId, quality);
                            notifyListeners("MorphoEvent", new JSObject().put("info", quality));
                        }
                    });
                    break;
                //case 4:
                //byte[] enrollcmd = (byte[]) message.getMessage();
            }
        } catch (Exception e) {
//            alert(e.getMessage());
            notifyListeners("MorphoEvent", new JSObject().put("info", e.getMessage()));
        }
    }
}
