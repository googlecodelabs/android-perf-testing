package com.google.android.perftesting;

import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.support.test.InstrumentationRegistry;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.Until;

import org.junit.rules.ExternalResource;

/**
 * Created by kevinchang on 2016/8/11.
 */
public class Config  extends ExternalResource {

    public static String TARGET_PACKAGE_NAME;
    private static UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

    public Config() {
        //Fill up the application's packagename you want to test.
        this.TARGET_PACKAGE_NAME = "your-target-package-name";
    }

    public Config(String sPKGName) {
        this.TARGET_PACKAGE_NAME = sPKGName;
    }

    public static void setPKGName(String sPKGName) {
        TARGET_PACKAGE_NAME = sPKGName;
    }

    public static String getPKGName() {
        return TARGET_PACKAGE_NAME;
    }

    public static void launch(int launchTimeout) {
        // Open the app
        Context context = InstrumentationRegistry.getContext();
        final Intent intent = context.getPackageManager()
                .getLaunchIntentForPackage(Config.getPKGName());

        // Clear out any previous instances
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);

        // Wait for the view to appear
        device.wait(Until.hasObject(By.pkg(Config.getPKGName()).depth(0)), launchTimeout);
    }

    public static void launch(String sPKGName, int launchTimeout) {
        Config.setPKGName(sPKGName);
        Context context = InstrumentationRegistry.getContext();
        final Intent intent = context.getPackageManager()
                .getLaunchIntentForPackage(Config.getPKGName());

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);

        device.wait(Until.hasObject(By.pkg(Config.getPKGName()).depth(0)), launchTimeout);
    }

    public static void close(int launchTimeout) throws RemoteException {
        device.pressRecentApps();
        device.wait(Until.hasObject(By.res("com.android.systemui:id/dismiss_task")), launchTimeout);
        device.findObject(By.res("com.android.systemui:id/dismiss_task")).click();
    }
}
