// ===================================
// = whitelist for Jenkins pipelines =
// ===================================
// a library to expose useful functions (blacklisted by default in jenkins) which should be whitelisted IMHO

import org.codehaus.groovy.runtime.StackTraceUtils


//********************************
//* SHOULD NOT BE BLACKLISTED ?? *
//********************************

// multiply string by a number
// TODO should not be blacklisted, report this to jenkins as a bug ?
@NonCPS
// blacklisted signature : staticMethod org.codehaus.groovy.runtime.DefaultGroovyMethods multiply java.lang.String java.lang.Number
String multiply(String lhs, Integer rhs) {
    return lhs*rhs
}


//**********************
//* ACCESS TO METADATA *
//**********************
// stacktrace, file, line number, classname, superclass

// get sanitized list of StackTraceElement for exception
@NonCPS
// blacklisted signature : method java.lang.Throwable getStackTrace
// blacklisted signature : staticMethod org.codehaus.groovy.runtime.StackTraceUtils sanitize java.lang.Throwable
StackTraceElement[] getStackTrace(Throwable e) {
    return StackTraceUtils.sanitize(e).stackTrace
}

// get sanitized list of StackTraceElement for current location
// do not use @NonCPS otherwise the stack is not what we want
// blacklisted signature : new java.lang.Throwable
// blacklisted signature : staticMethod org.codehaus.groovy.runtime.DefaultGroovyMethods drop java.lang.Object[] int
StackTraceElement[] getCurrentStackTrace() {
    // remove the first one (getCurrentStackTrace)
    return getStackTrace(new Throwable()).drop(1)
}

// get method name from StackTraceElement
@NonCPS
// blacklisted signature : method java.lang.StackTraceElement getMethodName
String getMethodName(StackTraceElement element) {
    return element.methodName
}

// get class name from StackTraceElement
@NonCPS
// blacklisted signature : method java.lang.StackTraceElement getClassName
String getClassName(StackTraceElement element) {
    return element.className
}

// get file name from the Nth function/method in the stack (0 = last calling function/method)
@NonCPS
// blacklisted signature : method java.lang.StackTraceElement getFileName
String getFileName(StackTraceElement element) {
    return element.fileName
}

// get line number from the Nth function/method in the stack (0 = last calling function/method)
@NonCPS
// blacklisted signature : method java.lang.StackTraceElement getLineNumber
Integer getLineNumber(StackTraceElement element) {
    return element.lineNumber
}

@NonCPS
// blacklisted signature : method java.lang.Class getSuperclass
Class getSuperclass(Class c) {
    return c.getSuperclass()
}

//********
//* LOGS *
//********

// get raw log from build (default current)
@NonCPS
// blacklisted signature : method org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper getRawBuild
// blacklisted signature : method hudson.model.Run getLog
String getRawBuildLog(org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper build = currentBuild) {
    return build.rawBuild.log
}

// get raw log from matrix build (map axisId:log)
// shall fail if build is not the runWrapper of a matrix build
@NonCPS
// blacklisted signature : method hudson.matrix.MatrixBuild getRuns
// blacklisted signature : method hudson.matrix.MatrixRun getParentBuild
// blacklisted signature : method hudson.model.Run getExternalizableId
// blacklisted signature : method hudson.model.Run getLog
// blacklisted signature : method org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper getRawBuild
java.util.LinkedHashMap getRawMatrixRunsLog(org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper build) {
    // cf https://stackoverflow.com/questions/40016424/remove-null-values-and-empty-maps-from-map-of-maps-to-undefined-depth-in-groov
    // .findAll { k, v -> v } to remove empty entries (otherwise logs from axis from other parents come up)
    return build.rawBuild.runs.collectEntries{ it.parentBuild == build.rawBuild ? [(it.externalizableId): it.log] : [] }.findAll { k, v -> v }
}

//*********************
//* BUILD INFORMATION *
//*********************

// get list of startup causes
@NonCPS
// blacklisted signature : method org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper getRawBuild
// blacklisted signature : method hudson.model.Run getCauses
// blacklisted signature : method hudson.model.Cause getShortDescription
List<String> getBuildStartupCauses() {
    return currentBuild.rawBuild.causes.collect{ it.getShortDescription() }
}

@NonCPS
// blacklisted signature : method org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper getRawBuild
// blacklisted signature : method hudson.model.Run getCause java.lang.Class
Boolean isJobStartedByScm() {
    return (currentBuild.rawBuild.getCause(hudson.triggers.SCMTrigger$SCMTriggerCause) != null) || (currentBuild.rawBuild.getCause(jenkins.branch.BranchIndexingCause) != null)
}

