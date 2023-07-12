package dk.dbc.dataio.sink.periodicjobs.pickup;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.harvester.types.FtpPickup;
import dk.dbc.dataio.sink.periodicjobs.DatablocksLocalFileBuffer;
import dk.dbc.dataio.sink.periodicjobs.PeriodicJobsDelivery;
import dk.dbc.ftp.FtpClient;
import dk.dbc.proxy.ProxyBean;
import dk.dbc.util.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class PeriodicJobsFtpFinalizerBean extends PeriodicJobsPickupFinalizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(PeriodicJobsFtpFinalizerBean.class);

    ProxyBean proxyBean;

    public PeriodicJobsFtpFinalizerBean() {
    }

    @Timed
    @Override
    public Chunk deliver(Chunk chunk, PeriodicJobsDelivery delivery) throws InvalidMessageException {
        if (isEmptyJob(chunk)) {
            return deliverEmptyFile(chunk, delivery);
        }
        return deliverDatablocks(chunk, delivery);
    }

    private Chunk deliverEmptyFile(Chunk chunk, PeriodicJobsDelivery delivery) {
        String remoteFile = getRemoteFilename(delivery) + ".EMPTY";
        FtpPickup ftpPickup = (FtpPickup) delivery.getConfig().getContent().getPickup();
        FtpClient ftpClient = null;
        try {
            ftpClient = open(ftpPickup);
            ftpClient.put(remoteFile, "");
        } finally {
            if (ftpClient != null) {
                ftpClient.close();
            }
        }
        return newResultChunk(chunk,
                String.format("Empty file %s uploaded to ftp host '%s'", remoteFile, ftpPickup.getFtpHost()));
    }

    private Chunk deliverDatablocks(Chunk chunk, PeriodicJobsDelivery delivery) throws InvalidMessageException {
        String remoteFile = getRemoteFilename(delivery);
        FtpPickup ftpPickup = (FtpPickup) delivery.getConfig().getContent().getPickup();
        File localFile = null;
        try {
            localFile = File.createTempFile("dataBlocks", ".tmp.file");
            new DatablocksLocalFileBuffer()
                    .withTmpFile(localFile)
                    .withEntityManager(entityManager)
                    .withDelivery(delivery)
                    .withMacroSubstitutor(getMacroSubstitutor(delivery))
                    .createLocalFile();
            if (localFile.length() > 0) {
                uploadLocalFileToFtp(ftpPickup, localFile, remoteFile);
                LOGGER.info("jobId '{}' uploaded to ftp host '{}'.", chunk.getJobId(), ftpPickup.getFtpHost());
            } else {
                LOGGER.warn("jobId '{}' NOT uploaded to ftp host '{}' - no datablocks",
                        chunk.getJobId(), ftpPickup.getFtpHost());
            }
        } catch (IOException e) {
            throw new InvalidMessageException(String.format("Unable to deliver datablocks for chuk: %d/%d",
                    chunk.getJobId(), chunk.getChunkId()),e);
        } finally {
            if (localFile != null) {
                localFile.delete();
            }
        }
        return newResultChunk(chunk,
                String.format("File %s uploaded to ftp host '%s'", remoteFile, ftpPickup.getFtpHost()));
    }

    private void uploadLocalFileToFtp(FtpPickup ftpPickup, File local, String remote) throws InvalidMessageException {
        FtpClient ftpClient = null;
        try (BufferedInputStream dataBlockStream = new BufferedInputStream(new FileInputStream(local), 1024)) {
            ftpClient = open(ftpPickup);
            ftpClient.put(remote, dataBlockStream, FtpClient.FileType.BINARY);
        } catch (IOException e) {
            throw new InvalidMessageException(String.format("Unable to deliver file to: %s@%s",
                    ftpPickup.getFtpUser(), ftpPickup.getFtpHost()), e);
        } finally {
            if (ftpClient != null) {
                ftpClient.close();
            }
        }
    }

    private Chunk newResultChunk(Chunk chunk, String data) {
        Chunk result = new Chunk(chunk.getJobId(), chunk.getChunkId(), Chunk.Type.DELIVERED);
        ChunkItem chunkItem = ChunkItem.successfulChunkItem()
                .withId(0)
                .withType(ChunkItem.Type.JOB_END)
                .withData(data)
                .withEncoding(StandardCharsets.UTF_8);
        result.insertItem(chunkItem);
        return result;
    }

    FtpClient open(FtpPickup ftpPickup) {
        String host = ftpPickup.getFtpHost();
        String subDir = ftpPickup.getFtpSubdirectory();
        Proxy proxy = Optional.ofNullable(proxyBean)
                .filter(p -> p.useProxy(host))
                .map(ProxyBean::getJavaProxy)
                .orElse(Proxy.NO_PROXY);
        LOGGER.info("Opening ftp connection to: {}, using proxy: {}", ftpPickup, proxy);
        FtpClient ftpClient = new FtpClient()
                .withHost(host)
                .withPort(Integer.valueOf(ftpPickup.getFtpPort()))
                .withUsername(ftpPickup.getFtpUser())
                .withPassword(ftpPickup.getFtpPassword())
                .withProxy(proxy);
        if (subDir != null && !subDir.isEmpty()) {
            ftpClient.cd(subDir);
        }
        return ftpClient;
    }

    public PeriodicJobsFtpFinalizerBean withProxyBean(ProxyBean proxyBean) {
        this.proxyBean = proxyBean;
        return this;
    }
}
