// stackTraceLogger : a function wrapper to add useful logs
// based on https://github.com/gdemengin/pipeline-whitelist/ "Global Pipeline Library" whitelist

// it shows stackTrace (class name, method name, file name and line number) & date
// when entering and leaving functions (optional)
// with arguments and return values (optional)
// with exceptions caught
// - highlighting the useful part of the stacktrace : the ones in WorkflowScript and loaded scripts
// - adding extra information (in Throwable::suppressed) when the stack is incomplete (no reference of WorkflowScript)

// TODO: find a way to put stackTraceLogger in an annotation (something like AspectJ annotations for example)

// import whitelist library
@Library('pipeline-whitelist@cause') _

// max log size: 10KB
@groovy.transform.Field
MAX_LOG_SIZE = 10*1024

// set to false if security-plugin version is less than 1.44
@groovy.transform.Field
ALLOW_NEW_EXCEPTION = null
whitelist.plugins().findAll{ it.shortName == 'script-security' }.each {
    def SSversionTokens = it.version.split(/\./)
    assert SSversionTokens.size() >= 2 && SSversionTokens[0] ==~ /\d+/ && SSversionTokens[1] ==~ /\d+/
    def SSmajor = SSversionTokens[0].toInteger()
    def SSminor = SSversionTokens[1].toInteger()

    assert ALLOW_NEW_EXCEPTION == null
    ALLOW_NEW_EXCEPTION = ((SSmajor > 1) || (SSmajor == 1 && SSminor >= 44))
}
assert ALLOW_NEW_EXCEPTION != null

enum LogLevel{
    NONE,
    DEBUG,
    INFO,
    WARN,
    ERROR;

    // https://issues.jenkins-ci.org/browse/JENKINS-33023
    public LogLevel() {}
}

@groovy.transform.Field
static LogLevel defaultLogLevel = LogLevel.INFO

// print logs if logLevel matches DefaultLogLevel
// depth: number of elements to skip in the stack to retrieve the stackTrace element to print
void log(LogLevel loglevel, msg, Integer depth = 0) {
    if (
        loglevel != LogLevel.NONE &&
        defaultLogLevel <= loglevel
    ) {
        def st = whitelist.getCurrentStackTrace()
        // remove technical stacks without line number
        // (remove elements with line -1 when there is a default function argument)
        st = st.findAll{ whitelist.getLineNumber(it) > 0 }

        def logMsg = loglevel.name()
        logMsg += ' - '
        if (st.size() > depth + 1) {
            logMsg += st[depth + 1].toString()
        } else {
            logMsg += '???'
        }
        logMsg += ' - '
        logMsg += new Date().format('dd/MM/yyyy HH:mm:ss').toString()
        logMsg += ': '
        logMsg += msg.toString()

        if (logMsg.size() > MAX_LOG_SIZE) {
            log(LogLevel.WARN, "next logline shall be truncated (actual size: ${logMsg.size()} ; max size: ${MAX_LOG_SIZE})", depth + 1)
            logMsg = logMsg.substring(0, MAX_LOG_SIZE) + "(...) <truncated to ${MAX_LOG_SIZE})>"
        }

        print logMsg
    }
}


// get exception and suppressed + their stacks
// preformated with '\tat '
// optional: add indentation
// optional: filter stack
@NonCPS
String exceptionToString(Throwable e, filterStack = true, indent = 0) {
    def st = whitelist.getStackTrace(e)
    if (filterStack) {
        st = whitelist.filterStackTrace(st)
    }
    def ret = "${e}\n${ st.collect{ "\tat ${it.toString()}" }.join('\n') }"

    def cause = whitelist.getCause(e)
    if (cause != null) {
        ret += 'Caused by: '
        ret += exceptionToString(cause, filterStack, indent + 1)
        ret += '\n'
    }

    whitelist.getSuppressed(e).each {
        ret += 'Suppressed: '
        ret += exceptionToString(it, filterStack, indent + 1)
        ret += '\n'
    }

    // add indent at the end in case exception contains \n
    ret = ret.split('\n').collect{ whitelist.multiply('\t', indent) + it }.join('\n')
    return ret
}

