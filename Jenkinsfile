// sample pipeline to test https://github.com/gdemengin/pipeline-whitelist/ "Global Pipeline Library" whitelist


// import logparser library
@Library('pipeline-whitelist@master') _

// sort list of maps by field
// https://issues.jenkins-ci.org/browse/JENKINS-44924
@NonCPS
def sortByField(list, fieldName) {
    list.sort{ it[fieldName] }
}

def testVersion() {
    print 'testing version'

    def version = whitelist.version()
    print "Jenkins version : ${version}"
    assert Integer.parseInt(version.tokenize('.')[0]) >= 2,
        "jenkins version ${version} is not greater than 2, this is unexpected"

    def plugins = whitelist.plugins()
    sortByField(plugins, 'shortName')
    print "Plugins : \n${plugins.collect{ "\t${it.displayName} (${it.shortName}) v${it.version}" }.join('\n')}"
    assert plugins.find{ it.shortName == 'workflow-job' } != null,
        'workflow-job plugin is not installed, this is unexpected'
}

def testStringManipulation() {
    print 'testing string manipulation'

    assert whitelist.multiply('toto ', 3) == 'toto toto toto ',
        "whitelist.multiply('toto ', 3) gives an unexpected result: '${whitelist.multiply('toto ', 3)}'"

    assert whitelist.escapeHtml4('é">&<') == '&eacute;&quot;&gt;&amp;&lt;',
        "whitelist.escapeHtml4('é\">&<') gives an unexpected result: '${whitelist.escapeHtml4('é\">&<')}'"
}

def testMetaDataAccess() {
    print 'testing metadata accessors'

    // stack, class, method, file and line number

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


    // syntax error
    def excStackTrace = null
    def beforeExcStackTrace = null
    try {
        beforeExcStackTrace = whitelist.getCurrentStackTrace()
        this_is_a_syntax_error
    } catch (e) {
        excStackTrace = whitelist.getStackTrace(e)
    }
    assert excStackTrace != null, 'could not get stack trace for syntax error'
    print "syntax error stacktrace = \n\t${excStackTrace.collect { it.toString() }.join('\n\t')}"
    // the line of syntax error is not the exception itself: look for it in the stack
    def excLine = whitelist.getLineNumber(beforeExcStackTrace[0]) + 1
    expected = "${currentClass}.${currentMethod}(${currentFile}:${excLine})"
    assert excStackTrace.collect{ it.toString() }.contains(expected.toString()),
        "'${expected}' not found in stacktrace"

    // exception
    excStackTrace = null
    beforeExcStackTrace = null
    try {
        beforeExcStackTrace = whitelist.getCurrentStackTrace()
        throw new Exception('this is an exception')
    } catch (e) {
        excStackTrace = whitelist.getStackTrace(e)
    }
    assert excStackTrace != null, 'could not get stack trace for exception'
    print "exception stacktrace = \n\t${excStackTrace.collect { it.toString() }.join('\n\t')}"
    // the line of the exception must be the first one in the stack
    excLine = whitelist.getLineNumber(beforeExcStackTrace[0]) + 1
    expected = "${currentClass}.${currentMethod}(${currentFile}:${excLine})"
    assert excStackTrace[0].toString() == expected.toString(), "'${expected}' not the first item of stacktrace"

    // error
    excStackTrace = null
    beforeExcStackTrace = null
    try {
        beforeExcStackTrace = whitelist.getCurrentStackTrace()
        error 'this is an error'
    } catch (e) {
        excStackTrace = whitelist.getStackTrace(e)
    }
    assert excStackTrace != null, 'could not get stack trace for error'
    print "error stacktrace = \n\t${excStackTrace.collect { it.toString() }.join('\n\t')}"
    // the line of the error must be the first one in the stack
    excLine = whitelist.getLineNumber(beforeExcStackTrace[0]) + 1
    expected = "${currentClass}.${currentMethod}(${currentFile}:${excLine})"
    assert excStackTrace.collect{ it.toString() }.contains(expected.toString()),
        "'${expected}' not found in stacktrace"


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

    // TODO find matrix job on the instance (if any) and try to call getRawMatrixRunsLog
    /*
    def jobResult = build job: 'testmatrix'
    assert whitelist.getRawMatrixRunsLog(jobResult).keySet().sort == ['axis1', 'axis2']
    whitelist.getRawMatrixRunsLog(jobResult).collect{ k,v -> assert v.contains('titi') }
    */
}

def testBuildInformation() {
    def build = whitelist.getRunWrapper(env.JOB_NAME, env.BUILD_NUMBER)
    print "getRunWrapper(${env.JOB_NAME}, ${env.BUILD_NUMBER}) = ${build.getFullDisplayName()}"
    assert build.getFullDisplayName() == currentBuild.getFullDisplayName(), 'could not get currentBuild with getRunWrapper'

    build = whitelist.getRunWrapper(env.JOB_NAME, Integer.parseInt(env.BUILD_NUMBER))
    print "getRunWrapper(${env.JOB_NAME}, ${env.BUILD_NUMBER}) = ${build.getFullDisplayName()}"
    assert build.getFullDisplayName() == currentBuild.getFullDisplayName(), 'could not get currentBuild with getRunWrapper'

    build = whitelist.getLastRunWrapper(env.JOB_NAME)
    print "getLastRunWrapper(${env.JOB_NAME}) = ${build.getFullDisplayName()}"
    assert build.getFullDisplayName() == currentBuild.getFullDisplayName(), 'could not get currentBuild with getLastRunWrapper'

    build = whitelist.getLastStableRunWrapper(env.JOB_NAME)
    print "getLastStableRunWrapper(${env.JOB_NAME}) = ${build.getFullDisplayName()}"

    // no simple idea how to assert the result of this call: for now just try to make the call to make sure it does not fail
    print "getBuildStartupCauses() = ${whitelist.getBuildStartupCauses()}"
    assert [whitelist.isJobStartedByScm(), whitelist.isJobStartedManually(), whitelist.isJobStartedByTimer()].sort() == [false, false, true],
        'something unexpected happened: the build was not started by scm, timer or human'
}

def testNodeInformation() {
    // TODO

    print whitelist.getNodes()
    assert whitelist.getNodes().size() != 0
    assert whitelist.getNodes('wrongLabelDoesNotExist').size() == 0
    assert whitelist.getNodes('master').size() == 1
    assert whitelist.getLabels().size() != 0

    // make sure we can call getNodes (no exception) on all labels
    for (label in whitelist.getLabels().collect{ it.name }) {
        print whitelist.getNodes(label)
    }

    // TODO test isMaster, ...
}

testVersion()
testStringManipulation()
testMetaDataAccess()
testLog()
testBuildInformation()
testNodeInformation()