@NonCPS
// blacklisted signature : method org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper getRawBuild
// blacklisted signature : method hudson.model.Run getCause java.lang.Class
Boolean isJobStartedManually() {
    return currentBuild.rawBuild.getCause(hudson.model.Cause$UserIdCause) != null
}

@NonCPS
// blacklisted signature : method org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper getRawBuild
// blacklisted signature : method hudson.model.Run getCause java.lang.Class
Boolean isJobStartedByTimer() {
    return currentBuild.rawBuild.getCause(hudson.triggers.TimerTrigger$TimerTriggerCause) != null
}


//************************
//* INSTANCE INFORMATION *
//************************

// expose useful (and public/harmless) properties of node (and computer object below node)
@NonCPS
// blacklisted signature : staticMethod jenkins.model.Jenkins getInstance
// blacklisted signature : method hudson.model.AbstractCIBase getNodes
// blacklisted signature : method hudson.model.Node getSelfLabel
// blacklisted signature : method hudson.model.Node getAssignedLabels
// blacklisted signature : field hudson.model.Slave name
// blacklisted signature : method hudson.model.Slave getComputer
// blacklisted signature : method hudson.model.Computer getNumExecutors
// blacklisted signature : method hudson.model.Computer countBusy
// blacklisted signature : method hudson.model.Computer isOffline
// blacklisted signature : method hudson.model.Computer isTemporarilyOffline
// blacklisted signature : method hudson.model.Computer getOfflineCause
// blacklisted signature : method jenkins.model.Jenkins getLabel java.lang.String
// blacklisted signature : method hudson.model.Label getNodes
// blacklisted signature : method jenkins.model.Jenkins getComputers
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
            // this is one of the 2 (already checked by isMaster)
            // it.getClass() == hudson.slaves.DumbSlave
            // it.getClass() == io.jenkins.docker.DockerTransientNode
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
            assert this.isDockerTransientNode(it)
            assert this.isDockerTransientNode(nodeProps)
        }

        return nodeProps
    }
}

@NonCPS
Boolean isMaster(node) {
    assert node.class in [hudson.model.Hudson, hudson.slaves.DumbSlave, io.jenkins.docker.DockerTransientNode], "unexpected class ${node.class} for node object"
    return (node.class == hudson.model.Hudson)
}

@NonCPS
Boolean isDumbSlave(node) {
    assert node.class in [hudson.model.Hudson, hudson.slaves.DumbSlave, io.jenkins.docker.DockerTransientNode], "unexpected class ${node.class} for node object"
    return (node.class == hudson.slaves.DumbSlave)
}

@NonCPS
Boolean isDockerTransientNode(node) {
    assert node.class in [hudson.model.Hudson, hudson.slaves.DumbSlave, io.jenkins.docker.DockerTransientNode], "unexpected class ${node.class} for node object"
    return (node.class == io.jenkins.docker.DockerTransientNode)
}

// expose useful (and public/harmless) properties of labels (and cloud objects below)
@NonCPS
// blacklisted signature : staticMethod jenkins.model.Jenkins getInstance
// blacklisted signature : method jenkins.model.Jenkins getLabels
// blacklisted signature : method hudson.model.Label getName
// blacklisted signature : method hudson.model.Label getClouds
// blacklisted signature : method com.nirima.jenkins.plugins.docker.DockerCloud getContainerCap
// blacklisted signature : method com.nirima.jenkins.plugins.docker.DockerCloud getTemplates
// blacklisted signature : method com.nirima.jenkins.plugins.docker.DockerTemplate getLabelString
// blacklisted signature : method com.nirima.jenkins.plugins.docker.DockerTemplate getInstanceCap
// blacklisted signature : method com.nirima.jenkins.plugins.docker.DockerTemplate getImage
List<java.util.LinkedHashMap> getLabels() {
    return jenkins.model.Jenkins.instance.labels.collect{
        def nameVar = it.name
        def labelProps=[:]

        labelProps.name = it.name
        labelProps.clouds = it.clouds.collect{
            def cloudProps = [:]
            cloudProps.containerCap = it.containerCap
            cloudProps.templates = it.templates.findAll{ nameVar == it.labelString }.collect{
                def templateProps = [:]
                templateProps.labelString = it.labelString
                templateProps.instanceCap = it.instanceCap
                templateProps.image = it.image
                return templateProps
            }
            return cloudProps
        }
        return labelProps
    }
}

//*************
//* ARCHIVING *
//*************

// archive buffer directly on the master (no need to instantiate a node like ArchiveArtifact)
@NonCPS
def archiveArtifactBuffer(buffer, name) {
    def jobRoot = currentBuild.rawBuild.getRootDir()
    def file = new File("${jobRoot}/archive/${name}")
    if (! file.parentFile.exists()){
        file.parentFile.mkdirs();
    }
    file.write(buffer)
}

// TODO unarchivebuffer from another job

return this
