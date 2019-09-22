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

- see 'whitelist' documentation in $JOB_URL/pipeline-syntax/globals (visible only after the library has been imported once)
- or see [whitelist.html](./vars/whitelist.html)
