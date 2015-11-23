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

import android.os.Trace;

import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.android.perftesting.common.PerfTestingUtils.getTestFile;

/**
 * This rule resets battery stats before a test and executes a dumpsys for batterystats after
 * performing the test. If the API level is less than 21 then this rule will do nothing since
 * this dumpsys command isn't supported. It has limited use for short tests and is meant for tests
 * you would typically mark as Large. For short tests you can manually use this in a
 * {@code org.junit.runner.notification.RunListener}.
 *
 * <pre>
 * @Rule
 * public EnableBatteryStatsDump mEnableBatteryStatsDump = new EnableBatteryStatsDump();
 * </pre>
 */
public class EnableBatteryStatsDump extends ExternalResource {

    private Logger logger = Logger.getLogger(EnableBatteryStatsDump.class.getName());

    private String mTestName;

    private String mTestClass;

    private File mLogFileAbsoluteLocation = null;

    public EnableBatteryStatsDump() { }

    /**
     * Allow the the log to be written to a specific location.
     */
    public EnableBatteryStatsDump(File logFileAbsoluteLocation) {
        mLogFileAbsoluteLocation = logFileAbsoluteLocation;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        mTestName = description.getMethodName();
        mTestClass = description.getClassName();
        return super.apply(base, description);
    }

    @Override
    public void before() {
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            try {
                ProcessBuilder builder = new ProcessBuilder();
                builder.command("dumpsys", "batterystats", "--reset");
                Process process = builder.start();
                process.waitFor();
            } catch (Exception exception) {
                logger.log(Level.SEVERE, "Unable to reset dumpsys", exception);
            }
        }
    }

    public void after() {
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            FileWriter fileWriter = null;
            BufferedReader bufferedReader = null;
            try {
                Trace.beginSection("Taking battery dumpsys");
                ProcessBuilder processBuilder = new ProcessBuilder();

                processBuilder.command("dumpsys", "batterystats");
                processBuilder.redirectErrorStream();
                Process process = processBuilder.start();
                if (mLogFileAbsoluteLocation == null) {
                    mLogFileAbsoluteLocation = getTestFile(mTestClass, mTestName,
                            "battery.dumpsys.log");
                }
                fileWriter = new FileWriter(mLogFileAbsoluteLocation);
                bufferedReader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    fileWriter.append(line);
                    fileWriter.append(System.lineSeparator());
                }
                process.waitFor();
                if (process.exitValue() != 0) {
                    throw new Exception("Error while taking dumpsys, exitCode=" +
                            process.exitValue());
                }
            } catch (Exception exception) {
                logger.log(Level.SEVERE, "Unable to take a dumpsys", exception);
            } finally {
                if (fileWriter != null) {
                    try { fileWriter.close(); } catch (Exception e) { e.printStackTrace(); }
                }
                if (bufferedReader != null) {
                    try { bufferedReader.close(); } catch (Exception e) { e.printStackTrace(); }
                }
            }
        }
    }
}