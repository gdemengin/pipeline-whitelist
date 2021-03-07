# pipeline-whitelist
a library to expose useful functions which are blacklisted by default in jenkins, and which should be whitelisted IMHO (in my humble opinion)

Content:
  * whitelist module provides an encapsulation of some functions blacklisted in Jenkins by default to make them available to all pipelines importing the library
    * It may use internally functions that we would not want to expose but its objective is to stay harmless by exposing only harmless functionalities
  * logparser module provides API to parse logs (from currentBuild or from another run or job) and append them with name of current branch/stage
    * as String for those who need to programatically parse logs
    * or as run artifacts for those who need to archive logs with branch names for later use
    * it provides accessors to 'pipeline step' logs
  * it provides accessors to 'Blue Ocean' logs urls for parallel branches and stages
  
Compatibility:
  * tested with 2.73.3, 2.190.1 & 2.263.4

## Table of contents
- [Documentation](#documentation)
- [Installation](#installation)
- [Know Limitations](#limitations)
- [Change log](#changelog)

## Documentation <a name="documentation"></a>

### import pipeline-whitelist library
in Jenkinsfile import library like this
```
@Library('pipeline-whitelist@logparser') _
```
_identifier "pipeline-whitelist" is the name of the library set by jenkins administrator in instance configuration:_
* _it may be different on your instance_
* _see below [Installation](#installation)_

### use library's functions:
- the library contains 2 modules called logparser and whitelist:
  ```
  // print jenkins version
  print whitelist.version()

  // get logs with branch prefix
  def mylog = logparser.getLogsWithBranchInfo()
  ```

### Detailed Documentation

- see online documentation here: [whitelist.txt](https://htmlpreview.github.io?https://github.com/gdemengin/pipeline-whitelist/blob/logparser/vars/whitelist.txt) and [logparser.txt](https://htmlpreview.github.io?https://github.com/gdemengin/pipeline-whitelist/blob/logparser/vars/logparser.txt)
* _also available in $JOB_URL/pipeline-syntax/globals#whitelist & $JOB_URL/pipeline-syntax/globals#logparser_
  * _visible only after the library has been imported once_
  * _requires configuring 'Markup Formater' as 'Safe HTML' in $JENKINS_URL/configureSecurity_


## Installation <a name="installation"></a>

install the library as a "Global Pipeline Library" in "Manage jenkins > Configure System > Global Pipeline Library" (cf https://jenkins.io/doc/book/pipeline/shared-libraries/)

Note:
  * it's also possible to copy the code in a Jenkinsfile and use functions from there
  * but it would imply approving whatever needs to be in "Manage jenkins > In-process Script Approval" (including some unsafe API's)
  * using this library as a "Global Pipeline Library" allows to avoid that (avoid getting access to unsafe API's)


## Known limitations <a name="limitations"></a>

### whitelist limitations:

### logparser limitations:

* calls to `logparser.getLogsWithBranchInfo()` may fail (and cause job to fail) when log is too big (millions of lines, hundreds of MB of logs) because of a lack of heap space

* logs of nested stages (stage inside stage) are not correctly handled in Blue Ocean (Blue Ocean limitation)

## Change log <a name="changelog"></a>

* 1.0 (12/2019) first version with functions to:
  - get version (jenkins and plugins)
  - do various string manipulation (escape HTML, multiply)
  - access (R/O) metadata (stacktrace, superclass, ...)
  - access (R/O) logs
  - access (R/O) jobs and runs and startup causes
  - access (R/O) nodes and labels
  - access (R/W) build artifacts without workspace
  - access (R/O) job config and pipeline scripts
  - implement Semaphore
  - provide samples
    * a function wrapper to print logs with methodName and line number
    * example of use with matrix jobs

* 1.1 (03/2020):
  - expose groovy.time.TimeCategory.minus and SimpleDateFormat.parse in new methods minusDate and parseDate

* 1.2 (08/2020):
  - access (R/O) Throwable.getCause()
  - fix StackTraceLogger sample

* 2.0 (08/2020):
  - getLabels() and isDockerTransientNode were not compatible with kubernetes plugin:
    change API to return a list of String labels + add function to test if label belongs to a cloud + rename isDockerTransientNode() to isCloudNode()
  - new function instanceVersion() get jenkins instance version (formerly version()
  - API change: version() now returns the version of this package (to allow checking which version was loaded in case another part of the pipeline already loaded another version of pipeline-whitelist)

* 2.0.1 (09/2020):
  - fix issue with getJobs() not compatible with folder type

* 3.0 (03/2021):
  - merge with https://github.com/gdemengin/pipeline-logparser (2.0.1)
  - add functions to move/copy/rename/enable jobs from/to folders (moveJob, copyJob, renameJob, saveJob, disableJob) and to edit views (setViewFilter)
