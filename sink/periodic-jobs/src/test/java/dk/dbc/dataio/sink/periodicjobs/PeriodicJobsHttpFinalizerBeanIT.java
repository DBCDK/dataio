/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.sink.periodicjobs;

import dk.dbc.dataio.commons.conversion.ConversionMetadata;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.harvester.types.HttpPickup;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.io.InputStream;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PeriodicJobsHttpFinalizerBeanIT extends IntegrationTest {
    private static final String FILE_STORE_URL = "http://filestore";
    private static final String FILE_ID = "123456789";

    private final FileStoreServiceConnectorBean fileStoreServiceConnectorBean =
            mock(FileStoreServiceConnectorBean.class);
    private final FileStoreServiceConnector fileStoreServiceConnector =
            mock(FileStoreServiceConnector.class);
    private final JobStoreServiceConnector jobStoreServiceConnector =
            mock(JobStoreServiceConnector.class);
    private final JobStoreServiceConnectorBean jobStoreServiceConnectorBean =
            mock(JobStoreServiceConnectorBean.class);

    @Before
    public void setupMocks() throws FileStoreServiceConnectorException {
        when(fileStoreServiceConnectorBean.getConnector())
                .thenReturn(fileStoreServiceConnector);
        when(fileStoreServiceConnector.addFile(any(InputStream.class)))
                .thenReturn(FILE_ID);
        when(fileStoreServiceConnector.getBaseUrl())
                .thenReturn(FILE_STORE_URL);
        when(fileStoreServiceConnector.searchByMetadata(
                any(ConversionMetadata.class), eq(PeriodicJobsHttpFinalizerBean.ExistingFile.class)))
                .thenReturn(Collections.emptyList());

        when(jobStoreServiceConnectorBean.getConnector())
                .thenReturn(jobStoreServiceConnector);
    }

    @Test
    public void deliver_onNonEmptyJob() throws FileStoreServiceConnectorUnexpectedStatusCodeException {
        final int jobId = 42;
        final PeriodicJobsDataBlock block0 = new PeriodicJobsDataBlock();
        block0.setKey(new PeriodicJobsDataBlock.Key(jobId, 0));
        block0.setSortkey("000000000");
        block0.setBytes(StringUtil.asBytes("0"));
        final PeriodicJobsDataBlock block1 = new PeriodicJobsDataBlock();
        block1.setKey(new PeriodicJobsDataBlock.Key(jobId, 1));
        block1.setSortkey("000000001");
        block1.setBytes(StringUtil.asBytes("1"));
        final PeriodicJobsDataBlock block2 = new PeriodicJobsDataBlock();
        block2.setKey(new PeriodicJobsDataBlock.Key(jobId, 2));
        block2.setSortkey("000000002");
        block2.setBytes(StringUtil.asBytes("2"));

        env().getPersistenceContext().run(() -> {
            env().getEntityManager().persist(block2);
            env().getEntityManager().persist(block1);
            env().getEntityManager().persist(block0);
        });

        final int receivingAgency = 12344321;
        final PeriodicJobsDelivery delivery = new PeriodicJobsDelivery(jobId);
        delivery.setConfig(new PeriodicJobsHarvesterConfig(1, 1,
                new PeriodicJobsHarvesterConfig.Content()
                        .withName("Deliver test")
                        .withSubmitterNumber("111111")
                        .withPickup(new HttpPickup()
                                .withReceivingAgency(String.valueOf(receivingAgency)))));
        final Chunk chunk = new Chunk(jobId, 3, Chunk.Type.PROCESSED);

        final PeriodicJobsHttpFinalizerBean periodicJobsHttpFinalizerBean = newPeriodicJobsHttpFinalizerBean();
        env().getPersistenceContext().run(() ->
                periodicJobsHttpFinalizerBean.deliver(chunk, delivery));

        final InOrder orderVerifier = Mockito.inOrder(fileStoreServiceConnector);
        orderVerifier.verify(fileStoreServiceConnector).appendToFile(FILE_ID, block1.getBytes());
        orderVerifier.verify(fileStoreServiceConnector).appendToFile(FILE_ID, block2.getBytes());

        final ConversionMetadata expectedMetadata = new ConversionMetadata(PeriodicJobsHttpFinalizerBean.ORIGIN)
                .withJobId(delivery.getJobId())
                .withAgencyId(receivingAgency)
                .withFilename("deliver_test." + delivery.getJobId());
        verify(fileStoreServiceConnector).addMetadata(FILE_ID, expectedMetadata);
    }

    @Test
    public void deliver_onEmptyJob() throws FileStoreServiceConnectorUnexpectedStatusCodeException {
        final int jobId = 42;

        final int receivingAgency = 12344321;
        final PeriodicJobsDelivery delivery = new PeriodicJobsDelivery(jobId);
        delivery.setConfig(new PeriodicJobsHarvesterConfig(1, 1,
                new PeriodicJobsHarvesterConfig.Content()
                        .withName("Deliver testÆØÅ")
                        .withSubmitterNumber("111111")
                        .withPickup(new HttpPickup()
                                .withReceivingAgency(String.valueOf(receivingAgency)))));
        final Chunk chunk = new Chunk(jobId, 0, Chunk.Type.PROCESSED);

        final PeriodicJobsHttpFinalizerBean periodicJobsHttpFinalizerBean = newPeriodicJobsHttpFinalizerBean();
        env().getPersistenceContext().run(() ->
                periodicJobsHttpFinalizerBean.deliver(chunk, delivery));

        final ConversionMetadata expectedMetadata = new ConversionMetadata(PeriodicJobsHttpFinalizerBean.ORIGIN)
                .withJobId(delivery.getJobId())
                .withAgencyId(receivingAgency)
                .withFilename("deliver_test." + delivery.getJobId() + ".EMPTY");
        verify(fileStoreServiceConnector).addMetadata(FILE_ID, expectedMetadata);
    }

    private PeriodicJobsHttpFinalizerBean newPeriodicJobsHttpFinalizerBean() {
        final PeriodicJobsHttpFinalizerBean periodicJobsHttpFinalizerBean = new PeriodicJobsHttpFinalizerBean();
        periodicJobsHttpFinalizerBean.entityManager = env().getEntityManager();
        periodicJobsHttpFinalizerBean.fileStoreServiceConnectorBean = fileStoreServiceConnectorBean;
        periodicJobsHttpFinalizerBean.jobStoreServiceConnectorBean = jobStoreServiceConnectorBean;
        return periodicJobsHttpFinalizerBean;
    }
}