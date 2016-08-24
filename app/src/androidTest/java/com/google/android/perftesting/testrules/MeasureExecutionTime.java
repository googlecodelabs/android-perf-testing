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
    private long startTime;
    private Long endTime;
    private long executionThresholdMs;

    @Override
    public Statement apply(Statement base, Description description) {
        mTestName = description.getMethodName();
        mTestClass = description.getClassName();
        return super.apply(base, description);
    }

    public MeasureExecutionTime(long executionThresholdMs) {
        this.executionThresholdMs = executionThresholdMs;
    }

    @Override
    public void before() { begin(); }

    public void after() {
        if (endTime == null){
            endTime = System.nanoTime();
        }

        FileWriter fileWriter = null;
        BufferedReader bufferedReader = null;

        try {
            fileWriter = new FileWriter(getTestFile(mTestClass, mTestName, "executiontime" + ".log"));
            long output = endTime - startTime;
            String strExecutionTime = String.valueOf("Execution Time : "+ (output/1000000f) + " ms\n");
            String strExecutionThresholdMs = String.valueOf("ExecutionThresholdMs : "+ executionThresholdMs + " ms");
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

    public void setExecutionThresholdMs(long executionThresholdMs) {
        this.executionThresholdMs = executionThresholdMs;
    }

    public void begin() {
        startTime = System.nanoTime();
    }

    public void end() {
        endTime = System.nanoTime();
    }

}
