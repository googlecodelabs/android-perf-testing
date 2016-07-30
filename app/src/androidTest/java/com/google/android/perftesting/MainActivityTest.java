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
import android.test.ActivityInstrumentationTestCase2;

import org.junit.Before;
import org.junit.Rule;

/**
 * For a small sample on just the Espresso framework see https://goo.gl/GOUP47
 */
// @RunWith(AndroidJUnit4.class)
// @LargeTest
// TODO(developer): Uncomment the below annotation to have this test added to the set of perf tests.
// @PerfTest
public class MainActivityTest {

    public static final String STRING_TO_BE_TYPED = "Espresso";

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
//    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(
//            MainActivity.class);
//
//    @Rule
//    public Timeout globalTimeout= new Timeout(20, TimeUnit.SECONDS);
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
//
//
//      // TODO(developer): Uncomment below test method to add a simple test to the project.
//    @Test
//    @PerfTest
//    public void changeTextSameActivityTest() throws InterruptedException {
//        // Type text and then press the button.
//        onView(withId(R.id.edit_text_view))
//                .perform(clearText(), typeText(STRING_TO_BE_TYPED), closeSoftKeyboard());
//        onView(withId(R.id.edit_text_button)).perform(click());
//
//        // Check that the text was changed.
//        // Note that this test may fail if test device or emulator uses 3rd party keyboard app.
//        // If that happens, retry this test after switching keyboard setting to default keyboard.
//        onView(withId(R.id.display_text_view)).check(matches(withText(STRING_TO_BE_TYPED)));
//    }
}
