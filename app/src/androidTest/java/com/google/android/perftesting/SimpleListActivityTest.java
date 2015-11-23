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

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.LargeTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

/**
 * For a small sample on just the Espresso framework see https://goo.gl/GOUP47
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
// TODO(developer): Uncomment the below annotation to have this test added to the set of perf tests.
// @PerfTest
public class SimpleListActivityTest {
    public static final int SCROLL_TIME_IN_MILLIS = 4000;
    public static final long MAX_ADAPTER_VIEW_PROCESSING_TIME_IN_MILLIS = 500;

    /**
     * A JUnit {@link Rule @Rule} to launch your activity under test. This is a replacement
     * for {@link ActivityInstrumentationTestCase2}.
     * <p>
     * Rules are interceptors which are executed for each test method and will run before
     * any of your setup code in the {@link Before @Before} method.
     * <p>
     * {@link ActivityTestRule} will create and launch of the activity for you and also expose
     * the activity under test. To get a reference to the activity you can use
     * the {@link ActivityTestRule#getActivity()} method.
     */
      // TODO(developer): Uncomment below member variable to add a test activity to this test class.
//    @Rule
//    public ActivityTestRule<SimpleListActivity> mActivityRule = new ActivityTestRule<>(
//            SimpleListActivity.class);
//
//    @Rule
//    public Timeout globalTimeout= new Timeout(
//        SCROLL_TIME_IN_MILLIS + MAX_ADAPTER_VIEW_PROCESSING_TIME_IN_MILLIS, TimeUnit.MILLISECONDS);
//
//    @Rule
//    public EnableTestTracing mEnableTestTracing = new EnableTestTracing();
//
//    @Rule
//    public EnablePostTestDumpsys mEnablePostTestDumpsys = new EnablePostTestDumpsys();
//
//    @Rule
//    public EnableLogcatDump mEnableLogcatDump = new EnableLogcatDump();
//
//    @Rule
//    public EnableNetStatsDump mEnableNetStatsDump = new EnableNetStatsDump();


      // TODO(developer): Uncomment below test method to add a list scrolling test to the project.
//    @Test
//    @PerfTest
//    public void scrollFullList() throws InterruptedException {
//        ListView listView = (ListView) mActivityRule.getActivity().findViewById(android.R.id.list);
//
//        // Get last position and offset for zero-indexed position tracking.
//        int lastPosition = listView.getAdapter().getCount() - 1;
//
//        // Espresso method of scrolling to the last item.
//        // onData(anything()).atPosition(lastPosition);
//
//        // Standard Android method of scrolling to the last position.
//        listView.smoothScrollToPositionFromTop(lastPosition, 0, SCROLL_TIME_IN_MILLIS);
//
//        // Scrolling is performed asynchronously so we need to periodically loop and detect if
//        // we're finished scrolling yet. This can be delayed by any work being done to display
//        // data items in the ListView.
//        while (listView.getLastVisiblePosition() != lastPosition) {
//            Thread.sleep(300);
//        }
//    }
}
