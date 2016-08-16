/**
 * Created by kevinchang on 2016/8/4.
 */

package com.google.android.perftesting.testrules;

import android.os.Trace;
import android.util.Log;

import com.google.android.perftesting.myPerfTest;

import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.BufferedReader;
import java.io.FileWriter;

import static com.google.android.perftesting.common.PerfTestingUtils.getTestFile;


public class GetExecutionTime extends ExternalResource {

    private String mTestName;
    private String mTestClass;
    private long startTime;
    private long endTime;
    private long thresholdInMillis;

    private static final String LOG_TAG="GetExecutionTime";

    @Override
    public Statement apply(Statement base, Description description) {
        mTestName = description.getMethodName();
        mTestClass = description.getClassName();
        return super.apply(base, description);
    }

    public GetExecutionTime(int thresholdInMillis) {
        this.thresholdInMillis = thresholdInMillis;
    }

    @Override
    public void before() {
        begin();
    }

    public void after() {

        FileWriter fileWriter = null;
        BufferedReader bufferedReader = null;

        try {
            fileWriter = new FileWriter(getTestFile(mTestClass, mTestName, "executiontime" + ".log"));

            long output = endTime - startTime;

            String strExecutionTime = String.valueOf("Execution Time : "+ (output/1000000f) + " ms\n");

            String strExpectedTime = String.valueOf("Expected Time : "+ thresholdInMillis + " ms");

            fileWriter.append(strExecutionTime);

            fileWriter.append(strExpectedTime);

        } catch (Exception exception) {
            Log.w(LOG_TAG, "------GetExecutionTime fail--------");

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

    public void setThresholdInMillis(long thresholdInMillis) {
        this.thresholdInMillis = thresholdInMillis;
    }

    public void begin() {
        startTime = System.nanoTime();
    }

    public void end() {
        endTime = System.nanoTime();
    }

}
