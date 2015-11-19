package com.google.android.perftesting

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logger

/**
 * Gradle Plugin automating the creation of performance testing tasks for each device currently
 * connected to the current system.
 *
 * To use simply add this line to your application build.gradle file below the similar Android
 * Gradle plugin "apply" directive.
 *
 *
 */
public class PerfTestTaskGeneratorPlugin implements Plugin<Project> {
    Logger logger

    public PerfTestTaskGeneratorPlugin() {

    }

    public void apply(Project project) {
        logger = project.getLogger()
        createLocalPerfTestTasks(project)
        createCloudRoboTestTask(project)
    }

    private void createCloudRoboTestTask(Project project) {
        // Create a parent performance test task that can run all of the device-specific tasks.
        project.tasks.create(name: 'runCloudRoboTests',
                type: RunCloudRoboTestsTask)
    }

    private void createLocalPerfTestTasks(Project project) {
        ArrayList<Task> createdTasks = new ArrayList<Task>()
        List<String> connectedDevices = getConnectedDeviceList(project)

        // Tasks the performance test tasks are dependent on.
        HashSet<String> dependentTasks = new HashSet<String>();
        // Retrieve install tasks that need to run before the performance test tasks run.
        dependentTasks.add("assembleDebug")
        dependentTasks.add("assembleAndroidTest")
        dependentTasks.add("installDebug")
        dependentTasks.add("installDebugAndroidTest")
        logger.info("Dependent tasks found: " + dependentTasks)

        // Tasks that should run after the performance test tasks.
        HashSet<String> postTestTasks = new HashSet<String>();
        // Retrieve uninstall tasks that need to be run after the performance tests are complete.
        postTestTasks.add("uninstallAll")
        logger.info("Post test dependent tasks found: " + postTestTasks)

        // Create a parent performance test task that can run all of the device-specific tasks.
        Task runLocalPerfTests = project.tasks.create(name: 'runLocalPerfTests',
                type: DefaultTask,
                group: 'verification',
                dependsOn: dependentTasks,
                description: 'Run performance tests on all connected devices.')

        // Create a perf test task for each connected device.
        connectedDevices.each { androidDeviceId ->
            RunLocalPerfTestsTask newTask = (RunLocalPerfTestsTask) project.tasks.create(
                    name: ('runLocalPerfTests_' + androidDeviceId),
                    type: RunLocalPerfTestsTask)
            newTask.deviceId = androidDeviceId
            // Ensure each device-specific task is run by the parent perf test task.
            runLocalPerfTests.dependsOn(newTask)

            // Ensure dependent tasks are depended upon by all device-specific tasks in case those
            // are run independently.
            newTask.dependsOn(dependentTasks)

            // Add task to list of tasks created.
            createdTasks.add(newTask)
        }

        // Ensure the postTestTasks are run AFTER the performance test tasks.
        createdTasks.each { Task createdTask ->
            postTestTasks.each { String taskName ->
                project.tasks.findByName(taskName).mustRunAfter(createdTask)
            }
        }
    }

    private List<String> getConnectedDeviceList(Project project) {
        def rootDir = project.rootDir
        def localProperties = new File(rootDir, "local.properties")
        def sdkDir = ""
        if (localProperties.exists()) {
            Properties properties = new Properties()
            localProperties.withInputStream { instr ->
                properties.load(instr)
            }
            sdkDir = properties.getProperty('sdk.dir')
        }
        String adbCommand = sdkDir + File.separator + "platform-tools" + File.separator + "adb"
        logger.info("Using ADB command: ${adbCommand}")
        // Compose a list of connected Android devices.
        ArrayList<String> devices = new ArrayList<String>()
        ProcessBuilder processBuilder = new ProcessBuilder()
        processBuilder.command(adbCommand, "devices", "-l")
        Process process = processBuilder.start()
        process.waitFor();
        if (process.exitValue() != 0) {
            throw new Exception("Error using ADB to list connected devices.");
        } else {
            // Process adb output to determine which devices are connected.
            process.inputStream.withReader { processOutputReader ->
                new BufferedReader(processOutputReader).with { bufferedReader ->
                    String outputLine;
                    while ((outputLine = bufferedReader.readLine()) != null) {
                        outputLine = outputLine.trim()
                        if (!outputLine.startsWith("List of devices attached") && !"".equals(outputLine)) {
                            String[] lineParts = outputLine.split(/\s+/) // The regex groups whitespace.
                            if (lineParts.length != 6) {
                                // "adb devices -l" isn't a formal API so we'll add a sanity check. If
                                // the 'spec' changes this should point us right to the issue.
                                throw new Exception("There should always be 6 parts to the output, " +
                                        "double check something isn't wrong: ${outputLine} parsed to " +
                                        "${lineParts}")
                            } else {
                                devices.add(lineParts[0])
                            }
                        }
                    }
                }
            }
        }
        logger.warn("Found connected devices for local performance testing tasks: " + devices)
        return devices
    }
}
