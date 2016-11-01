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
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.perftesting.common.PerfTestingUtils;
import com.google.android.perftesting.testrules.EnableDeviceGetPropsInfo;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;

/**
 * Initiate pre-test and post-test procedures; including cleaning and moving files from the
 * internal app data directory to the external one.  Also perform writing to test-start and test-end
 * files which are used to indicate whether there was a fatal test exception
 * (e.g. OutOfMemoryException).
 */
public class TestListener extends RunListener {
    private static final String LOG_TAG = "TestListener";
    public static final String TEST_DATA_SUBDIR_NAME = "testdata";
    public EnableDeviceGetPropsInfo mEnableDeviceGetPropsInfo;

    // TODO(developer): Uncomment the following two methods to enable log files to be pulled as well as battery and location request information to be requested.
    @Override
    public void testRunStarted(Description description) throws Exception {
        Log.w(LOG_TAG, "Test run started.");
        // Cleanup data from past test runs.
        deleteExistingTestFilesInAppData();
        deleteExistingTestFilesInExternalData();

        mEnableDeviceGetPropsInfo = new EnableDeviceGetPropsInfo(
                PerfTestingUtils.getTestRunFile("getprops.log"));
        mEnableDeviceGetPropsInfo.before();

        // This isn't available until the next version of Google Play services.
        // resetLocationRequestTracking();
        super.testRunStarted(description);
    }

    @Override
    public void testRunFinished(Result result) throws Exception {
        Log.w(LOG_TAG, "Test run finished.");

        super.testRunFinished(result);

        try {
            Log.w(LOG_TAG, "Test run finished");

            if (mEnableDeviceGetPropsInfo != null) {
                mEnableDeviceGetPropsInfo.after();
            }
            Log.w(LOG_TAG, "getprops collected.");

            dumpLocationRequestInformation();
            Log.w(LOG_TAG, "Location request information collected.");

            // Create a file indicating the test run is complete. This can be checked to ensure
            // the extraction of files for the test run was successful.
            File testRunFinishedFile = PerfTestingUtils.getTestRunFile("testRunComplete.log");
            testRunFinishedFile.createNewFile();
            Log.w(LOG_TAG, "testRunComplete file written.");

            copyTestFilesToExternalData();

            Log.w(LOG_TAG, "Done copying files to external data directory");
        } catch (Exception e) {
            Log.e(LOG_TAG, "Issue taking all log files after test run", e);
        }
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

        File externalAppStorageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File externalTestFilesStorageDir = new File(externalAppStorageDir, TEST_DATA_SUBDIR_NAME);
        if (!externalTestFilesStorageDir.exists()) {
            if (!externalTestFilesStorageDir.mkdirs()) {
                throw new RuntimeException("Not able to create exportable directory for test data");
            }
        }

        String srcAbsolutePath = PerfTestingUtils.getTestRunDir().getAbsolutePath();
        String destAbsolutePath = externalTestFilesStorageDir.getAbsolutePath();

        Log.w(LOG_TAG, "Moving test data from " + srcAbsolutePath + " to " + destAbsolutePath);

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
            String errorString = errOutput.toString();
            Log.e(LOG_TAG, errorString);
            throw new IOException("Not able to move test data to external storage directory:"
                    + " src=" + srcAbsolutePath + ", dest=" + destAbsolutePath + ", out=" +
                    errorString);
        }

    }

    private void deleteExistingTestFilesInExternalData() throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder();

        File externalAppStorageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File externalTestFilesStorageDir = new File(externalAppStorageDir, TEST_DATA_SUBDIR_NAME);
        String destAbsolutePath = externalTestFilesStorageDir.getAbsolutePath();

        processBuilder.command("rm", "-r", destAbsolutePath);
        processBuilder.redirectErrorStream();
        Process process = processBuilder.start();
        process.waitFor();
    }

    private void deleteExistingTestFilesInAppData() throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder();

        File externalTestFilesStorageDir = getAppDataLogDir();
        String destAbsolutePath = externalTestFilesStorageDir.getAbsolutePath();

        processBuilder.command("rm", "-r", destAbsolutePath);
        processBuilder.redirectErrorStream();
        Process process = processBuilder.start();
        process.waitFor();
    }

    private void resetLocationRequestTracking() throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder();

        processBuilder.command("dumpsys", "activity", "service",
                "com.google.android.location.internal.GoogleLocationManagerService", "--reset");
        processBuilder.redirectErrorStream();
        Process process = processBuilder.start();
        process.waitFor();
    }

    private void dumpLocationRequestInformation() {
        try {
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
                throw new IOException("Exit value non-zero.");
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Unable to retrieve location provide logging information", e);
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
