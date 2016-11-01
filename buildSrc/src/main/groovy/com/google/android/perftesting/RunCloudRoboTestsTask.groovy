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
 * Defined Gradle task to execute cloud "robo" Android device tests using Google Cloud Test Lab via
 * the Google Cloud SDK (gcloud). This task DOES NOT run the performance tests.
 */
public class RunCloudRoboTestsTask extends DefaultTask {
    Logger mLogger = getLogger()

    private static final String CLOUD_SDK_DIR_PROP_NAME =
            "cloud.sdk.dir"
    private static final String CLOUD_TEST_LAB_GOOGLE_ACCOUNT_PROP_NAME =
            "cloud.test_lab.google_account"
    private static final String CLOUD_TEST_LAB_PROJECT_ID_PROP_NAME =
            "cloud.test_lab.project_id"
    private static final String CLOUD_TEST_LAB_DEVICE_LIST_PROP_NAME =
            "cloud.test_lab.device_list"
    private static final String CLOUD_TEST_LAB_API_LEVEL_LIST_PROP_NAME =
            "cloud.test_lab.api_level_list"
    private static final String CLOUD_TEST_LAB_LANGUAGE_LIST_PROP_NAME =
            "cloud.test_lab.language_list"
    private static final String CLOUD_TEST_LAB_ORIENTATION_LIST_PROP_NAME =
            "cloud.test_lab.orientation_list"

    private static final String[] REQUIRED_PARAMETERS = [CLOUD_SDK_DIR_PROP_NAME,
                                                         CLOUD_TEST_LAB_GOOGLE_ACCOUNT_PROP_NAME,
                                                         CLOUD_TEST_LAB_PROJECT_ID_PROP_NAME,
                                                         CLOUD_TEST_LAB_DEVICE_LIST_PROP_NAME,
                                                         CLOUD_TEST_LAB_API_LEVEL_LIST_PROP_NAME,
                                                         CLOUD_TEST_LAB_LANGUAGE_LIST_PROP_NAME,
                                                         CLOUD_TEST_LAB_ORIENTATION_LIST_PROP_NAME]

    public RunCloudRoboTestsTask() {
        super()
        setGroup('verification')
        setDescription("Run \"robo\" tests on devices in the cloud.")
        getOutputs().upToDateWhen({ return false })
    }

    @TaskAction
    void execute(IncrementalTaskInputs inputs) {
         // TODO: Read these properties using a better method.
        Properties properties = new Properties()
        project.rootProject.file('local.properties').withDataInputStream { inputStream ->
            properties.load(inputStream)
        }

        // Checking properties here instead of during task creation since users may never run the
        // task and therefore shouldn't be required to configure the properties.
        validateProperties(properties)

        def cloudSdkDir = properties.getProperty('cloud.sdk.dir')

        def cmdExt = ''
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            cmdExt = '.bat'
        }

        String commandPath = Paths.get(cloudSdkDir, "bin", "gcloud" + cmdExt).toAbsolutePath()
        def apkFile = project.rootProject.file("app/build/outputs/apk/app-debug-unaligned.apk")

        if (!new File(commandPath).exists()) {
            throw new GradleException("Google Cloud SDK gcloud binary could not be found: " +
                    "${commandPath}")
        }
        // TODO: Validate gcloud is at least a certain version or on alpha.
        // TODO: Validate 'gcloud auth list' has the account specified in local.properties

        String[] cmd = [commandPath,
                        "-q", // Ensure user input is never requested by gcloud.
                        "alpha", "test", "android", "run",
                        "--type", "robo",
                        "--app", apkFile.getAbsolutePath(),
                        "--device-ids", properties.get(CLOUD_TEST_LAB_DEVICE_LIST_PROP_NAME),
                        "--os-version-ids", properties.get(CLOUD_TEST_LAB_API_LEVEL_LIST_PROP_NAME),
                        "--locales", properties.get(CLOUD_TEST_LAB_LANGUAGE_LIST_PROP_NAME),
                        "--orientations", properties.get(CLOUD_TEST_LAB_ORIENTATION_LIST_PROP_NAME)]
        mLogger.warn("Running command: " + cmd.toString().replace(", ", " "))

        ProcessBuilder processBuilder = new ProcessBuilder()
        processBuilder.command(cmd)
        processBuilder.redirectErrorStream()
        Process process = processBuilder.start()
        process.waitFor()

        // Redirect output from the script to the console so it's not supressed.
        process.in.eachLine() { line ->
            mLogger.warn("Script: " + line)
        }
        if (process.exitValue() != 0) {
            throw new GradleException("Cloud test couldn't complete")
        }
        mLogger.warn("Cloud tests complete")
    }

    private static void validateProperties(Properties properties) throws GradleException {
        REQUIRED_PARAMETERS.each { parameterName ->
            String parameterValue = properties.get(parameterName)
            if (parameterValue == null || parameterValue.isAllWhitespace()) {
                throw new GradleException("${parameterName} should be defined in local " +
                        "properties for this task to work correctly. The value was: " +
                        "${parameterValue}")
            }
        }
    }
}
