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
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObjectNotFoundException;

import com.google.android.perftesting.common.PerfTest;
import com.google.android.perftesting.testrules.EnableLogcatDump;
import com.google.android.perftesting.testrules.EnableNetStatsDump;
import com.google.android.perftesting.testrules.EnablePostTestDumpsys;
import com.google.android.perftesting.testrules.EnableTestTracing;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * For a small sample on just the Espresso framework see https://goo.gl/GOUP47
 */
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 18)
// TODO(developer): Uncomment the below annotation to have this test added to the set of perf tests.
 @PerfTest
public class RunAppUIautomator {
    private static final String BASIC_SAMPLE_PACKAGE
            = "com.example.android.testing.uiautomator.BasicSample";
    private static final int LAUNCH_TIMEOUT = 5000;
    private static final String STRING_TO_BE_TYPED = "UiAutomator";
    private UiDevice mDevice;

//    @Rule
//    public Timeout globalTimeout= new Timeout(
//            SCROLL_TIME_IN_MILLIS + MAX_ADAPTER_VIEW_PROCESSING_TIME_IN_MILLIS, TimeUnit.MILLISECONDS);

    @Rule
    public EnableTestTracing mEnableTestTracing = new EnableTestTracing();

    @Rule
    public EnablePostTestDumpsys mEnablePostTestDumpsys = new EnablePostTestDumpsys();

    @Rule
    public EnableLogcatDump mEnableLogcatDump = new EnableLogcatDump();

    @Rule
    public EnableNetStatsDump mEnableNetStatsDump = new EnableNetStatsDump();

    @Test
    @PerfTest
    public void startMainActivityAndSwipe() throws InterruptedException, UiObjectNotFoundException{
        // Initialize UiDevice instance
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());


        // open the app
        Context context = InstrumentationRegistry.getContext();
        final Intent intent = context.getPackageManager()
                .getLaunchIntentForPackage("com.skysoft.kkbox.android");
        // Clear out any previous instances
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);

        //scroll view
        int displayWidth = mDevice.getDisplayWidth();
        int displayHeight = mDevice.getDisplayHeight();

        for (int i = 0; i <= 5; i++) {
            mDevice.swipe(displayWidth / 2, (int) (displayHeight* .9),
                    displayWidth / 2, (int)(displayHeight* .25), 20);

            Thread.sleep(2000);
        }

    }

}