// execute closure C (no arguments) and log StackTrace before and after:
// before: log function name + arguments
// after: log function name + return value or exception
//        if exception is missing the root cause, consolidate it using whitelist.addTraceableStackTrace
// parameter args can be left to null if args should not be printed (or if no arguments)
// parameter levels can be used to change the logLevels (default [ beginEnd: LogLevel.INFO, retVal: LogLevel.DEBUG, args: LogLevel.DEBUG ])
// to avoid printing logs even in DEBUG mode use LogLevel.NONE (example: level=[ retVal: LogLevel.NONE ] )
def stackTraceLogger(
    Object args = null,
    LinkedHashMap levels = [:],
    Closure c
) {
    // set defaut level values
    def defaultLevels = [ beginEnd: LogLevel.INFO, retVal: LogLevel.DEBUG, args: LogLevel.DEBUG ]
    assert levels != null
    defaultLevels.putAll(levels)
    levels = defaultLevels

    assert levels.keySet().sort() == [ 'args', 'beginEnd', 'retVal' ]
    // assert levels.each{ k,v -> v instanceof LogLevel }
    assert levels.each{ k,v -> v in [ LogLevel.NONE, LogLevel.DEBUG, LogLevel.INFO, LogLevel.WARN, LogLevel.ERROR ]  }

    def currentStackTrace = whitelist.getCurrentStackTrace().findAll{ whitelist.getLineNumber(it) > 0 }[1]

    def className = whitelist.getClassName(currentStackTrace)
    def methodName = whitelist.getMethodName(currentStackTrace)
    def name = "${className}.${methodName}"

    log(levels.beginEnd, ">>>> entering ${name}", 1)
    if (args != null) {
        log(levels.args, "#### input args: ${args}", 1)
    }

    def ret
    def done = false
    def caught = false

    try {
        ret = c.call()
        done = true
    } catch(e) {
        caught = true
        whitelist.addTraceableStackTrace(e)
        log(LogLevel.ERROR, "<<<< leaving ${name} with Exception:\n${exceptionToString(e, true, 1)}", 1)
        throw e
    } catch(Exception e) { // sometimes exceptions are not caught by catch(e)
        caught = true
        whitelist.addTraceableStackTrace(e)
        log(LogLevel.ERROR, "<<<< leaving ${name} with Exception:\n${exceptionToString(e, true, 1)}", 1)
        throw e
    } catch(Error e) { // errors are not caught by catch(e)
        caught = true
        whitelist.addTraceableStackTrace(e)
        log(LogLevel.ERROR, "<<<< leaving ${name} with Error:\n${exceptionToString(e, true, 1)}", 1)
        throw e
    } finally {
        if (done) {
            log(levels.beginEnd, "<<<< leaving ${name}", 1)
            log(levels.retVal,   "**** return value: ${ret}", 1)
        } else if (! caught) {
            // exception catch seems to have bugs: add this log just in case
            // if no exception is raised after the finally, assert below shall raise an exception
            log(ERROR, "<<<< leaving ${name} with (unexpectedly!) uncaught exception", 1)
        }
    }
    // catching exceptions seems to have bugs: make sure we never reach here if done is false
    // it should never ever happen ... so the assert is harmless anyway ...
    assert done
    return ret
}


/*********/
/* TESTS */
/*********/


// simple function, no return value
def testNoReturn() {
    return stackTraceLogger() {
        def line = whitelist.getLineNumber(whitelist.getCurrentStackTrace()[0])
        print 'in testNoReturn'
        log(LogLevel.INFO, "in testNoReturn line ${line + 2}")
    }
}

// simple function with return value
def testReturn() {
    return stackTraceLogger() {
        return 0
    }
}

// arguments
def testArg(a) {
    return stackTraceLogger(a) {
        print "in testArg : ${a}"
    }
}

