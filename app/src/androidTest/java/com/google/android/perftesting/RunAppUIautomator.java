/*
 * Copyright 2015, Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.perftesting;

import android.content.Context;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.Until;
import android.util.Log;

import com.google.android.perftesting.common.PerfTest;
import com.google.android.perftesting.testrules.EnableLogcatDump;
import com.google.android.perftesting.testrules.EnableNetStatsDump;
import com.google.android.perftesting.testrules.EnablePostTestDumpsys;
import com.google.android.perftesting.testrules.EnableTestTracing;
import com.google.android.perftesting.testrules.GetExecutionTime;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runner.notification.RunListener;

//-----------------for rule chain--------------------//

import org.junit.rules.TestRule;
import org.junit.rules.RuleChain;

/**
 * For a small sample on just the Espresso framework see https://goo.gl/GOUP47
 */
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 18)
// TODO(developer): Uncomment the below annotation to have this test added to the set of perf tests.
 @PerfTest
public class RunAppUIautomator extends RunListener {
    private static final String BASIC_SAMPLE_PACKAGE
            = "com.skysoft.kkbox.android";
    private static final String LOG_TAG = "RunAppUIautomator";
    private static final int LAUNCH_TIMEOUT = 5000;
    private static final String STRING_TO_BE_TYPED = "UiAutomator";
    private static UiDevice Device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    private UiDevice mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

//    @Rule
//    public Timeout globalTimeout= new Timeout(
//            SCROLL_TIME_IN_MILLIS + MAX_ADAPTER_VIEW_PROCESSING_TIME_IN_MILLIS, TimeUnit.MILLISECONDS);

    //--------------------rule chain--------------------//

    public EnableTestTracing mEnableTestTracing = new EnableTestTracing();

    public EnablePostTestDumpsys mEnablePostTestDumpsys = new EnablePostTestDumpsys();

    public EnableLogcatDump mEnableLogcatDump = new EnableLogcatDump();

    public EnableNetStatsDump mEnableNetStatsDump = new EnableNetStatsDump();

    public GetExecutionTime mGetExecutionTime = new GetExecutionTime();

    @Rule
    public TestRule chain = RuleChain
            .outerRule(mEnableLogcatDump)
            .around(mEnableTestTracing)
            .around(mEnablePostTestDumpsys)
            .around(mEnableNetStatsDump)
            .around(mGetExecutionTime);



    @BeforeClass
    public static void openApp(){
         //open the app
        Context context = InstrumentationRegistry.getContext();
        final Intent intent = context.getPackageManager()
                .getLaunchIntentForPackage(BASIC_SAMPLE_PACKAGE);
         //Clear out any previous instances
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);

        //Wait for the view to appear
        Device.wait(Until.hasObject(By.res("com.skysoft.kkbox.android:id/view_runway")), LAUNCH_TIMEOUT);
    }


    @Test
    @PerfTest
    public void buttonclick() throws InterruptedException, UiObjectNotFoundException {

        long startTime = System.nanoTime();

        mDevice.findObject(By.text("類型")).click();

        mDevice.wait(Until.hasObject(By.text("華語")), LAUNCH_TIMEOUT);

        long endTime = System.nanoTime();

        Log.w(LOG_TAG, "Time about turn to scroll bar:" + String.valueOf((endTime - startTime)/1000000000f) + "sec");

    }

//    @Test
//    @PerfTest
//    public void startSwip2() throws InterruptedException, UiObjectNotFoundException {
//        Log.w(LOG_TAG, "swipe2 start~~~~~");
//        // Initialize UiDevice instance
//        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
//
//        //scroll view
//        int displayWidth = mDevice.getDisplayWidth();
//        int displayHeight = mDevice.getDisplayHeight();
//
//        for (int i = 0; i <= 5; i++) {
//            mDevice.swipe(displayWidth / 2, (int) (displayHeight* .9),
//                    displayWidth / 2, (int)(displayHeight* .25), 20);
//
//            Thread.sleep(2000);
//        }
//
//    }

}
