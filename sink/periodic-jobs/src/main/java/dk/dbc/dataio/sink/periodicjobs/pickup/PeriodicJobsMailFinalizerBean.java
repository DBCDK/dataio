package dk.dbc.dataio.sink.periodicjobs.pickup;

import dk.dbc.commons.jpa.ResultSet;
import dk.dbc.dataio.common.utils.io.UncheckedByteArrayOutputStream;
import dk.dbc.dataio.commons.macroexpansion.MacroSubstitutor;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.harvester.types.MailPickup;
import dk.dbc.dataio.sink.periodicjobs.GroupHeaderIncludePredicate;
import dk.dbc.dataio.sink.periodicjobs.I18n;
import dk.dbc.dataio.sink.periodicjobs.PeriodicJobsDataBlock;
import dk.dbc.dataio.sink.periodicjobs.PeriodicJobsDataBlockResultSetMapping;
import dk.dbc.dataio.sink.periodicjobs.PeriodicJobsDelivery;
import dk.dbc.dataio.sink.periodicjobs.SinkConfig;
import dk.dbc.util.Timed;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class PeriodicJobsMailFinalizerBean extends PeriodicJobsPickupFinalizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(PeriodicJobsMailFinalizerBean.class);

    Session mailSession;

    @Timed
    @Override
    public Chunk deliver(Chunk chunk, PeriodicJobsDelivery delivery, EntityManager entityManager) throws InvalidMessageException {
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
                content = datablocksMailBody(delivery, macroSubstitutor, entityManager);
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

    private String datablocksMailBody(PeriodicJobsDelivery delivery, MacroSubstitutor macroSubstitutor, EntityManager entityManager) throws InvalidMessageException {
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
             ResultSet<PeriodicJobsDataBlock> datablocks = new ResultSet<>(entityManager, getDataBlocksQuery,
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
            throw new InvalidMessageException(String.format("Unable to make mailbody. Jobid:%s", delivery.getJobId()), e);
        }
    }

    private void sendMail(MailPickup mailPickup, String content, MacroSubstitutor macroSubstitutor) throws InvalidMessageException {
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
            message.setFrom(SinkConfig.MAIL_FROM.asString());
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
                String filename =  mailPickup.getOverrideFilename() != null ?
                        mailPickup.getOverrideFilename() : "delivery.data";
                attachmentBodyPart.setFileName(String.format("%s.%s", macroSubstitutor.replace(filename),
                        filenameExtension));
                multipart.addBodyPart(attachmentBodyPart);
                message.setContent(multipart);
            } else {
                message.setText(content);
            }
            Transport.send(message);
        } catch (IOException | MessagingException e) {
            throw new InvalidMessageException(String.format("Unable to send mail bound for %s", mailPickup.getRecipients()), e);
        }
    }

    public PeriodicJobsMailFinalizerBean withSession(Session session) {
        this.mailSession = session;
        return this;
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
