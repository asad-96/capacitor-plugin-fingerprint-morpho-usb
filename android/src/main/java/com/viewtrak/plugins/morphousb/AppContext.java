package com.viewtrak.plugins.morphousb;

import android.app.Application;
import android.os.Environment;

import java.io.File;

/**
 * Created by Administrator on 2019/8/28.
 */

public class AppContext extends Application {
    public final static String RootPath = Environment.getExternalStorageDirectory() + File.separator;

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
