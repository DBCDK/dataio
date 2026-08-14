package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.jobstore.service.cdi.JobstoreDB;
import jakarta.ejb.Singleton;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

/**
 * This enterprise Java bean handles periodic pruning of stale
 * sink_record_delivery_watermark rows, keyed on time since last modification.
 */
@Singleton
public class WatermarkPurgeBean {

    @Inject
    @JobstoreDB
    EntityManager entityManager;

    @Inject
    @ConfigProperty(name = "WATERMARK_RETENTION", defaultValue = "P90D")
    Duration retention;

    private static final Logger LOGGER = LoggerFactory.getLogger(WatermarkPurgeBean.class);

    @Stopwatch
    public int purgeStaleWatermarks() {
        final Timestamp cutoff = Timestamp.from(Instant.now().minus(retention));
        final Query query = entityManager.createQuery(
                "delete from WatermarkEntity w where w.lastModified < :cutoff");
        query.setParameter("cutoff", cutoff);
        final int deleted = query.executeUpdate();
        LOGGER.info("purged {} stale watermark row(s) older than {}", deleted, cutoff);
        return deleted;
    }
}
