# Copyright 2015, Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Script to orchestrate running android performance tests while also
collecting batterystats, location request information, and a systrace.
"""

from __future__ import with_statement
from xml.etree import ElementTree
from xml.dom import minidom


import os
import shutil
import subprocess
import threading
import time
import re
import sys
import datetime


# Imports the monkeyrunner modules used by this program.
# from com.android.monkeyrunner import MonkeyRunner, MonkeyDevice # pylint: disable=import-error,unused-import

# Percentage of janky frames to detect to warn.

JANK_THRESHOLD = 60
DUMPSYS_FILENAME = 'dumpsys.log'
testcase_names = []
list_for_jank_perc = []
list_for_expected_perc = []
list_for_execution_time = []
list_for_expected_time = []

def perform_test(device, package_name):
    """Execution code for a test run thread."""

    print 'Starting test'

    # Run the test and print the timing result.
    cmd = "./gradlew connectedDebugAndroidTest"
    subprocess.call(cmd, shell=True)

    print 'Done running tests'


def perform_systrace(sdk_path, device_id, dest_dir, package_name):
    """Execution code for a systrace thread."""

    package_parameter = '--app=' + package_name
    systrace_path = os.path.join(sdk_path, 'platform-tools', 'systrace',
                                 'systrace.py')
    systrace_command = ['python', systrace_path,
                        '--serial=' + device_id,
                        package_parameter,
                        '--time=20',
                        '-o', os.path.join(dest_dir, 'trace.html'),
                        'gfx', 'input', 'view', 'wm', 'am', 'sm', 'hal',
                        'app', 'res', 'dalvik', 'power', 'freq', 'freq',
                        'idle', 'load']
    print 'Executing systrace'
    systrace_log_path = os.path.join(dest_dir, 'logs', 'capture_systrace.log')
    with open(systrace_log_path, 'w') as systrace_log:
        try:
            subprocess.call(systrace_command,
                            stdout=systrace_log,
                            stderr=subprocess.STDOUT,
                            shell=False)
        except OSError:
            print 'ERROR executing systrace ' + os_error
    print 'Done systrace logging'


# Enable the DUMP permission on the debuggable test APK (test APK because
# we declared the permission in the androidTest AndroidManifest.xml file.
def enable_dump_permission(sdk_path, device_id, dest_dir, package_name):
    """Enable the DUMP permission on the specified and installed Android
    app.
    """

    print 'Starting dump permission grant'
    perm_command = [os.path.join(sdk_path, 'platform-tools', 'adb'),
                    '-s', device_id,
                    'shell',
                    'pm', 'grant', package_name,
                    'android.permission.DUMP']
    log_file_path = os.path.join(dest_dir, 'logs', 'enable_dump_perm.log')
    with open(log_file_path, 'w') as log_file:
        try:
            subprocess.call(perm_command,
                            stdout=log_file,
                            stderr=subprocess.STDOUT,
                            shell=False)
        except OSError:
            print 'ERROR executing permission grant.'


# Enable the Storage permission on the debuggable test APK (test APK because
# we declared the permission in the androidTest AndroidManifest.xml file.
def enable_storage_permission(sdk_path, device_id, dest_dir, package_name):
    """Enable the WRITE_EXTERNAL_STORAGE permission on the specified and
    installed Android app.
    """

    print 'Starting storage permission grant'
    perm_command = [os.path.join(sdk_path, 'platform-tools',
                                 'adb'),
                    '-s', device_id,
                    'shell',
                    'pm', 'grant', package_name,
                    'android.permission.WRITE_EXTERNAL_STORAGE']
    log_file_path = os.path.join(dest_dir, 'logs', 'enable_storage_perm.log')
    with open(log_file_path, 'w') as log_file:
        try:
            subprocess.call(perm_command,
                            stdout=log_file,
                            stderr=subprocess.STDOUT,
                            shell=False)
        except OSError:
            print 'ERROR executing permission grant.'


def clean_test_files(dest_dir):
    """Removes the test files that are generated during a test run."""

    print 'Cleaning data files'
    folders = [os.path.join(dest_dir, 'testdata'),
               os.path.join(dest_dir, 'logs')]
    for the_folder in folders:
        if os.path.isdir(the_folder):
            for the_file in os.listdir(the_folder):
                file_path = os.path.join(the_folder, the_file)
                try:
                    if os.path.isfile(file_path):
                        os.unlink(file_path)
                    elif os.path.isdir(file_path):
                        shutil.rmtree(file_path)
                except IOError, exception:
                    print exception
    for the_folder in folders:
        if not os.path.isdir(the_folder):
            try:
                os.makedirs(the_folder)
            except OSError:
                print 'ERROR Could not create directory structure for tests.'


def pull_device_data_files(sdk_path, device_id, source_dir, dest_dir, package_name, log_suffix):
    """Extrace test files from a device after a test run."""

    print 'Starting adb pull for test files'
    test_data_location = (source_dir +
                          'testdata')
    pull_test_data_command = [os.path.join(sdk_path, 'platform-tools',
                                           'adb'),
                              '-s', device_id, 'pull', test_data_location,
                              dest_dir]
    log_file_path = os.path.join(dest_dir, 'logs', 'pull_test_files' + log_suffix + '.log')
    with open(log_file_path, 'w') as pull_test_data_log_file:
        try:
            subprocess.call(pull_test_data_command,
                            stdout=pull_test_data_log_file,
                            stderr=subprocess.STDOUT,
                            shell=False)
        except OSError:
            print 'ERROR extracting test files.'


def reset_graphics_dumpsys(device, package_name):
    """Reset all existing data in graphics buffer."""
    print 'Clearing gfxinfo on device'
    #device.shell('dumpsys gfxinfo ' + package_name + ' reset')
    subprocess.call('adb shell dumpsys gfxinfo ' + package_name + ' reset', shell=True)


def run_tests_and_systrace(sdk_path, device, device_id, dest_dir,
                           package_name):
    """Create and start threads to run tests and collect tracing information.
    """
    systrace_thread = threading.Thread(name='SystraceThread',
                                       target=perform_systrace,
                                       args=(sdk_path,
                                             device_id,
                                             dest_dir,
                                             package_name))
    test_thread = threading.Thread(name='TestThread',
                                   target=perform_test,
                                   args=(device,
                                         package_name))
    systrace_thread.start()
    test_thread.start()

    # Join the parallel thread processing to continue when both complete.
    systrace_thread.join()
    test_thread.join()
    trace_time_completion = int(time.time())
    print 'Systrace Thread Done'

    test_thread.join()
    test_time_completion = int(time.time())
    print 'Test Thread Done'
    print ('Time between test and trace thread completion: ' +
           str(test_time_completion - trace_time_completion))


def parse_dump_file(filename):
    """Parse dump file data."""
    with open(filename, 'r') as dump_file:
        results = dict()
        for line in dump_file:
            test_name = re.search(r'TestName:([\w+\.]+)', line)
            match = re.search(r'Janky frames: (\d+) \(([\d\.]+)%\)', line)
            exp_perc = re.search(r'Expected percentage : ([\d\.]+) %', line)
            if test_name is not None:
                results['Testname'] = str(test_name.group(1))
            if match is not None:
                results['jankNum'] = int(match.group(1))
                results['jank_percent'] = float(match.group(2))
            if exp_perc is not None:
                results['expected_percentage'] = (exp_perc.group(1))
        return results



def parse_executiontime_file(filename):
    with open(filename, 'r') as time_file:
        results = dict()
        for line in time_file:
            exe_time = re.search(r'Execution Time : ([\d+\.]+) ms', line)
            exp_time = re.search(r'Expected Time : ([\d+\.]+) ms', line)
            if exe_time is not None:
                results['execution_time'] = (exe_time.group(1))
            if exp_time is not None:
                results['expected_time'] = (exp_time.group(1))
        return  results


def analyze_data_files(dest_dir):
    """Analyze data files for issues that indicate a test failure."""
    overall_passed = True
    test_data_dir = os.path.join(dest_dir, 'testdata')
    for dir_name, sub_dir_list, file_list in os.walk(test_data_dir):
        if dir_name == os.path.join(dest_dir, 'testdata'):
            # in the root folder
            for fname in file_list:
                if fname == 'batterystats.dumpsys.log':
                    # pylint: disable=fixme
                    # TODO(developer): process battery data.
                    continue
                elif fname == 'locationRequests.dumpsys.log':
                    # pylint: disable=fixme
                    # TODO(developer): process location requests information.
                    continue
        else:
            # in a test folder
            print '\nAnalysing test: ' + dest_dir
            passed = True

            for fname in file_list:
                full_filename = os.path.join(dir_name, fname)
                if fname == 'gfxinfo.dumpsys.log':
                    # get the names of testcases
                    get_testcase_name(dest_dir, full_filename)
                    # process gfxinfo for janky frames
                    dump_results = parse_dump_file(full_filename)
                    jank_perc = dump_results['jank_percent']
                    jank_threshold = dump_results['expected_percentage']
                    list_for_jank_perc.append(str(jank_perc))
                    list_for_expected_perc.append(str(jank_threshold))
                    # if jank_perc:
                    #
                    #     if jank_perc > JANK_THRESHOLD:
                    #         print ('FAIL: High level of janky frames ' +
                    #                'detected (' + str(jank_perc) + '%)' +
                    #                '. See trace.html for details.')
                    #         passed = False
                    # else:
                    #     print 'ERROR: No dump results could be found.'
                    #     passed = False
                if fname == 'executiontime.log':
                    executiontime_results = parse_executiontime_file(full_filename)
                    execution_time = executiontime_results['execution_time']
                    expected_time = executiontime_results['expected_time']
                    list_for_execution_time.append(execution_time)
                    list_for_expected_time.append(expected_time)

                    ##############  test  ###############
                    print list_for_execution_time
                    print list_for_expected_time
                    ##############  test  ###############

                elif fname == 'test.failure.log':
                    # process test failure logs
                    print ('FAIL: Test failed. See ' + full_filename +
                           ' for details.')
                    passed = False

            if passed:
                print 'PASS. No issues detected.'
            else:
                overall_passed = False

    test_complete_file = os.path.join(dest_dir, 'testdata',
                                      'testRunComplete.log')
    if not os.path.isfile(test_complete_file):
        overall_passed = False
        print ('\nFAIL: Could not find file indicating the test run ' +
               'completed. Check that the TestListener is writing files to external storage')
    if overall_passed:
        print '\nOVERALL: PASSED.'
        return 0
    else:
        print '\nOVERALL: FAILED. See above for more information.'
        return 1


def get_testcase_name(dest_dir, full_filename):
    dump_results = parse_dump_file(full_filename)
    testcase_name = dump_results['Testname']
    testcase_names.append(testcase_name)

def xml(dest_dir, jank_perc, expected_perc, execution_time, expected_time):
    xml_file_dir = os.path.join(dest_dir, 'app/build/outputs/androidTest-results/connected')
    for file in os.listdir(xml_file_dir):
        xml_file_name = file
    tree = ElementTree.ElementTree(file = xml_file_dir + '/' + xml_file_name)

    for element in tree.findall('testcase'):
        name = element.get('name')
        for testcase_name, jank_perc, expected_perc, execution_time, expected_time in zip(testcase_names,list_for_jank_perc,
                                                                                          list_for_expected_perc,list_for_execution_time,list_for_expected_time):

            convert_execution_time = execution_time
            float_execution_time = float(convert_execution_time)
            convert_expected_time = expected_time
            float_expected_time = float(convert_expected_time)

            convert_jank_perc = jank_perc
            float_jank_perc = float(convert_jank_perc)
            convert_expected_perc = expected_perc
            float_expected_perc = float(convert_expected_perc)

            if name == testcase_name:
                element.text = '\n    '
                ElementTree.SubElement(element,'system-out').text = '<measurement><name>Jank-Percentage (%)</name><value>' + jank_perc + '</value></measurement>' \
                                                                    + '<measurement><name>Execution-Time (ms)</name><value>' + execution_time + '</value></measurement>'

                element.set('jank-percentage', jank_perc)
                element.set('expected-percentage', expected_perc)
                element.set('execution-time', execution_time)
                element.set('expected-time', expected_time)

                if float_execution_time > float_expected_time or float_jank_perc > float_expected_perc:
                    ElementTree.SubElement(element,'failure',{'message' : 'Fail.'})

    tree.write("results.xml")

def main():
    """Run this script with
    monkeyrunner run_perf_tests.py . <DEVICE_ID>
    """

    dest_dir = sys.argv[1:][0] or '.'
    print 'Writing logs to: ' + dest_dir

    device_id = sys.argv[1:][1] or null
    print 'Using device_id: ' + device_id


    # Organize test output by device in case multiple devices are being tested.
    dest_dir = os.path.join(dest_dir, "perftesting", device_id)

    android_home = os.environ['ANDROID_HOME']
    print 'Your ANDROID_HOME is set to: ' + android_home
    # Uncomment this next line to hardcode your android_home if you can't set
    # it in your environment.
    # android_home = '/full/path/to/android/sdk'

    platform_tools = os.path.join(android_home, 'platform-tools')
    current_path = os.environ.get('PATH', '')
    os.environ['PATH'] = (platform_tools if current_path == '' else current_path +
                                                                    os.pathsep + platform_tools)

    if not os.path.isdir(android_home):
        print 'Your ANDROID_HOME path do not appear to be set in your environment'
        sys.exit(1)

    # Your SDK path. Adjust this to your needs.
    sdk_path = android_home

    # sets a variable with the package's internal name
    package_name = 'com.google.android.perftesting'

    clean_test_files(dest_dir)

    # Connects to the current device, returning a MonkeyDevice object
    print 'Waiting for a device to be connected.'
    device = None
    print 'Device connected.'

    # Protip1: Remove the screen lock on your test devices then uncomment
    # this like and the same one farther down. This will prevent you from
    # worrying about whether your device display has gone to sleep.
    # Alternatively, you can use the "never sleep when charging" developer
    # ption.
    # device.press("KEYCODE_POWER", "DOWN_AND_UP")

    enable_dump_permission(sdk_path, device_id, dest_dir, package_name)
    enable_storage_permission(sdk_path, device_id, dest_dir, package_name)


    # Clear the dumpsys data for the next run must be done immediately
    # after open_app().
    reset_graphics_dumpsys(device, package_name)

    run_tests_and_systrace(sdk_path, device, device_id, dest_dir,
                           package_name)

    # Device files could be in either location on various devices.
    pull_device_data_files(sdk_path, device_id,
                           '/storage/emulated/0/Documents/',
                           dest_dir, package_name, '1')
    pull_device_data_files(sdk_path, device_id,
                           '/storage/emulated/legacy/Documents/',
                           dest_dir, package_name, '2')

    # Protip1: See comment above.
    # device.press("KEYCODE_POWER", "DOWN_AND_UP")

    analyze_data_files(dest_dir)

    # adding janky frames and execution time to the xml file
    dest_dir = sys.argv[1:][0] or '.'
    xml(dest_dir, list_for_jank_perc, list_for_expected_perc, list_for_execution_time,list_for_expected_time)


if __name__ == '__main__':
    main()
