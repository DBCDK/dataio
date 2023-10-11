package dk.dbc.dataio.sink.periodicjobs.pickup;

import dk.dbc.commons.sftpclient.SFTPConfig;
import dk.dbc.commons.sftpclient.SFtpClient;
import dk.dbc.commons.sftpclient.SFtpClientException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.harvester.types.SFtpPickup;
import dk.dbc.dataio.sink.periodicjobs.DatablocksLocalFileBuffer;
import dk.dbc.dataio.sink.periodicjobs.PeriodicJobsDelivery;
import dk.dbc.proxy.ProxyBean;
import dk.dbc.util.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public class PeriodicJobsSFtpFinalizerBean extends PeriodicJobsPickupFinalizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(PeriodicJobsSFtpFinalizerBean.class);

    ProxyBean proxyBean;

    @Timed
    @Override
    public Chunk deliver(Chunk chunk, PeriodicJobsDelivery delivery) throws InvalidMessageException {
        if (isEmptyJob(chunk)) {
            return deliverEmptyFile(chunk, delivery);
        }
        return deliverDatablocks(chunk, delivery);
    }

    private Chunk deliverEmptyFile(Chunk chunk, PeriodicJobsDelivery delivery) {
        final String remoteFile = getRemoteFilename(delivery) + ".EMPTY";
        final SFtpPickup sFtpPickup = (SFtpPickup) delivery.getConfig().getContent().getPickup();
        try (SFtpClient sFtpClient = open(sFtpPickup)) {
            sFtpClient.putContent(remoteFile, new ByteArrayInputStream("".getBytes()));
        }
        return newResultChunk(chunk,
                String.format("Empty file %s uploaded to sftp host '%s'", remoteFile, sFtpPickup.getsFtpHost()));
    }

    private Chunk deliverDatablocks(Chunk chunk, PeriodicJobsDelivery delivery) throws InvalidMessageException {
        final String remoteFile = getRemoteFilename(delivery);
        final SFtpPickup sftpPickup = (SFtpPickup) delivery.getConfig().getContent().getPickup();
        File localFile = null;
        try {
            localFile = File.createTempFile("dataBlocks", ".tmp.file");
            new DatablocksLocalFileBuffer()
                    .withDelivery(delivery)
                    .withEntityManager(entityManager)
                    .withTmpFile(localFile)
                    .withMacroSubstitutor(getMacroSubstitutor(delivery))
                    .createLocalFile();
            if (localFile.length() > 0) {
                uploadLocalFileToSFtp(sftpPickup, localFile, remoteFile);
                LOGGER.info("jobId '{}' uploaded to sftp host '{}'.", chunk.getJobId(), sftpPickup.getsFtpHost());
            } else {
                LOGGER.warn("jobId '{}' NOT uploaded to sftp host '{}' - no datablocks",
                        chunk.getJobId(), sftpPickup.getsFtpHost());
            }
        } catch (IOException e) {
            throw new InvalidMessageException(String.format("Unable to deliver datablocks for:%d", delivery.getJobId()),e);
        } finally {
            if (localFile != null) {
                localFile.delete();
            }
        }
        return newResultChunk(chunk,
                String.format("File %s uploaded to sftp host '%s'", remoteFile, sftpPickup.getsFtpHost()));
    }

    private SFtpClient open(SFtpPickup sFtpPickup) {
        return new SFtpClient(
                new SFTPConfig()
                .withHost(sFtpPickup.getsFtpHost())
                .withPort(Integer.parseInt(sFtpPickup.getsFtpPort()))
                .withUsername(sFtpPickup.getsFtpUser())
                .withPassword(sFtpPickup.getsFtpPassword())
                .withDir(sFtpPickup.getsFtpSubdirectory()),
                proxyBean.getProxy(),
                proxyBean.getNonProxyHosts() == null ? Set.of() : proxyBean.getNonProxyHosts());
    }

    private void uploadLocalFileToSFtp(SFtpPickup sFtpPickup, File local, String remote) throws InvalidMessageException {
        try (BufferedInputStream dataBlockStream = new BufferedInputStream(new FileInputStream(local), 1024);
             SFtpClient sFtpClient = open(sFtpPickup)) {
             sFtpClient.putContent(remote, dataBlockStream);
        } catch (IOException | SFtpClientException e) {
            throw new InvalidMessageException(String.format("Unable to upload file to: %s@%s",
                    sFtpPickup.getsFtpUser(), sFtpPickup.getsFtpHost()), e);
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

    public PeriodicJobsSFtpFinalizerBean withProxyBean(ProxyBean proxyBean) {
        this.proxyBean = proxyBean;
        return this;
    }

}
