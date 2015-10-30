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

import com.google.android.perftesting.common.PerfTestingUtils;
import com.google.android.perftesting.testrules.EnableBatteryStatsDump;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import android.content.Context;
import android.support.annotation.NonNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;

/**
 * Initiate post-test procedures to allow test-run data to be pulled from the device.
 */
public class TestListener extends RunListener {

    public static final String TEST_DATA_SUBDIR_NAME = "testdata";
    public EnableBatteryStatsDump mEnableBatteryStatsDump;

    // TODO(developer): Uncomment the following two methods to enable log files to be pulled as well as battery and location request information to be requested.
    @Override
    public void testRunStarted(Description description) throws Exception {
        mEnableBatteryStatsDump = new EnableBatteryStatsDump(
                PerfTestingUtils.getTestRunFile("batterstats.dumpsys.log"));
        mEnableBatteryStatsDump.before();
        deleteExistingTestFilesInAppData();
        // This isn't available until the next version of Google Play services.
        // resetLocationRequestTracking();
        super.testRunStarted(description);
    }

    @Override
    public void testRunFinished(Result result) throws Exception {
        super.testRunFinished(result);

        if (mEnableBatteryStatsDump != null) {
            mEnableBatteryStatsDump.after();
        }
        try {
            deleteExistingTestFilesInExternalData();
        } catch (Exception ignored) {
            // There may not be any data to delete.
        }
        dumpLocationRequestInformation();
        copyTestFilesToExternalData();
    }

    @Override
    public void testFailure(Failure failure) throws Exception {
        super.testFailure(failure);
        logTestFailure(failure);
    }

    /**
     * Move files from the app's internal file location to a location that can be read on retail
     * devices with simple ADB pull commands.
     */
    private void copyTestFilesToExternalData() throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder();

        Context appUnderTestContext = PerfTestingUtils.getAppContext();
        File externalAppStorageDir = appUnderTestContext.getExternalFilesDir(null);
        File externalTestFilesStorageDir = new File(externalAppStorageDir, TEST_DATA_SUBDIR_NAME);
        if (!externalTestFilesStorageDir.exists()) {
            if (!externalTestFilesStorageDir.mkdirs()) {
                throw new RuntimeException("Not able to create exportable directory for test data");
            }
        }

        String srcAbsolutePath = PerfTestingUtils.getTestRunDir().getAbsolutePath();
        String destAbsolutePath = externalTestFilesStorageDir.getAbsolutePath();

        processBuilder.command("cp", "-r", srcAbsolutePath, destAbsolutePath);
        processBuilder.redirectErrorStream();
        Process process = processBuilder.start();
        process.waitFor();
        if (process.exitValue() != 0) {
            StringBuilder errOutput = new StringBuilder();
            char[] charBuffer = new char[1024];
            int readSize;
            InputStream errorStream = null;
            Reader reader = null;
            try {
                errorStream = process.getInputStream();
                reader = new InputStreamReader(errorStream);
                while ((readSize = reader.read()) > 0) {
                    errOutput.append(charBuffer, 0, readSize);
                }
            } finally {
                if (errorStream != null) try { errorStream.close(); } catch (Exception ignored) {}
                if (reader != null) try { reader.close(); } catch (Exception ignored) {}
            }
            throw new IOException("Not able to move test data to external storage directory:"
                    + " src=" + srcAbsolutePath + ", dest=" + destAbsolutePath + ", out=" +
                    errOutput);
        }
    }

    private void deleteExistingTestFilesInExternalData() throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder();

        Context appUnderTestContext = PerfTestingUtils.getAppContext();
        File externalAppStorageDir = appUnderTestContext.getExternalFilesDir(null);
        File externalTestFilesStorageDir = new File(externalAppStorageDir, TEST_DATA_SUBDIR_NAME);
        String destAbsolutePath = externalTestFilesStorageDir.getAbsolutePath();

        processBuilder.command("rm", "-r", destAbsolutePath);
        processBuilder.redirectErrorStream();
        Process process = processBuilder.start();
        process.waitFor();
        if (process.exitValue()!= 0) {
            throw new IOException("Not able to delete external test data in " +
                    destAbsolutePath);
        }
    }

    private void deleteExistingTestFilesInAppData() throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder();

        File externalTestFilesStorageDir = getAppDataLogDir();
        String destAbsolutePath = externalTestFilesStorageDir.getAbsolutePath();

        processBuilder.command("rm", "-r", destAbsolutePath);
        processBuilder.redirectErrorStream();
        Process process = processBuilder.start();
        process.waitFor();
        if (process.exitValue()!= 0) {
            throw new IOException("Not able to delete in app test data in " +
                    destAbsolutePath);
        }
    }

    private void resetLocationRequestTracking() throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder();

        processBuilder.command("dumpsys", "activity", "service",
                "com.google.android.location.internal.GoogleLocationManagerService", "--reset");
        processBuilder.redirectErrorStream();
        Process process = processBuilder.start();
        process.waitFor();
        if (process.exitValue()!= 0) {
            throw new IOException("Not able to reset location provider logging info.");
        }
    }

    private void dumpLocationRequestInformation() throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder();

        File externalTestFilesStorageDir = getAppDataLogDir();
        File locationRequestLogFile = new File(externalTestFilesStorageDir, "locationRequests"
                + ".dumpsys.log");
        String destAbsolutePath = locationRequestLogFile.getAbsolutePath();

        processBuilder.command("dumpsys", "activity", "service",
                "com.google.android.location.internal.GoogleLocationManagerService");
        processBuilder.redirectErrorStream();
        Process process = processBuilder.start();
        process.waitFor();

        FileWriter fileWriter = null;
        InputStreamReader inputStreamReader = null;
        try {
            fileWriter = new FileWriter(destAbsolutePath);
            inputStreamReader = new InputStreamReader(process.getInputStream());
            char[] charBuffer = new char[1024];
            int readSize;
            while ((readSize = inputStreamReader.read(charBuffer, 0, charBuffer.length)) > -1) {
                fileWriter.write(charBuffer, 0, readSize);
            }

        } finally {
            try { inputStreamReader.close(); } catch (Exception ignored) {}
            try { fileWriter.close(); } catch (Exception ignored) {}
        }

        if (process.exitValue()!= 0) {
            throw new IOException("Not retrieve location provider logging info.");
        }
    }

    private void logTestFailure(Failure failure) throws IOException, InterruptedException {
        File failureLogFile = PerfTestingUtils.getTestFile(
                failure.getDescription().getClassName(),
                failure.getDescription().getMethodName(), "test.failure.log");

        FileWriter fileWriter = null;
        PrintWriter printWriter = null;
        String eol = System.getProperty("line.separator");
        try {
            fileWriter = new FileWriter(failureLogFile);
            printWriter = new PrintWriter(fileWriter);
            printWriter.append(failure.getTestHeader());
            printWriter.append(eol);
            failure.getException().printStackTrace(printWriter);
            printWriter.append(eol);
        } finally {
            if (printWriter != null) { try { printWriter.close(); } catch (Exception ignored) { } }
            if (fileWriter != null) { try { fileWriter.close(); } catch (Exception ignored) { } }
        }
    }

    @NonNull
    private File getAppDataLogDir() {
        Context appUnderTestContext = PerfTestingUtils.getAppContext();
        File externalAppStorageDir = appUnderTestContext.getFilesDir();
        File externalTestFilesStorageDir = new File(externalAppStorageDir, TEST_DATA_SUBDIR_NAME);
        if (!externalTestFilesStorageDir.exists()) {
            externalTestFilesStorageDir.mkdirs();
        }
        return externalTestFilesStorageDir;
    }
}
