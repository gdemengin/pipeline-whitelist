# pipeline-whitelist
a library to expose useful functions which are blacklisted by default in jenkins, and which should be whitelisted IMHO (in my humble opinion)

## Table of contents
- [Library Content](#content)
- [Documentation](#documentation)
- [Installation](#installation)
- [Change log](#changelog)


## Library Content <a name="content"></a>
- it provides an encapsulation of some functions blacklisted in Jenkins by default to make them available to all pipelines importing the library
  It may use internally functions that we would not want to expose but its objective is to stay harmless by exposing only harmless functionalities

## Documentation <a name="documentation"></a>

### import pipeline-whitelist library
in Jenkinsfile import library like this
```
@Library('pipeline-whitelist@folder') _
```
_identifier "pipeline-whitelist" is the name of the library set by jenkins administrator in instance configuration:_
* _it may be different on your instance_
* _see below [Installation](#installation)_

### use library's functions:
- the name of the package is whitelist:
  ```
  // print jenkins version
  print whitelist.version()
  ```

- see complete documentation here: [whitelist.txt](https://htmlpreview.github.io?https://github.com/gdemengin/pipeline-whitelist/blob/folder/vars/whitelist.txt)
also available in $JOB_URL/pipeline-syntax/globals#whitelist (visible only after the library has been imported once)


## Installation <a name="installation"></a>

pipeline-whitelist is meant to be used as a "Global Pipeline Library"
- configured by jenkins administrator ("Manage jenkins > Configure System > Global Pipeline Library")
- cf https://jenkins.io/doc/book/pipeline/shared-libraries/


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

* folder (09/2020):
  - getJobs() compatible with folder type
