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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.android.perftesting.common.PerfTestingUtils.getTestFile;

/**
 * This rule records the result of calling the getprops command on a device. It has limited use for
 * short tests except when these properties are changing between tests. Alternatively, it can be
 * used manually in a {@code org.junit.runner.notification.RunListener}.
 *
 * <pre>
 * @Rule
 * public EnableDeviceGetPropsInfo mEnableDeviceGetPropsInfo = new EnableDeviceGetPropsInfo();
 * </pre>
 */
public class EnableDeviceGetPropsInfo extends ExternalResource {

    private Logger logger = Logger.getLogger(EnableDeviceGetPropsInfo.class.getName());

    private String mTestName;

    private String mTestClass;

    private File mLogFileAbsoluteLocation = null;

    public EnableDeviceGetPropsInfo() { }

    /**
     * Allow the the log to be written to a specific location.
     */
    public EnableDeviceGetPropsInfo(File logFileAbsoluteLocation) {
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
        // Do nothing.
    }

    public void after() {
        FileWriter fileWriter = null;
        BufferedReader bufferedReader = null;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();

            processBuilder.command("getprop");
            processBuilder.redirectErrorStream();
            Process process = processBuilder.start();
            if (mLogFileAbsoluteLocation == null) {
                mLogFileAbsoluteLocation = getTestFile(mTestClass, mTestName,
                        "getprops.log");
            }
            fileWriter = new FileWriter(mLogFileAbsoluteLocation);
            bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                fileWriter.append(line);
                fileWriter.append("\n");
            }
            process.waitFor();
            if (process.exitValue() != 0) {
                throw new Exception("Error while taking getprops, exitCode=" +
                        process.exitValue());
            }
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unable to take a getprops", exception);
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