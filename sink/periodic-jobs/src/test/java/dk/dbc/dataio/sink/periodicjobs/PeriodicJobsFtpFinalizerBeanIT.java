package dk.dbc.dataio.sink.periodicjobs;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.harvester.types.FtpPickup;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
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
import java.net.Authenticator;
import java.net.PasswordAuthentication;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PeriodicJobsFtpFinalizerBeanIT extends IntegrationTest {
    static final String USERNAME = "FtpClientTest";
    static final String PASSWORD = "FtpClientTestPass";
    static final String HOME_DIR = "/home/ftp";
    static final String PUT_DIR = "put";
    static final String ABSOLUTE_PUT_DIR = String.join("/", HOME_DIR, PUT_DIR);
    static final FakeFtpServer fakeFtpServer = new FakeFtpServer();

    @Before
    public void setUp() {
        fakeFtpServer.setServerControlPort(0);  // use any free port
        fakeFtpServer.addUserAccount(new UserAccount(USERNAME,
                PASSWORD, HOME_DIR));
        final FileSystem fileSystem = new UnixFakeFileSystem();
        fileSystem.add(new DirectoryEntry(HOME_DIR));
        final DirectoryEntry putDir = new DirectoryEntry(ABSOLUTE_PUT_DIR);
        fileSystem.add(putDir);
        fakeFtpServer.setFileSystem(fileSystem);
        fakeFtpServer.start();
    }

    @Test
    public void deliver_onNonEmptyJobNoDataBlocks() {
        final int jobId = 42;
        final PeriodicJobsDelivery delivery = new PeriodicJobsDelivery(jobId);
        delivery.setConfig(new PeriodicJobsHarvesterConfig(1, 1,
                new PeriodicJobsHarvesterConfig.Content()
                        .withName("Deliver test")
                        .withSubmitterNumber("111111")
                        .withPickup(new FtpPickup()
                                .withFtpHost("localhost")
                                .withFtpUser(USERNAME)
                                .withFtpPassword(PASSWORD)
                                .withFtpSubdirectory(PUT_DIR))));
        final Chunk chunk = new Chunk(jobId, 3, Chunk.Type.PROCESSED);
        final PeriodicJobsFtpFinalizerBean periodicJobsFtpFinalizerBean = newPeriodicJobsFtpFinalizerBean();
        env().getPersistenceContext().run(() ->
                periodicJobsFtpFinalizerBean.deliver(chunk, delivery));
        FtpClient ftpClient = new FtpClient()
                .withHost("localhost")
                .withPort(fakeFtpServer.getServerControlPort())
                .withUsername(USERNAME)
                .withPassword(PASSWORD);
        ftpClient.cd(PUT_DIR);
        String listing = String.join("\n",ftpClient.list());
        String fileName = String.format("periodisk-job-%d.data", jobId);
        assertThat("File is NOT present", listing.contains(fileName),is(false));
    }

    @Test
    public void deliver_onNonEmptyJob() throws IOException {
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

        final PeriodicJobsDelivery delivery = new PeriodicJobsDelivery(jobId);
        delivery.setConfig(new PeriodicJobsHarvesterConfig(1, 1,
                new PeriodicJobsHarvesterConfig.Content()
                        .withName("Deliver testÆØÅ")
                        .withSubmitterNumber("111111")
                        .withPickup(new FtpPickup()
                                .withFtpHost("localhost")
                                .withFtpUser(USERNAME)
                                .withFtpPassword(PASSWORD)
                                .withFtpSubdirectory(PUT_DIR))));
        final Chunk chunk = new Chunk(jobId, 3, Chunk.Type.PROCESSED);
        final PeriodicJobsFtpFinalizerBean periodicJobsFtpFinalizerBean = newPeriodicJobsFtpFinalizerBean();
        env().getPersistenceContext().run(() ->
                periodicJobsFtpFinalizerBean.deliver(chunk, delivery));
        FtpClient ftpClient = new FtpClient()
                .withHost("localhost")
                .withPort(fakeFtpServer.getServerControlPort())
                .withUsername(USERNAME)
                .withPassword(PASSWORD);
        ftpClient.cd(PUT_DIR);
        String dataSentUsingFtp = readInputStream(ftpClient.get(String.format("deliver_test.%d", jobId)));
        assertThat("Content received", dataSentUsingFtp, is("012"));
    }

    @Test
    public void deliver_onEmptyJob() throws IOException {
        final int jobId = 42;
        final PeriodicJobsDelivery delivery = new PeriodicJobsDelivery(jobId);
        delivery.setConfig(new PeriodicJobsHarvesterConfig(1, 1,
                new PeriodicJobsHarvesterConfig.Content()
                        .withName("Deliver testÆØÅ")
                        .withSubmitterNumber("111111")
                        .withPickup(new FtpPickup()
                                .withFtpHost("localhost")
                                .withFtpUser(USERNAME)
                                .withFtpPassword(PASSWORD)
                                .withFtpSubdirectory(PUT_DIR))));
        final Chunk chunk = new Chunk(jobId, 0, Chunk.Type.PROCESSED);
        final PeriodicJobsFtpFinalizerBean periodicJobsFtpFinalizerBean = newPeriodicJobsFtpFinalizerBean();

        env().getPersistenceContext().run(() ->
                periodicJobsFtpFinalizerBean.deliver(chunk, delivery));

        final FtpClient ftpClient = new FtpClient()
                .withHost("localhost")
                .withPort(fakeFtpServer.getServerControlPort())
                .withUsername(USERNAME)
                .withPassword(PASSWORD)
                .cd(PUT_DIR);
        assertThat("data uploaded",
                readInputStream(ftpClient.get(String.format("deliver_test.%d.EMPTY", jobId))),
                is(""));
    }

    @Test
    public void useAuthenticationWithProxy() {
        PeriodicJobsFtpFinalizerBean finalizerBean = newPeriodicJobsFtpFinalizerBean();
        finalizerBean.proxyHost = "a.proxyhost.near.you";
        finalizerBean.proxyUser = "proxyUser";
        finalizerBean.proxyPassword = "password";
        finalizerBean.proxyPort = "1080";
        finalizerBean.setAuthentication();
        PasswordAuthentication authenticator = Authenticator.requestPasswordAuthentication(finalizerBean.proxyHost,
                null,
                Integer.parseInt(finalizerBean.proxyPort),
                "FTP",
                "",
                "ftp",
                null,
                Authenticator.RequestorType.PROXY);
        assertThat("proxy-username is set", authenticator.getUserName(), is(finalizerBean.proxyUser));
        assertThat("proxy-password is set", authenticator.getPassword(), is(finalizerBean.proxyPassword.toCharArray()));
    }


    @After
    public void tearDown() {
        fakeFtpServer.stop();
    }

    private static String readInputStream(InputStream is) throws IOException {
        try (final BufferedReader in = new BufferedReader(
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
        final PeriodicJobsFtpFinalizerBean periodicJobsFtpFinalizerBean = new PeriodicJobsFtpFinalizerBean();
        periodicJobsFtpFinalizerBean.ftpClient.withPort(fakeFtpServer.getServerControlPort());
        periodicJobsFtpFinalizerBean.entityManager = env().getEntityManager();
        return periodicJobsFtpFinalizerBean;
    }
}