// ===================================
// = whitelist for Jenkins pipelines =
// ===================================
// a library to expose useful functions (blacklisted by default in jenkins) which should be whitelisted IMHO

import org.codehaus.groovy.runtime.StackTraceUtils
// commons/text not available in older versions of jenkins
//import org.apache.commons.text.StringEscapeUtils
import org.apache.commons.lang3.StringEscapeUtils
import java.text.SimpleDateFormat
import java.util.concurrent.Semaphore
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper

//***************
//* API VERSION *
//***************
// global pipeline libraries can be loaded multiple times
// the first one who loads it wins
// caller can use this to double check the version is the one intended
String version() {
    return '3.0'
}

//*****************************
//* JENKINS & PLUGINS VERSION *
//*****************************

@NonCPS
String instanceVersion() {
    return Jenkins.instance.getVersion()
}

@NonCPS
List<java.util.LinkedHashMap> plugins() {
    return Jenkins.instance.pluginManager.plugins.collect {
        [
            displayName: it.getDisplayName(),
            shortName: it.getShortName(),
            version: it.getVersion()
        ]
    }
}

//******************
//* STRING HELPERS *
//******************

// multiply string by a number
// should not be blacklisted ...
@NonCPS
String multiply(String lhs, Integer rhs) {
    return lhs*rhs
}

// should not be blacklisted ...
@NonCPS
String escapeHtml4(String input) {
    return StringEscapeUtils.escapeHtml4(input)
}

//****************
//* DATE HELPERS *
//****************

// should not be blacklisted ...
@NonCPS
java.util.LinkedHashMap minusDate(Date lhs, Date rhs) {
    def duration = groovy.time.TimeCategory.minus(lhs, rhs)
    return [ days: duration.days, hours: duration.hours, minutes: duration.minutes, seconds: duration.seconds, millis: duration.millis ]
}

// should not be blacklisted ...
@NonCPS
Date parseDate(String format, String date) {
    return new SimpleDateFormat(format).parse(date)
}

//**********************
//* ACCESS TO METADATA *
//**********************
// stacktrace, file, line number, classname, superclass

// get sanitized list of StackTraceElement for exception
@NonCPS
StackTraceElement[] getStackTrace(Throwable e) {
    return StackTraceUtils.sanitize(e).stackTrace
}

// get sanitized list of StackTraceElement for current location
// do not use @NonCPS otherwise the stack is not what we want
StackTraceElement[] getCurrentStackTrace() {
    // remove the first one (the one from current function getCurrentStackTrace)
    return getStackTrace(new Throwable()).drop(1)
}

@NonCPS
String getMethodName(StackTraceElement element) {
    return element.methodName
}

@NonCPS
String getClassName(StackTraceElement element) {
    return element.className
}

@NonCPS
String getFileName(StackTraceElement element) {
    return element.fileName
}

@NonCPS
Integer getLineNumber(StackTraceElement element) {
    return element.lineNumber
}

@NonCPS
Class getSuperclass(Class c) {
    return c.getSuperclass()
}

// access list of suppressed exceptions (needed to access their stackTrace)
@NonCPS
Throwable[] getSuppressed(Throwable e) {
    return e.getSuppressed()
}

@NonCPS
Throwable getCause(Throwable e) {
    return e.getCause()
}

// filter elements not belonging to a user script
// to help track where it came from in user's code
@NonCPS
StackTraceElement[] filterStackTrace(StackTraceElement[] st) {
    // filter only stack lines with filename
    // - WorkflowScript (the actual pipeline script)
    // - or Script1, Script2, etc ... (scripts loaded inside pipeline)
    return st.findAll {
        // keep only the ones from WorkflowScript, Script1.groovy, Script2.groovy, ...
        def filename = getFileName(it)
        // remove the ones with no line number (added for default function arguments)
        def lineNb = getLineNumber(it)
        return lineNb > 0 && (
            filename == 'WorkflowScript' ||
            ( filename != null && filename.find(/^Script[0-9]+.groovy$/) )
        )
    }
}

// if exception does not contain any stackTrace with user's script in it
// append it with a "suppressed" exception with the current stackTrace
// (as suppressed exceptions can be used to help find the root cause of the exception)
// do not use @NonCPS (getCurrentStackTrace does not allow it)
void addTraceableStackTrace(Throwable e) {
    // filter stackTrace of exception, suppressed and cause and check if anything pops up
    // if so we have information about where the exception came from, no need to add more
    if (
        filterStackTrace(getStackTrace(e)).size() == 0 &&
        e.getSuppressed().findAll {
            filterStackTrace(getStackTrace(it)).size() > 0
        }.size() == 0 &&
        (e.getCause() == null || filterStackTrace(getStackTrace(e.getCause())).size() == 0)
    ) {
        def showUserStack = new Exception("Traceable user stackTrace")
        // filter to show only stackTraceElements from user's script leading up to here
        showUserStack.setStackTrace(filterStackTrace(getCurrentStackTrace()))
        e.addSuppressed(showUserStack)
    }
}

