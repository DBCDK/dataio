package dk.dbc.dataio.sink.periodicjobs;

import dk.dbc.commons.jpa.ResultSet;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.harvester.types.FtpPickup;
import dk.dbc.dataio.sink.types.SinkException;
import dk.dbc.ftp.FtpClient;
import dk.dbc.util.Timed;
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
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        final FtpPickup ftpPickup = (FtpPickup) delivery.getConfig().getContent().getPickup();
        File tmpFile = null;
        try {
            tmpFile = File.createTempFile("dataBlocks", ".tmp.file");
            buildFtpFile(delivery, tmpFile);
            deliverAsFtp(ftpPickup, tmpFile, chunk.getJobId());
            LOGGER.info("Data for jobid '{}' delivered to ftp host '{}'.", chunk.getJobId(), ftpPickup.getFtpHost());
        } catch (IOException e) {
            throw new SinkException(e);
        } finally {
            if (tmpFile != null) {
                tmpFile.delete();
            }
        }
        return newResultChunk(chunk, ftpPickup);
    }

    private void buildFtpFile(PeriodicJobsDelivery delivery, File tmpFile) throws SinkException {
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
            if (tmpFile.length() == 0) {
                throw new SinkException("No datablocks found");
            }
        } catch (IOException e) {
            throw new SinkException(e);
        }
    }

    private void deliverAsFtp(FtpPickup ftpPickup, File tmpFile, Long jobId) throws SinkException {
        try (BufferedInputStream dataBlockStream = new BufferedInputStream(new FileInputStream(tmpFile), 1024);) {
            ftpClient = open(ftpPickup);
            ftpClient.put(String.format("periodisk-job-%d.data", jobId), dataBlockStream, FtpClient.FileType.BINARY);
        } catch (IOException e) {
            throw new SinkException(e);
        } finally {
            ftpClient.close();
        }
    }

    private Chunk newResultChunk(Chunk chunk, FtpPickup ftpPickup) {
        final Chunk result = new Chunk(chunk.getJobId(), chunk.getChunkId(), Chunk.Type.DELIVERED);
        final ChunkItem chunkItem;
        chunkItem = ChunkItem.successfulChunkItem()
                .withData(String.format("Data for jobid '%d' delivered to ftp host '%s'", chunk.getJobId(), ftpPickup.getFtpHost()));
        result.insertItem(chunkItem
                .withId(0)
                .withType(ChunkItem.Type.STRING)
                .withEncoding(StandardCharsets.UTF_8));
        return result;
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