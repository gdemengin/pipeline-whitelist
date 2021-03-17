// test pipeline for https://github.com/gdemengin/pipeline-whitelist


// import whitelist library
@Library('pipeline-whitelist@3.0') _


properties([
    parameters([
        booleanParam(defaultValue: false, description: 'set to to true to run extra long tests (multiple hours + may fail if not enough heap)', name: 'FULL_LOGPARSER_TEST'),
        booleanParam(defaultValue: false, description: 'FULL_LOGPARSER_TEST + even more aggressive: with log editing', name: 'FULL_LOGPARSER_TEST_WITH_LOG_EDIT'),
        booleanParam(defaultValue: false, description: 'run multithread timing test (may last a few hours + manual check of elapsed time)', name: 'MANYTHREAD_TIMING_TEST')
    ])
])

// ===============
// = constants   =
// ===============

// label on the instance with linux hosts
// leave null if all hosts fit the description
LABEL_LINUX='linux'
// label on the instance configured as docker cloud
LABELS_CLOUD=[ 'centos-dockercloud' ]

// set to to true to run extra long tests
// (multiple hours + may fail if not enough heap)
RUN_FULL_LOGPARSER_TEST = params.FULL_LOGPARSER_TEST == true
// even more aggressive: with log editing
RUN_FULL_LOGPARSER_TEST_WITH_LOG_EDIT = params.FULL_LOGPARSER_TEST_WITH_LOG_EDIT == true
// test with many threads to check the time spent
RUN_MANYTHREAD_TIMING_TEST = params.MANYTHREAD_TIMING_TEST == true

// =============
// = globals   =
// =============

// uncomment if needed
// logparser.setVerbose(true)

// true when "new Exception" is allowed (if plugin script-security is 1.44 or more)
@groovy.transform.Field
newExcAllowed = true

// =====================
// = whitelist tests   =
// =====================

// sort list of maps by field must be done in @NonCPS method
// https://issues.jenkins-ci.org/browse/JENKINS-44924
@NonCPS
def sortByField(list, fieldName) {
    list.sort{ it[fieldName] }
}

def testVersion() {
    print 'testing version'

    def version = whitelist.version()
    assert version == '3.0'

    def instanceVersion = whitelist.instanceVersion()
    def versionStr = "Jenkins Instance Version : ${instanceVersion}"
    print versionStr

    def versionTokens = instanceVersion.split(/\./)
    assert versionTokens.size() >= 2 && versionTokens[0] ==~ /\d+/ && versionTokens[1] ==~ /\d+/
    def major = versionTokens[0].toInteger()
    def minor = versionTokens[1].toInteger()

    assert major >= 2,
        "jenkins version ${instanceVersion} is not greater than 2, this is unexpected"

    def plugins = whitelist.plugins()
    sortByField(plugins, 'shortName')
    def pluginsVersionStr = "Plugins : \n${plugins.collect{ "\t${it.displayName} (${it.shortName}) v${it.version}" }.join('\n')}"
    print pluginsVersionStr
    assert plugins.find{ it.shortName == 'workflow-job' } != null,
        'workflow-job plugin is not installed, this is unexpected'

    // check if script security version is less than 1.44
    def SSpluginVerList = plugins.findAll{ it.shortName == 'script-security' }.collect { it.version }
    assert SSpluginVerList.size() == 1, 'could not find script-security plugin version'

    def SSversionTokens = SSpluginVerList[0].split(/\./)
    assert SSversionTokens.size() >= 2 && SSversionTokens[0] ==~ /\d+/ && SSversionTokens[1] ==~ /\d+/
    def SSmajor = SSversionTokens[0].toInteger()
    def SSminor = SSversionTokens[1].toInteger()

    newExcAllowed = ((SSmajor > 1) || (SSmajor == 1 && SSminor >= 44))

    // return version as string to be archived
    return "${versionStr}\n${pluginsVersionStr}"
}

def testString() {
    print 'testing string manipulation'

    def result = whitelist.multiply('toto ', 3)
    def expected = 'toto toto toto '
    assert result == expected,
        "whitelist.multiply('toto ', 3) gives an unexpected result: '${result}', expected: '${expected}'"

    result = whitelist.escapeHtml4('">&<')
    expected = '&quot;&gt;&amp;&lt;'
    assert result == expected,
        "whitelist.escapeHtml4('\">&<') gives an unexpected result: '${result}', expected: '${expected}'"
}

def testDate() {
    print 'testing date manipulation'

    def t1 = whitelist.parseDate("dd/MM/yyyy HH:mm:ss", '01/01/2020 12:00:24');
    def t2 = whitelist.parseDate("dd/MM/yyyy HH:mm:ss", '01/01/2041 00:00:00');

    def interval = whitelist.minusDate(t2, t1)
    print interval
    assert interval.days == 7670 && interval.hours == 11 && interval. minutes == 59 && interval.seconds == 36 && interval.millis == 0

    t1 = whitelist.parseDate("dd/MM/yyyy HH:mm:ss", '01/01/2020 12:00:24');
    t2 = whitelist.parseDate("dd/MM/yyyy HH:mm:ss", '01/01/2020 12:02:00');

    interval = whitelist.minusDate(t2, t1)
    print interval
    assert interval.days == 0 && interval.hours == 0 && interval. minutes == 1 && interval.seconds == 36 && interval.millis == 0

    interval = whitelist.minusDate(t1, t1)
    print interval
    assert interval.days == 0 && interval.hours == 0 && interval. minutes == 0 && interval.seconds == 0 && interval.millis == 0

    t1 = new Date()
    // wait a few ms, print shall actually do that
    print 'waiting a few ms'
    interval = whitelist.minusDate(new Date(), t1)
    print interval
    assert interval.days == 0 && interval.hours == 0 && interval.minutes == 0 && interval.seconds >= 0 && interval.millis > 0
}

// utility function to check exceptions
// beforeExcStackTrace is the stack one line before the exception
// if exceptionInStack is false:
//   we expect that the stack does not contain the line causing the issue in it
//   (assert would cause that for example, unfortunately)
def catchAndCheckException(
    Throwable e,
    StackTraceElement[] beforeExcStackTrace,
    Class expectedClass,
    Boolean exceptionInStack,
    Boolean exceptionFirstInStack
) {
    print 'checking exception'

    print "class = ${e.class}"
    assert e.class == expectedClass, "${e.class} does not match expected ${expectedClass}"

    def excStackTrace = whitelist.getStackTrace(e)
    print "${expectedClass} stacktrace = \n\t${ excStackTrace.collect { it.toString() }.join('\n\t') }"
    assert excStackTrace.size() != 0, 'stacktrace is empty'

    def excFilteredStackTrace = whitelist.filterStackTrace(excStackTrace)
    print "${expectedClass} filtered stacktrace = \n\t${ excFilteredStackTrace.collect { it.toString() }.join('\n\t') }"

    def suppressed = whitelist.getSuppressed(e)
    print "suppressed exceptions = \n${ suppressed.collect{ "${it}\n\t${ whitelist.getStackTrace(it).collect { it.toString() }.join('\n\t') }" }.join('\n') }"

    // the line of syntax error is not the exception itself (not the first one): look for it in the stack
    assert beforeExcStackTrace != null
    assert beforeExcStackTrace.size() > 0
    def excLine = whitelist.getLineNumber(beforeExcStackTrace[0]) + 1
    def currentClass = whitelist.getClassName(beforeExcStackTrace[0])
    def currentMethod = whitelist.getMethodName(beforeExcStackTrace[0])
    def currentFile = whitelist.getFileName(beforeExcStackTrace[0])
    def expected = "${currentClass}.${currentMethod}(${currentFile}:${excLine})"

    if (exceptionInStack) {
        assert suppressed.size() == 0, 'suppressed is not empty'

        if (exceptionFirstInStack) {
            assert excStackTrace[0].toString() == expected.toString(),
                "'${expected}' not first item in stacktrace"
        } else {
            // the line of syntax error is not the exception itself (not the first one): look for it in the stack
            assert excStackTrace.collect{ it.toString() }.contains(expected.toString()),
                "'${expected}' not found in stacktrace"
        }

        // after filtering, the line of the exception is the first one
        assert excFilteredStackTrace.size() != 0,
            'filtered stacktrace is empty'
        assert excFilteredStackTrace[0].toString() == expected.toString(),
            "'${expected}' not first item in filtered stacktrace"
    } else {
        // after filtering the stack is empty (which is why we have a suppressed not empty)
        assert excFilteredStackTrace.size() == 0,
            'filtered stacktrace is not empty'

        assert suppressed.size() != 0, 'suppressed exception list is empty'
        // TODO : handle more than 1 ? not needed for now
        assert suppressed.size() == 1, 'suppressed exception list has more than 1 element'

        def suppressedStackTrace = whitelist.getStackTrace(suppressed[0])
        assert suppressedStackTrace.size() != 0,
            'suppressed stacktrace is empty'

        assert suppressedStackTrace[0].toString() == expected.toString(),
            "'${expected}' not first item in suppressed stacktrace"
    }
}

