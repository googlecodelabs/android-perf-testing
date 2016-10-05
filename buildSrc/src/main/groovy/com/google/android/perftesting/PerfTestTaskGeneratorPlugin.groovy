package com.google.android.perftesting

import jdk.internal.org.objectweb.asm.tree.analysis.Value
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logger

import java.util.regex.Matcher
import java.util.regex.Pattern

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
        Map<String, String> connectedDeviceDict = getConnectedDeviceDict(project)
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
        connectedDeviceDict.each {androidDeviceModel, androidDeviceId ->
            RunLocalPerfTestsTask newTask = (RunLocalPerfTestsTask) project.tasks.create(
                    name: ('runLocalPerfTests_' + androidDeviceId),
                    type: RunLocalPerfTestsTask)
            newTask.deviceId = androidDeviceId
            newTask.deviceModel = androidDeviceModel

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

    private Map<String, String> getConnectedDeviceDict(Project project) {
        def rootDir = project.rootDir

        def localProperties = new File(rootDir, "local.properties")
        def sdkDir = ""
        if (localProperties.exists()) {
            Properties properties = new Properties()
            localProperties.withInputStream { instr ->
                properties.load(instr)
            }
            sdkDir = properties.getProperty('sdk.dir')
        } else {
            sdkDir = System.getenv("ANDROID_HOME")
        }

        String adbCommand = sdkDir + File.separator + "platform-tools" + File.separator + "adb"
        logger.info("Using ADB command: ${adbCommand}")
        // Compose a list of connected Android devices.
        Map<String, String> devices = new HashMap<String, String>()
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
                    // Use regex named groups to check if devices are connected.
                    // "adb devices -l" to view detail of connected device : <deviceID> device (usb) product model device,
                    // if use TCPIP to connect debug, you won't see the usb item.
                    //
                    // List of devices attached
                    // CA7B49C0GF             device usb:352795843X product:E6553 model:E6553 device:E6553
                    // 192.168.12.34:5555     device product:E6653 model:E6653 device:E6653
                    Pattern pattern = Pattern.compile("^(?<deviceID>[\\w\\.:]+)\\s+device\\s+(usb:\\w+\\s+)?product:\\w+\\s+model:(?<deviceModel>\\w+)\\s+device:\\w+");
                    while ((outputLine = bufferedReader.readLine()) != null) {
                        Matcher matcher = pattern.matcher(outputLine);
                        if (matcher.matches()) {
                            devices.put(matcher.group("deviceModel"), matcher.group("deviceID"));
                        }
                    }
                }
            }
        }
        logger.warn("Found connected devices for local performance testing tasks: " + devices)
        return devices
    }
}
