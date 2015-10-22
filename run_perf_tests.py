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

import logging
import os
import shutil
import subprocess
import threading
import time
import re
import sys

destDir = sys.argv[1:][0] or '.'
print 'Writing logs to: ' + destDir

# Imports the monkeyrunner modules used by this program.
from com.android.monkeyrunner import MonkeyRunner, MonkeyDevice

androidHome = os.environ['ANDROID_HOME']
print 'Your ANDROID_HOME is set to: ' + androidHome
# Uncomment this next line to hardcode your androidHome if you can't set it in your environment.
# androidHome = '/full/path/to/android/sdk'

platformTools = os.path.join(androidHome, 'platform-tools')
currentPath = os.environ.get('PATH', '')
os.environ['PATH'] = platformTools if currentPath == '' else currentPath + os.pathsep + platformTools

if not os.path.isdir(androidHome):
  print 'Your ANDROID_HOME path doesn''t appear to be set in your environment'
  sys.exit(1)

# Your SDK path. Adjust this to your needs.
SDK_PATH = androidHome
DUMPSYS_FILENAME = 'dumpsys.log'
JANK_THRESHOLD = 25 # Percentage of janky frames to detect to warn.

# sets a variable with the package's internal name
packageName = 'com.google.android.perftesting'
testPackageName = packageName + '.test'

def performTest(disableAnalytics):
    print 'Starting test'

    params = dict({
        'listener': 'com.google.android.perftesting.TestListener',
        'annotation': 'com.google.android.perftesting.common.PerfTest',
        'disableAnalytics': 'true' if disableAnalytics else 'false',
    })

    testRunner = packageName + '.test' + '/android.support.test.runner.AndroidJUnitRunner'

    # Run the test and print the timing result.
    print device.instrument(testRunner, params)['stream']
    print 'Done running tests'

def performSystraceLogging():
    packageParameter = '--app=' + packageName
    systracePath = os.path.join(SDK_PATH, 'platform-tools', 'systrace', 'systrace.py')
    systraceCommand = ['python', systracePath, packageParameter, '--time=20', '-o', os.path.join(destDir, 'testdata', 'trace.html'),
            'gfx', 'input', 'view', 'wm', 'am', 'sm', 'hal', 'app', 'res', 'dalvik', 'power', 'freq', 'freq', 'idle', 'load']
    print 'Executing systrace'
    systraceLogfile = open(os.path.join(destDir, 'logs', 'capture_systrace.log'), 'w')
    subprocess.call(systraceCommand, stdout=systraceLogfile, stderr=subprocess.STDOUT, shell=False)
    systraceLogfile.close()
    print 'Done trace logging'

# Enable the DUMP permission on the debuggable test APK (test APK because we declared the
# permission in the androidTest AndroidManifest.xml file.
def enableDumpPermission():
    print 'Starting dump permission grant'
    dumpPermissionCommand = [os.path.join(SDK_PATH, 'platform-tools', 'adb'), 'shell', 'pm', 'grant', packageName, 'android.permission.DUMP']
    dumpPermissionLogfile = open(os.path.join(destDir, 'logs', 'enable_dump_permission.log'), 'w')
    subprocess.call(dumpPermissionCommand, stdout=dumpPermissionLogfile, stderr=subprocess.STDOUT, shell=False)
    dumpPermissionLogfile.close()

def cleanHostTestDataFiles():
    print 'Cleaning data files'
    folders = [os.path.join(destDir, 'testdata'), os.path.join(destDir, 'logs')]
    for the_folder in folders:
        if os.path.isdir(the_folder):
            for the_file in os.listdir(the_folder):
                file_path = os.path.join(the_folder, the_file)
                try:
                    if os.path.isfile(file_path):
                        os.unlink(file_path)
                    elif os.path.isdir(file_path): shutil.rmtree(file_path)
                except Exception, e:
                    print e
    for the_folder in folders:
        if not os.path.isdir(the_folder):
            os.makedirs(the_folder)

