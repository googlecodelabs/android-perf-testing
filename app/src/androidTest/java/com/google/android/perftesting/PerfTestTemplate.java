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
import android.support.test.uiautomator.Until;

import com.google.android.perftesting.common.PerfTest;
import com.google.android.perftesting.testrules.EnableLogcatDump;
import com.google.android.perftesting.testrules.EnableNetStatsDump;
import com.google.android.perftesting.testrules.EnablePostTestDumpsys;
import com.google.android.perftesting.testrules.EnableTestTracing;
import com.google.android.perftesting.testrules.GetExecutionTime;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 18)
// @PerfTest
public class PerfTestTemplate {
    private static final int LAUNCH_TIMEOUT = 5000;

    //--------------------rule chain: order testrules---------------------//

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

    //----------Beforeclass: Run Before Testcase------------//
    @BeforeClass
    public static void setup() {
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

    //---------------------Testcase----------------------//
    @Test
    public void performanceTest() {
        // Put operations you want to measure during the test execution here.
    }

    @AfterClass
    public static void teardown() {
    }

}
