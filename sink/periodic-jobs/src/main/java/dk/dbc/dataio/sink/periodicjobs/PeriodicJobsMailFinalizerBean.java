package dk.dbc.dataio.sink.periodicjobs;

import dk.dbc.commons.jpa.ResultSet;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.harvester.types.MailPickup;
import dk.dbc.dataio.sink.types.SinkException;
import dk.dbc.util.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Stateless
public class PeriodicJobsMailFinalizerBean implements PeriodicJobsPickupFinalizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(PeriodicJobsMailFinalizerBean.class);

    @PersistenceContext(unitName = "periodic-jobs_PU")
    EntityManager entityManager;

    @Resource(lookup = "mail/dataio/periodicjobs/delivery")
    Session mailSession;

    @EJB public JobStoreServiceConnectorBean jobStoreServiceConnectorBean;

    @Timed
    @Override
    public Chunk deliver(Chunk chunk, PeriodicJobsDelivery delivery) throws SinkException {
        final MailPickup mailPickup = (MailPickup) delivery.getConfig().getContent().getPickup();
        String mailBody;
        if (isEmptyJob(chunk, jobStoreServiceConnectorBean.getConnector())) {
            mailBody = emptyJobMailBody();
        } else {
            mailBody = datablocksMailBody(delivery);
        }
        if (mailBody != null && !mailBody.trim().isEmpty()) {
            sendMail(mailPickup, mailBody);
            LOGGER.info("Job {}: mail sent to {}", chunk.getJobId(), mailPickup.getRecipients());
        } else {
            LOGGER.warn("Job {}: no mail sent", chunk.getJobId());
        }
        return newResultChunk(chunk, mailPickup);
    }

    private String emptyJobMailBody() {
        return "Periodisk job fandt ingen nye poster";
    }

    private String datablocksMailBody(PeriodicJobsDelivery delivery) throws SinkException {
        final Query getDataBlocksQuery = entityManager
                .createNamedQuery(PeriodicJobsDataBlock.GET_DATA_BLOCKS_QUERY_NAME)
                .setParameter(1, delivery.getJobId());
        try (final ByteArrayOutputStream datablocksOutputStream = new ByteArrayOutputStream();
             final ResultSet<PeriodicJobsDataBlock> datablocks = new ResultSet<>(entityManager, getDataBlocksQuery,
                     new PeriodicJobsDataBlockResultSetMapping())) {
            for (PeriodicJobsDataBlock datablock : datablocks) {
                datablocksOutputStream.write(datablock.getBytes());
            }
            datablocksOutputStream.flush();
            return StringUtil.asString(datablocksOutputStream.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new SinkException(e);
        }
    }

    private void sendMail(MailPickup mailPickup, String mailBody) throws SinkException {
        final MimeMessage message = new MimeMessage(mailSession);
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
        final ChunkItem chunkItem = ChunkItem.successfulChunkItem()
                .withId(0)
                .withType(ChunkItem.Type.STRING)
                .withEncoding(StandardCharsets.UTF_8)
                .withData(String.format("Mail sent to '%s' with subject '%s'",
                        mailPickup.getRecipients(), mailPickup.getSubject()));
        result.insertItem(chunkItem);
        return result;
    }
}
