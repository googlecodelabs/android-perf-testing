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

package com.google.android.perftesting.SampleCode;

import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.Until;

import com.google.android.perftesting.Config;
import com.google.android.perftesting.common.PerfTest;
import com.google.android.perftesting.testrules.MeasureBatteryStats;
import com.google.android.perftesting.testrules.MeasureExecutionTime;
import com.google.android.perftesting.testrules.MeasureGraphicStats;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 18)
//@PerfTest
public class BatterySample {
    private static final int LAUNCH_TIMEOUT = 2000;
    private UiDevice mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

    @ClassRule
    public static Config config = new Config("com.google.android.perftesting");

    @Rule
    public MeasureBatteryStats mMeasureBatteryStats = new MeasureBatteryStats(0.05);

    @BeforeClass
    public static void setupClass() {
        // Open the app
    }

    @Before
    public void setUp() {
        // Complete your setup here.
        config.launch(LAUNCH_TIMEOUT);
    }

    @Test
    public void actionRepeat() {
        // Put operations you want to measure during the test execution here.
        mMeasureBatteryStats.begin();

        // continuously repeat a series of action
        for (int i = 0; i < 5; i++) {

            mDevice.findObject(By.text("Open Recycler View")).click();
            mDevice.wait(Until.hasObject(By.res("com.google.android.perftesting:id/contactList")), LAUNCH_TIMEOUT);

            int displayWidth = mDevice.getDisplayWidth();
            int displayHeight = mDevice.getDisplayHeight();

            //scroll view
            for (int j = 0; j <= 5; j++) {
                mDevice.swipe(displayWidth / 2, (int) (displayHeight * .9),
                        displayWidth / 2, (int) (displayHeight * .25), 20);
            }

            mDevice.pressBack();
        }

        mMeasureBatteryStats.end();
    }

    @After
    public void tearDown() throws RemoteException {
        // Complete your teardown here.
    }

    @AfterClass
    public static void teardownClass() throws RemoteException {
        //Close the app
        config.close(LAUNCH_TIMEOUT);
    }
}