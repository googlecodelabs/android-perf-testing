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

package com.google.android.perftesting

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs

import java.nio.file.Paths


/**
 * Defined Gradle task to execute local android device tests using monkeyrunner. This task makes it
 * easier to run automated performance tests with out leaving AndroidStudio.
 */
public class RunLocalPerfTestsTask extends DefaultTask {
    Logger mLogger = getLogger()

    /**
     * Default to not supplying a value unless a deviceId was set.
     */
    String mDeviceId = ""

    public RunLocalPerfTestsTask() {
        super()
        setGroup('verification')
        setDescription("Run performance tests on a specific device.")
        // Forces this task to always run.
        if (deviceId != null) {
            mDeviceId = deviceId
        }
        getOutputs().upToDateWhen({ return false })
    }

    @TaskAction
    void execute(IncrementalTaskInputs inputs) {
        mLogger.warn("Starting monkeyrunner")
        ProcessBuilder processBuilder = new ProcessBuilder()

        // TODO: Read these properties using a better method.
        Properties properties = new Properties()
        project.rootProject.file('local.properties').withDataInputStream { inputStream ->
            properties.load(inputStream)
        }
        def sdkDir = properties.getProperty('sdk.dir')

        def monkeyExt = ''
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            monkeyExt = '.bat'
        }

        def monkeyPath = Paths.get(sdkDir, "tools", "monkeyrunner" + monkeyExt).toAbsolutePath().toString()
        def rootDir = getProject().getRootDir().getAbsolutePath()
        def monkeyScriptPath = Paths.get(rootDir, "run_perf_tests.py").toAbsolutePath().toString()
        processBuilder.command(monkeyPath, monkeyScriptPath, rootDir, mDeviceId)
        processBuilder.environment().put("ANDROID_HOME", sdkDir)
        processBuilder.redirectErrorStream()
        Process process = processBuilder.start()
        process.waitFor()

        // Redirect output from the script to the console so it's not supressed.
        process.in.eachLine() { line ->
            mLogger.warn("Script: " + line)
        }
        if (process.exitValue() != 0) {
            throw new GradleException("Monkeyrunner script didn't complete")
        }
        mLogger.warn("Monkeyrunner complete")
    }

    public void setDeviceId(String deviceId) {
        setDescription("Run performance tests on device with serial ${deviceId}")
        mDeviceId = deviceId
    }

    public String getDeviceId() {
        return mDeviceId
    }
}
