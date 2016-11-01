/**
 * Created by kevinchang on 2016/8/4.
 */

package com.google.android.perftesting.testrules;

import android.os.Trace;

import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.android.perftesting.common.PerfTestingUtils.getTestFile;


public class MeasureExecutionTime extends ExternalResource {

    private Logger logger = Logger.getLogger(MeasureBatteryStats.class.getName());
    private String mTestName;
    private String mTestClass;
    private Long startTime;
    private Long endTime;
    private long thresholdMillis;
    private boolean flag = false;

    @Override
    public Statement apply(Statement base, Description description) {
        mTestName = description.getMethodName();
        mTestClass = description.getClassName();
        return super.apply(base, description);
    }

    public MeasureExecutionTime(long thresholdMillis) {
        this.thresholdMillis = thresholdMillis;
    }

    @Override
    public void before() { begin(); }

    public void after() {
        if (flag == false)
        {
            endTime = System.nanoTime();
        }

        FileWriter fileWriter = null;
        BufferedReader bufferedReader = null;

        try {
            fileWriter = new FileWriter(getTestFile(mTestClass, mTestName, "executiontime" + ".log"));
            long output = endTime - startTime;
            String strExecutionTime = "Execution Time : " + (output/1000000f) + " ms\n";
            String strExecutionThresholdMs = "ThresholdMillis : " + thresholdMillis + " ms";
            fileWriter.append(strExecutionTime);
            fileWriter.append(strExecutionThresholdMs);
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unable to get execution time", exception);
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

    public void setThresholdMillis(long thresholdMillis) {
        this.thresholdMillis = thresholdMillis;
    }

    public void begin() {
        if(startTime == null )
            startTime = System.nanoTime();
    }

    public void end() {
        flag = !flag;
        endTime = System.nanoTime();
    }

}
