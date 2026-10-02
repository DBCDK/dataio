package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.jobstore.service.AbstractJobStoreIT;
import dk.dbc.dataio.jobstore.service.entity.WatermarkEntity;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class WatermarkPurgeBeanIT extends AbstractJobStoreIT {
    private WatermarkPurgeBean watermarkPurgeBean;

    @org.junit.Before
    public void initializeWatermarkPurgeBean() {
        watermarkPurgeBean = new WatermarkPurgeBean();
        watermarkPurgeBean.entityManager = entityManager;
        watermarkPurgeBean.retention = Duration.ofDays(90);
    }

    @org.junit.Test
    public void purgeStaleWatermarks_removesOnlyRowsOlderThanRetention() {
        final WatermarkEntity staleWatermark = newPersistedWatermarkEntity(
                new WatermarkEntity.Key(1, "870970:12345678"), 100, 0, (short) 0,
                Timestamp.from(Instant.now().minus(Duration.ofDays(91))));

        final WatermarkEntity freshWatermark = newPersistedWatermarkEntity(
                new WatermarkEntity.Key(1, "870970:87654321"), 100, 0, (short) 1,
                Timestamp.from(Instant.now().minus(Duration.ofDays(1))));

        final int deleted = persistenceContext.run(() -> watermarkPurgeBean.purgeStaleWatermarks());

        /* the JPQL bulk delete doesn't detach the still-managed instances from this
           test's long-lived persistence context, so find() below would otherwise
           return a stale object instead of reflecting the DB delete */
        entityManager.clear();

        assertThat("number of rows purged", deleted, is(1));
        assertThat("stale watermark removed",
                entityManager.find(WatermarkEntity.class, staleWatermark.getKey()), is(nullValue()));
        assertThat("fresh watermark kept",
                entityManager.find(WatermarkEntity.class, freshWatermark.getKey()), is(notNullValue()));
    }

    @org.junit.Test
    public void purgeStaleWatermarks_noStaleRows_deletesNothing() {
        newPersistedWatermarkEntity(new WatermarkEntity.Key(1, "870970:12345678"), 100, 0, (short) 0,
                Timestamp.from(Instant.now()));

        final int deleted = persistenceContext.run(() -> watermarkPurgeBean.purgeStaleWatermarks());

        assertThat("number of rows purged", deleted, is(0));
    }

    @org.junit.Test
    public void persist_lastModifiedIsDbManaged_defaultsToNowRegardlessOfEntityState() {
        final WatermarkEntity watermarkEntity = new WatermarkEntity()
                .withKey(new WatermarkEntity.Key(1, "870970:12345678"))
                .withJobId(100)
                .withChunkId(0)
                .withItemId((short) 0);

        persist(watermarkEntity);
        entityManager.refresh(watermarkEntity);

        assertThat("lastModified defaulted on persist", watermarkEntity.getLastModified(), is(notNullValue()));
    }
}