//********
//* LOGS *
//********

// get raw log from build (default current)
@NonCPS
String getRawBuildLog(RunWrapper build = currentBuild) {
    return build.rawBuild.log
}

// get raw log from matrix build (map axisId:log)
// shall fail if build is not the runWrapper of a matrix build
@NonCPS
java.util.LinkedHashMap getRawMatrixRunsLog(RunWrapper build) {
    return build.rawBuild.runs.findAll{ it.parentBuild == build.rawBuild }.collectEntries{ [(it.externalizableId): it.log] }
}

//*****************
//* JOBS & BUILDS *
//*****************

@NonCPS
java.util.LinkedHashMap getJobs() {
    def jobs = Jenkins.getInstance().getAllItems().findAll{
            // DO NOT include matrix configurations (children jobs of MatrixProject)
            // they can be retrieved by getMatrixConfiguration and they are not jobs startable independently
            it instanceof hudson.model.Job && ((it instanceof hudson.matrix.MatrixConfiguration) == false)
        }

    return jobs.collectEntries{ [(it.fullName): it] }
}

// to be used in conjonction with getJobs().findAll{ k, v -> v instanceof hudson.matrix.MatrixProject }
@NonCPS
java.util.LinkedHashMap getMatrixConfiguration(hudson.matrix.MatrixProject job) {
    return job.items.collectEntries{ [(it.fullName): it] }
}

@NonCPS
hudson.model.Job getJobByName(String name) {
    return Jenkins.getInstance().getItemByFullName(name)
}

@NonCPS
List<Integer> getRunIds(hudson.model.Job job) {
    return job.getBuilds().collect{ it.id }
}


// get run wrapper from build by name/id
@NonCPS
RunWrapper getRunWrapper(hudson.model.Job job, Integer runId) {
    def rawBuild = job.getBuildByNumber(runId)
    return rawBuild ? new RunWrapper(rawBuild, false) : null
}

@NonCPS
RunWrapper getRunWrapper(hudson.model.Job job, String runId) {
    return getRunWrapper(job, Integer.parseInt(runId))
}

// get run wrapper from last build by name
@NonCPS
RunWrapper getLastRunWrapper(hudson.model.Job job) {
    def rawBuild = job.getLastBuild()
    return rawBuild ? new RunWrapper(rawBuild, false) : null
}

// get run wrapper from last stable build by name
@NonCPS
RunWrapper getLastStableRunWrapper(hudson.model.Job job) {
    def rawBuild = job.getLastStableBuild()
    return rawBuild ? new RunWrapper(rawBuild, false) : null
}

//***************************
//* ACCESS TO JOB/RUN FILES *

// archive text file directly on the master (no need to instantiate a node like ArchiveArtifact)
@NonCPS
void archiveStringArtifact(String name, String buffer) {
    def jobRoot = currentBuild.rawBuild.getRootDir()
    def file = new File("${jobRoot}/archive/${name}")
    if (! file.parentFile.exists()){
        file.parentFile.mkdirs();
    }
    file.write(buffer)
}

// unarchive text artifact
@NonCPS
String unArchiveStringArtifact(String name, RunWrapper build = currentBuild) {
    def jobRoot = build.rawBuild.getRootDir()
    def file = new File("${jobRoot}/archive/${name}")
    if (! file.exists()){
        return null
    }
    return file.text
}

// get job config file
@NonCPS
String getJobConfig(RunWrapper build = currentBuild) {
    return Jenkins.instance.getItemByFullName(build.fullProjectName).configFile.file.text
}

// get pipeline script from run
// shall fail if this is not a workflow run
@NonCPS
java.util.LinkedHashMap getBuildPipelineScripts(RunWrapper build = currentBuild) {

    def jobRoot = build.rawBuild.getRootDir()
    def configFile = new File(jobRoot, 'build.xml')
    assert configFile.exists()

    def rootnode = new XmlSlurper().parse(configFile.path)
    def script = rootnode.execution.script.toString()
    def loadedScripts = [:]
    rootnode.execution.loadedScripts.entry.each{
        loadedScripts["${it.string[0].toString()}".toString()] = it.string[1].toString()
    }

    return [ script: script, loadedScripts: loadedScripts ]
}

//******************
//* STARTUP CAUSES *
// in recent versions this is not needed anymore as RunWrapper now has public method to to the same:
// keep them for compatibility with previous versions

// get list of startup causes
// OBSOLETE: use RunWrapper.getBuildCauses.collect{ it.getShortDescription() }
@NonCPS
List<String> getBuildStartupCauses(RunWrapper build = currentBuild) {
    return build.rawBuild.causes.collect{ it.getShortDescription() }
}

