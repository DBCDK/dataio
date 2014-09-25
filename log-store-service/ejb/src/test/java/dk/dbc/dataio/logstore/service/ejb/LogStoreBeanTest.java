package dk.dbc.dataio.logstore.service.ejb;

import dk.dbc.dataio.logstore.service.entity.LogEntryEntity;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LogStoreBeanTest {
    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private final String jobId = "jobId";
    private final long chunkId = 42L;
    private final long itemId = 1L;

    @Test
    public void getItemLog_noLogEntriesFound_returnsEmptyString() {
        when(entityManager.createNamedQuery(LogEntryEntity.QUERY_FIND_ITEM_ENTRIES)).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());

        final LogStoreBean logStoreBean = newLogStoreBean();
        final String itemLog = logStoreBean.getItemLog(jobId, chunkId, itemId);
        assertThat(itemLog.isEmpty(), is(true));
    }

    @Test
    public void getItemLog_logEntriesFound_returnsLog() {
        final LogEntryEntity logEntryEntity = new LogEntryEntity();
        logEntryEntity.setTimestamp(new Timestamp(new Date().getTime()));
        when(entityManager.createNamedQuery(LogEntryEntity.QUERY_FIND_ITEM_ENTRIES)).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(logEntryEntity));

        final LogStoreBean logStoreBean = newLogStoreBean();
        final String itemLog = logStoreBean.getItemLog(jobId, chunkId, itemId);
        assertThat(itemLog.isEmpty(), is(false));
    }

    private LogStoreBean newLogStoreBean() {
        final LogStoreBean logStoreBean = new LogStoreBean();
        logStoreBean.entityManager = entityManager;
        return logStoreBean;
    }

}