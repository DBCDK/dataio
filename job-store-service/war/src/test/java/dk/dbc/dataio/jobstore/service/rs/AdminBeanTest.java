package dk.dbc.dataio.jobstore.service.rs;

import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity;
import org.junit.Assert;
import org.junit.Test;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public class AdminBeanTest {
    private final static Map<Integer, Sink> SINKS = Map.of(
            1, new Sink(1, 1, newSinkContent("sink1", Duration.ofHours(2))),
            2, new Sink(2, 1, newSinkContent("sink2", Duration.ofHours(1))),
            3, new Sink(3, 1, newSinkContent("sink3", Duration.ofHours(4))),
            4, new Sink(4, 1, newSinkContent("sink4", Duration.ofHours(3)))
    );
    @Test
    public void findMin() {
        Duration min = AdminBean.findMinTimeout(SINKS.values());
        Assert.assertEquals(Duration.ofHours(1), min);
    }

    @Test
    public void getSinkName() {
        DependencyTrackingEntity dte = new DependencyTrackingEntity().setSinkid(2);
        String sinkName = AdminBean.getSinkName(dte, SINKS);
        Assert.assertEquals("sink2", sinkName);
    }

    @Test
    public void isTimeout() {
        Assert.assertFalse(AdminBean.isTimeout(new TestDependencyTrackingEntity(Instant.now()).setSinkid(2), SINKS));
        Assert.assertTrue(AdminBean.isTimeout(new TestDependencyTrackingEntity(Instant.now().minus(Duration.ofHours(2))).setSinkid(2), SINKS));
        Assert.assertFalse(AdminBean.isTimeout(new TestDependencyTrackingEntity(Instant.now().minus(Duration.ofHours(2))).setSinkid(3), SINKS));
    }

    public static SinkContent newSinkContent(String name, Duration timeout) {
        return new SinkContent(name, "queue", "description", SinkContent.SinkType.DUMMY, null, SinkContent.SequenceAnalysisOption.ALL, timeout);
    }

    private static class TestDependencyTrackingEntity extends DependencyTrackingEntity {
        private final Instant lm;

        private TestDependencyTrackingEntity(Instant lm) {
            this.lm = lm;
        }

        @Override
        public Timestamp getLastModified() {
            return new Timestamp(lm.toEpochMilli());
        }
    }
}
