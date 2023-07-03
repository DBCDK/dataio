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
import dk.dbc.weekresolver.WeekResolverConnector;
import dk.dbc.weekresolver.WeekResolverConnectorException;
import dk.dbc.weekresolver.WeekResolverResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PeriodicJobsHttpFinalizerBeanIT extends IntegrationTest {
    private static final String FILE_STORE_URL = "http://filestore";
    private static final String FILE_ID = "123456789";

    private final FileStoreServiceConnector fileStoreServiceConnector =
            mock(FileStoreServiceConnector.class);
    private final JobStoreServiceConnector jobStoreServiceConnector =
            mock(JobStoreServiceConnector.class);
    private final WeekResolverConnector weekResolverConnector =
            mock(WeekResolverConnector.class);

    @Before
    public void setupMocks() throws FileStoreServiceConnectorException {
        when(fileStoreServiceConnector.addFile(any(InputStream.class)))
                .thenReturn(FILE_ID);
        when(fileStoreServiceConnector.getBaseUrl())
                .thenReturn(FILE_STORE_URL);
        when(fileStoreServiceConnector.searchByMetadata(
                any(ConversionMetadata.class), eq(PeriodicJobsHttpFinalizerBean.ExistingFile.class)))
                .thenReturn(Collections.emptyList());
    }

    @Test
    public void deliver_onNonEmptyJob() throws FileStoreServiceConnectorUnexpectedStatusCodeException {
        final int jobId = 42;
        final PeriodicJobsDataBlock block0 = new PeriodicJobsDataBlock();
        block0.setKey(new PeriodicJobsDataBlock.Key(jobId, 0, 0));
        block0.setSortkey("000000000");
        block0.setBytes(StringUtil.asBytes("0\n"));
        block0.setGroupHeader(StringUtil.asBytes("groupA\n"));
        final PeriodicJobsDataBlock block1 = new PeriodicJobsDataBlock();
        block1.setKey(new PeriodicJobsDataBlock.Key(jobId, 1, 0));
        block1.setSortkey("000000001");
        block1.setBytes(StringUtil.asBytes("1\n"));
        final PeriodicJobsDataBlock block2 = new PeriodicJobsDataBlock();
        block2.setKey(new PeriodicJobsDataBlock.Key(jobId, 2, 0));
        block2.setSortkey("000000002");
        block2.setBytes(StringUtil.asBytes("2\n"));
        block2.setGroupHeader(StringUtil.asBytes("groupB\n"));

        env().getPersistenceContext().run(() -> {
            env().getEntityManager().persist(block2);
            env().getEntityManager().persist(block1);
            env().getEntityManager().persist(block0);
        });

        final int receivingAgency = 12344321;
        final PeriodicJobsDelivery delivery = new PeriodicJobsDelivery(jobId);
        delivery.setConfig(new PeriodicJobsHarvesterConfig(1, 1,
                new PeriodicJobsHarvesterConfig.Content()
                        .withTimeOfLastHarvest(new Date())
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
        orderVerifier.verify(fileStoreServiceConnector).appendToFile(FILE_ID, StringUtil.asBytes("groupB\n2\n"));

        final ConversionMetadata expectedMetadata = new ConversionMetadata(PeriodicJobsHttpFinalizerBean.ORIGIN)
                .withJobId(delivery.getJobId())
                .withAgencyId(receivingAgency)
                .withFilename("deliver_test." + delivery.getJobId());
        verify(fileStoreServiceConnector).addMetadata(FILE_ID, expectedMetadata);
    }

    @Test
    public void deliver_autoprintJob() throws FileStoreServiceConnectorUnexpectedStatusCodeException {
        final int jobId = 42;
        final PeriodicJobsDataBlock block0 = new PeriodicJobsDataBlock();
        block0.setKey(new PeriodicJobsDataBlock.Key(jobId, 0, 0));
        block0.setSortkey("0");
        block0.setBytes(StringUtil.asBytes("0\n"));

        env().getPersistenceContext().run(() -> env().getEntityManager().persist(block0));

        final int receivingAgency = 123456;
        final PeriodicJobsDelivery delivery = new PeriodicJobsDelivery(jobId);
        delivery.setConfig(new PeriodicJobsHarvesterConfig(1, 1,
                new PeriodicJobsHarvesterConfig.Content()
                        .withTimeOfLastHarvest(new Date())
                        .withName("Deliver test")
                        .withSubmitterNumber("111111")
                        .withPickup(new HttpPickup()
                                .withReceivingAgency(String.valueOf(receivingAgency))
                                .withOverrideFilename("autoprint"))));
        final Chunk chunk = new Chunk(jobId, 3, Chunk.Type.PROCESSED);

        final PeriodicJobsHttpFinalizerBean periodicJobsHttpFinalizerBean = newPeriodicJobsHttpFinalizerBean();
        env().getPersistenceContext().run(() ->
                periodicJobsHttpFinalizerBean.deliver(chunk, delivery));

        final ConversionMetadata expectedMetadata = new ConversionMetadata(PeriodicJobsHttpFinalizerBean.ORIGIN)
                .withJobId(delivery.getJobId())
                .withAgencyId(receivingAgency)
                .withFilename("autoprint.deliver_test." + delivery.getJobId());
        verify(fileStoreServiceConnector).addMetadata(FILE_ID, expectedMetadata);
    }

    @Test
    public void deliver_onEmptyJob() throws FileStoreServiceConnectorUnexpectedStatusCodeException {
        final int jobId = 42;

        final int receivingAgency = 12344321;
        final PeriodicJobsDelivery delivery = new PeriodicJobsDelivery(jobId);
        delivery.setConfig(new PeriodicJobsHarvesterConfig(1, 1,
                new PeriodicJobsHarvesterConfig.Content()
                        .withTimeOfLastHarvest(new Date())
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
                .withFilename("no_content.deliver_test." + delivery.getJobId());
        verify(fileStoreServiceConnector).addMetadata(FILE_ID, expectedMetadata);
    }

    @Test
    public void deliver_file_with_header_and_footer() throws FileStoreServiceConnectorException,
            WeekResolverConnectorException {
        final WeekResolverResult weekResolverResult = new WeekResolverResult();
        final ArgumentCaptor<InputStream> inputStreamArgumentCaptor =
                ArgumentCaptor.forClass(InputStream.class);

        weekResolverResult.setYear(2020);
        weekResolverResult.setWeekNumber(41);
        when(weekResolverConnector.getCurrentWeekCode(eq("EMO"), any(LocalDate.class))).thenReturn(weekResolverResult);

        final int jobId = 42;
        final PeriodicJobsDataBlock block0 = new PeriodicJobsDataBlock();
        block0.setKey(new PeriodicJobsDataBlock.Key(jobId, 0, 0));
        block0.setSortkey("000000000");
        block0.setBytes(StringUtil.asBytes("0\n"));
        block0.setGroupHeader(StringUtil.asBytes("groupA\n"));
        final PeriodicJobsDataBlock block1 = new PeriodicJobsDataBlock();
        block1.setKey(new PeriodicJobsDataBlock.Key(jobId, 1, 0));
        block1.setSortkey("000000001");
        block1.setBytes(StringUtil.asBytes("1\n"));
        final PeriodicJobsDataBlock block2 = new PeriodicJobsDataBlock();
        block2.setKey(new PeriodicJobsDataBlock.Key(jobId, 2, 0));
        block2.setSortkey("000000002");
        block2.setBytes(StringUtil.asBytes("2\n"));
        block2.setGroupHeader(StringUtil.asBytes("groupB\n"));

        env().getPersistenceContext().run(() -> {
            env().getEntityManager().persist(block2);
            env().getEntityManager().persist(block1);
            env().getEntityManager().persist(block0);
        });

        final int receivingAgency = 12344321;
        final PeriodicJobsDelivery delivery = new PeriodicJobsDelivery(jobId);
        delivery.setConfig(new PeriodicJobsHarvesterConfig(1, 1,
                new PeriodicJobsHarvesterConfig.Content()
                        .withTimeOfLastHarvest(new Date())
                        .withName("Deliver test")
                        .withSubmitterNumber("111111")
                        .withPickup(new HttpPickup()
                                .withReceivingAgency(String.valueOf(receivingAgency))
                                .withContentHeader("Ugekorrektur uge ${__WEEKCODE_EMO__}\n")
                                .withContentFooter("\nslut uge ${__WEEKCODE_EMO__}"))));
        final Chunk chunk = new Chunk(jobId, 3, Chunk.Type.PROCESSED);

        final PeriodicJobsHttpFinalizerBean periodicJobsHttpFinalizerBean = newPeriodicJobsHttpFinalizerBean();
        env().getPersistenceContext().run(() ->
                periodicJobsHttpFinalizerBean.deliver(chunk, delivery));

        final InOrder orderVerifier = Mockito.inOrder(fileStoreServiceConnector);
        orderVerifier.verify(fileStoreServiceConnector).appendToFile(FILE_ID, block1.getBytes());
        orderVerifier.verify(fileStoreServiceConnector).appendToFile(FILE_ID, StringUtil.asBytes("groupB\n2\n"));
        orderVerifier.verify(fileStoreServiceConnector).appendToFile(FILE_ID, StringUtil.asBytes("\nslut uge 202041"));

        final ConversionMetadata expectedMetadata = new ConversionMetadata(PeriodicJobsHttpFinalizerBean.ORIGIN)
                .withJobId(delivery.getJobId())
                .withAgencyId(receivingAgency)
                .withFilename("deliver_test." + delivery.getJobId());
        verify(fileStoreServiceConnector).addMetadata(FILE_ID, expectedMetadata);
        verify(fileStoreServiceConnector).addFile(inputStreamArgumentCaptor.capture());
        final String actualAddFileData = StringUtil.asString(inputStreamArgumentCaptor.getValue(), StandardCharsets.UTF_8);
        assertThat("AddFile is called with data from contentHeader",
                actualAddFileData, is("Ugekorrektur uge 202041\n"));

    }

    private PeriodicJobsHttpFinalizerBean newPeriodicJobsHttpFinalizerBean() {
        final PeriodicJobsHttpFinalizerBean periodicJobsHttpFinalizerBean = new PeriodicJobsHttpFinalizerBean();
        periodicJobsHttpFinalizerBean.entityManager = env().getEntityManager();
        periodicJobsHttpFinalizerBean.fileStoreServiceConnector = fileStoreServiceConnector;
        periodicJobsHttpFinalizerBean.jobStoreServiceConnector = jobStoreServiceConnector;
        periodicJobsHttpFinalizerBean.weekResolverConnector = weekResolverConnector;
        return periodicJobsHttpFinalizerBean;
    }
}
