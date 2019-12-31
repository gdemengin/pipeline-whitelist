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
@Library('pipeline-whitelist@2.0') _
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

- see complete documentation here: [whitelist.txt](https://htmlpreview.github.io?https://github.com/gdemengin/pipeline-whitelist/blob/master/vars/whitelist.txt)
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

* 2.0 (in progress):
  - expose groovy.time.TimeCategory.minus and SimpleDateFormat.parse in new methods minusDate and parseDate
