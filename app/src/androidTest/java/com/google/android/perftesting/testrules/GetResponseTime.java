/**
 * Created by kevinchang on 2016/8/3.
 */

package com.google.android.perftesting.testrules;


import android.os.Trace;
import android.util.Log;

import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.util.logging.Logger;

import static com.google.android.perftesting.common.PerfTestingUtils.getTestFile;


public class GetResponseTime extends ExternalResource {
    private static final String LOG_TAG="GetResponseTime";
    private Logger logger = Logger.getLogger(EnablePostTestDumpsys.class.getName());

    private String mTestName;
    private String mTestClass;
    private long StartTime;
    private long EndTime;

    @Override
    public Statement apply(Statement base, Description description) {
        mTestName = description.getMethodName();
        mTestClass = description.getClassName();
        return super.apply(base, description);
    }

    @Override
    public void before() {
        try {
            Log.w(LOG_TAG,"--------------start------------");
            StartTime = System.nanoTime();


        } catch (Exception exception) {


        }
    }

    public void after() {

        FileWriter fileWriter = null;
        BufferedReader bufferedReader = null;

        try {
            Log.w(LOG_TAG,"--------------end------------");
            EndTime = System.nanoTime();


            fileWriter = new FileWriter(getTestFile(mTestClass, mTestName, "responsetime" + ".log"));

            long output = EndTime - StartTime;

            String str = String.valueOf("Response Time : "+(output/1000000000f) + " sec");

            fileWriter.append(str);


        } catch (Exception exception) {


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
