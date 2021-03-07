// sample pipeline to demonstrate semaphore functionalities of https://github.com/gdemengin/pipeline-whitelist

// import whitelist library with semaphore
@Library('pipeline-whitelist@logparser') _


def thread (name, sem, time) {
    whitelist.acquireSemaphore(sem)
    try {
        print "starting ${name}"
        sleep(time)
        print "stopping ${name}"
    }
    finally {
        whitelist.releaseSemaphore(sem)
    }
}

echo '''\
************************
* TEST 1 : few threads *
************************
'''

timestamps {
    def s = whitelist.semaphore(2)
    echo 'check thread names and timestamps below: no more than 2 threads shall run at the same time'
    def threads = [ failFast: false ]
    threads."thread1" = { thread("thread1", s, 4) }
    threads."thread2" = { thread("thread2", s, 5) }
    threads."thread3" = { thread("thread3", s, 7) }
    parallel threads
}


echo '''\
*************************
* TEST 2 : more threads *
*************************
'''

timestamps {
    // starting with 10 threads you may reduce the timeout to 0 safely
    // (  otherwise you can notice delay when switching from one branch to the other
    //    delay = number of threads waiting for semaphore * timeout
    //    since each thread blocked is actually sleeping and blocking the single threaded VM  )
    def s = whitelist.semaphore(2, 0)
    echo 'check thread names and timestamps below: no more than 2 threads shall run at the same time'
    def threads = [ failFast: false ]
    10.times{
        threads."thread${it}" = {
            thread("thread${it}", s, it)
        }
    }
    parallel threads
}