def testMetaDataAccess() {
    print 'testing metadata accessors'


    // test stack, class, method, file and line number

    def currentStackTrace = whitelist.getCurrentStackTrace()
    print "current stacktrace = \n\t${currentStackTrace.collect { it.toString() }.join('\n\t')}"

    def currentStackElement = currentStackTrace[0]
    print "first stacktrace element = ${currentStackElement}"

    def currentClass = whitelist.getClassName(currentStackElement)
    print "corresponding class = ${currentClass}"

    def currentMethod = whitelist.getMethodName(currentStackElement)
    print "corresponding method = ${currentMethod}"

    def currentFile = whitelist.getFileName(currentStackElement)
    print "corresponding file = ${currentFile}"

    def currentLine = whitelist.getLineNumber(currentStackElement)
    print "corresponding line = ${currentLine}"

    def expected = "${currentClass}.${currentMethod}(${currentFile}:${currentLine})"
    def message = "Stack element '${currentStackElement.toString()}' does not correspond to"
    message += " class,method,file,line '${expected}' ... or the format has changed unexpectedly"
    assert currentStackElement.toString() == expected, message


    // test filtering

    def filteredCurrentStackTrace = whitelist.filterStackTrace(currentStackTrace)
    print "filtered current stacktrace = \n\t${filteredCurrentStackTrace.collect { it.toString() }.join('\n\t')}"

    def filteredCurrentStackElement = filteredCurrentStackTrace[0]
    print "first filtered stacktrace element = ${filteredCurrentStackElement}"

    assert filteredCurrentStackElement == currentStackElement,
        'filtering failed, wrong first element ${filteredCurrentStackElement}'


    // test various kind of Exceptions and Errors

    def beforeExcStackTrace = null

    // syntax error (groovy.lang.MissingPropertyException)
    beforeExcStackTrace = null
    try {
        beforeExcStackTrace = whitelist.getCurrentStackTrace()
        this_is_a syntax_error
    } catch (e) {
        print "caught ${e}"

        // append stackTrace in a suppressed exception if needed
        whitelist.addTraceableStackTrace(e)

        catchAndCheckException(
            e,
            beforeExcStackTrace,
            groovy.lang.MissingPropertyException,
            true,
            false
        )
    }

    // no such method error (java.lang.NoSuchMethodError)
    beforeExcStackTrace = null
    try {
        beforeExcStackTrace = whitelist.getCurrentStackTrace()
        this_is_an_error()
    } catch (Error e) {
        print "caught ${e}"

        // append stackTrace in a suppressed exception if needed
        whitelist.addTraceableStackTrace(e)

        catchAndCheckException(
            e,
            beforeExcStackTrace,
            java.lang.NoSuchMethodError,
            true,
            false
        )
    }

    // new Exception not allowed with older version (not sure since when)
    if (newExcAllowed) {
        // plain exception
        beforeExcStackTrace = null
        try {
            beforeExcStackTrace = whitelist.getCurrentStackTrace()
            throw new Exception('this is an exception')
        } catch (e) {
            print "caught ${e}"

            // append stackTrace in a suppressed exception if needed
            whitelist.addTraceableStackTrace(e)

            catchAndCheckException(
                e,
                beforeExcStackTrace,
                java.lang.Exception,
                true,
                true
            )
        }
    }

    // error statement (hudson.AbortException)
    beforeExcStackTrace = null
    try {
        beforeExcStackTrace = whitelist.getCurrentStackTrace()
        error 'this is an error'
    } catch (e) {
        print "caught ${e}"

        // append stackTrace in a suppressed exception if needed
        whitelist.addTraceableStackTrace(e)

        catchAndCheckException(
            e,
            beforeExcStackTrace,
            hudson.AbortException,
            true,
            false
        )
    }

    // error statement (org.codehaus.groovy.runtime.powerassert.PowerAssertionError)
    // without the exception in the stack (maybe this is a bug but sometimes it happens. thus whitelist.addTraceableStackTrace)
    beforeExcStackTrace = null
    try {
        assert 1 == 0
    } catch (Error e) {
        print "caught ${e}"

        // append stackTrace in a suppressed exception if needed
        beforeExcStackTrace = whitelist.getCurrentStackTrace()
        whitelist.addTraceableStackTrace(e)

        catchAndCheckException(
            e,
            beforeExcStackTrace,
            org.codehaus.groovy.runtime.powerassert.PowerAssertionError,
            false,
            false
        )
    }


    // test superclass

    print "superclass of String = ${whitelist.getSuperclass(String)}"
    assert whitelist.getSuperclass(String) == Object,
        "superclass of String is not Object but ${whitelist.getSuperclass(String)}, this is unexpected"
}

def testLog() {

    // print marker in log
    print 'mark logs'
    // and look for it
    // wait 2s to make sure logs are flushed (1s is usually enough)
    sleep 2
    assert whitelist.getRawBuildLog().contains('mark logs'), 'mark logs not found in logs'
    assert whitelist.getRawBuildLog(currentBuild).contains('mark logs'), 'mark logs not found in logs'
    print 'OK marker found in logs'

    // whitelist.getRawMatrixRunsLog() is tested through samples/MatrixAccess.groovy
    // (which requires script approval by admin: too inconvenient to test here)
    // (TODO: find a better way to test it)
    /*
    def jobResult = build job: 'testmatrix'
    assert whitelist.getRawMatrixRunsLog(jobResult).keySet().sort == ['axis1', 'axis2']
    whitelist.getRawMatrixRunsLog(jobResult).collect{ k,v -> assert v.contains('teststring') }
    */
}

def testJobsAndBuilds() {
    def jobs = whitelist.getJobs()
    print "jobs : \n\t${jobs.collect{ it.toString() }.join('\n\t')}"
    assert jobs."${env.JOB_NAME}" != null, "${env.JOB_NAME} not in job list"

    // whitelist.getMatrixConfiguration() is tested through samples/MatrixAccess.groovy
    // (which requires script approval by admin: too inconvenient to test here)
    // (TODO: find a better way to test it)

    def job = whitelist.getJobByName(env.JOB_NAME)
    print "getJobByName(${env.JOB_NAME}) == ${job}"
    assert jobs."${env.JOB_NAME}" == job

    def runIds = whitelist.getRunIds(job)
    print "run ids for current job = ${runIds}"
    assert runIds.contains(env.BUILD_NUMBER), "${env.BUILD_NUMBER} not in list of ids for current job"

    def build = whitelist.getRunWrapper(job, env.BUILD_NUMBER)
    print "getRunWrapper(${job}, ${env.BUILD_NUMBER}) = ${build.getFullDisplayName()}"
    assert build.getFullDisplayName() == currentBuild.getFullDisplayName(), 'could not get currentBuild with getRunWrapper'

    build = whitelist.getRunWrapper(job, Integer.parseInt(env.BUILD_NUMBER))
    print "getRunWrapper(${job}, Integer.parseInt('${env.BUILD_NUMBER}')) = ${build.getFullDisplayName()}"
    assert build.getFullDisplayName() == currentBuild.getFullDisplayName(), 'could not get currentBuild with getRunWrapper'

    build = whitelist.getLastRunWrapper(job)
    print "getLastRunWrapper(${job}) = ${build.getFullDisplayName()}"
    assert build.getFullDisplayName() == currentBuild.getFullDisplayName(), 'could not get currentBuild with getLastRunWrapper'

    build = whitelist.getLastStableRunWrapper(job)
    print "getLastStableRunWrapper(${job}) = ${build ? build.getFullDisplayName() : null}"

    // no simple idea how to assert the result of this call: for now just try to make the call to make sure it does not fail
    print "getBuildStartupCauses() = ${whitelist.getBuildStartupCauses()}"
    print "isJobStartedByScm() = ${whitelist.isJobStartedByScm()}"
    print "isJobStartedManually() = ${whitelist.isJobStartedManually()}"
    print "isJobStartedByTimer() = ${whitelist.isJobStartedByTimer()}"
    assert [whitelist.isJobStartedByScm(), whitelist.isJobStartedManually(), whitelist.isJobStartedByTimer()].sort() == [false, false, true],
        'something unexpected happened: the build was not started by scm, timer or human'
}

