package dk.dbc.dataio.sink.periodicjobs.pickup;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.harvester.types.FtpPickup;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import dk.dbc.dataio.sink.periodicjobs.IntegrationTest;
import dk.dbc.dataio.sink.periodicjobs.PeriodicJobsDataBlock;
import dk.dbc.dataio.sink.periodicjobs.PeriodicJobsDelivery;
import dk.dbc.ftp.FtpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class PeriodicJobsFtpFinalizerBeanIT extends IntegrationTest {
    static final String USERNAME = "FtpClientTest";
    static final String PASSWORD = "FtpClientTestPass";
    static final String HOME_DIR = "/home/ftp";
    static final String PUT_DIR = "put";
    static final String ABSOLUTE_PUT_DIR = String.join("/", HOME_DIR, PUT_DIR);
    static final FakeFtpServer fakeFtpServer = new FakeFtpServer();

    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);

    @Before
    public void setUp() {
        fakeFtpServer.setServerControlPort(0);  // use any free port
        fakeFtpServer.addUserAccount(new UserAccount(USERNAME,
                PASSWORD, HOME_DIR));
        FileSystem fileSystem = new UnixFakeFileSystem();
        fileSystem.add(new DirectoryEntry(HOME_DIR));
        DirectoryEntry putDir = new DirectoryEntry(ABSOLUTE_PUT_DIR);
        fileSystem.add(putDir);
        fakeFtpServer.setFileSystem(fileSystem);
        fakeFtpServer.start();
    }

    @Test
    public void deliver_onNonEmptyJobNoDataBlocks() {
        final int jobId = 42;
        PeriodicJobsDelivery delivery = new PeriodicJobsDelivery(jobId);
        delivery.setConfig(new PeriodicJobsHarvesterConfig(1, 1,
                new PeriodicJobsHarvesterConfig.Content()
                        .withName("Deliver test")
                        .withSubmitterNumber("111111")
                        .withTimeOfLastHarvest(new Date())
                        .withPickup(new FtpPickup()
                                .withFtpHost("localhost")
                                .withFtpPort(String.valueOf(fakeFtpServer.getServerControlPort()))
                                .withFtpUser(USERNAME)
                                .withFtpPassword(PASSWORD)
                                .withFtpSubdirectory(PUT_DIR))));
        Chunk chunk = new Chunk(jobId, 3, Chunk.Type.PROCESSED);
        PeriodicJobsFtpFinalizerBean periodicJobsFtpFinalizerBean = newPeriodicJobsFtpFinalizerBean();
        env().getPersistenceContext().run(() ->
                periodicJobsFtpFinalizerBean.deliver(chunk, delivery, env().getEntityManager()));
        FtpClient ftpClient = new FtpClient()
                .withHost("localhost")
                .withPort(fakeFtpServer.getServerControlPort())
                .withUsername(USERNAME)
                .withPassword(PASSWORD);
        ftpClient.cd(PUT_DIR);
        String listing = String.join("\n", ftpClient.list());
        String fileName = String.format("periodisk-job-%d.data", jobId);
        assertThat("File is NOT present", listing.contains(fileName), is(false));
    }

    @Test
    public void deliver_onNonEmptyJob() throws IOException {
        final int jobId = 42;
        PeriodicJobsDataBlock block0 = new PeriodicJobsDataBlock();
        block0.setKey(new PeriodicJobsDataBlock.Key(jobId, 0, 0));
        block0.setSortkey("000000000");
        block0.setBytes(StringUtil.asBytes("0\n"));
        block0.setGroupHeader(StringUtil.asBytes("groupA\n"));
        PeriodicJobsDataBlock block1 = new PeriodicJobsDataBlock();
        block1.setKey(new PeriodicJobsDataBlock.Key(jobId, 1, 0));
        block1.setSortkey("000000001");
        block1.setBytes(StringUtil.asBytes("1\n"));
        PeriodicJobsDataBlock block2 = new PeriodicJobsDataBlock();
        block2.setKey(new PeriodicJobsDataBlock.Key(jobId, 2, 0));
        block2.setSortkey("000000002");
        block2.setBytes(StringUtil.asBytes("2"));
        block2.setGroupHeader(StringUtil.asBytes("groupB\n"));

        env().getPersistenceContext().run(() -> {
            env().getEntityManager().persist(block2);
            env().getEntityManager().persist(block1);
            env().getEntityManager().persist(block0);
        });

        PeriodicJobsDelivery delivery = new PeriodicJobsDelivery(jobId);
        delivery.setConfig(new PeriodicJobsHarvesterConfig(1, 1,
                new PeriodicJobsHarvesterConfig.Content()
                        .withName("Deliver testÆØÅ")
                        .withSubmitterNumber("111111")
                        .withTimeOfLastHarvest(new Date())
                        .withPickup(new FtpPickup()
                                .withFtpHost("localhost")
                                .withFtpPort(String.valueOf(fakeFtpServer.getServerControlPort()))
                                .withFtpUser(USERNAME)
                                .withFtpPassword(PASSWORD)
                                .withFtpSubdirectory(PUT_DIR))));
        Chunk chunk = new Chunk(jobId, 3, Chunk.Type.PROCESSED);
        PeriodicJobsFtpFinalizerBean periodicJobsFtpFinalizerBean = newPeriodicJobsFtpFinalizerBean();
        env().getPersistenceContext().run(() ->
                periodicJobsFtpFinalizerBean.deliver(chunk, delivery, env().getEntityManager()));
        FtpClient ftpClient = new FtpClient()
                .withHost("localhost")
                .withPort(fakeFtpServer.getServerControlPort())
                .withUsername(USERNAME)
                .withPassword(PASSWORD);
        ftpClient.cd(PUT_DIR);
        String dataSentUsingFtp = readInputStream(ftpClient.get(String.format("deliver_test.%d", jobId)));
        assertThat("Content received", dataSentUsingFtp, is("groupA\n0\n1\ngroupB\n2"));
    }

    @Test
    public void deliver_onEmptyJob() throws IOException {
        final int jobId = 42;
        PeriodicJobsDelivery delivery = new PeriodicJobsDelivery(jobId);
        delivery.setConfig(new PeriodicJobsHarvesterConfig(1, 1,
                new PeriodicJobsHarvesterConfig.Content()
                        .withName("Deliver testÆØÅ")
                        .withSubmitterNumber("111111")
                        .withTimeOfLastHarvest(new Date())
                        .withPickup(new FtpPickup()
                                .withFtpHost("localhost")
                                .withFtpPort(String.valueOf(fakeFtpServer.getServerControlPort()))
                                .withFtpUser(USERNAME)
                                .withFtpPassword(PASSWORD)
                                .withFtpSubdirectory(PUT_DIR))));
        Chunk chunk = new Chunk(jobId, 0, Chunk.Type.PROCESSED);
        PeriodicJobsFtpFinalizerBean periodicJobsFtpFinalizerBean = newPeriodicJobsFtpFinalizerBean();

        env().getPersistenceContext().run(() ->
                periodicJobsFtpFinalizerBean.deliver(chunk, delivery, env().getEntityManager()));

        FtpClient ftpClient = new FtpClient()
                .withHost("localhost")
                .withPort(fakeFtpServer.getServerControlPort())
                .withUsername(USERNAME)
                .withPassword(PASSWORD)
                .cd(PUT_DIR);
        assertThat("data uploaded",
                readInputStream(ftpClient.get(String.format("deliver_test.%d.EMPTY", jobId))),
                is(""));
    }

    @After
    public void tearDown() {
        fakeFtpServer.stop();
    }

    private static String readInputStream(InputStream is) throws IOException {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(is))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString().trim();
        }
    }

    private PeriodicJobsFtpFinalizerBean newPeriodicJobsFtpFinalizerBean() {
        PeriodicJobsFtpFinalizerBean periodicJobsFtpFinalizerBean = new PeriodicJobsFtpFinalizerBean();
        periodicJobsFtpFinalizerBean.jobStoreServiceConnector = jobStoreServiceConnector;
        return periodicJobsFtpFinalizerBean;
    }
}
