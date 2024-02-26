package dk.dbc.dataio.registry;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.Timer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JMXMetricRegistryTest {
    private static final MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();
    private static final JMXMetricRegistry metricRegistry = JMXMetricRegistry.create();

    @AfterEach
    public void clean() {
        metricRegistry.removeAll();
    }

    @Test
    public void testCounter() throws MalformedObjectNameException, ReflectionException, AttributeNotFoundException, InstanceNotFoundException, MBeanException {
        Tag tag = new Tag("fest", "hest");
        metricRegistry.counter("test", tag).inc();
        metricRegistry.counter("test", tag).inc(4);
        Counter counter = metricRegistry.getMetric(new MetricID("test", tag), Counter.class);
        assertEquals(5, counter.getCount(), "The count should be 5");
        Set<ObjectInstance> beans = beanServer.queryMBeans(new ObjectName("dbc:name=test,fest=hest,*"), null);
        assertEquals(1, beans.size(), "There should be one matching MBean");
        ObjectInstance instance = beans.iterator().next();
        Object count = beanServer.getAttribute(instance.getObjectName(), "Count");
        assertEquals(5L, count);
    }

    @Test
    public void testGauge() {
        AtomicInteger test = new AtomicInteger(666);
        Gauge<Integer> gauge = metricRegistry.gauge("test", test::get);
        assertEquals(666, gauge.getValue(), "The value should be 666");
    }

    @Test
    public void testTimer() {
        Timer timer = metricRegistry.timer("test");
        timer.update(Duration.ofSeconds(1));
        timer.update(Duration.ofSeconds(2));
        assertEquals(Duration.ofSeconds(3), timer.getElapsedTime(), "Elapsed time should be 3 seconds");
        assertEquals(2, timer.getCount(), "Count should be 2");
    }
}