// multiple arguments
def testMultipleArgs(a,b) {
    return stackTraceLogger([a,b]) {
        return a+b
    }
}

// map argument
def testMapArg(Object...args) {
    return stackTraceLogger(args) {
        return args[0].toto
    }
}

// nested call
def testNested(Object...args) {
    return stackTraceLogger(args) {
        return testMapArg(args)
    }
}

// exception
def testException() {
    return stackTraceLogger() {
        throw new Exception('blabla')
    }
}

// error (without WorkflowScript in the Stacktrace : the exception shall be enriched with a suppressed exception with the stacktrace)
def testError() {
    return stackTraceLogger() {
        assert 1 == 0
    }
}

// nested call with exception: the workarround to add the suppressed exception is done only once)
def testNestedError() {
    return stackTraceLogger() {
        testError()
    }
}

// change levels
def testLevels(a,b) {
    def levels = [ beginEnd: LogLevel.DEBUG, retVal: LogLevel.WARN, args: LogLevel.INFO ]
    print levels
    return stackTraceLogger([a,b], levels) {
        return a+b
    }
}

def testLevels2(a,b) {
    def levels = [ beginEnd: LogLevel.WARN, retVal: LogLevel.ERROR ]
    print levels
    return stackTraceLogger([a,b], levels) {
        return a+b
    }
}

def testLevelsNoReturnValue(a,b) {
    def levels = [ retVal: LogLevel.NONE ]
    print levels
    return stackTraceLogger([a,b], levels) {
        return a+b
    }
}



print '************* TEST DEFAULT LOG LEVEL & SIZE *************'

def line = whitelist.getLineNumber(whitelist.getCurrentStackTrace()[0])
log(LogLevel.INFO, "test log in default (INFO) mode: debug log should not appear line=${line + 1}")
log(LogLevel.DEBUG, 'test debug log not visible')
defaultLogLevel = LogLevel.DEBUG
log(LogLevel.INFO, "test log in DEBUG mode : debug log should appear line=${line + 4}")
log(LogLevel.DEBUG, 'test debug log visible')
defaultLogLevel = LogLevel.INFO
log(LogLevel.INFO, "test log back in INFO mode : debug log should not appear line=${line + 7}")
log(LogLevel.DEBUG, 'test debug log not visible')
log(LogLevel.INFO, 'test_max_log_size_' + whitelist.multiply('*', MAX_LOG_SIZE * 2))

print '************* TEST stackTraceLogger in INFO mode *************'

assert testNoReturn() == null
assert testReturn() == 0
assert testArg(1) == null
assert testMultipleArgs(1,3) == 4
assert testMapArg(toto:1, titi:2) == 1
assert testNested(toto:4, titi:2) == 4

print '************* TEST stackTraceLogger with Exceptions/Errors *************'

if (ALLOW_NEW_EXCEPTION) {
    def catched1 = false
    try {
        testException()
    } catch(e) {
        print e
        assert e.class == Exception
        catched1 = true
    }
    assert catched1
}

def catched2 = false
try {
    testError()
} catch(Error e) {
    print e
    assert e.class == org.codehaus.groovy.runtime.powerassert.PowerAssertionError
    catched2 = true
}
assert catched2


def catched3 = false
try {
    testNestedError()
} catch(Error e) {
    print e
    assert e.class == org.codehaus.groovy.runtime.powerassert.PowerAssertionError
    catched3 = true
}
assert catched3

print '************* TEST stackTraceLogger in DEBUG mode *************'
defaultLogLevel = LogLevel.DEBUG

assert testNoReturn() == null
assert testReturn() == 0
assert testArg(1) == null
assert testMultipleArgs(1,3) == 4
assert testMapArg(toto:1, titi:2) == 1
assert testNested(toto:4, titi:2) == 4


print '************* TEST stackTraceLogger levels *************'
defaultLogLevel = LogLevel.INFO

assert testLevels(1,1) == 2
assert testLevels2(1,1) == 2

defaultLogLevel = LogLevel.DEBUG
assert testLevelsNoReturnValue(1,1) == 2

