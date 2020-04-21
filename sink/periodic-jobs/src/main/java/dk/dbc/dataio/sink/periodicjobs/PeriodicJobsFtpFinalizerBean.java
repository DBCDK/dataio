package dk.dbc.dataio.sink.periodicjobs;

import dk.dbc.commons.jpa.ResultSet;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.harvester.types.FtpPickup;
import dk.dbc.dataio.sink.types.SinkException;
import dk.dbc.ftp.FtpClient;
import dk.dbc.util.Timed;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;

@Stateless
public class PeriodicJobsFtpFinalizerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(PeriodicJobsFtpFinalizerBean.class);

    @PersistenceContext(unitName = "periodic-jobs_PU")
    EntityManager entityManager;

    @Inject
    @ConfigProperty(name = "PROXY_HOST")
    protected String proxyHost;

    @Inject
    @ConfigProperty(name = "PROXY_PORT")
    protected String proxyPort;

    @Inject
    @ConfigProperty(name = "PROXY_USER")
    protected String proxyUser;

    @Inject
    @ConfigProperty(name = "PROXY_PASSWORD")
    protected String proxyPassword;

    protected FtpClient ftpClient = new FtpClient();

    @Timed
    public Chunk deliver(Chunk chunk, PeriodicJobsDelivery delivery) throws SinkException {
        if (chunk.getChunkId() == 0) {
            // End chunk having ID 0 means job is empty
            return deliverEmptyFile(chunk, delivery);
        }
        return deliverDatablocks(chunk, delivery);
    }

    private Chunk deliverEmptyFile(Chunk chunk, PeriodicJobsDelivery delivery) {
        final String remoteFile = getRemoteFilename(delivery) + ".EMPTY";
        final FtpPickup ftpPickup = (FtpPickup) delivery.getConfig().getContent().getPickup();
        try {
            ftpClient = open(ftpPickup);
            ftpClient.put(remoteFile, "");
        } finally {
            ftpClient.close();
        }
        return newResultChunk(chunk,
                String.format("Empty file %s uploaded to ftp host '%s'", remoteFile, ftpPickup.getFtpHost()));
    }

    private Chunk deliverDatablocks(Chunk chunk, PeriodicJobsDelivery delivery) throws SinkException {
        final String remoteFile = getRemoteFilename(delivery);
        final FtpPickup ftpPickup = (FtpPickup) delivery.getConfig().getContent().getPickup();
        File localFile = null;
        try {
            localFile = File.createTempFile("dataBlocks", ".tmp.file");
            createLocalFile(delivery, localFile);
            if (localFile.length()>0) {
                uploadLocalFileToFtp(ftpPickup, localFile, remoteFile);
                LOGGER.info("Data for jobid '{}' delivered to ftp host '{}'.", chunk.getJobId(), ftpPickup.getFtpHost());
            }
            else {
                LOGGER.warn("Data for jobid '{}' NOT delivered to ftp host '{}'. Filesize is 0.", chunk.getJobId(), ftpPickup.getFtpHost());
            }
        } catch (IOException e) {
            throw new SinkException(e);
        } finally {
            if (localFile != null) {
                localFile.delete();
            }
        }
        return newResultChunk(chunk,
                String.format("File %s uploaded to ftp host '%s'", remoteFile, ftpPickup.getFtpHost()));
    }

    private void createLocalFile(PeriodicJobsDelivery delivery, File tmpFile) throws SinkException {
        final Query getDataBlocksQuery = entityManager
                .createNamedQuery(PeriodicJobsDataBlock.GET_DATA_BLOCKS_QUERY_NAME)
                .setParameter(1, delivery.getJobId());
        try (final FileOutputStream datablocksOutputStream = new FileOutputStream(tmpFile);
             final ResultSet<PeriodicJobsDataBlock> datablocks = new ResultSet<>(entityManager, getDataBlocksQuery,
                     new PeriodicJobsDataBlockResultSetMapping());) {
            for (PeriodicJobsDataBlock datablock : datablocks) {
                datablocksOutputStream.write(datablock.getBytes());
            }
            datablocksOutputStream.flush();
        } catch (IOException e) {
            throw new SinkException(e);
        }
    }

    private void uploadLocalFileToFtp(FtpPickup ftpPickup, File local, String remote) throws SinkException {
        try (BufferedInputStream dataBlockStream = new BufferedInputStream(new FileInputStream(local), 1024)) {
            ftpClient = open(ftpPickup);
            ftpClient.put(remote, dataBlockStream, FtpClient.FileType.BINARY);
        } catch (IOException e) {
            throw new SinkException(e);
        } finally {
            ftpClient.close();
        }
    }

    private Chunk newResultChunk(Chunk chunk, String data) {
        final Chunk result = new Chunk(chunk.getJobId(), chunk.getChunkId(), Chunk.Type.DELIVERED);
        final ChunkItem chunkItem;
        chunkItem = ChunkItem.successfulChunkItem()
                .withData(data);
        result.insertItem(chunkItem
                .withId(0)
                .withType(ChunkItem.Type.STRING)
                .withEncoding(StandardCharsets.UTF_8));
        return result;
    }

    private String getRemoteFilename(PeriodicJobsDelivery delivery) {
        return delivery.getConfig().getContent()
                .getName()
                .toLowerCase()
                .replaceAll("[^\\p{ASCII}]", "")
                .replaceAll("\\s+","_")
                + "." + delivery.getJobId();
    }

    protected FtpClient open(FtpPickup ftpPickup) {
        final String subDir = ftpPickup.getFtpSubdirectory();
        ftpClient.withHost(ftpPickup.getFtpHost())
                .withUsername(ftpPickup.getFtpUser())
                .withPassword(ftpPickup.getFtpPassword());
        if (proxyHost != null && proxyPort != null && proxyUser != null && proxyPassword != null) {
            final InetSocketAddress address = new InetSocketAddress(proxyHost, Integer.parseInt(proxyPort));
            ftpClient.withProxy(new Proxy(Proxy.Type.SOCKS, address));
        }
        setAuthentication();
        if (subDir != null && !subDir.isEmpty()) {
            ftpClient.cd(subDir);
        }
        return ftpClient;
    }

    protected void setAuthentication() {
        Authenticator.setDefault(
                new Authenticator() {
                    @Override
                    public PasswordAuthentication getPasswordAuthentication() {
                        if (proxyUser == null || proxyPassword == null) {
                            return null;
                        }
                        if (getRequestingHost().equalsIgnoreCase(proxyHost)) {
                            return new PasswordAuthentication(
                                    proxyUser, proxyPassword.toCharArray());
                        }
                        return null;
                    }
                }
        );
    }
}