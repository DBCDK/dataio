package dk.dbc.dataio.sink.periodicjobs;

import dk.dbc.commons.jpa.ResultSet;
import dk.dbc.dataio.common.utils.io.UncheckedByteArrayOutputStream;
import dk.dbc.dataio.commons.macroexpansion.MacroSubstitutor;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.harvester.types.MailPickup;
import dk.dbc.dataio.sink.types.SinkException;
import dk.dbc.util.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.persistence.Query;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Stateless
public class PeriodicJobsMailFinalizerBean extends PeriodicJobsPickupFinalizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(PeriodicJobsMailFinalizerBean.class);

    @Resource(lookup = "mail/dataio/periodicjobs/delivery")
    Session mailSession;

    @Timed
    @Override
    public Chunk deliver(Chunk chunk, PeriodicJobsDelivery delivery) throws SinkException {
        final MacroSubstitutor macroSubstitutor = getMacroSubstitutor(delivery);
        final MailPickup mailPickup = (MailPickup) delivery.getConfig().getContent().getPickup();

        try {
            InternetAddress.parse(mailPickup.getRecipients());
        } catch (AddressException e) {
            return newFailedResultChunk(chunk, "Invalid mail recipient: " + e.getMessage());
        }

        final String content;
        if (isEmptyJob(chunk)) {
            content = I18n.get("mail.empty_job.body");
        } else {
            try {
                content = datablocksMailBody(delivery, macroSubstitutor);
            } catch (IllegalStateException e) {
                return newFailedResultChunk(chunk, "IllegalStateException: " + e.getMessage());
            }
        }
        if (!content.trim().isEmpty()) {
            sendMail(mailPickup, content, macroSubstitutor);
            LOGGER.info("Job {}: mail sent to {}", chunk.getJobId(), mailPickup.getRecipients());
        } else {
            LOGGER.warn("Job {}: no mail sent", chunk.getJobId());
        }
        return newResultChunk(chunk, mailPickup);
    }

    private String datablocksMailBody(PeriodicJobsDelivery delivery, MacroSubstitutor macroSubstitutor) throws SinkException {
        final GroupHeaderIncludePredicate groupHeaderIncludePredicate = new GroupHeaderIncludePredicate();
        final MailPickup mailPickup = (MailPickup) delivery.getConfig().getContent().getPickup();
        String contentHeader = delivery.getConfig().getContent().getPickup().getContentHeader();
        String contentFooter = delivery.getConfig().getContent().getPickup().getContentFooter();

        if (contentHeader != null) {
            contentHeader = macroSubstitutor.replace(contentHeader);
        } else {
            contentHeader = "";
        }

        if (contentFooter != null) {
            contentFooter = macroSubstitutor.replace(contentFooter);
        } else {
            contentFooter = "";
        }

        final Integer recordLimit = mailPickup.getRecordLimit();
        int recordCount = 0;

        final Query getDataBlocksQuery = entityManager
                .createNamedQuery(PeriodicJobsDataBlock.GET_DATA_BLOCKS_QUERY_NAME)
                .setParameter(1, delivery.getJobId());
        try (UncheckedByteArrayOutputStream datablocksOutputStream = new UncheckedByteArrayOutputStream();
             final ResultSet<PeriodicJobsDataBlock> datablocks = new ResultSet<>(entityManager, getDataBlocksQuery,
                     new PeriodicJobsDataBlockResultSetMapping())) {
            datablocksOutputStream.write(contentHeader.getBytes());
            for (PeriodicJobsDataBlock datablock : datablocks) {
                if (groupHeaderIncludePredicate.test(datablock)) {
                    datablocksOutputStream.write(datablock.getGroupHeader());
                }
                datablocksOutputStream.write(datablock.getBytes());
                if (recordLimit != null) {
                    recordCount++;
                    if (recordCount > recordLimit) {
                        throw new IllegalStateException("Record count exceeded record limit of " +
                                mailPickup.getRecordLimit());
                    }
                }
            }
            datablocksOutputStream.write(contentFooter.getBytes());
            datablocksOutputStream.flush();
            return StringUtil.asString(datablocksOutputStream.toByteArray(), StandardCharsets.UTF_8);

        } catch (IOException e) {
            throw new SinkException(e);
        }
    }

    private void sendMail(MailPickup mailPickup, String content, MacroSubstitutor macroSubstitutor) throws SinkException {
        final MimeMessage message = new MimeMessage(mailSession);
        final String mimeType = mailPickup.getMimetype();

        try {
            String subject = mailPickup.getSubject();
            if (subject != null) {
                subject = macroSubstitutor.replace(subject);
            }
            String body = mailPickup.getBody();
            if (body != null) {
                body = macroSubstitutor.replace(body);
            }
            message.setRecipients(MimeMessage.RecipientType.TO, mailPickup.getRecipients());
            message.setSubject(subject);
            if (mimeType != null && !mimeType.isEmpty()) {
                final Multipart multipart = new MimeMultipart();
                final MimeBodyPart attachmentBodyPart = new MimeBodyPart();
                final MimeBodyPart textBodyPart = new MimeBodyPart();
                textBodyPart.setText(body != null ? body : I18n.get("mail.attachment.body"));
                multipart.addBodyPart(textBodyPart);
                final DataSource dataSource = new ByteArrayDataSource(content, mailPickup.getMimetype());
                attachmentBodyPart.setDataHandler(new DataHandler(dataSource));
                final String filenameExtension;
                if (mimeType.split("/").length > 1) {
                    filenameExtension = mimeType.split("/")[1];
                } else {
                    filenameExtension = mimeType;
                }
                attachmentBodyPart.setFileName(String.format("%s.%s", "delivery.data", filenameExtension));
                multipart.addBodyPart(attachmentBodyPart);
                message.setContent(multipart);
            } else {
                message.setText(content);
            }
            Transport.send(message);
        } catch (MessagingException | IOException e) {
            throw new SinkException(e);
        }
    }

    private Chunk newResultChunk(Chunk chunk, MailPickup mailPickup) {
        final Chunk result = new Chunk(chunk.getJobId(), chunk.getChunkId(), Chunk.Type.DELIVERED);
        final ChunkItem chunkItem = ChunkItem.successfulChunkItem()
                .withId(0)
                .withType(ChunkItem.Type.JOB_END)
                .withEncoding(StandardCharsets.UTF_8)
                .withData(String.format("Mail sent to '%s' with subject '%s'",
                        mailPickup.getRecipients(), mailPickup.getSubject()));
        result.insertItem(chunkItem);
        return result;
    }

    private Chunk newFailedResultChunk(Chunk chunk, String cause) {
        final Chunk result = new Chunk(chunk.getJobId(), chunk.getChunkId(), Chunk.Type.DELIVERED);
        final ChunkItem chunkItem = ChunkItem.failedChunkItem()
                .withId(0)
                .withType(ChunkItem.Type.JOB_END)
                .withEncoding(StandardCharsets.UTF_8)
                .withData(cause);
        result.insertItem(chunkItem);
        return result;
    }
}
