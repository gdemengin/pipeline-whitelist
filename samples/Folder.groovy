// sample pipeline to test (and demonstrate) folders and views functionalities of https://github.com/gdemengin/pipeline-whitelist

// requires a test job, an empty folder and a view (list view)
// the folder and view will be edited

// import whitelist library
@Library('pipeline-whitelist@logparser') _

// freestyle job (will be copied)
TEST_JOB='testFolder/testNestedFolder/testFreeStyle'
TEST_JOB_NAME='testFreeStyle'
// empty folder (will be edited)
TEST_FOLDER='testFolder/testNestedFolder2'
TEST_FOLDER_NAME='testNestedFolder2'
// test view (will be edited)
TEST_VIEW='testFolder/testView'

assert whitelist.getJobs().findAll { k, v -> k.startsWith("${TEST_FOLDER}/") }.size() == 0

whitelist.setViewFilter(TEST_VIEW, "[^/]*|${TEST_FOLDER_NAME}/.*", true, true, true)
whitelist.copyJob(TEST_JOB, TEST_FOLDER)
whitelist.renameJob("${TEST_FOLDER}/${TEST_JOB_NAME}", 'jobrename')
whitelist.copyJob(TEST_JOB, TEST_FOLDER)
whitelist.renameJob("${TEST_FOLDER}/${TEST_JOB_NAME}", 'jobmove')
whitelist.moveJob("${TEST_FOLDER}/jobmove", '/')
whitelist.moveJob('jobmove', TEST_FOLDER)
whitelist.copyJob(TEST_JOB, TEST_FOLDER)
whitelist.renameJob("${TEST_FOLDER}/${TEST_JOB_NAME}", 'jobdisable')
whitelist.disableJob("${TEST_FOLDER}/jobdisable", true)
whitelist.saveJob("${TEST_FOLDER}/jobdisable")
whitelist.copyJob(TEST_JOB, TEST_FOLDER)
whitelist.renameJob("${TEST_FOLDER}/${TEST_JOB_NAME}", 'jobenable')
whitelist.disableJob("${TEST_FOLDER}/jobenable", true)
whitelist.disableJob("${TEST_FOLDER}/jobenable", false)
whitelist.saveJob("${TEST_FOLDER}/jobenable")
