// sample pipeline to test https://github.com/gdemengin/pipeline-whitelist/ "Global Pipeline Library" whitelist


// import whitelist library
@Library('pipeline-whitelist@cloudlabel') _

// ===============
// = constants   =
// ===============

// label on the instance with linux hosts
// leave null if all hosts fit the description
LABEL_LINUX='linux'
// label on the instance configured as docker cloud
LABELS_CLOUD=[ 'centos-dockercloud' ]

// =============
// = globals   =
// =============

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
    assert version == 'cloudlabel'

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

            node(label) {
                def nodes = whitelist.getNodes(env.NODE_NAME)
                assert nodes.size() == 1
                def node = nodes[0]
                print "node ${node} type: master=${whitelist.isMaster(node)} dumbSlave=${whitelist.isDumbSlave(node)} cloud=${whitelist.isCloudNode(node)}"
                assert whitelist.isCloudNode(node)
                assert node in whitelist.getNodes(label)
            }
        }
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


// ===============
// = run tests   =
// ===============


def versionStr = ''
stage('testVersion') {
    versionStr = testVersion()
}
// save version asap (useful to reproduce issues on another test instance)
stage('testJobFilesAccess') {
    testJobFilesAccess(versionStr)
}
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
