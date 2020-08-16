// sample pipeline to test (and demonstrate) matrix functionalities of https://github.com/gdemengin/pipeline-whitelist/ "Global Pipeline Library" whitelist

// requires job-dsl and configuration-as-code plugins to create and update matrix jobs
// some DSL scripts will need to be approved by administrator
//   before this sample can be run without error

// import whitelist library
@Library('pipeline-whitelist@2.0') _

def prepare() {
    // make sure we have the plugins we need to use DSL scripts
    def plugins = whitelist.plugins().collect { it.shortName }
    assert 'job-dsl' in plugins
    assert 'configuration-as-code' in plugins
    assert 'configuration-as-code-groovy' in plugins

    // return value
    def matrixDesc = [:]
    matrixDesc.matrixName = "${env.JOB_NAME}-matrix".replace('/','-')
    // matrix job with 3 axis
    def dslScriptTemplate = """\
        matrixJob('${matrixDesc.matrixName}') {
            axes {
                text('axis', '1', '2', '3')
            }
            combinationFilter('__combination_to_replace__')
            steps {
                shell('__shell_to_replace__')
            }
        }""".stripIndent()

    // skip axis 2
    matrixDesc.dslScript1 = dslScriptTemplate.replace('__combination_to_replace__', 'axis!="2"')
    matrixDesc.dslScript1 = matrixDesc.dslScript1.replace('__shell_to_replace__', 'echo Hello World')

    // failing job and skip axis 1 & 3
    matrixDesc.dslScript2 = dslScriptTemplate.replace('__combination_to_replace__', 'axis=="2"')
    matrixDesc.dslScript2 = matrixDesc.dslScript2.replace('__shell_to_replace__', 'exit 1')

    // no axis
    matrixDesc.dslScript3 = dslScriptTemplate.replace('__combination_to_replace__', 'axis=="4"')
    matrixDesc.dslScript3 = matrixDesc.dslScript3.replace('__shell_to_replace__', 'exit 1')

    // run each DslScript once to make sure they can be run (admin will need to approve them)
    node {
        jobDsl scriptText: matrixDesc.dslScript1
        jobDsl scriptText: matrixDesc.dslScript2
        jobDsl scriptText: matrixDesc.dslScript3
    }

    return matrixDesc
}

def testMatrixLogs(matrixDesc) {
    // create job and run it
    node {
        jobDsl scriptText: matrixDesc.dslScript1
    }
    def matrixBuild = build matrixDesc.matrixName

    // get logs
    def matrixLogs = whitelist.getRawMatrixRunsLog(matrixBuild)
    print matrixLogs.keySet()
    assert matrixLogs.keySet().sort() == [1, 3].collect { "${matrixDesc.matrixName}/axis=${it}#${matrixBuild.id}" }
    matrixLogs.collect{ k,v -> assert v.contains('Hello World') }

    // update job and run it again
    node {
        jobDsl scriptText: matrixDesc.dslScript2
    }
    // job should fail
    matrixBuild = build job: matrixDesc.matrixName, propagate: false
    assert matrixBuild.result == 'FAILURE'

    // get logs
    matrixLogs = whitelist.getRawMatrixRunsLog(matrixBuild)
    print matrixLogs.keySet()
    assert matrixLogs.keySet().sort() == ["${matrixDesc.matrixName}/axis=2#${matrixBuild.id}"]
    matrixLogs.collect{ k,v -> assert v.contains('exit 1') }

    // update job and run it again
    node {
        jobDsl scriptText: matrixDesc.dslScript3
    }
    // job should fail
    matrixBuild = build job: matrixDesc.matrixName

    // get logs
    matrixLogs = whitelist.getRawMatrixRunsLog(matrixBuild)
    print matrixLogs
    assert matrixLogs.size() == 0
}

def testMatrixConfiguration(matrixDesc) {
    def matrixMap = whitelist.getJobs().findAll{ k, v -> v instanceof hudson.matrix.MatrixProject }
    print matrixMap.keySet()
    assert matrixDesc.matrixName.toString() in matrixMap.keySet()

    def job = matrixMap."${matrixDesc.matrixName}"
    def configs = whitelist.getMatrixConfiguration(job)
    print configs.keySet()
    assert configs.keySet().sort() == [1, 2, 3].collect { "${matrixDesc.matrixName}/axis=${it}" }
    def config1 = configs."${matrixDesc.matrixName}/axis=1"
    print whitelist.getRunIds(config1)
    def lastRunConfig1 = whitelist.getLastRunWrapper(config1)
    def lastStableRunConfig1 = whitelist.getLastStableRunWrapper(config1)
    print lastRunConfig1
    print lastStableRunConfig1
    assert lastRunConfig1 != null
    assert lastStableRunConfig1 != null
    print lastRunConfig1.id
    print lastStableRunConfig1.id
    assert lastRunConfig1.id == lastStableRunConfig1.id

    // check access to job config and subjob config possible (subjob config is useless though)
    def matrixXml = whitelist.getJobConfig(whitelist.getLastRunWrapper(job))
    print matrixXml
    def config1Xml = whitelist.getJobConfig(lastRunConfig1)
    print config1Xml
}

def cleanup(matrixDesc) {
    // requires approving dangerous method
    // TODO: find a way to clean with DSL without creating a new job
    //Jenkins.instance.getItemByFullName(matrixDesc.matrixName).delete()
}

def matrixDesc = null
stage('prepare') {
    matrixDesc = prepare()
}
stage('testMatrixLogs') {
    testMatrixLogs(matrixDesc)
}
stage('testMatrixConfiguration') {
    testMatrixConfiguration(matrixDesc)
}
stage('cleanup') {
    cleanup(matrixDesc)
}
