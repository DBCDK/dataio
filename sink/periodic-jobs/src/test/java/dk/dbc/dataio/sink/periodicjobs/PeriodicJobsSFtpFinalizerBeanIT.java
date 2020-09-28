package dk.dbc.dataio.sink.periodicjobs;

import com.github.stefanbirkner.fakesftpserver.rule.FakeSftpServerRule;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.harvester.types.FtpPickup;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import dk.dbc.dataio.harvester.types.SFtpPickup;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PeriodicJobsSFtpFinalizerBeanIT extends IntegrationTest {
    static final String testDir = "/test";
    static final String sftpUser = "sftpuser";
    static final String sftPassword = "sftppassword";

    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private final JobStoreServiceConnectorBean jobStoreServiceConnectorBean = mock(JobStoreServiceConnectorBean.class);


    @Rule
    public final FakeSftpServerRule fakeSFtpServer = new FakeSftpServerRule()
            .addUser(sftpUser, sftPassword);


    @Before
    public void setUp() throws IOException {
        fakeSFtpServer.createDirectory(testDir);

        when(jobStoreServiceConnectorBean.getConnector())
                .thenReturn(jobStoreServiceConnector);
    }

    @Test
    public void deliver_onNonEmptyJobNoDataBlocks() {
        final int jobId = 42;
        final PeriodicJobsDelivery delivery = new PeriodicJobsDelivery(jobId);

        delivery.setConfig(new PeriodicJobsHarvesterConfig(1, 1,
                new PeriodicJobsHarvesterConfig.Content()
                        .withName("Deliver to SFTP test")
                        .withSubmitterNumber("22222222")
                        .withPickup(new SFtpPickup()
                                .withSFtpHost("localhost")
                                .withSFtpPort(String.valueOf(fakeSFtpServer.getPort()))
                                .withSFtpuser(sftpUser)
                                .withSFtpPassword(sftPassword)
                                .withSFtpSubdirectory(testDir))));
        final Chunk chunk = new Chunk(jobId, 3, Chunk.Type.PROCESSED);
        final PeriodicJobsSFtpFinalizerBean periodicJobsSFtpFinalizerBean = newPeriodicJobsSFtpFinalizerBean();
        env().getPersistenceContext().run(() ->
                periodicJobsSFtpFinalizerBean.deliver(chunk, delivery));
        final String fileName = String.format("%s/periodisk-job-%d.data", testDir, jobId);
        assertThat("File is NOT present", fakeSFtpServer.existsFile(fileName), is(false));
    }

    @Test
    public void deliver_onNonEmptyJob() throws IOException {
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
        block2.setBytes(StringUtil.asBytes("2"));
        block2.setGroupHeader(StringUtil.asBytes("groupB\n"));

        env().getPersistenceContext().run(() -> {
            env().getEntityManager().persist(block2);
            env().getEntityManager().persist(block1);
            env().getEntityManager().persist(block0);
        });

        final PeriodicJobsDelivery delivery = new PeriodicJobsDelivery(jobId);
        delivery.setConfig(new PeriodicJobsHarvesterConfig(1, 1,
                new PeriodicJobsHarvesterConfig.Content()
                        .withName("Deliver testÆØÅ")
                        .withSubmitterNumber("111111")
                        .withPickup(new SFtpPickup()
                                .withSFtpHost("localhost")
                                .withSFtpPort(String.valueOf(fakeSFtpServer.getPort()))
                                .withSFtpuser(sftpUser)
                                .withSFtpPassword(sftPassword)
                                .withSFtpSubdirectory(testDir))));
        final Chunk chunk = new Chunk(jobId, 3, Chunk.Type.PROCESSED);
        final PeriodicJobsSFtpFinalizerBean periodicJobsSFtpFinalizerBean = newPeriodicJobsSFtpFinalizerBean();
        env().getPersistenceContext().run(() ->
                periodicJobsSFtpFinalizerBean.deliver(chunk, delivery));

        String dataSentUsingSFtp = fakeSFtpServer.getFileContent(String
                .format("%s/%s", testDir, String.
                        format("deliver_test.%d", jobId)), StandardCharsets.UTF_8);
        assertThat("Content received", dataSentUsingSFtp, is("groupA\n0\n1\ngroupB\n2"));
    }

    @Test
    public void deliver_file_with_override_filename() throws IOException {
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
        block2.setBytes(StringUtil.asBytes("2"));
        block2.setGroupHeader(StringUtil.asBytes("groupB\n"));

        env().getPersistenceContext().run(() -> {
            env().getEntityManager().persist(block2);
            env().getEntityManager().persist(block1);
            env().getEntityManager().persist(block0);
        });

        final PeriodicJobsDelivery delivery = new PeriodicJobsDelivery(jobId);
        delivery.setConfig(new PeriodicJobsHarvesterConfig(1, 1,
                new PeriodicJobsHarvesterConfig.Content()
                        .withName("Deliver testÆØÅ")
                        .withSubmitterNumber("111111")
                        .withPickup(new SFtpPickup()
                                .withSFtpHost("localhost")
                                .withSFtpPort(String.valueOf(fakeSFtpServer.getPort()))
                                .withSFtpuser(sftpUser)
                                .withSFtpPassword(sftPassword)
                                .withSFtpSubdirectory(testDir)
                                .withOverrideFilename("testMyNewFileName.data"))));
        final Chunk chunk = new Chunk(jobId, 3, Chunk.Type.PROCESSED);
        final PeriodicJobsSFtpFinalizerBean periodicJobsSFtpFinalizerBean = newPeriodicJobsSFtpFinalizerBean();
        env().getPersistenceContext().run(() ->
                periodicJobsSFtpFinalizerBean.deliver(chunk, delivery));

        String dataSentUsingSFtp = fakeSFtpServer.getFileContent(
                String.format("%s/%s",testDir, "testMyNewFileName.data"), StandardCharsets.UTF_8);
        assertThat("Content received", dataSentUsingSFtp, is("groupA\n0\n1\ngroupB\n2"));
    }

    private PeriodicJobsSFtpFinalizerBean newPeriodicJobsSFtpFinalizerBean() {
        final PeriodicJobsSFtpFinalizerBean periodicJobsSFtpFinalizerBean = new PeriodicJobsSFtpFinalizerBean();
        periodicJobsSFtpFinalizerBean.entityManager = env().getEntityManager();
        periodicJobsSFtpFinalizerBean.jobStoreServiceConnectorBean = jobStoreServiceConnectorBean;
        periodicJobsSFtpFinalizerBean.proxyHost = "";
        periodicJobsSFtpFinalizerBean.proxyPort = "";
        periodicJobsSFtpFinalizerBean.proxyUser = "";
        periodicJobsSFtpFinalizerBean.proxyPassword = "";
        return periodicJobsSFtpFinalizerBean;
    }
}