def testJobFilesAccess(versionStr) {
    print 'testing archive/unarchive'
    whitelist.archiveStringArtifact('tested-with.txt', versionStr)
    def text = whitelist.unArchiveStringArtifact('tested-with.txt')
    assert text == versionStr

    print "job config = \n\t${ whitelist.getJobConfig().split('\n').join('\n\t') }"

    def scripts = whitelist.getBuildPipelineScripts()
    print "pipeline script = \n\t${scripts.script.split('\n').join('\n\t')}"
    assert scripts.loadedScripts.size() == 0

    node(LABEL_LINUX) {
        writeFile file: 'toload1', text: 'echo "this is a loaded script"'
        load 'toload1'
    }
    scripts = whitelist.getBuildPipelineScripts()
    assert scripts.loadedScripts.size() == 1
    print "pipeline loadedScripts = \n\t${ scripts.loadedScripts.collect{ k, v -> "$k:\n\t\t${v.split('\n').join('\n\t\t')}" }.join('\n\t') }"
    assert scripts.loadedScripts.Script1 == 'echo "this is a loaded script"'

    node(LABEL_LINUX) {
        writeFile file: 'toload2', text: 'echo "this is another loaded script"'
        load 'toload2'
    }
    scripts = whitelist.getBuildPipelineScripts()
    assert scripts.loadedScripts.size() == 2

    print scripts.loadedScripts
    print "pipeline loadedScripts = \n\t${ scripts.loadedScripts.collect{ k, v -> "$k:\n\t\t${v.split('\n').join('\n\t\t')}" }.join('\n\t') }"
}

def testNodesAndLabels() {
    print "nodes : \n\t${whitelist.getNodes().collect { it.toString() }.join('\n\t')}"
    assert whitelist.getNodes().size() != 0

    for (node in whitelist.getNodes()) {
        assert whitelist.isMaster(node) || whitelist.isDumbSlave(node) || whitelist.isCloudNode(node)
        print "node ${node} type: master=${whitelist.isMaster(node)} dumbSlave=${whitelist.isDumbSlave(node)} cloud=${whitelist.isCloudNode(node)}"
    }

    assert whitelist.getNodes('wrongLabelDoesNotExist').size() == 0

    // master may not be in the list returned by getLabels but it is a label of one element
    assert whitelist.getNodes('master').size() == 1
    assert whitelist.isMaster(whitelist.getNodes('master')[0])

    print "labels : \n\t${whitelist.getLabels().collect { it.toString() }.join('\n\t')}"
    assert whitelist.getLabels().size() != 0

    for (label in whitelist.getLabels()) {
        print "label ${label} type: cloud=${whitelist.isCloudLabel(label)}"
    }

    // make sure we can call getNodes (no exception) on all labels
    for (label in whitelist.getLabels()) {
        print "nodes for label ${label} : \n\t${whitelist.getNodes(label).collect { it.toString() }.join('\n\t')}"
    }

    // test specific cloud labels to test that we can access them with getNodes without error

    for (label in LABELS_CLOUD) {
        assert label in whitelist.getLabels()
        assert whitelist.isCloudLabel(label)
        // wait a little bit in case label is busy
        timeout(time: 5, unit: 'MINUTES') {
            // TODO recursive acquire until no more physical node
            node(label) {
                def nodes = whitelist.getNodes(env.NODE_NAME)
                assert nodes.size() == 1
                def node = nodes[0]
                print "node ${node} type: master=${whitelist.isMaster(node)} dumbSlave=${whitelist.isDumbSlave(node)} cloud=${whitelist.isCloudNode(node)}"
                assert whitelist.isCloudNode(node)
                assert node in whitelist.getNodes(label)
            }
        }
        // TODO : catch exception and check if lable was busy and could not be acquired
    }
}

def testSemaphore() {
    print 'test semaphore'
    timeout(time: 30, unit: 'SECONDS') {
        def s = whitelist.semaphore(1)
        whitelist.acquireSemaphore(s)
        whitelist.releaseSemaphore(s)

        s = whitelist.semaphore(1)
        def b1 = 0
        def b2 = 0
        def didWait = false
        parallel b1: {
            whitelist.acquireSemaphore(s)
            b1 = 1
            if (b2 == 0) {
                sleep 5
                // this assert would fail if the other branch did go passed the acquire within 5 seconds
                // if not it's proof the other branch did not start
                assert b2 == 0
                didWait = true
            }
            whitelist.releaseSemaphore(s)
        }, b2: {
            whitelist.acquireSemaphore(s)
            b2 = 1
            if (b1 == 0) {
                sleep 5
                // this assert would fail if the other branch did go passed the acquire
                // if not it's proof the other branch did not start
                assert b1 == 0
                didWait = true
            }
            whitelist.releaseSemaphore(s)
        }, failFast: true

        // this assert would fail if both branches did go passed the acquire at the same time
        assert didWait == true
    }
    print 'ok'
}

// =====================
// = logparser tests   =
// =====================

// =========================
// = run parallel branches =
// = and nested branches   =
// =========================

// alternate sleeps and echo to have mixed logs
def testBranch(name, loop, expectedLogMap) {
    def line
    expectedLogMap."$name" = ''
    for (def i=0; i < loop; i++) {
        sleep 1
        expectedLogMap."$name" += 'Sleeping for 1 sec\n'
        line="i=$i in $name"
        echo line
        expectedLogMap."$name" += line + '\n'
    }
    line="in $name"
    echo line
    expectedLogMap."$name" += line + '\n'
}

def runBranches(expectedLogMap) {
    def line
    def branches = [:]
    def loop=2

    // simple branch with logs
    branches.branch0 = {
        testBranch('branch0', loop, expectedLogMap)
    }

    // simple branch with logs both here and nested in branch2
    branches.branch1 = {
        testBranch('branch1', loop, expectedLogMap)
    }

    // branch with nested branches
    // one of the nested branches has the same name as branch1
    branches.branch2 = {
        testBranch('branch2', loop, expectedLogMap)

        def nestedBranches = [:]
        nestedBranches.branch21 = {
            testBranch('branch2.branch21', loop, expectedLogMap)
        }
        line='in branch2 before to run nested branches'
        echo line
        expectedLogMap.'branch2' += line + '\n'
        nestedBranches.branch22 = {
            testBranch('branch2.branch22', loop, expectedLogMap)
        }

        // see how we manage to have 2 branches with the same name
        nestedBranches.branch1 = {
            testBranch('branch2.branch1', loop, expectedLogMap)
        }
        parallel nestedBranches
        expectedLogMap.'branch2' += '<nested branch [branch2] [branch21]>\n'
        expectedLogMap.'branch2' += '<nested branch [branch2] [branch22]>\n'
        expectedLogMap.'branch2' += '<nested branch [branch2] [branch1]>\n'
    }

    // branch with no logs
    expectedLogMap.'branch3' = ''
    branches.branch3 = {
        def k = 0
        k += 1
    }

    // run branches
    parallel branches
    expectedLogMap.'null' += '<nested branch [branch0]>\n'
    expectedLogMap.'null' += '<nested branch [branch1]>\n'
    expectedLogMap.'null' += '<nested branch [branch2]>\n'
    expectedLogMap.'null' += '<nested branch [branch2] [branch21]>\n'
    expectedLogMap.'null' += '<nested branch [branch2] [branch22]>\n'
    expectedLogMap.'null' += '<nested branch [branch2] [branch1]>\n'
    expectedLogMap.'null' += '<nested branch [branch3]>\n'

    line='this log is not in any branch'
    echo line
    expectedLogMap."null" += line + '\n'
}

