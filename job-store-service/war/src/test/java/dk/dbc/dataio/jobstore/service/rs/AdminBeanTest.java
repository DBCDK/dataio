package dk.dbc.dataio.jobstore.service.rs;

import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public class AdminBeanTest {
    private final static Map<Integer, Sink> SINKS = Map.of(
            1, new Sink(1, 1, newSinkContent("sink1", 2)),
            2, new Sink(2, 1, newSinkContent("sink2", 1)),
            3, new Sink(3, 1, newSinkContent("sink3", 4)),
            4, new Sink(4, 1, newSinkContent("sink4", 3))
    );

    @Test
    public void getSinkName() {
        DependencyTrackingEntity dte = new DependencyTrackingEntity().setSinkid(2);
        String sinkName = new TestAdminBean().getSink(dte.getSinkid()).getContent().getName();
        Assertions.assertEquals("sink2", sinkName);
    }

    @Test
    public void isTimeout() {
        Assertions.assertFalse(new TestAdminBean().isTimeout(new TestDependencyTrackingEntity(Instant.now()).setSinkid(2)));
        Assertions.assertTrue(new TestAdminBean().isTimeout(new TestDependencyTrackingEntity(Instant.now().minus(Duration.ofHours(2))).setSinkid(2)));
        Assertions.assertFalse(new TestAdminBean().isTimeout(new TestDependencyTrackingEntity(Instant.now().minus(Duration.ofHours(2))).setSinkid(3)));
    }

    public static SinkContent newSinkContent(String name, int timeout) {
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

    private static class TestAdminBean extends AdminBean {
        @Override
        Sink getSink(int id) {
            return SINKS.get(id);
        }
    }
}
