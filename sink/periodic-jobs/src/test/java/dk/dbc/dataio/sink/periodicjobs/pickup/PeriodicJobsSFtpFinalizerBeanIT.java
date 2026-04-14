package dk.dbc.dataio.sink.periodicjobs.pickup;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import dk.dbc.dataio.harvester.types.SFtpPickup;
import dk.dbc.dataio.sink.periodicjobs.ContainerTest;
import dk.dbc.dataio.sink.periodicjobs.PeriodicJobsDataBlock;
import dk.dbc.dataio.sink.periodicjobs.PeriodicJobsDelivery;
import dk.dbc.proxy.ProxyBean;
import dk.dbc.weekresolver.connector.WeekResolverConnector;
import dk.dbc.weekresolver.connector.WeekResolverConnectorException;
import dk.dbc.weekresolver.model.WeekResolverResult;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PeriodicJobsSFtpFinalizerBeanIT extends ContainerTest {
    static final String testDir = "/test";
    static final String sftpUser = "sftpuser";
    static final String sftPassword = "sftppassword";

    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private final WeekResolverConnector weekResolverConnector =
            mock(WeekResolverConnector.class);

    private final String SFTP_SERVER = getLocalIPAddress();

    @Rule
    public final SftpServerRule fakeSFtpServer = new SftpServerRule()
            .addUser(sftpUser, sftPassword);

    @Before
    public void setUp() throws IOException {
        fakeSFtpServer.createDirectory(testDir);
    }

    @Test
    public void deliver_onNonEmptyJobNoDataBlocks() {
        final int jobId = 42;
        final PeriodicJobsDelivery delivery = new PeriodicJobsDelivery(jobId);

        delivery.setConfig(new PeriodicJobsHarvesterConfig(1, 1,
                new PeriodicJobsHarvesterConfig.Content()
                        .withName("Deliver to SFTP test")
                        .withSubmitterNumber("22222222")
                        .withTimeOfLastHarvest(new Date())
                        .withPickup(getPickup())));
        final Chunk chunk = new Chunk(jobId, 3, Chunk.Type.PROCESSED);
        final PeriodicJobsSFtpFinalizerBean periodicJobsSFtpFinalizerBean = newPeriodicJobsSFtpFinalizerBean();
        env().getPersistenceContext().run(() ->
                periodicJobsSFtpFinalizerBean.deliver(chunk, delivery, env().getEntityManager()));
        final String fileName = String.format("%s/periodisk-job-%d.data", testDir, jobId);
        assertThat("File is NOT present", fakeSFtpServer.existsFile(fileName), is(false));
    }

    @Test
    public void deliver_onNonEmptyJob() throws IOException {
        final int jobId = 42;
        persistDataBlocks(jobId);

        final PeriodicJobsDelivery delivery = new PeriodicJobsDelivery(jobId);
        delivery.setConfig(new PeriodicJobsHarvesterConfig(1, 1,
                new PeriodicJobsHarvesterConfig.Content()
                        .withName("Deliver testÆØÅ")
                        .withSubmitterNumber("111111")
                        .withTimeOfLastHarvest(new Date())
                        .withPickup(getPickup())));
        final Chunk chunk = new Chunk(jobId, 3, Chunk.Type.PROCESSED);
        final PeriodicJobsSFtpFinalizerBean periodicJobsSFtpFinalizerBean = newPeriodicJobsSFtpFinalizerBean();
        env().getPersistenceContext().run(() ->
                periodicJobsSFtpFinalizerBean.deliver(chunk, delivery, env().getEntityManager()));

        String dataSentUsingSFtp = fakeSFtpServer.getFileContent(String
                .format("%s/%s", testDir, String.
                        format("deliver_test.%d", jobId)), StandardCharsets.UTF_8);
        assertThat("Content received", dataSentUsingSFtp, is("groupA\n0\n1\ngroupB\n2"));
    }

    @Test
    public void deliver_fileWithOverrideFilename() throws IOException {
        final int jobId = 42;
        persistDataBlocks(jobId);

        final PeriodicJobsDelivery delivery = new PeriodicJobsDelivery(jobId);
        delivery.setConfig(new PeriodicJobsHarvesterConfig(1, 1,
                new PeriodicJobsHarvesterConfig.Content()
                        .withName("Deliver testÆØÅ")
                        .withSubmitterNumber("111111")
                        .withTimeOfLastHarvest(new Date())
                        .withPickup(getPickup()
                                .withOverrideFilename("testMyNewFileName.data"))));
        final Chunk chunk = new Chunk(jobId, 3, Chunk.Type.PROCESSED);
        final PeriodicJobsSFtpFinalizerBean periodicJobsSFtpFinalizerBean = newPeriodicJobsSFtpFinalizerBean();
        env().getPersistenceContext().run(() ->
                periodicJobsSFtpFinalizerBean.deliver(chunk, delivery, env().getEntityManager()));

        String dataSentUsingSFtp = fakeSFtpServer.getFileContent(
                String.format("%s/%s", testDir, "testMyNewFileName.data"), StandardCharsets.UTF_8);
        assertThat("Content received", dataSentUsingSFtp, is("groupA\n0\n1\ngroupB\n2"));
    }

    @Test
    public void deliver_fileWithHeaderAndFooter() throws IOException, WeekResolverConnectorException {
        final WeekResolverResult weekResolverResult = new WeekResolverResult();
        weekResolverResult.setYear(2020);
        weekResolverResult.setWeekNumber(41);
        when(weekResolverConnector.getCurrentWeekCodeForDate(eq("EMO"), any(LocalDate.class))).thenReturn(weekResolverResult);
        final int jobId = 42;
        persistDataBlocks(jobId);

        final PeriodicJobsDelivery delivery = new PeriodicJobsDelivery(jobId);
        delivery.setConfig(new PeriodicJobsHarvesterConfig(1, 1,
                new PeriodicJobsHarvesterConfig.Content()
                        .withName("Deliver testÆØÅ")
                        .withSubmitterNumber("111111")
                        .withTimeOfLastHarvest(new Date())
                        .withPickup(getPickup()
                                .withOverrideFilename("testMyNewFileName${__WEEKCODE_EMO__}.data")
                                .withContentHeader("Ugekorrektur uge ${__WEEKCODE_EMO__}\n")
                                .withContentFooter("\nslut uge ${__WEEKCODE_EMO__}"))));
        final Chunk chunk = new Chunk(jobId, 3, Chunk.Type.PROCESSED);
        final PeriodicJobsSFtpFinalizerBean periodicJobsSFtpFinalizerBean = newPeriodicJobsSFtpFinalizerBean();
        env().getPersistenceContext().run(() ->
                periodicJobsSFtpFinalizerBean.deliver(chunk, delivery, env().getEntityManager()));

        String dataSentUsingSFtp = fakeSFtpServer.getFileContent(
                String.format("%s/%s", testDir, "testMyNewFileName202041.data"), StandardCharsets.UTF_8);
        assertThat("Content received", dataSentUsingSFtp, is("Ugekorrektur uge 202041\ngroupA\n0\n1\ngroupB\n2\nslut uge 202041"));
    }

    @Test
    public void deliver_testThatSftpGoesViaProxy() {
        assertThat("sftp traffic goes via proxy", getProxyLog(), containsString("local client closed.  Session duration:"));
    }

    private PeriodicJobsSFtpFinalizerBean newPeriodicJobsSFtpFinalizerBean() {
        final PeriodicJobsSFtpFinalizerBean periodicJobsSFtpFinalizerBean = new PeriodicJobsSFtpFinalizerBean();
        periodicJobsSFtpFinalizerBean.jobStoreServiceConnector = jobStoreServiceConnector;
        periodicJobsSFtpFinalizerBean.proxyBean = new ProxyBean(PROXY_HOST, PROXY_PORT)
                .withProxyUsername(PROXY_USER)
                .withProxyPassword(PROXY_PASSWORD);
        periodicJobsSFtpFinalizerBean.weekResolverConnector = weekResolverConnector;
        return periodicJobsSFtpFinalizerBean;
    }

    private SFtpPickup getPickup() {
        return new SFtpPickup()
                .withSFtpHost(SFTP_SERVER)
                .withSFtpPort(String.valueOf(fakeSFtpServer.getPort()))
                .withSFtpuser(sftpUser)
                .withSFtpPassword(sftPassword)
                .withSFtpSubdirectory(testDir);
    }

    private void persistDataBlocks(int jobId) {
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
    }

    /**
     * JUnit 4 rule that starts an Apache MINA SSHD 2.x SFTP server on a random port,
     * backed by a temporary directory on the real filesystem.
     */
    static class SftpServerRule extends ExternalResource {
        private final Map<String, String> credentials = new HashMap<>();
        private SshServer sshd;
        private Path rootDir;

        SftpServerRule addUser(String username, String password) {
            credentials.put(username, password);
            return this;
        }

        @Override
        protected void before() throws Throwable {
            rootDir = Files.createTempDirectory("sftp-test-");
            sshd = SshServer.setUpDefaultServer();
            sshd.setPort(0);
            sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
            sshd.setPasswordAuthenticator((username, password, session) ->
                    credentials.containsKey(username) && credentials.get(username).equals(password));
            sshd.setSubsystemFactories(List.of(new SftpSubsystemFactory()));
            sshd.setFileSystemFactory(new VirtualFileSystemFactory(rootDir));
            sshd.start();
        }

        @Override
        protected void after() {
            try {
                if (sshd != null) {
                    sshd.stop(true);
                }
            } catch (IOException e) {
                // ignore
            }
            if (rootDir != null) {
                try {
                    Files.walk(rootDir)
                            .sorted(Comparator.reverseOrder())
                            .forEach(p -> p.toFile().delete());
                } catch (IOException e) {
                    // ignore
                }
            }
        }

        int getPort() {
            return sshd.getPort();
        }

        void createDirectory(String path) throws IOException {
            Files.createDirectories(resolve(path));
        }

        boolean existsFile(String path) {
            return Files.isRegularFile(resolve(path));
        }

        String getFileContent(String path, Charset charset) throws IOException {
            return Files.readString(resolve(path), charset);
        }

        private Path resolve(String sftpPath) {
            String relative = sftpPath.startsWith("/") ? sftpPath.substring(1) : sftpPath;
            return rootDir.resolve(relative);
        }
    }
}
