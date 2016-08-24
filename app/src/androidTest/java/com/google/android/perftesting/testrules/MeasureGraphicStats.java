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

import com.google.android.perftesting.Config;

import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.android.perftesting.common.PerfTestingUtils.getTestFile;

/**
 * This rule executes a dumpsys graphics data dump after performing the test. If the API level is
 * less than 23 then this rule will do nothing since this dumpsys command isn't supported.
 *
 * <pre>
 * @Rule
 * public EnablePostTestDumpSys mEnablePostTestDumpSys = new EnablePostTestDumpSys();
 * </pre>
 */
public class MeasureGraphicStats extends ExternalResource {

    private Logger logger = Logger.getLogger(MeasureGraphicStats.class.getName());
    private String mTestName;
    private String mTestClass;
    private double jankPercentageThreshold;
    private FileWriter fileWriter = null;

    @Override
    public Statement apply(Statement base, Description description) {
        mTestName = description.getMethodName();
        mTestClass = description.getClassName();
        return super.apply(base, description);
    }

    public MeasureGraphicStats(double jankPercentageThreshold) {
        this.jankPercentageThreshold = jankPercentageThreshold;
    }

    @Override
    public void before() {
        begin();
    }

    public void after() {
        if(fileWriter == null){
            end();
        }
    }

    public void setJankPercentageThreshold(double jankPercentageThreshold) {
        this.jankPercentageThreshold = jankPercentageThreshold;
    }

    public void begin() {
        try {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command("dumpsys", "gfxinfo", "--reset",
                    // NOTE: Using the android app BuildConfig specifically.
                    Config.TARGET_PACKAGE_NAME);
            Process process = builder.start();
            process.waitFor();
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unable to reset dumpsys", exception);
        }
    }

    public void end() {
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            BufferedReader bufferedReader = null;
            try {
                Trace.beginSection("Taking Dumpsys");
                ProcessBuilder processBuilder = new ProcessBuilder();

                // TODO: If less than API level 23 we should remove framestats.
                processBuilder.command("dumpsys", "gfxinfo",
                        // NOTE: Using the android app BuildConfig specifically.
                        Config.TARGET_PACKAGE_NAME,
                        "framestats");
                processBuilder.redirectErrorStream();
                Process process = processBuilder.start();
                fileWriter = new FileWriter(getTestFile(mTestClass, mTestName, "gfxinfo.dumpsys"
                        + ".log"));
                bufferedReader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));
                String line;
                String strJankPercentageThreshold = String.valueOf("JankPercentageThreshold : "+ jankPercentageThreshold + " %");
                fileWriter.append(strJankPercentageThreshold + "\n");
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
                Trace.endSection();
            }
        }
    }
}