def runSingleBranches(expectedLogMap) {
    def line='test single parallel branch'
    print line
    expectedLogMap.'null' += line + '\n'

    // parallel with only one branch
    // it's a way to generate block of logs (wish there was a dedicated log command to do that)
    expectedLogMap.'init' = ''
    parallel init: {
        line=1
        print line
        expectedLogMap."init" += line + '\n'
    }

    expectedLogMap.'empty' = ''
    parallel empty: {
    }

    expectedLogMap.'endl' = ''
    parallel endl: {
        print ''
        expectedLogMap."endl" += '\n'
    }

    // same with nested tasks
    expectedLogMap.'main' = ''
    parallel main: {
        expectedLogMap.'main.build' = ''
        parallel build: {
            line=2
            print line
            expectedLogMap."main.build" += line + '\n'
        }
        expectedLogMap.'main.test' = ''
        parallel test: {
            line='\n\ntest empty lines\n\nand more empty lines\n\n'
            print line
            expectedLogMap."main.test" += line + '\n'
        }
        expectedLogMap.'main' += '<nested branch [main] [build]>\n'
        expectedLogMap.'main' += '<nested branch [main] [test]>\n'
    }
    expectedLogMap.'null' += '<nested branch [init]>\n'
    expectedLogMap.'null' += '<nested branch [empty]>\n'
    expectedLogMap.'null' += '<nested branch [endl]>\n'
    expectedLogMap.'null' += '<nested branch [main]>\n'
    expectedLogMap.'null' += '<nested branch [main] [build]>\n'
    expectedLogMap.'null' += '<nested branch [main] [test]>\n'
}

def runBranchesWithManyLines(nblines, expectedLogMap) {
    // run branches with manylines and making sure lines are mixed between branches
    // without technical pipeline pieces of logs between each switch of branch in the logs
    def script = '''\
        set +x
        echo "+ set +x" > output.txt
        i=0
        while [ $i -lt ''' + nblines.toString() + ''' ]
        do
            echo line $i
            ''' + (expectedLogMap != null ? 'echo line $i >> output.txt' : '') + '''
            i=$(( $i+1 ))
            if [ $i = 50 ]
            then
                # sleep once in the middle to make sure the other branch adds some logs in the middle
                sleep 1
            fi
        done
        '''
    script = script.stripIndent()

    print script
    if (expectedLogMap != null) {
        expectedLogMap.'null' += script + '\n'

        expectedLogMap.'one' = ''
        expectedLogMap.'two' = ''
    }

    def start = 0
    parallel one: {
        print 'STRIP_NODE_LOG_START'
        node(LABEL_LINUX) {
            timeout(5) { // wait for other branch ... but no more than 5 minutes to avoid deadlock
                print 'STRIP_NODE_LOG_STOP'
                start += 1
                while (start < 2) {}
            }
            sh script
            if (expectedLogMap != null) {
                expectedLogMap.'one' += readFile('output.txt')
            }
        }
    }, two: {
        print 'STRIP_NODE_LOG_START'
        node(LABEL_LINUX) {
            timeout(5) { // wait for other branch ... but no more than 5 minutes to avoid deadlock
                print 'STRIP_NODE_LOG_STOP'
                start += 1
                while (start < 2) {}
            }
            sh script
            if (expectedLogMap != null) {
                expectedLogMap.'two' += readFile('output.txt')
            }
        }
    }
    if (expectedLogMap != null) {
        expectedLogMap.'null' += '<nested branch [one]>\n'
        expectedLogMap.'null' += '<nested branch [two]>\n'
    }
}

def runStagesAndBranches(expectedLogMap, expectedLogMapWithStages) {
    def line = 'testing stages'
    print line
    expectedLogMap.'null' += line + '\n'
    expectedLogMapWithStages.'null' += line + '\n'

    expectedLogMapWithStages.'stage1' = ''
    stage('stage1') {
        line='in stage1'
        echo line
        expectedLogMap.'null' += line + '\n'
        expectedLogMapWithStages.'stage1' += line + '\n'
    }

    expectedLogMapWithStages.'stage2' = ''
    stage('stage2') {
        expectedLogMap.'s2b1' = ''
        expectedLogMap.'s2b2' = ''
        expectedLogMapWithStages.'stage2.s2b1' = ''
        expectedLogMapWithStages.'stage2.s2b2' = ''
        expectedLogMapWithStages.'stage2.stage3' = ''

        stage('stage3') {
            line='in stage2.stage3'
            echo line
            expectedLogMap.'null' += line + '\n'
            expectedLogMapWithStages.'stage2.stage3' += line + '\n'
        }

        parallel 's2b1': {
            line='in stage2.s2b1'
            echo line
            expectedLogMap.'s2b1' += line + '\n'
            expectedLogMapWithStages.'stage2.s2b1' += line + '\n'
        }, 's2b2': {
            line='in stage2.s2b2'
            echo line
            expectedLogMap.'s2b2' += line + '\n'
            expectedLogMapWithStages.'stage2.s2b2' += line + '\n'
        }

        expectedLogMap.'null' += '<nested branch [s2b1]>\n'
        expectedLogMap.'null' += '<nested branch [s2b2]>\n'
        expectedLogMapWithStages.'stage2' += '<nested branch [stage2] [stage3]>\n'
        expectedLogMapWithStages.'stage2' += '<nested branch [stage2] [s2b1]>\n'
        expectedLogMapWithStages.'stage2' += '<nested branch [stage2] [s2b2]>\n'
    }
    expectedLogMapWithStages.'null' += '<nested branch [stage1]>\n'
    expectedLogMapWithStages.'null' += '<nested branch [stage2]>\n'
    expectedLogMapWithStages.'null' += '<nested branch [stage2] [stage3]>\n'
    expectedLogMapWithStages.'null' += '<nested branch [stage2] [s2b1]>\n'
    expectedLogMapWithStages.'null' += '<nested branch [stage2] [s2b2]>\n'
}

// =======================================
// = parse logs and archive/check them   =
// =======================================

def archiveStringArtifact(name, buffer) {
     whitelist.archiveStringArtifact(name, buffer)
}

def checkLogs(log1, editedLog1, name1, log2, editedLog2, name2) {
    def tocmp1 = editedLog1 == null ? log1 : editedLog1
    def tocmp2 = editedLog2 == null ? log2 : editedLog2

    if (tocmp1 != tocmp2) {
        // TODO: print side by side differences
        print "${name1} = '''\\\n${log1}'''"
        archiveStringArtifact("check/${name1}.txt", log1)
        if (log1 != tocmp1) {
            print "${name1} (edited) ='''\\\n${tocmp1}'''"
            archiveStringArtifact("check/${name1}.edited.txt", tocmp1)
        }
        print "${name2} = '''\\\n${log2}'''"
        archiveStringArtifact("check/${name2}.txt", log2)
        if (log2 != tocmp2) {
            print "${name2} (edited) ='''\\\n${tocmp2}'''"
            archiveStringArtifact("check/${name2}.edited.txt", tocmp2)
        }
        error "${name1} and ${name2} differ, see files in ${BUILD_URL}/artifact/check"
    } else {
        print "${name1} and ${name2} are identical"
    }
}

def checkBranchLogs(logs, name, expected) {
    checkLogs(removeShellCall(logs), null, "logs.'${name}'", expected, null, 'expected')
}

def extractMainLogs(mainLogs, begin, end) {
    // add \n in case log starts or ends with begin/end
    def logs = '\n' + mainLogs + '\n'
    // count not allowed with older versions
    //assert logs.count("\n${begin}\n").size() == 1
    //assert logs.count("\n${end}\n").size() == 1
    // use split to count
    assert logs.split(/\n${begin}\n/).size() == 2, logs
    assert logs.split(/\n${end}\n/).size() == 2, logs

    return logs.replaceFirst(/(?s).*\n${begin}\n(.*\n)${end}\n.*/, /$1/)
}

// logs from node acquisition are not predictible (depend on node runtime availability)
def stripNodeLogs(logs, n) {
    def begin = 'STRIP_NODE_LOG_START'
    def end = 'STRIP_NODE_LOG_STOP'
    // count not allowed with older versions
    //assert logs.count("${begin}\n").size() == n
    //assert logs.count("\n${end}\n").size() == n
    // use split to count
    assert logs.split(/${begin}\n/).size() == n + 1, logs
    assert logs.split(/${end}\n/).size() == n + 1, logs

    def regexSeparated = "(?s)\\[(one|two)\\] ${begin}((?!${begin}).)*?${end}\n"
    def regexImbricated = "(?s)\\[(one|two)\\] ${begin}.*${end}\n(?!${end})"

    return logs.replaceAll(/${regexSeparated}|${regexImbricated}/, '')
}

