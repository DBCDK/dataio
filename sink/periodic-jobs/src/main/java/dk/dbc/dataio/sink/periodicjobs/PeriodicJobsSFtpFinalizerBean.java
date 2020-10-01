package dk.dbc.dataio.sink.periodicjobs;

import com.jcraft.jsch.ProxySOCKS5;
import dk.dbc.commons.jpa.ResultSet;
import dk.dbc.commons.sftpclient.SFTPConfig;
import dk.dbc.commons.sftpclient.SFtpClient;
import dk.dbc.dataio.common.utils.io.UncheckedFileOutputStream;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.harvester.types.SFtpPickup;
import dk.dbc.dataio.sink.types.SinkException;
import dk.dbc.util.Timed;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Stateless
public class PeriodicJobsSFtpFinalizerBean extends PeriodicJobsPickupFinalizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(PeriodicJobsSFtpFinalizerBean.class);

    @PersistenceContext(unitName = "periodic-jobs_PU")
    EntityManager entityManager;

    @Inject
    @ConfigProperty(name = "PROXY_HOST")
    String proxyHost;

    @Inject
    @ConfigProperty(name = "PROXY_PORT")
    String proxyPort;

    @Inject
    @ConfigProperty(name = "PROXY_USER")
    String proxyUser;

    @Inject
    @ConfigProperty(name = "PROXY_PASSWORD")
    String proxyPassword;

    @Inject
    @ConfigProperty(name = "NON_PROXYED_SFTP_DOMAINS")
    String nonProxyedDomains;

    @EJB public JobStoreServiceConnectorBean jobStoreServiceConnectorBean;

    @Timed
    @Override
    public Chunk deliver(Chunk chunk, PeriodicJobsDelivery delivery) throws SinkException {
        if (isEmptyJob(chunk, jobStoreServiceConnectorBean.getConnector())) {
            return deliverEmptyFile(chunk, delivery);
        }
        return deliverDatablocks(chunk, delivery);
    }

    private Chunk deliverEmptyFile(Chunk chunk, PeriodicJobsDelivery delivery) {
        final String remoteFile = getRemoteFilename(delivery) + ".EMPTY";
        final SFtpPickup sFtpPickup = (SFtpPickup) delivery.getConfig().getContent().getPickup();
        try(SFtpClient sFtpClient = open(sFtpPickup)) {
            sFtpClient.putContent(remoteFile, new ByteArrayInputStream("".getBytes()));
        }
        return newResultChunk(chunk,
                String.format("Empty file %s uploaded to sftp host '%s'", remoteFile, sFtpPickup.getsFtpHost()));
    }

    private Chunk deliverDatablocks(Chunk chunk, PeriodicJobsDelivery delivery) throws SinkException {
        final String remoteFile = getRemoteFilename(delivery);
        final SFtpPickup sftpPickup = (SFtpPickup) delivery.getConfig().getContent().getPickup();
        File localFile = null;
        try {
            localFile = File.createTempFile("dataBlocks", ".tmp.file");
            createLocalFile(delivery, localFile);
            if (localFile.length()>0) {
                uploadLocalFileToSFtp(sftpPickup, localFile, remoteFile);
                LOGGER.info("jobId '{}' uploaded to sftp host '{}'.", chunk.getJobId(), sftpPickup.getsFtpHost());
            } else {
                LOGGER.warn("jobId '{}' NOT uploaded to sftp host '{}' - no datablocks",
                        chunk.getJobId(), sftpPickup.getsFtpHost());
            }
        } catch (IOException e) {
            throw new SinkException(e);
        } finally {
            if (localFile != null) {
                localFile.delete();
            }
        }
        return newResultChunk(chunk,
                String.format("File %s uploaded to sftp host '%s'", remoteFile, sftpPickup.getsFtpHost()));
    }

    private SFtpClient open(SFtpPickup sFtpPickup) {
        ProxySOCKS5 proxyHandlerBean = null;
        if (!proxyHost.isEmpty()  && !proxyPort.isEmpty()  && proxyUser.isEmpty() && proxyPassword.isEmpty()) {
            proxyHandlerBean = new ProxySOCKS5(proxyHost, Integer.parseInt(proxyPort));
            proxyHandlerBean.setUserPasswd(proxyUser, proxyPassword);
        }
        return new SFtpClient(
                new SFTPConfig()
                .withHost(sFtpPickup.getsFtpHost())
                .withPort(Integer.parseInt(sFtpPickup.getsFtpPort()))
                .withUsername(sFtpPickup.getsFtpUser())
                .withPassword(sFtpPickup.getsFtpPassword())
                .withDir(sFtpPickup.getsFtpSubdirectory()),
                proxyHandlerBean,
                Arrays.asList(nonProxyedDomains.split("\\s*,\\s*")));
    }

    private void createLocalFile(PeriodicJobsDelivery delivery, File tmpFile) throws SinkException {
        final GroupHeaderIncludePredicate groupHeaderIncludePredicate = new GroupHeaderIncludePredicate();
        final Query getDataBlocksQuery = entityManager
                .createNamedQuery(PeriodicJobsDataBlock.GET_DATA_BLOCKS_QUERY_NAME)
                .setParameter(1, delivery.getJobId());
        try (final UncheckedFileOutputStream datablocksOutputStream = new UncheckedFileOutputStream(tmpFile);
             final ResultSet<PeriodicJobsDataBlock> datablocks = new ResultSet<>(entityManager, getDataBlocksQuery,
                     new PeriodicJobsDataBlockResultSetMapping())) {
            for (PeriodicJobsDataBlock datablock : datablocks) {
                if (groupHeaderIncludePredicate.test(datablock)) {
                    datablocksOutputStream.write(datablock.getGroupHeader());
                }
                datablocksOutputStream.write(datablock.getBytes());
            }
            datablocksOutputStream.flush();
        } catch (IOException e) {
            throw new SinkException(e);
        }
    }

    private void uploadLocalFileToSFtp(SFtpPickup sFtpPickup, File local, String remote) throws SinkException {
        try (BufferedInputStream dataBlockStream = new BufferedInputStream(new FileInputStream(local), 1024);
            SFtpClient sFtpClient = open(sFtpPickup)) {
            sFtpClient.putContent(remote, dataBlockStream);
        } catch (IOException e) {
            throw new SinkException(e);
        }
    }

    private Chunk newResultChunk(Chunk chunk, String data) {
        final Chunk result = new Chunk(chunk.getJobId(), chunk.getChunkId(), Chunk.Type.DELIVERED);
        final ChunkItem chunkItem = ChunkItem.successfulChunkItem()
                .withId(0)
                .withType(ChunkItem.Type.JOB_END)
                .withData(data)
                .withEncoding(StandardCharsets.UTF_8);
        result.insertItem(chunkItem);
        return result;
    }

}
