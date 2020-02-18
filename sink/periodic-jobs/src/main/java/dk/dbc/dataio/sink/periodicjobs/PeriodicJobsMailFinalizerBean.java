package dk.dbc.dataio.sink.periodicjobs;

import dk.dbc.commons.jpa.ResultSet;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.harvester.types.MailPickup;
import dk.dbc.dataio.sink.types.SinkException;
import dk.dbc.util.Timed;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Stateless
public class PeriodicJobsMailFinalizerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(PeriodicJobsMailFinalizerBean.class);

    public static final String ORIGIN = "dataio/sink/periodic-jobs";

    @PersistenceContext(unitName = "periodic-jobs_PU")
    EntityManager entityManager;

    @Resource(lookup = "mail/dataio/periodicjobs/delivery")
    Session mailSession;

    @Timed
    public Chunk deliver(Chunk chunk, PeriodicJobsDelivery delivery) throws SinkException {
        final MailPickup mailPickup = (MailPickup) delivery.getConfig().getContent().getPickup();
        File dataBlocksFile = null;
        try {
            dataBlocksFile = streamDatablocksToFile(delivery);
            deliverAsMail(mailPickup, dataBlocksFile.getAbsolutePath());
        }
        finally {
            if (dataBlocksFile != null) {
                dataBlocksFile.delete();
            }

        }
        return newResultChunk(chunk, mailPickup);
    }

    private File streamDatablocksToFile(PeriodicJobsDelivery delivery) throws SinkException {
        File datablocksFile = null;
        FileOutputStream datablocksOutputStream = null;
        try {
            datablocksFile = File.createTempFile("datablocks", ".tmp");
            datablocksOutputStream = new FileOutputStream(datablocksFile);
            final Query getDataBlocksQuery = entityManager
                    .createNamedQuery(PeriodicJobsDataBlock.GET_DATA_BLOCKS_QUERY_NAME)
                    .setParameter(1, delivery.getJobId());
            try (final ResultSet<PeriodicJobsDataBlock> datablocks = new ResultSet<>(entityManager, getDataBlocksQuery,
                    new PeriodicJobsDataBlockResultSetMapping())) {
                for (PeriodicJobsDataBlock datablock : datablocks) {
                    datablocksOutputStream.write(datablock.getBytes());
                    datablocksOutputStream.write("\n".getBytes());
                }
            }

        }
        catch (IOException e) {
            throw new SinkException(e);
        }
        finally {
            if (datablocksOutputStream != null) {
                try {
                    datablocksOutputStream.close();
                }
                catch (IOException e) {
                    throw new SinkException(e);
                }

            }
        }
        return datablocksFile;
    }

    private void deliverAsMail(MailPickup mailPickup, String dataBlocksFilename) throws SinkException {
        MimeMessage message = new MimeMessage(mailSession);
        /*
        Todo: Stream as bytes
         */
        try {
            message.setText(StringUtil.asString(Files.readAllBytes(Paths.get(dataBlocksFilename))));
            message.setRecipients(MimeMessage.RecipientType.TO, mailPickup.getRecipients());
            Transport.send(message);
        } catch (MessagingException | IOException e){
            throw new SinkException(e);
        }

    }

    private Chunk newResultChunk(Chunk chunk, MailPickup mailPickup){
        final Chunk result = new Chunk(chunk.getJobId(), chunk.getChunkId(), Chunk.Type.DELIVERED);
        final ChunkItem chunkItem;
        chunkItem = ChunkItem.successfulChunkItem()
                .withData(String.format("Mail sent to '{}' with subject '{}'", mailPickup.getRecipients(), mailPickup.getSubject()));
        result.insertItem(chunkItem
                .withId(0)
                .withType(ChunkItem.Type.STRING)
                .withEncoding(StandardCharsets.UTF_8));
        return result;
    }

}