def removeTimestamps(logs) {
    return logs.replaceAll(/(?m)^(?!<nested)(?!\[Pipeline\])(.*)\[[^\[\]]*\] (.*)$/, '$1$2')
}

def removeFilters(logs) {
    return logs.replaceAll(/(?m)^\<nested branch \[.*\]>$\n/, '')
}

def removeShellCall(logs) {
    return logs.replaceAll(/(?m)^.* Running shell script$\n/, '')
}

def expectedBranchLogs(expectedLogMap, key, branchInfo) {
    if (expectedLogMap."${key}".size() == 0 ) {
        return ''
    }

    assert expectedLogMap."${key}"[-1] == '\n'
    def expected = expectedLogMap."${key}".substring(0, expectedLogMap."${key}".size() - 1).split('\n', -1).collect{
        if (it.startsWith('<nested branch [')) {
            return it
        }
        return "${branchInfo}${it}"
    }.join('\n') + '\n'
    return expected
}

def unsortedCompare(log1, log2) {
    def sortedLog1 = log1.split('\n', -1).sort().join('\n')
    def sortedLog2 = log2.split('\n', -1).sort().join('\n')

    checkLogs(sortedLog1, null, 'sortedLog1', sortedLog2, null, 'sortedLog2')
}

def parseLogs(expectedLogMap, expectedLogMapWithStages, begin, end) {
    // 1/ archivelogs (for manual check)

    // archive full logs
    timestamps {
        print 'before parsing and archiving consoleText.txt'
        logparser.archiveLogsWithBranchInfo('consoleText.txt')
        print 'after parsing and archiving consoleText.txt'

        print 'before parsing and archiving branch0.txt'
        logparser.archiveLogsWithBranchInfo('branch0.txt', [filter: ['branch0']])
        print 'after parsing and archiving branch0.txt'

        print 'before parsing and archiving full.txt'
        logparser.archiveLogsWithBranchInfo('full.txt', [ showStages: true, hidePipeline: false, hideVT100: false ])
        print 'after parsing and archiving full.txt'

        print 'before parsing and archiving branch2.txt'
        logparser.archiveLogsWithBranchInfo('branch2.txt', [filter: ['branch2']])
        print 'after parsing and archiving branch2.txt'

        print 'before parsing and archiving branch2NoNested.txt'
        logparser.archiveLogsWithBranchInfo('branch2NoNested.txt', [filter: ['branch2'], markNestedFiltered: false ])
        print 'after parsing and archiving branch2NoNested.txt'

        print 'before parsing and archiving branch2NoParent.txt'
        logparser.archiveLogsWithBranchInfo('branch2NoParent.txt', [filter: ['branch2'], showParents: false ])
        print 'after parsing and archiving branch2NoParent.txt'

        print 'before parsing and archiving branch21.txt'
        logparser.archiveLogsWithBranchInfo('branch21.txt', [filter: ['branch21']])
        print 'after parsing and archiving branch21.txt'

        print 'before parsing and archiving branch21NoParent.txt'
        logparser.archiveLogsWithBranchInfo('branch21NoParent.txt', [filter: ['branch21'], showParents: false])
        print 'after parsing and archiving branch21NoParent.txt'
    }

    // 2/ access logs programmatically using various options

    // full logs
    def fullLog = logparser.getLogsWithBranchInfo()

    // branch by branch
    def logsNoBranch = logparser.getLogsWithBranchInfo(filter:[null])
    def logsBranch0 = logparser.getLogsWithBranchInfo(filter:['branch0'])
    def logsBranch1 = logparser.getLogsWithBranchInfo(filter:['branch1'])
    def logsBranch2 = logparser.getLogsWithBranchInfo(filter:['branch2'])
    def logsBranch21 = logparser.getLogsWithBranchInfo(filter:['branch21'])
    def logsBranch22 = logparser.getLogsWithBranchInfo(filter:['branch22'])
    def logsBranch3 = logparser.getLogsWithBranchInfo(filter:['branch3'])
    def logsInit = logparser.getLogsWithBranchInfo(filter:['init'])
    def logsEmpty = logparser.getLogsWithBranchInfo(filter:['empty'])
    def logsEndl = logparser.getLogsWithBranchInfo(filter:['endl'])
    def logsMain = logparser.getLogsWithBranchInfo(filter:['main'])
    def logsBuild = logparser.getLogsWithBranchInfo(filter:['build'])
    def logsTest = logparser.getLogsWithBranchInfo(filter:['test'])
    def logsOne = logparser.getLogsWithBranchInfo(filter:['one'])
    def logsTwo = logparser.getLogsWithBranchInfo(filter:['two'])
    def logsS2b1 = logparser.getLogsWithBranchInfo(filter:['s2b1'])
    def logsS2b2 = logparser.getLogsWithBranchInfo(filter:['s2b2'])

    // multiple branches
    def logsBranchStar = logparser.getLogsWithBranchInfo(filter:[ 'branch.*' ])
    def logsS2b1S2b2 = logparser.getLogsWithBranchInfo(filter:[ 's2b1', 's2b2' ])
    def logsStar = logparser.getLogsWithBranchInfo(filter:[ '.*' ])
    def logsFullStar = logparser.getLogsWithBranchInfo(filter:[ null, '.*' ])

    // stages
    def logsNoBranchWithStages = logparser.getLogsWithBranchInfo(filter:[null], showStages:true)
    def logsS2b1WithStages = logparser.getLogsWithBranchInfo(filter:[ 's2b1' ], showStages:true)
    def logsS2b2WithStages = logparser.getLogsWithBranchInfo(filter:[ 's2b2' ], showStages:true)
    def logsStage1 = logparser.getLogsWithBranchInfo(filter:[ 'stage1' ], showStages:true)
    def logsStage2 = logparser.getLogsWithBranchInfo(filter:[ 'stage2' ], showStages:true)
    def logsStage3 = logparser.getLogsWithBranchInfo(filter:[ 'stage3' ], showStages:true)

    // other options
    def fullLogVT100 = logparser.getLogsWithBranchInfo([hideVT100:false])
    def fullLogPipeline = logparser.getLogsWithBranchInfo([hidePipeline:false])
    def fullLogPipelineVT100 = logparser.getLogsWithBranchInfo([hidePipeline:false, hideVT100:false])
    def fullLogNoNest = logparser.getLogsWithBranchInfo([markNestedFiltered:false])
    def logsBranch2NoNest = logparser.getLogsWithBranchInfo(filter:['branch2'], markNestedFiltered:false)
    def logsBranch21NoParent = logparser.getLogsWithBranchInfo(filter:['branch21'], showParents:false)
    def logsBranch2NoParent = logparser.getLogsWithBranchInfo(filter:['branch2'], showParents:false)

    // archive the raw buffers for debug
    archiveStringArtifact("dump/fullLog.txt", fullLog)
    archiveStringArtifact("dump/logsNoBranch.txt", logsNoBranch)
    archiveStringArtifact("dump/logsBranch0.txt", logsBranch0)
    archiveStringArtifact("dump/logsBranch1.txt", logsBranch1)
    archiveStringArtifact("dump/logsBranch2.txt", logsBranch2)
    archiveStringArtifact("dump/logsBranch21.txt", logsBranch21)
    archiveStringArtifact("dump/logsBranch22.txt", logsBranch22)
    archiveStringArtifact("dump/logsBranch3.txt", logsBranch3)
    archiveStringArtifact("dump/logsInit.txt", logsInit)
    archiveStringArtifact("dump/logsEmpty.txt", logsEmpty)
    archiveStringArtifact("dump/logsEndl.txt", logsEndl)
    archiveStringArtifact("dump/logsMain.txt", logsMain)
    archiveStringArtifact("dump/logsBuild.txt", logsBuild)
    archiveStringArtifact("dump/logsTest.txt", logsTest)
    archiveStringArtifact("dump/logsOne.txt", logsOne)
    archiveStringArtifact("dump/logsTwo.txt", logsTwo)
    archiveStringArtifact("dump/logsS2b1.txt", logsS2b1)
    archiveStringArtifact("dump/logsS2b2.txt", logsS2b2)

    archiveStringArtifact("dump/logsBranchStar.txt", logsBranchStar)
    archiveStringArtifact("dump/logsS2b1S2b2.txt", logsS2b1S2b2)
    archiveStringArtifact("dump/logsStar.txt", logsStar)
    archiveStringArtifact("dump/logsFullStar.txt", logsFullStar)

    archiveStringArtifact("dump/logsNoBranchWithStages.txt", logsNoBranchWithStages)
    archiveStringArtifact("dump/logsS2b1WithStages.txt", logsS2b1WithStages)
    archiveStringArtifact("dump/logsS2b2WithStages.txt", logsS2b2WithStages)
    archiveStringArtifact("dump/logsStage1.txt", logsStage1)
    archiveStringArtifact("dump/logsStage2.txt", logsStage2)
    archiveStringArtifact("dump/logsStage3.txt", logsStage3)

    archiveStringArtifact("dump/fullLogVT100.txt", fullLogVT100)
    archiveStringArtifact("dump/fullLogPipeline.txt", fullLogPipeline)
    archiveStringArtifact("dump/fullLogPipelineVT100.txt", fullLogPipelineVT100)
    archiveStringArtifact("dump/fullLogNoNest.txt", fullLogNoNest)
    archiveStringArtifact("dump/logsBranch2NoNest.txt", logsBranch2NoNest)
    archiveStringArtifact("dump/logsBranch21NoParent.txt", logsBranch21NoParent)
    archiveStringArtifact("dump/logsBranch2NoParent.txt", logsBranch2NoParent)

    // 3/ detect if timestamp is set for all pipelines
    parallel \
        'notimestamp': {
            echo ''
        },
        'timestamp': {
            timestamps {
                echo ''
            }
        }

    //print '"' + logparser.getLogsWithBranchInfo(filter:['notimestamp']) + '"'
    //print '"' + logparser.getLogsWithBranchInfo(filter:['timestamp']) + '"'
    def notimestampLog = logparser.getLogsWithBranchInfo(filter:['notimestamp'])
    def timestampLog = logparser.getLogsWithBranchInfo(filter:['timestamp'])

    def noGlobalTimestamp = notimestampLog == '[notimestamp] \n'
    def globalTimestamp = notimestampLog ==~ /\[notimestamp\] \[[^\[\]]*\] \n/
    // make sure we detected global timestamp setting correclty (if both are false something is wrong)
    assert noGlobalTimestamp != globalTimestamp, "failed to detect global timestamps setting branch\nnotimestamp log:\n'''${notimestampLog}'''"

    def localTimestamp = timestampLog ==~ /\[timestamp\] \[[^\[\]]*\] \n/
    // make sure local and global timestamp are coherent
    assert noGlobalTimestamp == localTimestamp, "failed to detect global timestamps setting branch\nnotimestamp log:\n'''${notimestampLog}'''\nlocal timestamp log:\n'''${timestampLog}'''"


    // 3.5/ strip logs accordingly
    if (globalTimestamp) {
        fullLog = removeTimestamps(fullLog)
        logsNoBranch = removeTimestamps(logsNoBranch)
        logsBranch0 = removeTimestamps(logsBranch0)
        logsBranch1 = removeTimestamps(logsBranch1)
        logsBranch2 = removeTimestamps(logsBranch2)
        logsBranch21 = removeTimestamps(logsBranch21)
        logsBranch22 = removeTimestamps(logsBranch22)
        logsBranch3 = removeTimestamps(logsBranch3)
        logsInit = removeTimestamps(logsInit)
        logsEmpty = removeTimestamps(logsEmpty)
        logsEndl = removeTimestamps(logsEndl)
        logsMain = removeTimestamps(logsMain)
        logsBuild = removeTimestamps(logsBuild)
        logsTest = removeTimestamps(logsTest)
        logsOne = removeTimestamps(logsOne)
        logsTwo = removeTimestamps(logsTwo)
        logsS2b1 = removeTimestamps(logsS2b1)
        logsS2b2 = removeTimestamps(logsS2b2)

        logsBranchStar = removeTimestamps(logsBranchStar)
        logsS2b1S2b2 = removeTimestamps(logsS2b1S2b2)
        logsStar = removeTimestamps(logsStar)
        logsFullStar = removeTimestamps(logsFullStar)

        logsNoBranchWithStages = removeTimestamps(logsNoBranchWithStages)
        logsS2b1WithStages = removeTimestamps(logsS2b1WithStages)
        logsS2b2WithStages = removeTimestamps(logsS2b2WithStages)
        logsStage1 = removeTimestamps(logsStage1)
        logsStage2 = removeTimestamps(logsStage2)
        logsStage3 = removeTimestamps(logsStage3)

        fullLogVT100 = removeTimestamps(fullLogVT100)
        fullLogPipeline = removeTimestamps(fullLogPipeline)
        fullLogPipelineVT100 = removeTimestamps(fullLogPipelineVT100)
        fullLogNoNest = removeTimestamps(fullLogNoNest)
        logsBranch2NoNest = removeTimestamps(logsBranch2NoNest)
        logsBranch21NoParent = removeTimestamps(logsBranch21NoParent)
        logsBranch2NoParent = removeTimestamps(logsBranch2NoParent)

        // archive the raw buffers for debug
        archiveStringArtifact("dump/removeTimestamps/fullLog.txt", fullLog)
        archiveStringArtifact("dump/removeTimestamps/logsNoBranch.txt", logsNoBranch)
        archiveStringArtifact("dump/removeTimestamps/logsBranch0.txt", logsBranch0)
        archiveStringArtifact("dump/removeTimestamps/logsBranch1.txt", logsBranch1)
        archiveStringArtifact("dump/removeTimestamps/logsBranch2.txt", logsBranch2)
        archiveStringArtifact("dump/removeTimestamps/logsBranch21.txt", logsBranch21)
        archiveStringArtifact("dump/removeTimestamps/logsBranch22.txt", logsBranch22)
        archiveStringArtifact("dump/removeTimestamps/logsBranch3.txt", logsBranch3)
        archiveStringArtifact("dump/removeTimestamps/logsInit.txt", logsInit)
        archiveStringArtifact("dump/removeTimestamps/logsEmpty.txt", logsEmpty)
        archiveStringArtifact("dump/removeTimestamps/logsEndl.txt", logsEndl)
        archiveStringArtifact("dump/removeTimestamps/logsMain.txt", logsMain)
        archiveStringArtifact("dump/removeTimestamps/logsBuild.txt", logsBuild)
        archiveStringArtifact("dump/removeTimestamps/logsTest.txt", logsTest)
        archiveStringArtifact("dump/removeTimestamps/logsOne.txt", logsOne)
        archiveStringArtifact("dump/removeTimestamps/logsTwo.txt", logsTwo)
        archiveStringArtifact("dump/removeTimestamps/logsS2b1.txt", logsS2b1)
        archiveStringArtifact("dump/removeTimestamps/logsS2b2.txt", logsS2b2)

        archiveStringArtifact("dump/removeTimestamps/logsBranchStar.txt", logsBranchStar)
        archiveStringArtifact("dump/removeTimestamps/logsS2b1S2b2.txt", logsS2b1S2b2)
        archiveStringArtifact("dump/removeTimestamps/logsStar.txt", logsStar)
        archiveStringArtifact("dump/removeTimestamps/logsFullStar.txt", logsFullStar)

        archiveStringArtifact("dump/removeTimestamps/logsNoBranchWithStages.txt", logsNoBranchWithStages)
        archiveStringArtifact("dump/removeTimestamps/logsS2b1WithStages.txt", logsS2b1WithStages)
        archiveStringArtifact("dump/removeTimestamps/logsS2b2WithStages.txt", logsS2b2WithStages)
        archiveStringArtifact("dump/removeTimestamps/logsStage1.txt", logsStage1)
        archiveStringArtifact("dump/removeTimestamps/logsStage2.txt", logsStage2)
        archiveStringArtifact("dump/removeTimestamps/logsStage3.txt", logsStage3)

        archiveStringArtifact("dump/removeTimestamps/fullLogVT100.txt", fullLogVT100)
        archiveStringArtifact("dump/removeTimestamps/fullLogPipeline.txt", fullLogPipeline)
        archiveStringArtifact("dump/removeTimestamps/fullLogPipelineVT100.txt", fullLogPipelineVT100)
        archiveStringArtifact("dump/removeTimestamps/fullLogNoNest.txt", fullLogNoNest)
        archiveStringArtifact("dump/removeTimestamps/logsBranch2NoNest.txt", logsBranch2NoNest)
        archiveStringArtifact("dump/removeTimestamps/logsBranch21NoParent.txt", logsBranch21NoParent)
        archiveStringArtifact("dump/removeTimestamps/logsBranch2NoParent.txt", logsBranch2NoParent)
    }

    // then strip node logs
    logsOne = stripNodeLogs(logsOne, 1)
    logsTwo = stripNodeLogs(logsTwo, 1)
    logsStar = stripNodeLogs(logsStar, 2)
    logsFullStar = stripNodeLogs(logsFullStar, 2)

    archiveStringArtifact("dump/stripNodeLogs/logsOne.txt", logsOne)
    archiveStringArtifact("dump/stripNodeLogs/logsTwo.txt", logsTwo)
    archiveStringArtifact("dump/stripNodeLogs/logsStar.txt", logsStar)
    archiveStringArtifact("dump/stripNodeLogs/logsFullStar.txt", logsFullStar)


    // 4/ check log content

    // check each branch
    checkBranchLogs(extractMainLogs(logsNoBranch, begin, end), 'null', expectedLogMap.'null')
    checkBranchLogs(logsBranch0, 'branch0', expectedBranchLogs(expectedLogMap, 'branch0', '[branch0] '))
    checkBranchLogs(logsBranch1, 'branch1',
        expectedBranchLogs(expectedLogMap, 'branch1', '[branch1] ') +
        expectedBranchLogs(expectedLogMap, 'branch2.branch1', '[branch2] [branch1] ')
    )
    checkBranchLogs(logsBranch2, 'branch2', expectedBranchLogs(expectedLogMap, 'branch2', '[branch2] '))
    checkBranchLogs(logsBranch21, 'branch21', expectedBranchLogs(expectedLogMap, 'branch2.branch21', '[branch2] [branch21] '))
    checkBranchLogs(logsBranch22, 'branch22', expectedBranchLogs(expectedLogMap, 'branch2.branch22', '[branch2] [branch22] '))
    checkBranchLogs(logsBranch3, 'branch3', expectedBranchLogs(expectedLogMap, 'branch3', '[branch3] '))
    checkBranchLogs(logsInit, 'init', expectedBranchLogs(expectedLogMap, 'init', '[init] '))
    checkBranchLogs(logsEmpty, 'empty', expectedBranchLogs(expectedLogMap, 'empty', '[empty] '))
    checkBranchLogs(logsEndl, 'endl', expectedBranchLogs(expectedLogMap, 'endl', '[endl] '))
    checkBranchLogs(logsMain, 'main', expectedBranchLogs(expectedLogMap, 'main', '[main] '))
    checkBranchLogs(logsBuild, 'build', expectedBranchLogs(expectedLogMap, 'main.build', '[main] [build] '))
    checkBranchLogs(logsTest, 'test', expectedBranchLogs(expectedLogMap, 'main.test', '[main] [test] '))
    checkBranchLogs(logsOne, 'one', expectedBranchLogs(expectedLogMap, 'one', '[one] '))
    checkBranchLogs(logsTwo, 'two', expectedBranchLogs(expectedLogMap, 'two', '[two] '))
    checkBranchLogs(logsS2b1, 's2b1', expectedBranchLogs(expectedLogMap, 's2b1', '[s2b1] '))
    checkBranchLogs(logsS2b2, 's2b2', expectedBranchLogs(expectedLogMap, 's2b2', '[s2b2] '))

    checkBranchLogs(extractMainLogs(logsNoBranchWithStages, begin, end), 'null', expectedLogMapWithStages.'null')
    checkBranchLogs(logsS2b1WithStages, 's2b1', expectedBranchLogs(expectedLogMapWithStages, 'stage2.s2b1', '[stage2] [s2b1] '))
    checkBranchLogs(logsS2b2WithStages, 's2b2', expectedBranchLogs(expectedLogMapWithStages, 'stage2.s2b2', '[stage2] [s2b2] '))
    checkBranchLogs(logsStage1, 'stage1', expectedBranchLogs(expectedLogMapWithStages, 'stage1', '[stage1] '))
    checkBranchLogs(logsStage2, 'stage2', expectedBranchLogs(expectedLogMapWithStages, 'stage2', '[stage2] '))
    checkBranchLogs(logsStage3, 'stage3', expectedBranchLogs(expectedLogMapWithStages, 'stage2.stage3', '[stage2] [stage3] '))

    // check full logs
    print 'checking fullLog contain the same lines as each branch (different order)'
    unsortedCompare(
        stripNodeLogs(extractMainLogs(fullLog, begin, end), 2),
        removeFilters(
            extractMainLogs(logsNoBranch, begin, end) +
            logsBranch0 +
            logsBranch1 +
            logsBranch2 +
            logsBranch21 +
            logsBranch22 +
            logsBranch3 +
            logsInit +
            logsEmpty +
            logsEndl +
            logsMain +
            logsBuild +
            logsTest +
            logsOne +
            logsTwo +
            logsS2b1 +
            logsS2b2
        )
    )

    // check multiple branches
    print 'checking logsBranchStar contain the same lines as each branch* and main thread (different order)'
    unsortedCompare(
        logsBranchStar,
        removeFilters(
            logsBranch0 +
            logsBranch1 +
            logsBranch2 +
            logsBranch21 +
            logsBranch22 +
            logsBranch3
        )
    )
    print 'checking logsS2b1S2b2 contain the same lines as branches s2b1 and s2b2 (different order)'
    unsortedCompare(
        logsS2b1S2b2,
        removeFilters(
            logsS2b1 +
            logsS2b2
        )
    )
    print 'checking logsStar contain the same lines as each branch (different order)'
    unsortedCompare(
        logsStar,
        removeFilters(
            logsBranch0 +
            logsBranch1 +
            logsBranch2 +
            logsBranch21 +
            logsBranch22 +
            logsBranch3 +
            logsInit +
            logsEmpty +
            logsEndl +
            logsMain +
            logsBuild +
            logsTest +
            logsOne +
            logsTwo +
            logsS2b1 +
            logsS2b2
        )
    )

    checkLogs(extractMainLogs(logsFullStar, begin, end), null, 'logsFullStar', stripNodeLogs(extractMainLogs(fullLog, begin, end), 2), null, 'fullLog')


    // check other options
    assert fullLogVT100 ==~ /(?s).*\x1B\[8m.*?\x1B\[0m.*/
    assert fullLog      !=~ /(?s).*\x1B\[8m.*?\x1B\[0m.*/
    assert fullLogVT100.replaceAll(/\x1B\[8m.*?\x1B\[0m/, '') == fullLog

    assert fullLogPipeline ==~ /(?s).*\[Pipeline\] .*/
    assert fullLog         !=~ /(?s).*\[Pipeline\] .*/
    checkLogs(fullLogPipeline.replaceAll(/(?m)^\[Pipeline\] .*$\n/, ''), null, 'fullLogPipeline without pipeline', fullLog, null, 'fullLog')

    assert fullLogPipelineVT100 ==~ /(?s).*\x1B\[8m.*?\x1B\[0m.*/
    checkLogs(fullLogPipelineVT100.replaceAll(/\x1B\[8m.*?\x1B\[0m/, '').replaceAll(/(?m)^\[Pipeline\] .*$\n/, ''), null, 'fullLogPipelineVT100 without pipeline', fullLog, null, 'fullLog')

    checkLogs(fullLogNoNest, null, 'fullLogNoNest', fullLog, null, 'fullLog')

    assert logsBranch2NoNest !=~ /(?s).*\<nested branch \[.*\]>.*/
    assert logsBranch2       ==~ /(?s).*\<nested branch \[.*\]>.*/
    checkLogs(logsBranch2NoNest, null, 'logsBranch2NoNest', removeFilters(logsBranch2), null, 'removeFilters(logsBranch2)')

    checkBranchLogs(logsBranch21NoParent, 'branch21', expectedBranchLogs(expectedLogMap, 'branch2.branch21', '[branch21] '))

    checkLogs(logsBranch2NoParent, null, 'logsBranch2NoParent', expectedBranchLogs(expectedLogMap, 'branch2', '[branch2] ').replace('<nested branch [branch2] [branch', '<nested branch [branch'), null, 'expected')
}

def printUrls(check) {
    def bou
    def psu
    timestamps {
        print 'before getBlueOceanUrls()'
        bou = logparser.getBlueOceanUrls()
        print 'after getBlueOceanUrls()'

        print 'before getPipelineStepsUrls()'
        psu = logparser.getPipelineStepsUrls()
        print 'after getPipelineStepsUrls()'
    }

    if (check) {
        [ bou, psu ].each {
            assert it.findAll{ it.parent == null }.size() == 1

            // check expected steps
            assert it.findAll{ it.name == 'branch0' }.size() == 1
            assert it.findAll{ it.name == 'branch1' }.size() == 2
            assert it.findAll{ it.name == 'branch2' }.size() == 1
            assert it.findAll{ it.name == 'branch21' }.size() == 1
            assert it.findAll{ it.name == 'branch22' }.size() == 1
            assert it.findAll{ it.name == 'branch3' }.size() == 1
            assert it.findAll{ it.name == 'init' }.size() == 1
            assert it.findAll{ it.name == 'empty' }.size() == 1
            assert it.findAll{ it.name == 'endl' }.size() == 1
            assert it.findAll{ it.name == 'main' }.size() == 1
            assert it.findAll{ it.name == 'build' }.size() == 1
            assert it.findAll{ it.name == 'test' }.size() == 1
            assert it.findAll{ it.name == 'one' }.size() == 1
            assert it.findAll{ it.name == 'two' }.size() == 1
            assert it.findAll{ it.name == 's2b1' }.size() == 1
            assert it.findAll{ it.name == 's2b2' }.size() == 1
            assert it.findAll{ it.name == 'timestamp' }.size() == 1
            assert it.findAll{ it.name == 'notimestamp' }.size() == 1

            // check expected stages
            assert it.findAll{ it.name == 'stage1' }.size() == 1
            assert it.findAll{ it.name == 'stage1' }.findAll{ it.stage }.size() == 1
            assert it.findAll{ it.stage }.findAll{ it.name == 'stage1' }.size() == 1

            assert it.findAll{ it.name == 'stage2' }.size() == 1
            assert it.findAll{ it.name == 'stage2' }.findAll{ it.stage }.size() == 1
            assert it.findAll{ it.stage }.findAll{ it.name == 'stage2' }.size() == 1

            assert it.findAll{ it.name == 'stage3' }.size() == 1
            assert it.findAll{ it.name == 'stage3' }.findAll{ it.stage }.size() == 1
            assert it.findAll{ it.stage }.findAll{ it.name == 'stage3' }.size() == 1
            // 3 stages before logparser tests
            assert it.findAll{ it.stage }.size() == 3 + 3
            assert it.findAll{ it.name != null }.size() == 3 + 3 + 19, it.findAll{ it.name != null }.collect { it.name }

            // check nested steps and stages
            [ [ 'branch21', 'branch2' ], [ 'branch22', 'branch2' ], [ 's2b1', 'stage2' ], [ 's2b2', 'stage2' ], [ 'stage1', null ] ].each{ lit ->
                def st = lit[0]
                def exp = lit[1]
                def parent = null
                def parentName = null
                def found = false
                assert it.findAll{ it.name == st }.size() == 1, "${st} ${exp}"
                it.findAll{ it.name == st }.each{ parent = it.parent }
                while(!found)  {
                    assert it.findAll{ it.id == parent }.size() == 1
                    it.findAll{ it.id == parent }.each{
                        if (it.name != null || it.parent == null) {
                            found = true
                            assert it.name == exp
                        } else {
                            parent = it.parent
                        }
                    }
                }
            }
        }
    }

    def str = ''

    str += '\n*************************\n'
    str += '* Pipelines Steps links *\n'
    str += '*************************\n'
    psu.each {
        def offset = ''
        for(def i = 0; i < it.parents.size(); i++) { offset += '    ' }
        str += "${offset}"
        if (it.stage) { str += "stage " }
        if (it.name) { str += "${it.name}" } else { str += "<step ${it.id}>" }
        str += " id=${it.id} parent=${it.parent} parents=${it.parents} children=${it.children}"
        str += "\n"
        str += "${offset}- url = ${it.url}\n"
        if (it.log) { str += "${offset}- log = ${it.log}\n" }
    }

    str += '\n********************\n'
    str += '* Blue Ocean links *\n'
    str += '********************\n'
    bou.each {
        def offset = ''
        for(def i = 0; i < it.parents.size(); i++) { offset += '    ' }
        str += "${offset}"
        if (it.stage) { str += "stage " }
        if (it.name) { str += "${it.name}" } else { str += "Start of Pipeline" }
        str += " id=${it.id} parent=${it.parent} parents=${it.parents}\n"
        str += "${offset}- url = ${it.url}\n"
        str += "${offset}- log = ${it.log}\n"
    }

    print str
}

def testLogparser() {
    // expected map of logs
    def expectedLogMap = [ 'null': '' ]

    // markers to look only at the relevant part of the logs
    def begin = 'BEGIN_TEST_LOG_BRANCH'
    def end = 'END_TEST_LOG_BRANCH'

    print begin

    runBranches(expectedLogMap)
    runSingleBranches(expectedLogMap)
    runBranchesWithManyLines(100, expectedLogMap)
    // deep copy
    def expectedLogMapWithStages = expectedLogMap.collectEntries{ k,v -> [ "$k".toString(), "$v".toString() ] }
    runStagesAndBranches(expectedLogMap, expectedLogMapWithStages)

    print end

    // add VT100 markers
    node(LABEL_LINUX) {
    }

    parseLogs(expectedLogMap, expectedLogMapWithStages, begin, end)
    printUrls(true)

    if (RUN_FULL_LOGPARSER_TEST || RUN_FULL_LOGPARSER_TEST_WITH_LOG_EDIT) {
        // test with 10 million lines (multiple hours of test, may fail if not enough heap space)
        [ 1, 10, 100, 1000, 10000 ].each {
            stage("test ${it}*1000 lines") {
                runBranchesWithManyLines(it * 1000, null)
                timestamps {
                    print 'before parsing'
                    printUrls(false)
                    if (RUN_FULL_LOGPARSER_TEST_WITH_LOG_EDIT) {
                        logparser.archiveLogsWithBranchInfo("manylines${it * 1000}.txt")
                    }
                    print 'after parsing'
                }
            }
        }
    }
}

// N threads with M groups of P lines all read logs regularly
def testManyThreads(nbthread, nbloop, nbsubloop) {

    torun = [:]
    nbthread.times {
        def id = it
        def threadName = "parallel_${id}".toString()
        torun[threadName] = {
            node('linux') {
                nbloop.times {
                    def str = ''
                    def it1 = it+1
                    sh """#!/bin/bash +x
                    i=\$(( 0 ))
                    while [ \$i -lt ${nbsubloop} ]
                    do
                        echo \"thread ${threadName} / ${nbthread} loop ${it1} / ${nbloop} subloop \$i / ${nbsubloop}\"
                        i=\$(( \$i + 1 ))
                    done
                    """
                }
                logparser.archiveLogsWithBranchInfo("${threadName}.txt", [ filter : [threadName] ])
            }
        }
    }
    stage("test ${nbthread} threads x ${nbloop} x ${nbsubloop} lines") {
        timestamps {
            parallel torun
            logparser.archiveLogsWithBranchInfo("full_testManyThreads.txt")
        }
    }
}

// ===============
// = run tests   =
// ===============

def versionStr = ''
stage('testVersion') {
    versionStr = testVersion()
}
// save version artifact asap (useful to reproduce issues on another test instance)
stage('testJobFilesAccess') {
    testJobFilesAccess(versionStr)
}
// test logparser before there is too many logs
stage('before testLogparser') {}
// cannot be inside a stage
testLogparser()
if (RUN_MANYTHREAD_TIMING_TEST) {
    testManyThreads(50,20,500)
}
stage('after testLogparser') {}
stage('testString') {
    testString()
}
stage('testDate') {
    testDate()
}
stage('testMetaDataAccess') {
    testMetaDataAccess()
}
stage('testLog') {
    testLog()
}
stage('testJobsAndBuilds') {
    testJobsAndBuilds()
}
stage('testNodesAndLabels') {
    testNodesAndLabels()
}
stage('testSemaphore') {
    testSemaphore()
}
