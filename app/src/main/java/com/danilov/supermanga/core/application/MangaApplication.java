package com.danilov.supermanga.core.application;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.android.httpimage.BitmapMemoryCache;
import com.android.httpimage.FileSystemPersistence;
import com.android.httpimage.HttpImageManager;
import com.danilov.supermanga.R;
import com.danilov.supermanga.core.cache.CacheDirectoryManagerImpl;
import com.danilov.supermanga.core.database.HistoryDAO;
import com.danilov.supermanga.core.database.MangaDAO;
import com.danilov.supermanga.core.database.UpdatesDAO;
import com.danilov.supermanga.core.http.ExtendedHttpClient;
import com.danilov.supermanga.core.http.HttpBitmapReader;
import com.danilov.supermanga.core.http.HttpBytesReader;
import com.danilov.supermanga.core.http.HttpStreamReader;
import com.danilov.supermanga.core.receiver.AlarmReceiver;
import com.danilov.supermanga.core.service.LocalImageManager;
import com.danilov.supermanga.core.util.Constants;
import com.danilov.supermanga.core.util.ServiceContainer;
import com.danilov.supermanga.test.DBTest;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import java.io.File;
import java.util.Calendar;

/**
 * Created by Semyon Danilov on 02.08.2014.
 */
@ReportsCrashes(formKey = "", // will not be used
        mailTo = "senya.danilov@gmail.com",
        mode = ReportingInteractionMode.TOAST,
        resToastText = R.string.crash_toast_text)
public class MangaApplication extends Application {

    public static Context context;

    @Override
    public void onCreate() {
        context = getApplicationContext();
        File mydir = getBaseContext().getDir("mydir", Context.MODE_PRIVATE);
        CacheDirectoryManagerImpl cacheDirectoryManager = new CacheDirectoryManagerImpl(mydir, ApplicationSettings.get(this), ApplicationSettings.PACKAGE_NAME);
        FileSystemPersistence fsp = new FileSystemPersistence(cacheDirectoryManager);
        HttpStreamReader httpStreamReader = new HttpStreamReader(new ExtendedHttpClient(), getResources());
        HttpBytesReader httpBytesReader = new HttpBytesReader(httpStreamReader, getResources());
        HttpBitmapReader httpBitmapReader = new HttpBitmapReader(httpBytesReader);
        BitmapMemoryCache bmc = new BitmapMemoryCache(0.4f);
        HttpImageManager httpImageManager = new HttpImageManager(bmc, fsp, getResources(), httpBitmapReader);
        LocalImageManager localImageManager = new LocalImageManager(bmc, getResources());
        HistoryDAO historyDAO = new HistoryDAO();
        UpdatesDAO updatesDAO = new UpdatesDAO();
        MangaDAO mangaDAO = new MangaDAO();
        ServiceContainer.addService(cacheDirectoryManager);
        ServiceContainer.addService(httpBytesReader);
        ServiceContainer.addService(httpStreamReader);
        ServiceContainer.addService(httpImageManager);
        ServiceContainer.addService(localImageManager);
        ServiceContainer.addService(historyDAO);
        ServiceContainer.addService(updatesDAO);
        ServiceContainer.addService(mangaDAO);
        cacheDirectoryManager.trimCacheIfNeeded();

        AlarmReceiver.setUpdateAlarm(this);

        //Google Play Service ты офигел!
        try {
            Class.forName("android.os.AsyncTask");
        } catch(Throwable ignore) {
        }

        // The following line triggers the initialization of ACRA
        ACRA.init(this);
    }



    public static Context getContext() {
        return context;
    }

}