def pullDeviceTestDataFiles():
    print 'Starting adb pull for test files'
    testDataLocation = '/storage/emulated/0/Android/data/' + packageName + '/files/testdata'
    pullDeviceTestDataCommand = [os.path.join(SDK_PATH, 'platform-tools', 'adb'), 'pull', testDataLocation, os.path.join(destDir)]
    pullDeviceTestDataLogfile = open(os.path.join(destDir, 'logs', 'pull_test_files.log'), 'w')
    subprocess.call(pullDeviceTestDataCommand, stdout=pullDeviceTestDataLogfile, stderr=subprocess.STDOUT, shell=False)
    pullDeviceTestDataLogfile.close()

def openApp():
    device.shell('am start -n ' + packageName + '/' + packageName + '.MainActivity')

def resetGfxinfo():
    device.shell('dumpsys gfxinfo ' + packageName + ' reset')

def clearDumpsys():
    print 'Clearing dumpsys log on device'
    device.shell('dumpsys gfxinfo ' + packageName + ' reset >/dev/null')

def runTestsAndTakeSystrace():
    # Create and start threads to run tests and collect tracing information.
    systraceThread = threading.Thread(name='SystraceThread', target=performSystraceLogging)
    testThread = threading.Thread(name='TestThread', target=performTest, kwargs={ 'disableAnalytics': True })
    systraceThread.start()
    testThread.start()

    # Join the parrallel thread processing so this completes when both are complete.
    systraceThread.join()
    traceTimeCompletion = int(time.time())
    print 'Systrace Thread Done'

    testThread.join()
    testTimeCompletion = int(time.time())
    print 'Test Thread Done'
    print 'Time between test and trace thread completion: ' + str(testTimeCompletion - traceTimeCompletion)


def parseDumpsysFile(filename):
    jankPercent = -1
    f = open(filename, 'r')
    results = dict()
    for line in f:
        match = re.search('Janky frames: (\d+) \(([\d\.]+)%\)', line)
        if match:
            results['jankNum'] = int(match.group(1))
            results['jankPercent'] = float(match.group(2))
    return results

def analyseTestDataFiles():
    overallPassed = True
    for dirName, subdirList, fileList in os.walk(os.path.join(destDir, 'testdata')):
        if dirName == os.path.join(destDir, 'testdata'):
            # in the root folder
            for fname in fileList:
                if fname == 'batterystats.dumpsys.log':
                    # TODO: process battery stats
                    continue
                elif fname == 'locationRequests.dumpsys.log':
                    # TODO: process location requests
                    continue
        else:
            # in a test folder
            print '\nAnalysing test: ' + dirName
            passed = True

            for fname in fileList:
                fullFilename = os.path.join(dirName, fname)
                if fname == 'gfxinfo.dumpsys.log':
                    # process gfxinfo for janky frames
                    dumpResults = parseDumpsysFile(fullFilename)
                    jankPerc = dumpResults['jankPercent']
                    if jankPerc:
                        if jankPerc > JANK_THRESHOLD:
                            print 'FAIL: High level of janky frames detected ' \
                                '(' + str(jankPerc) + '%). See trace.html for' \
                                'details.'
                            passed = False
                    else:
                        print 'ERROR: No dump results could be found.'
                        passed = False
                elif fname == 'test.failure.log':
                    # process test failure logs
                    print 'FAIL: Test failed. See ' + fullFilename + \
                        ' for details.'
                    passed = False
            if passed:
                print 'PASS. No issues detected.'
            else:
                overallPassed = False
    if overallPassed:
        print '\nOVERALL: PASSED.'
        return 0
    else:
        print '\nOVERALL: FAILED. See above for more information.'
        return 1

cleanHostTestDataFiles()

# Connects to the current device, returning a MonkeyDevice object
print 'Waiting for a device to be connected.'
device = MonkeyRunner.waitForConnection()
print 'Device connected.'

enableDumpPermission()

# Clear the dumpsys data for the next run.
clearDumpsys()

openApp()

resetGfxinfo()

runTestsAndTakeSystrace()

pullDeviceTestDataFiles()

analyseTestDataFiles()
