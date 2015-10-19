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

package com.google.android.perftesting.common;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import java.io.File;

/**
 * Centralized methods to ensure tests perform certain actions in the same manner.
 */
public class PerfTestingUtils {
    /**
     * Retrieve the directory where test files should be written to.
     * This directory is local to the application and removed when the app is uninstalled.
     */
    public static File getTestDir(String className, String testName) {

        File rootDir = getAppContext().getFilesDir();

        // Create a test data subdirectory.
        File testFileDir = new File(rootDir, "testdata");
        if (getTranslatedTestName(className, testName) != null) {
            testFileDir = new File(testFileDir, getTranslatedTestName(className, testName));
        }

        if (!testFileDir.exists()) {
            if (!testFileDir.mkdirs()) {
                throw new RuntimeException("Unable to create test file directory.");
            }
        }
        return testFileDir;

    }

    /**
     * Retrieve the app-under-test's {@code Context}.
     */
    public static Context getAppContext() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    /**
     * Retrieve a file handle that is within the testing directory where tests should be written to.
     */
    public static File getTestRunFile(String filename) {
        return new File(getTestDir(null, null), filename);
    }

    /**
     * Retrieve the test run directory where tests should be written to.
     */
    public static File getTestRunDir() {
        return getTestDir(null, null);
    }

    private static String getTranslatedTestName(String className, String testName) {
        if (className == null || testName == null) {
            return null;
        }
        String base = className + "_" + testName;

        // Shorten the common strings so "com.google.android" comes out as "c.g.a" for brevity.
        base = base.replace("com", "c")
                   .replace("google", "g")
                   .replace("android", "a")
                   .replace("perfmatters", "pm")
                   .replace("automatingperformancetesting", "apt");
        return base;

    }

    /**
     * Retrieve a file handle that is within the testing directory where tests should be written to.
     */
    public static File getTestFile(String className, String testName, String filename) {
        return new File(getTestDir(className, testName), filename);
    }
}
