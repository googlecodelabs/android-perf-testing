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

import com.google.android.perftesting.common.PerfTest;
import com.google.android.perftesting.testrules.EnableBatteryStatsDump;
import com.google.android.perftesting.testrules.EnableLogcatDump;
import com.google.android.perftesting.testrules.EnableNetStatsDump;
import com.google.android.perftesting.testrules.EnablePostTestDumpsys;
import com.google.android.perftesting.testrules.EnableTestTracing;
import com.google.android.perftesting.testrules.GetExecutionTime;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 18)
 @PerfTest
public class myPerfTest {
    private static final int LAUNCH_TIMEOUT = 5000;

    //--------------------rule chain: order testrules---------------------//

    public EnableTestTracing mEnableTestTracing = new EnableTestTracing();

    public EnablePostTestDumpsys mEnablePostTestDumpsys = new EnablePostTestDumpsys(15);

    public EnableLogcatDump mEnableLogcatDump = new EnableLogcatDump();

    public EnableNetStatsDump mEnableNetStatsDump = new EnableNetStatsDump();

    public GetExecutionTime mGetExecutionTime = new GetExecutionTime(7000);


    @Rule
    public TestRule chain = RuleChain
            .outerRule(mEnableLogcatDump)
            .around(mEnableNetStatsDump)
            .around(mEnableTestTracing)
            .around(mEnablePostTestDumpsys)
            .around(mGetExecutionTime);

    //----------Beforeclass: setup------------//
    @BeforeClass
    public static void setupClass() {
         // Open the app
        Context context = InstrumentationRegistry.getContext();
        final Intent intent = context.getPackageManager()
                .getLaunchIntentForPackage(Config.TARGET_PACKAGE_NAME);

         // Clear out any previous instances
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);

        // Wait for the view to appear
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        device.wait(Until.hasObject(By.pkg(Config.TARGET_PACKAGE_NAME).depth(0)), LAUNCH_TIMEOUT);

        // Complete your setup here.
    }


    @Before
    public void setUp() {
        // do something
    }

    //---------------------Testcase----------------------//
    @Test
    public void Swip1() throws InterruptedException, UiObjectNotFoundException {
        // Put operations you want to measure during the test execution here.

        UiDevice mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        int displayWidth = mDevice.getDisplayWidth();
        int displayHeight = mDevice.getDisplayHeight();

        // start
        mGetExecutionTime.begin();
        mEnablePostTestDumpsys.begin();

        for (int i = 0; i <= 2; i++) {
            mDevice.swipe(displayWidth / 2, (int) (displayHeight* .9),
                    displayWidth / 2, (int)(displayHeight* .25), 20);

            Thread.sleep(2000);
        }

        // end
        mEnablePostTestDumpsys.end();
        mGetExecutionTime.end();

        // do something else
    }

    @Test
    public void Swip2() throws InterruptedException, UiObjectNotFoundException {
        // Put operations you want to measure during the test execution here.
        mGetExecutionTime.setThresholdInMillis(8000);
        mEnablePostTestDumpsys.setThreshold(14);

        UiDevice mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        int displayWidth = mDevice.getDisplayWidth();
        int displayHeight = mDevice.getDisplayHeight();

        // start
        mEnablePostTestDumpsys.begin();
        mGetExecutionTime.begin();

        for (int i = 0; i <= 2; i++) {
            mDevice.swipe(displayWidth / 2, (int) (displayHeight* .9),
                    displayWidth / 2, (int)(displayHeight* .25), 20);

            Thread.sleep(2000);
        }

        // end
        mGetExecutionTime.end();
        mEnablePostTestDumpsys.end();
    }


    //----------AfterClass: teardown------------//

    @After
    public void tearDown() {

    }

    @AfterClass
    public static void teardownClass() {
    }

}


//    @Test
//    @PerfTest
//    public void buttonclick() throws InterruptedException, UiObjectNotFoundException {
//
//        UiDevice mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
//
//        //long startTime = System.nanoTime();
//
//        mDevice.findObject(By.text("類型")).click();
//
//        mDevice.wait(Until.hasObject(By.text("華語")), LAUNCH_TIMEOUT);
//
//        //long endTime = System.nanoTime();
//
//        //Log.w(LOG_TAG, "Time about turn to scroll bar:" + String.valueOf((endTime - startTime)/1000000000f) + "sec");
//
//    }