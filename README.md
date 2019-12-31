# pipeline-whitelist
a library to expose useful functions which are blacklisted by default in jenkins, and which should be whitelisted IMHO (in my humble opinion)

## content
- it provides an encapsulation of some functions blacklisted in Jenkins by default to make them available to all pipelines importing the library
- It may use internally functions that we would not want to expose but its objective is to stay harmless by exposing only harmless functionalities

## installation

pipeline-whitelist is meant to be used as a "Global Pipeline Library"
- configured by jenkins administrator ("Manage jenkins > Configure System > Global Pipeline Library")
- cf https://jenkins.io/doc/book/pipeline/shared-libraries/


## usage

- in Jenkinsfile import library like this (identifier 'pipeline-whitelist' is the name of the library set by jenkins administrator in configuration: it may be different on your instance):
* `@Library('pipeline-whitelist@1.0') _`

- then call one of the functions available in whitelist
  * example `whitelist.getNodes()`

## documentation

- see 'whitelist' documentation in $JOB_URL/pipeline-syntax/globals#whitelist (visible only after the library has been imported once)
- or see [whitelist.txt](./vars/whitelist.txt)

## change log

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
