package dk.dbc.dataio.jobstore;

import org.junit.Test;
import org.testcontainers.containers.Container;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Guards the JVM options the service depends on but cannot assert from inside itself.
 */
public class JvmOptionsIT extends AbstractJobStoreServiceContainerTest {

    /**
     * eclipselink.concurrency.manager.waittime must reach the JVM.
     * <p>
     * The bundled EclipseLink waits on cache-key locks with
     * {@code toWaitOn.wait(ConcurrencyUtil.SINGLETON.getAcquireWaitTime())} and defaults that
     * property to 0, which is an indefinite wait. A leaked lock then parks an EJB pool thread
     * forever, orphans a job queue entry in IN_PROGRESS, and stalls partitioning for that sink
     * and submitter until the pod is restarted.
     * <p>
     * The property is set by seeding scripts/jdk_options_file.txt in the service Dockerfile,
     * which works only because the base image's payara-configreader appends to that file rather
     * than truncating it. Nothing fails loudly if that stops being true: the option would simply
     * vanish and the next stall would go unnoticed for days. Hence this test.
     */
    @Test
    public void eclipseLinkCacheKeyWaitIsBounded() throws Exception {
        // Ask the live JVM for its system property table, which is exactly what
        // ConcurrencyUtil reads. Asserting on the property rather than on the container log
        // keeps this independent of how the option happens to be delivered and of the JDK's
        // "Picked up JAVA_TOOL_OPTIONS" message wording.
        final Container.ExecResult result = jobStoreServiceContainer.execInContainer(
                "sh", "-c", "jcmd $(pgrep -f PayaraMicro | head -1) VM.system_properties");

        assertThat("eclipselink.concurrency.manager.waittime as seen by the running JVM",
                result.getStdout(), containsString("eclipselink.concurrency.manager.waittime=5000"));
    }
}