// OBSOLETE: use
//     RunWrapper.getBuildCauses(hudson.triggers.SCMTrigger$SCMTriggerCause) != null ||
//     RunWrapper.getBuildCauses('jenkins.branch.BranchIndexingCause') != null
@NonCPS
Boolean isJobStartedByScm(RunWrapper build = currentBuild) {
    return (build.rawBuild.getCause(hudson.triggers.SCMTrigger$SCMTriggerCause) != null) || (build.rawBuild.getCause(jenkins.branch.BranchIndexingCause) != null)
}

// OBSOLETE: use RunWrapper.getBuildCauses('hudson.model.Cause$UserIdCause') != null
@NonCPS
Boolean isJobStartedManually(RunWrapper build = currentBuild) {
    return build.rawBuild.getCause(hudson.model.Cause$UserIdCause) != null
}

// OBSOLETE: use RunWrapper.getBuildCauses('hudson.triggers.TimerTrigger$TimerTriggerCause') != null
@NonCPS
Boolean isJobStartedByTimer(RunWrapper build = currentBuild) {
    return build.rawBuild.getCause(hudson.triggers.TimerTrigger$TimerTriggerCause) != null
}

//******************
//* NODES & LABELS *
//******************

// expose useful (and public/harmless) properties of node (and computer object below node)
@NonCPS
List<java.util.LinkedHashMap> getNodes(String label = null) {
    def nodes
    if (label == null) {
        nodes = jenkins.model.Jenkins.instance.nodes
    } else {
        nodes = jenkins.model.Jenkins.instance.getLabel(label).nodes
    }

    return nodes.collect {
        def nodeProps=[:]
        nodeProps.class=it.class
        nodeProps.selfLabel=it.selfLabel.toString()
        nodeProps.assignedLabels=it.assignedLabels.collect{ it.toString() }
        nodeProps.computer=[:]

        def computer = null

        // master and other slaves do not handle name and list of computers the same way ... stretch that in the node model
        // TODO handle multiple computers for master ?
        if (this.isMaster(it)) {
            nodeProps.name=it.displayName

            if (it.computers.size() > 0 && it.computers[0] != null) {
                //assert it.computers[0].getClass() == hudson.model.Hudson$MasterComputer, "unexpected class ${it.computers[0].getClass()}"
                // use the first computer only
                computer = it.computers[0]
            }
            // else leave null
        } else {
            // should be hudson.model.Slave
            nodeProps.name=it.name

            computer = it.computer
        }

        if (computer != null) {
            nodeProps.computer.numExecutors=computer.numExecutors
            nodeProps.computer.countBusy=computer.countBusy()
            nodeProps.computer.offline=computer.offline
            nodeProps.computer.temporarilyOffline=computer.temporarilyOffline
            // as string to avoid not serializable exceptions
            nodeProps.computer.offlineCause=computer.offlineCause.toString()
        } else {
            // not sure why this happens sometimes : pretend computer is not there
            nodeProps.computer.numExecutors=0
            nodeProps.computer.countBusy=0
            nodeProps.computer.offline=true
            nodeProps.computer.temporarilyOffline=false
            nodeProps.computer.offlineCause='null computer'
        }

        // make sure the node we just created is recognized as what it should be
        if (this.isMaster(it)) {
            assert this.isMaster(nodeProps)
        } else if (this.isDumbSlave(it)) {
            assert this.isDumbSlave(nodeProps)
        } else {
            assert this.isCloudNode(it)
            assert this.isCloudNode(nodeProps)
        }

        return nodeProps
    }
}

@NonCPS
Boolean isMaster(node) {
    return (node.class == hudson.model.Hudson)
}

@NonCPS
Boolean isDumbSlave(node) {
    return (node.class == hudson.slaves.DumbSlave)
}

@NonCPS
Boolean isCloudNode(node) {
    // assume any node not master of dumbSlave is cloud based
    // asumption might be wrong ...
    return (!isMaster(node)) && (!isDumbSlave(node))
}

// expose list of labels
@NonCPS
List<String> getLabels() {
    return jenkins.model.Jenkins.instance.labels.collect{ it.name }
}

// check if label belongs to a cloud or not
@NonCPS
Boolean isCloudLabel(String label) {
    return jenkins.model.Jenkins.instance.labels.count{ it.name == label && it.clouds.size() > 0 } > 0
}

//*************
//* SEMAPHORE *
//*************

class NotBlockingSemaphore extends Semaphore {
    private timeout

    // keep timeout low to avoid blocking all branches for too long
    // (in a single threade JVM, when a branch is blocked, everyone is blocked)
    // for example one may set it to:
    //   50ms when 10 branches or less might be blocked concurrently seems fair (500 ms max wait to go over all branches)
    //   0 when more than 10 branches might be blocked concurrently
    NotBlockingSemaphore(Integer limit, Integer timeout) {
        super(limit)
        this.timeout = timeout
    }

