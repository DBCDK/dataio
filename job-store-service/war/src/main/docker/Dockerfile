FROM docker-dbc.artifacts.dbccloud.dk/payara6-micro:${docker.payara.version}
LABEL org.opencontainers.image.authors="${team}"

# Bound EclipseLink's cache-key wait.
#
# The bundled provider is Payara's patched EclipseLink (4.0.7.payara-p1v202509010857 at the time
# of writing), whose WriteLockManager.acquireLocksForClone does
#
#     toWaitOn.wait(ConcurrencyUtil.SINGLETON.getAcquireWaitTime());
#
# and defaults that property to 0, which is an indefinite wait.  EclipseLink's own deadlock
# breaker runs once per iteration of the enclosing loop, so with an untimed wait it can never
# run a second time and a leaked cache-key lock parks the thread forever.  Giving the wait a
# period makes the breaker reachable: it aborts with a ConcurrencyException once the thread has
# been stuck for eclipselink.concurrency.manager.maxsleeptime (40s by default), which the
# partitioning retry policy already handles.
#
# This is a limit on how long ONE clone may wait for ONE cache-key lock, not a limit on how long
# an operation may take.  Partitioning a large job is unaffected: each chunk is its own
# transaction with its own clones, and each gets a fresh budget it normally uses none of.
#
# Must be a JVM system property.  ConcurrencyUtil reads it from SystemProperties at class-load
# and nothing ever calls setAcquireWaitTime, so the identically named constant in
# PersistenceUnitProperties is not wired up and setting it in persistence.xml does nothing.
#
# Set via JAVA_TOOL_OPTIONS because the base image offers no hook for an arbitrary JVM option.
# scripts/jdk_options_file.txt looks like the natural place, but common-start-payara.bash
# truncates it with a single '>' before payara-configreader appends to it, so anything seeded
# at image build time is discarded at container start.  The only options that file can carry
# come from payara-configreader's hardcoded profiles, and none of them fit.
# JvmOptionsIT asserts this actually reaches the JVM.
ENV APP_NAME=${app-name} \
    TZ=Europe/Copenhagen \
    JAVA_TOOL_OPTIONS=-Declipselink.concurrency.manager.waittime=5000

RUN printf '%s\n' \
  'set server.thread-pools.thread-pool.http-thread-pool.max-thread-pool-size=200' \
  'set server.thread-pools.thread-pool.http-thread-pool.min-thread-pool-size=50' \
  'set server.thread-pools.thread-pool.thread-pool-1.max-thread-pool-size=200' \
  'set server.thread-pools.thread-pool.thread-pool-1.min-thread-pool-size=50' \
  >> scripts/prebootcommandfile.txt

ADD target/*.war target/docker/*.json target/docker/hz-data.xml deployments/