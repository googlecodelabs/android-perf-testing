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

package com.google.android.perftesting.testrules;

import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * This rule enables pulling an atrace file that can be run through systrace.py
 *
 * This JUnit rule requires the test be running on a device that has been rooted. If you don't
 * have a device with root you should prefer the systrace be pulled at the system level.
 *
 * To enable, add this rule to your test class.
 *
 * <pre>
 * @Rule
 * public EnablePerTestTraceFile mEnablePerTestTraceFile = new EnablePerTestTraceFile();
 * </pre>
 */
public class EnablePerTestTraceFile extends ExternalResource {

    private String mTestName;
    private boolean aTraceInUse = false;

    @Override
    public Statement apply(Statement base, Description description) {
        mTestName = description.getMethodName();
        return super.apply(base, description);
    }

    @Override
    public void before() {
        try {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command("atrace", "--async_start", "-a",
                    // NOTE: Using the android app BuildConfig specifically.
                    com.google.android.perftesting.BuildConfig.APPLICATION_ID);
            Process process = builder.start();
            process.waitFor();
            if (process.exitValue() == 0) {
                aTraceInUse = true;
            }
        } catch (Exception ignored) {
            // Not much we can do if atrace isn't enabled on the device.
        }
    }

    @Override
    public void after() {
        if (aTraceInUse) {
            try {
                ProcessBuilder builder = new ProcessBuilder();
                builder.command("atrace", "--async_stop", "-a",
                        // NOTE: Using the android app BuildConfig specifically.
                        com.google.android.perftesting.BuildConfig.APPLICATION_ID);
                Process process = builder.start();
                process.waitFor();
            } catch (Exception ignored) {
                // Not much we can do if atrace isn't enabled on the device.
            }
        }
    }
}
