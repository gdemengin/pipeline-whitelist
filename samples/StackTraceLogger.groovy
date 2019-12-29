// stackTraceLogger : a function wrapper to have neat logs
// with stackTrace (class name, method name, file name and line number)
// with date
// when entering and leaving functions (optional)
// with arguments and return values (optional)
// with exceptions caught
// - highlighting the useful part of the stacktrace : the ones in WorkflowScript and loaded scripts
// - adding extra information (in Throwable::suppressed) when the stack is incomplete (no reference of WorkflowScript)

// TODO: find a way to put stackTraceLogger in an annotation (something like AspectJ annotations for example)


// based on library pipeline-whitelist https://github.com/gdemengin/pipeline-whitelist
// TODO use tag
@Library('pipeline-whitelist@master') _

LogLevel = [:]
LogLevel.INFO = 'INFO'
LogLevel.WARN = 'WARN'
LogLevel.ERROR = 'ERROR'
LogLevel.DEBUG = 'DEBUG'


void log(logLevel, msg, depth = 0) {
    assert logLevel in LogLevel
    print "${logLevel} - ${whitelist.getCurrentStackTrace()[depth + 1].toString()} - ${new Date().format('dd/MM/yyyy HH:mm:ss')}: ${msg.toString()}"
}


// get exception and suppressed + their stacks
// preformated with '\tat '
// optional: add indentation
// optional: filter stack
@NonCPS
String exceptionToString(Throwable e, filterStack = true, indent = 0) {
    def ret = ''

    def suppressed = ''
    whitelist.getSuppressed(e).each {
        ret += 'Also: '
        ret += exceptionToString(it, filterStack, 1)
        ret += '\n'
    }

    def st = whitelist.getStackTrace(e)
    if (filterStack) {
        st = whitelist.filterStackTrace(st)
    }
    ret += "${e}\n${st.collect{ "\tat ${it.toString()}" }.join('\n')}"

    // add indent at the end in case exception contains \n
    ret = ret.split('\n').collect{ whitelist.multiply('\t', indent) + it }.join('\n')
    return ret
}

def stackTraceLogger(Closure c, Object[] args = null, showIO = true, showReturnValue = true) {
    def currentStackTrace = whitelist.getCurrentStackTrace()[1]
    def className = whitelist.getClassName(currentStackTrace)
    def methodName = whitelist.getMethodName(currentStackTrace)
    def name = "${className}.${methodName}"
    if (showIO) {
        if (args != null) {
            log(LogLevel.INFO, ">>>> entering ${name}, args = ${args[0]}", 1)
        } else {
            log(LogLevel.INFO, ">>>> entering ${name}", 1)
        }
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
    } catch(Error e) { // sometimes errors are not caught by catch(e)
        caught = true
        whitelist.addTraceableStackTrace(e)
        log(LogLevel.ERROR, "<<<< leaving ${name} with Error:\n${exceptionToString(e, true, 1)}", 1)
        throw e
    } finally {
        if (done) {
            if (showIO) {
                if (showReturnValue) {
                    log(LogLevel.INFO, "<<<< leaving ${name} with return value: ${ret}", 1)
                } else {
                    log(LogLevel.INFO, "<<<< leaving ${name}", 1)
                }
            }
        } else if (! caught) {
            log(ERROR, "<<<< leaving ${name} with (unexpectidely!) uncaught exception", 1)
        }
    }
    // catching exceptions seems to have bugs: make sure we never reach here without one
    assert done && ! caught
    return ret
}

// simple function, no return value
def titi() {
    return stackTraceLogger({
        print 'titi'
        log(LogLevel.INFO, 'titi')
    })
}

// simple function with return value
def tata() {
    return stackTraceLogger({
        return 0
    })
}

// arguments
def tutu(a) {
    return stackTraceLogger({
        print a
    }, [a])
}

// multiple arguments
def tutu2(a,b) {
    return stackTraceLogger({
        return a+b
    }, [a,b])
}

// map argument
def tutu3(Object...args) {
    return stackTraceLogger({
        return args[0].toto
    }, args)
}

// nested call
def tutu4(Object...args) {
    return stackTraceLogger({
        return tutu3(args)
    }, args)
}

// exception
def toto1() {
    return stackTraceLogger({
        throw new Exception('blabla')
    })
}

// error (without WorkflowScript in the Stacktrace : the exception shall be enriched with a suppressed exception with the stacktrace)
def toto2() {
    return stackTraceLogger({
        assert 1 == 0
    })
}

// nested call with exception: the workarround to add the suppressed exception is done only once)
def toto3() {
    return stackTraceLogger({
        toto2()
    })
}

assert titi() == null
assert tata() == 0
assert tutu(1) == null
assert tutu2(1,3) == 4
assert tutu3(toto:1, titi:2) == 1
assert tutu4(toto:4, titi:2) == 4

def catched1 = false
try {
    toto1()
} catch(e) {
    print e
    assert e.class == Exception
    catched1 = true
}
assert catched1

def catched2 = false
try {
    toto2()
} catch(Error e) {
    print e
    assert e.class == org.codehaus.groovy.runtime.powerassert.PowerAssertionError
    catched2 = true
}
assert catched2


def catched3 = false
try {
    toto3()
} catch(Error e) {
    print e
    assert e.class == org.codehaus.groovy.runtime.powerassert.PowerAssertionError
    catched3 = true
}
assert catched3

