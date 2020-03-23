package dk.dbc.dataio.sink.periodicjobs;

import dk.dbc.commons.jpa.ResultSet;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.harvester.types.MailPickup;
import dk.dbc.dataio.sink.types.SinkException;
import dk.dbc.util.Timed;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
        final String mailBody = buildMailBody(delivery);
        if (mailBody != null && !"".equals(mailBody.trim())) {
            deliverAsMail(mailPickup, mailBody);
            LOGGER.info("Delivered mail to {}. JobId:{}. ", mailPickup.getRecipients(), chunk.getJobId());
        }
        else {
            LOGGER.warn("Delivering using mail to {} skipped: No data to send, jobId:{}. ", mailPickup.getRecipients(), chunk.getJobId());
        }
        return newResultChunk(chunk, mailPickup);
    }

    private String buildMailBody(PeriodicJobsDelivery delivery) throws SinkException {
        String result;
        final Query getDataBlocksQuery = entityManager
                .createNamedQuery(PeriodicJobsDataBlock.GET_DATA_BLOCKS_QUERY_NAME)
                .setParameter(1, delivery.getJobId());
        try (final ByteArrayOutputStream datablocksOutputStream = new ByteArrayOutputStream();
             final ResultSet<PeriodicJobsDataBlock> datablocks = new ResultSet<>(entityManager, getDataBlocksQuery,
                     new PeriodicJobsDataBlockResultSetMapping());) {
            for (PeriodicJobsDataBlock datablock : datablocks) {
                datablocksOutputStream.write(datablock.getBytes());
            }
            datablocksOutputStream.flush();
            result = StringUtil.asString(datablocksOutputStream.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new SinkException(e);
        }
        return result;
    }

    private void deliverAsMail(MailPickup mailPickup, String mailBody) throws SinkException {
        MimeMessage message = new MimeMessage(mailSession);

        try {
            message.setText(mailBody);
            message.setRecipients(MimeMessage.RecipientType.TO, mailPickup.getRecipients());
            message.setSubject(mailPickup.getSubject());
            Transport.send(message);
        } catch (MessagingException e) {
            throw new SinkException(e);
        }

    }

    private Chunk newResultChunk(Chunk chunk, MailPickup mailPickup) {
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
