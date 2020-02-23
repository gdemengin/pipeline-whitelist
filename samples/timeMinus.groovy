@Library('pipeline-whitelist@experiment') _

def format(interval) {
    def dict = [days: 'd', hours: 'h', minutes: 'm', seconds: 's', millis: 'ms']
    def str = interval.collect { k, v -> k != 'millis' && v > 0 ? "${v}${dict."${k}"} " : '' }.sum()
    str = str ?: "${interval.millis}${dict.millis}"
    return str
}

def t1 = whitelist.parseDate("dd/MM/yyyy HH:mm:ss", '01/01/2020 12:00:24');
def t2 = whitelist.parseDate("dd/MM/yyyy HH:mm:ss", '01/01/2041 00:00:00');

def interval = whitelist.minusDate(t2, t1)
print '21 y 11h 59m and 36s: 21*365 = 7665 + 5 years of 366 days = 7670 days'
print format(interval)

t1 = whitelist.parseDate("dd/MM/yyyy HH:mm:ss", '01/01/2020 12:00:24');
t2 = whitelist.parseDate("dd/MM/yyyy HH:mm:ss", '01/01/2020 12:02:00');

interval = whitelist.minusDate(t2, t1)
print '1 minutes 36s'
print format(interval)

interval = whitelist.minusDate(t1, t1)
print '0'
print format(interval)

t1 = new Date()
// print shall actually wait a few ms
print 'a few ms'
interval = whitelist.minusDate(new Date(), t1)
print format(interval)