    void setTimeout(Integer timeout) {
        this.timeout = timeout
    }

    // acquire semaphore with active wait
    // to prevent the single threaded pipeline JVM to block in parrallel branches
    void acquire() {
        while( !tryAcquire(timeout, java.util.concurrent.TimeUnit.MILLISECONDS) ) {
        }
    }
}

// access semaphore
NotBlockingSemaphore semaphore(Integer limit, Integer timeout = 50) {
    return new NotBlockingSemaphore(limit, timeout)
}

void setSemaphoreTimeout(NotBlockingSemaphore sem, Integer timeout) {
    sem.setTimeout(timeout)
}

void acquireSemaphore(NotBlockingSemaphore sem) {
    sem.acquire()
}

void releaseSemaphore(NotBlockingSemaphore sem) {
    sem.release()
}

@NonCPS
def setViewFilter(String name, String regex, Boolean recurse = true, Boolean filterExecutors = true, Boolean filterQueue = true, List<String> jobNames = []) {
    def viewName = name
    def parentItem = Jenkins.instance
    if (name.contains('/')) {
        def folderName = name.split('/').dropRight(1).join('/')
        viewName = name.split('/').takeRight(1)[0]
        parentItem = Jenkins.getInstance().getItemByFullName(folderName)
        assert parentItem != null, "'${folderName}' missing"
    }
    parentItem.views.findAll { it.name == viewName }.each {
        assert it instanceof hudson.model.ListView
        it.setJobNames(jobNames.toSet())
        it.setIncludeRegex(regex)
        it.setRecurse(recurse)
        it.filterExecutors = filterExecutors
        it.filterQueue = filterQueue
        it.save()

        print "jobs in ${name}:"
        it.getAllItems().each{
            print "    ${it.getFullName()}"
        }
    }
}

@NonCPS
def saveJob(String name) {
    Jenkins.instance.getItemByFullName(name).save()
}

@NonCPS
def disableJob(String name, Boolean disable) {
    Jenkins.instance.getItemByFullName(name).disabled = disable
}

@NonCPS
def copyJob(String src, String dstFolder) {
    Jenkins.getInstance().checkPermission(Item.CREATE)

    def srcName = (new File(src)).name

    def dstFolderItem = null
    def target = null
    if (dstFolder != '/') {
        dstFolderItem = Jenkins.getInstance().getItemByFullName(dstFolder)
        assert dstFolderItem != null, "'${dstFolder}' missing"
        target = dstFolder + '/' + srcName
    }
    else {
        dstFolderItem = Jenkins.getInstance()
        target = srcName
    }

    assert Jenkins.getInstance().getItemByFullName(target) == null, "Job '${target}' already exists"

    def srcJob = Jenkins.getInstance().getItemByFullName(src)
    assert srcJob != null, "Job '${src}' missing"

    dstFolderItem.copy(srcJob, srcName)

    def newJob = Jenkins.getInstance().getItemByFullName(target)
    assert null != newJob, "failed to copy '${src}' as '${target}'"
    newJob.save()
}

@NonCPS
def renameJob(String src, String dst) {
    Jenkins.getInstance().checkPermission(Item.CREATE)

    def srcJob = Jenkins.getInstance().getItemByFullName(src)
    assert srcJob != null, "Job '${src}' missing"

    assert (new File(dst)).name == dst, "${dst} cannot include folder name"

    srcJob.renameTo(dst)

    def target = (new File(src)).parent + '/' + dst
    def newJob = Jenkins.getInstance().getItemByFullName(target)
    assert null != newJob, "failed to rename job '${src}' as '${target}'"
    newJob.save()
}

@NonCPS
def moveJob(String src, String dstFolder) {
    Jenkins.getInstance().checkPermission(Item.CREATE)

    def srcName = (new File(src)).name

    def dstFolderItem = null
    def target = null
    if (dstFolder != '/') {
        dstFolderItem = Jenkins.getInstance().getItemByFullName(dstFolder)
        assert dstFolderItem != null, "'${dstFolder}' missing"
        target = dstFolder + '/' + srcName
    }
    else {
        dstFolderItem = Jenkins.getInstance()
        target = srcName
    }

    assert Jenkins.getInstance().getItemByFullName(target) == null, "Job '${target}' already exists"

    def srcJob = Jenkins.getInstance().getItemByFullName(src)
    assert srcJob != null, "Job '${src}' missing"

    hudson.model.Items.move(srcJob, dstFolderItem)

    def newJob = Jenkins.getInstance().getItemByFullName(target)
    assert null != newJob, "failed to move '${src}' as '${target}'"
    newJob.save()
}

return this
