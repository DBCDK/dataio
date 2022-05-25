package dk.dbc.dataio.jobstore.service.util;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.jobstore.service.entity.NotificationEntity;
import dk.dbc.dataio.jobstore.types.JobStoreException;

import javax.activation.DataHandler;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static dk.dbc.dataio.commons.types.Constants.MISSING_FIELD_VALUE;
import static dk.dbc.dataio.jobstore.service.util.JobInfoSnapshotConverter.toJobInfoSnapshot;

/**
 * Class wrapping a NotificationEntity instance as an email notification
 */
public class MailNotification {
    private static final String JOB_CREATED_OK_TEMPLATE = "/notifications/job_created_ok.template";
    private static final String JOB_CREATED_FAIL_TEMPLATE = "/notifications/job_created_fail.template";
    private static final String JOB_COMPLETED_TEMPLATE = "/notifications/job_completed.template";
    private static final String JOB_COMPLETED_WITH_FAILURES_TEMPLATE = "/notifications/job_completed_with_failures.template";
    private static final String INVALID_TRANSFILE_TEMPLATE = "/notifications/invalid_transfile.template";
    private static final String SUBJECT_FOR_JOB_CREATED = "DANBIB:postmester";
    private static final String SUBJECT_FOR_JOB_COMPLETED = "DANBIB:baseindlaeg";
    private static final String FROM_ADDRESS_PERSONAL_NAME = "DANBIB Fællesbruger";

    private final MailDestination mailDestination;
    private final NotificationEntity notification;
    private final StringBuilder builder;
    private final Map<String, String> overwrites;

    private Attachment attachment;

    public static boolean isUndefined(String value) {
        return value == null || value.trim().isEmpty() || value.equals(MISSING_FIELD_VALUE);
    }

    public MailNotification(MailDestination mailDestination, NotificationEntity notification) throws JobStoreException {
        this.mailDestination = mailDestination;
        this.notification = notification;
        this.overwrites = new HashMap<>();
        if (isUndefined(notification.getContent())) {
            format();
        }
        builder = new StringBuilder(notification.getContent());
    }

    /**
     * Formats and sends this email notification
     * @throws JobStoreException in case of error during formatting or sending
     */
    public void send() throws JobStoreException {
        try {
            // sends the e-mail
            notification.setDestination(mailDestination.toString());
            Transport.send(buildMimeMessage());
        } catch (Exception e) {
            throw new JobStoreException("Unable to send notification", e);
        }
    }

    public void append(JobExporter.FailedItemsContent failedItemsContent) {
        if (failedItemsContent.hasFatalItems()) {
            mailDestination.useFallbackDestination();
        }
        append(failedItemsContent.getContent().toByteArray());
    }

    public void append(byte[] addenda) {
        builder.append(new String(addenda, StandardCharsets.UTF_8));
    }

    public void attach(Attachment attachment) {
        this.attachment = attachment;
    }

    private void format() throws JobStoreException {
        final JSONBContext jsonbContext = new JSONBContext();
        final JsonValueTemplateEngine templateEngine = new JsonValueTemplateEngine(jsonbContext);
        try {
            switch (notification.getType()) {
                case JOB_CREATED:
                case JOB_COMPLETED:
                    overwriteJobIdIfPreviousJobIdExists();
                    notification.setContent(templateEngine.apply(getNotificationTemplate(),
                            jsonbContext.marshall(toJobInfoSnapshot(notification.getJob())), overwrites));

                    break;
                default:
                    notification.setContent(templateEngine.apply(getNotificationTemplate(),
                            notification.getContext()));
            }
        } catch (JSONBException e) {
            throw new JobStoreException("Unable to marshall linked job", e);
        }
    }

    private void overwriteJobIdIfPreviousJobIdExists() {
        final JobSpecification.Ancestry ancestry = notification.getJob().getSpecification().getAncestry();
        if (ancestry != null && ancestry.getPreviousJobId() > 0) {
            final String overwrite = String.format("%d (genkørsel af job %d)", notification.getJobId(),
                    ancestry.getPreviousJobId());
            overwrites.put("jobId", overwrite);
        }
    }

    private String getNotificationTemplate() {
        final String resource;
        switch (notification.getType()) {
            case INVALID_TRANSFILE:
                resource = INVALID_TRANSFILE_TEMPLATE;
                break;
            case JOB_COMPLETED:
                if (notification.getJob().hasFailedItems()) {
                    resource = JOB_COMPLETED_WITH_FAILURES_TEMPLATE;
                } else {
                    resource = JOB_COMPLETED_TEMPLATE;
                }
                break;
            default:
                if (notification.getJob().hasFatalDiagnostics()) {
                    resource = JOB_CREATED_FAIL_TEMPLATE;
                } else {
                    resource = JOB_CREATED_OK_TEMPLATE;
                }
                break;
        }
        return getNotificationTemplateResource(resource);
    }

    private String getNotificationTemplateResource(String resource) {
        final StringBuilder buffer = new StringBuilder();
        try (
                InputStreamReader isr = new InputStreamReader(getClass().getResourceAsStream(resource), StandardCharsets.UTF_8);
                BufferedReader br = new BufferedReader(isr)) {
            for (int c = br.read(); c != -1; c = br.read())
                buffer.append((char) c);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return buffer.toString();
    }

    private MimeMessage buildMimeMessage() throws MessagingException, IOException {
        final MimeMessage message = new MimeMessage(mailDestination.getMailSession());
        message.setFrom(new InternetAddress(
                mailDestination.getMailSession().getProperty("mail.from"), FROM_ADDRESS_PERSONAL_NAME));
        message.setRecipients(Message.RecipientType.TO, mailDestination.getToAddresses());
        message.setSentDate(new Date());

        switch (notification.getType()) {
            case JOB_COMPLETED:
                message.setSubject(SUBJECT_FOR_JOB_COMPLETED);
                break;
            default:
                message.setSubject(SUBJECT_FOR_JOB_CREATED);
                break;
        }

        final DataHandler messageDataHandler = new DataHandler(builder.toString(), "text/plain; charset=UTF-8");
        if (attachment != null) {
            message.setContent(buildMimeMultipart(messageDataHandler));
        } else {
            message.setDataHandler(messageDataHandler);
        }
        message.saveChanges();
        return message;
    }

    private Multipart buildMimeMultipart(DataHandler messageDataHandler) throws MessagingException {

        // Creating body part for mail content
        final BodyPart mailContent = new MimeBodyPart();
        mailContent.setDataHandler(messageDataHandler);

        // Creating body part for mail attachment
        final MimeBodyPart mailAttachment = new MimeBodyPart();
        final ByteArrayDataSource attachmentDataSource = new ByteArrayDataSource(attachment.getContent(), attachment.getContentType());
        mailAttachment.setDataHandler(new DataHandler(attachmentDataSource));
        mailAttachment.setDisposition(Part.ATTACHMENT);
        mailAttachment.setFileName(attachment.getFileName());

        // Creating MimeMultipart
        final MimeMultipart multipart = new MimeMultipart();
        multipart.addBodyPart(mailContent);
        multipart.addBodyPart(mailAttachment);
        return multipart;
    }
}
