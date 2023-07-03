package dk.dbc.dataio.sink.periodicjobs;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.harvester.types.MailPickup;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import dk.dbc.weekresolver.WeekResolverConnector;
import dk.dbc.weekresolver.WeekResolverConnectorException;
import dk.dbc.weekresolver.WeekResolverResult;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMultipart;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.mock_javamail.Mailbox;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PeriodicJobsMailFinalizerBeanIT extends IntegrationTest {
    private final String mailFrom = "dataio@dbc.dk";
    private final String recipients = "someone_out_there@outthere.dk";
    private final String subject = "delivery";

    private final JobStoreServiceConnector jobStoreServiceConnector =
            mock(JobStoreServiceConnector.class);
    private final JobStoreServiceConnectorBean jobStoreServiceConnectorBean =
            mock(JobStoreServiceConnectorBean.class);
    private final WeekResolverConnector weekResolverConnector =
            mock(WeekResolverConnector.class);

    @Before
    public void setupMocks() {
        Mailbox.clearAll();

        when(jobStoreServiceConnectorBean.getConnector())
                .thenReturn(jobStoreServiceConnector);
    }

    @Test
    public void deliver_onNonEmptyJobNoDatablocks() throws AddressException {
        final int jobId = 42;
        final PeriodicJobsDelivery delivery = new PeriodicJobsDelivery(jobId);
        delivery.setConfig(new PeriodicJobsHarvesterConfig(1, 1,
                new PeriodicJobsHarvesterConfig.Content()
                        .withTimeOfLastHarvest(new Date())
                        .withName("Deliver test")
                        .withSubmitterNumber("111111")
                        .withPickup(new MailPickup()
                                .withRecipients(recipients)
                                .withSubject(subject))));
        final Chunk chunk = new Chunk(jobId, 3, Chunk.Type.PROCESSED);
        final PeriodicJobsMailFinalizerBean periodicJobsMailFinalizerBean = newPeriodicJobsMailFinalizerBean();
        env().getPersistenceContext().run(() ->
                periodicJobsMailFinalizerBean.deliver(chunk, delivery));
        Mailbox inbox = Mailbox.get("someone_out_there@outthere.dk");
        assertThat("Inbox size", inbox.size(), is(0));
    }

    @Test
    public void deliver_onNonEmptyJob() throws IOException, MessagingException, AddressException {
        final int jobId = 42;
        final PeriodicJobsDataBlock block0 = new PeriodicJobsDataBlock();
        block0.setKey(new PeriodicJobsDataBlock.Key(jobId, 0, 0));
        block0.setSortkey("000000000");
        block0.setBytes(StringUtil.asBytes("0\n"));
        block0.setGroupHeader(StringUtil.asBytes("groupA\n"));
        final PeriodicJobsDataBlock block1 = new PeriodicJobsDataBlock();
        block1.setKey(new PeriodicJobsDataBlock.Key(jobId, 1, 0));
        block1.setSortkey("000000001");
        block1.setBytes(StringUtil.asBytes("1\n"));
        final PeriodicJobsDataBlock block2 = new PeriodicJobsDataBlock();
        block2.setKey(new PeriodicJobsDataBlock.Key(jobId, 2, 0));
        block2.setSortkey("000000002");
        block2.setBytes(StringUtil.asBytes("2\n"));
        block2.setGroupHeader(StringUtil.asBytes("groupB\n"));

        env().getPersistenceContext().run(() -> {
            env().getEntityManager().persist(block2);
            env().getEntityManager().persist(block1);
            env().getEntityManager().persist(block0);
        });

        final PeriodicJobsDelivery delivery = new PeriodicJobsDelivery(jobId);
        delivery.setConfig(new PeriodicJobsHarvesterConfig(1, 1,
                new PeriodicJobsHarvesterConfig.Content()
                        .withTimeOfLastHarvest(new Date())
                        .withName("Deliver test")
                        .withSubmitterNumber("111111")
                        .withPickup(new MailPickup()
                                .withRecipients(recipients)
                                .withSubject(subject)
                                .withRecordLimit(3))));
        final Chunk chunk = new Chunk(jobId, 3, Chunk.Type.PROCESSED);
        final PeriodicJobsMailFinalizerBean periodicJobsMailFinalizerBean = newPeriodicJobsMailFinalizerBean();
        env().getPersistenceContext().run(() ->
                periodicJobsMailFinalizerBean.deliver(chunk, delivery));
        Mailbox inbox = Mailbox.get("someone_out_there@outthere.dk");
        assertThat("Inbox size", inbox.size(), is(1));
        Message receivedMail = inbox.get(0);
        assertThat("Recipients is ok", receivedMail.getAllRecipients(),
                is(new InternetAddress[]{new InternetAddress(recipients)}));
        assertThat("Mail content is intact", receivedMail.getContent(), is("groupA\n0\n1\ngroupB\n2\n"));
    }

    @Test
    public void deliver_onEmptyJob() throws MessagingException, IOException, MessagingException {
        final int jobId = 42;

        final PeriodicJobsDelivery delivery = new PeriodicJobsDelivery(jobId);
        delivery.setConfig(new PeriodicJobsHarvesterConfig(1, 1,
                new PeriodicJobsHarvesterConfig.Content()
                        .withTimeOfLastHarvest(new Date())
                        .withName("Deliver test")
                        .withSubmitterNumber("111111")
                        .withPickup(new MailPickup()
                                .withRecipients(recipients)
                                .withSubject(subject))));
        final Chunk chunk = new Chunk(jobId, 0, Chunk.Type.PROCESSED);
        final PeriodicJobsMailFinalizerBean periodicJobsMailFinalizerBean = newPeriodicJobsMailFinalizerBean();
        env().getPersistenceContext().run(() ->
                periodicJobsMailFinalizerBean.deliver(chunk, delivery));
        Mailbox inbox = Mailbox.get("someone_out_there@outthere.dk");
        assertThat("Inbox size", inbox.size(), is(1));
        Message receivedMail = inbox.get(0);
        assertThat("mail recipients", receivedMail.getAllRecipients(),
                is(new InternetAddress[]{new InternetAddress(recipients)}));
        assertThat("mail body", receivedMail.getContent(),
                is("Periodisk job fandt ingen nye poster"));
    }

    @Test
    public void onInvalidMailRecipients() {
        final int jobId = 42;

        final PeriodicJobsDelivery delivery = new PeriodicJobsDelivery(jobId);
        delivery.setConfig(new PeriodicJobsHarvesterConfig(1, 1,
                new PeriodicJobsHarvesterConfig.Content()
                        .withTimeOfLastHarvest(new Date())
                        .withName("Deliver test")
                        .withSubmitterNumber("111111")
                        .withPickup(new MailPickup()
                                .withRecipients("not a valid email address")
                                .withSubject(subject))));
        final Chunk chunk = new Chunk(jobId, 0, Chunk.Type.PROCESSED);
        final PeriodicJobsMailFinalizerBean periodicJobsMailFinalizerBean = newPeriodicJobsMailFinalizerBean();
        final Chunk result = env().getPersistenceContext().run(() ->
                periodicJobsMailFinalizerBean.deliver(chunk, delivery));
        assertThat(result.getItems().get(0).getStatus(), is(ChunkItem.Status.FAILURE));
    }

    @Test
    public void weekcodeInMailSubject() throws WeekResolverConnectorException, MessagingException {
        final WeekResolverResult weekResolverResult = new WeekResolverResult();
        weekResolverResult.setYear(2020);
        weekResolverResult.setWeekNumber(31);
        when(weekResolverConnector.getCurrentWeekCode(eq("EMO"), any(LocalDate.class))).thenReturn(weekResolverResult);

        final int jobId = 42;
        final PeriodicJobsDelivery delivery = new PeriodicJobsDelivery(jobId);
        delivery.setConfig(new PeriodicJobsHarvesterConfig(1, 1,
                new PeriodicJobsHarvesterConfig.Content()
                        .withTimeOfLastHarvest(new Date())
                        .withName("Deliver test")
                        .withSubmitterNumber("111111")
                        .withPickup(new MailPickup()
                                .withRecipients(recipients)
                                .withSubject("mail for week ${__WEEKCODE_EMO__}"))));
        final Chunk chunk = new Chunk(jobId, 0, Chunk.Type.PROCESSED);
        final PeriodicJobsMailFinalizerBean periodicJobsMailFinalizerBean = newPeriodicJobsMailFinalizerBean();
        env().getPersistenceContext().run(() ->
                periodicJobsMailFinalizerBean.deliver(chunk, delivery));
        final List<Message> inbox = Mailbox.get("someone_out_there@outthere.dk");
        assertThat("Inbox size", inbox.size(), is(1));
        final Message receivedMail = inbox.get(0);
        assertThat("mail subject", receivedMail.getSubject(), is("mail for week 202031"));
    }

    @Test
    public void deliver_file_with_header_and_footer() throws MessagingException, IOException, WeekResolverConnectorException {
        final WeekResolverResult weekResolverResult = new WeekResolverResult();
        weekResolverResult.setYear(2020);
        weekResolverResult.setWeekNumber(41);
        when(weekResolverConnector.getCurrentWeekCode(eq("EMO"), any(LocalDate.class))).thenReturn(weekResolverResult);
        final int jobId = 42;
        final PeriodicJobsDataBlock block0 = new PeriodicJobsDataBlock();
        block0.setKey(new PeriodicJobsDataBlock.Key(jobId, 0, 0));
        block0.setSortkey("000000000");
        block0.setBytes(StringUtil.asBytes("0\n"));
        block0.setGroupHeader(StringUtil.asBytes("groupA\n"));
        final PeriodicJobsDataBlock block1 = new PeriodicJobsDataBlock();
        block1.setKey(new PeriodicJobsDataBlock.Key(jobId, 1, 0));
        block1.setSortkey("000000001");
        block1.setBytes(StringUtil.asBytes("1\n"));
        final PeriodicJobsDataBlock block2 = new PeriodicJobsDataBlock();
        block2.setKey(new PeriodicJobsDataBlock.Key(jobId, 2, 0));
        block2.setSortkey("000000002");
        block2.setBytes(StringUtil.asBytes("2\n"));
        block2.setGroupHeader(StringUtil.asBytes("groupB\n"));

        env().getPersistenceContext().run(() -> {
            env().getEntityManager().persist(block2);
            env().getEntityManager().persist(block1);
            env().getEntityManager().persist(block0);
        });

        final PeriodicJobsDelivery delivery = new PeriodicJobsDelivery(jobId);
        delivery.setConfig(new PeriodicJobsHarvesterConfig(1, 1,
                new PeriodicJobsHarvesterConfig.Content()
                        .withTimeOfLastHarvest(new Date())
                        .withName("Deliver test")
                        .withSubmitterNumber("111111")
                        .withPickup(new MailPickup()
                                .withRecipients(recipients)
                                .withSubject(subject)
                                .withContentHeader("Ugekorrektur uge ${__WEEKCODE_EMO__}\n")
                                .withContentFooter("\nslut"))));
        final Chunk chunk = new Chunk(jobId, 3, Chunk.Type.PROCESSED);
        final PeriodicJobsMailFinalizerBean periodicJobsMailFinalizerBean = newPeriodicJobsMailFinalizerBean();
        env().getPersistenceContext().run(() ->
                periodicJobsMailFinalizerBean.deliver(chunk, delivery));
        List<Message> inbox = Mailbox.get("someone_out_there@outthere.dk");
        assertThat("Inbox size", inbox.size(), is(1));
        Message receivedMail = inbox.get(0);
        assertThat("Recipients is ok", receivedMail.getAllRecipients(),
                is(new InternetAddress[]{new InternetAddress(recipients)}));
        assertThat("Mail content is intact", receivedMail.getContent(), is("Ugekorrektur uge 202041\ngroupA\n0\n1\ngroupB\n2\n\nslut"));
    }

    @Test
    public void deliver_mail_as_attachment() throws MessagingException, IOException, WeekResolverConnectorException {
        final WeekResolverResult weekResolverResult = new WeekResolverResult();
        weekResolverResult.setYear(2020);
        weekResolverResult.setWeekNumber(41);
        when(weekResolverConnector.getCurrentWeekCode(eq("EMO"), any(LocalDate.class))).thenReturn(weekResolverResult);
        final int jobId = 42;
        final PeriodicJobsDataBlock block0 = new PeriodicJobsDataBlock();
        block0.setKey(new PeriodicJobsDataBlock.Key(jobId, 0, 0));
        block0.setSortkey("000000000");
        block0.setBytes(StringUtil.asBytes("0\n"));
        block0.setGroupHeader(StringUtil.asBytes("groupA\n"));
        final PeriodicJobsDataBlock block1 = new PeriodicJobsDataBlock();
        block1.setKey(new PeriodicJobsDataBlock.Key(jobId, 1, 0));
        block1.setSortkey("000000001");
        block1.setBytes(StringUtil.asBytes("1\n"));
        final PeriodicJobsDataBlock block2 = new PeriodicJobsDataBlock();
        block2.setKey(new PeriodicJobsDataBlock.Key(jobId, 2, 0));
        block2.setSortkey("000000002");
        block2.setBytes(StringUtil.asBytes("2\n"));
        block2.setGroupHeader(StringUtil.asBytes("groupB\n"));

        env().getPersistenceContext().run(() -> {
            env().getEntityManager().persist(block2);
            env().getEntityManager().persist(block1);
            env().getEntityManager().persist(block0);
        });

        final PeriodicJobsDelivery delivery = new PeriodicJobsDelivery(jobId);
        delivery.setConfig(new PeriodicJobsHarvesterConfig(1, 1,
                new PeriodicJobsHarvesterConfig.Content()
                        .withTimeOfLastHarvest(new Date())
                        .withName("Deliver test")
                        .withSubmitterNumber("111111")
                        .withPickup(new MailPickup()
                                .withRecipients(recipients)
                                .withSubject(subject)
                                .withMimetype("text/html")
                                .withContentHeader("Ugekorrektur uge ${__WEEKCODE_EMO__}\n")
                                .withContentFooter("\nslut"))));
        final Chunk chunk = new Chunk(jobId, 3, Chunk.Type.PROCESSED);
        final PeriodicJobsMailFinalizerBean periodicJobsMailFinalizerBean = newPeriodicJobsMailFinalizerBean();
        env().getPersistenceContext().run(() ->
                periodicJobsMailFinalizerBean.deliver(chunk, delivery));
        List<Message> inbox = Mailbox.get("someone_out_there@outthere.dk");
        assertThat("Inbox size", inbox.size(), is(1));
        Message receivedMail = inbox.get(0);
        assertThat("Recipients is ok", receivedMail.getAllRecipients(),
                is(new InternetAddress[]{new InternetAddress(recipients)}));
        MimeMultipart mimeMultipart = (MimeMultipart) receivedMail.getContent();
        String defaultText = (String) mimeMultipart.getBodyPart(0).getContent();
        String attachmentAsText = (String) mimeMultipart.getBodyPart(1).getContent();

        assertThat("Mail text message is default message", defaultText,
                is("Se vedhæftede fil for resultat af kørslen."));

        assertThat("Mail attachment as text is expected", attachmentAsText,
                is("Ugekorrektur uge 202041\ngroupA\n0\n1\ngroupB\n2\n\nslut"));
    }

    @Test
    public void deliver_mail_as_configured_body_with_attachment()
            throws MessagingException, IOException, WeekResolverConnectorException {
        final WeekResolverResult weekResolverResult = new WeekResolverResult();
        weekResolverResult.setYear(2020);
        weekResolverResult.setWeekNumber(41);
        when(weekResolverConnector.getCurrentWeekCode(eq("EMO"), any(LocalDate.class))).thenReturn(weekResolverResult);
        final int jobId = 42;
        final PeriodicJobsDataBlock block0 = new PeriodicJobsDataBlock();
        block0.setKey(new PeriodicJobsDataBlock.Key(jobId, 0, 0));
        block0.setSortkey("000000000");
        block0.setBytes(StringUtil.asBytes("0\n"));

        env().getPersistenceContext().run(() -> {
            env().getEntityManager().persist(block0);
        });

        final PeriodicJobsDelivery delivery = new PeriodicJobsDelivery(jobId);
        delivery.setConfig(new PeriodicJobsHarvesterConfig(1, 1,
                new PeriodicJobsHarvesterConfig.Content()
                        .withTimeOfLastHarvest(new Date())
                        .withName("Deliver test")
                        .withSubmitterNumber("111111")
                        .withPickup(new MailPickup()
                                .withRecipients(recipients)
                                .withSubject(subject)
                                .withMimetype("text/html")
                                .withBody("Ugekorrektur uge ${__WEEKCODE_EMO__}"))));
        final Chunk chunk = new Chunk(jobId, 3, Chunk.Type.PROCESSED);
        final PeriodicJobsMailFinalizerBean periodicJobsMailFinalizerBean = newPeriodicJobsMailFinalizerBean();
        env().getPersistenceContext().run(() ->
                periodicJobsMailFinalizerBean.deliver(chunk, delivery));
        final List<Message> inbox = Mailbox.get("someone_out_there@outthere.dk");
        assertThat("Inbox size", inbox.size(), is(1));
        Message receivedMail = inbox.get(0);
        assertThat("Recipients is ok", receivedMail.getAllRecipients(),
                is(new InternetAddress[]{new InternetAddress(recipients)}));

        final MimeMultipart mimeMultipart = (MimeMultipart) receivedMail.getContent();

        final String body = (String) mimeMultipart.getBodyPart(0).getContent();
        assertThat("Mail body", body, is("Ugekorrektur uge 202041"));

        final String attachmentAsText = (String) mimeMultipart.getBodyPart(1).getContent();
        assertThat("Mail attachment", attachmentAsText, is("0\n"));
    }

    private PeriodicJobsMailFinalizerBean newPeriodicJobsMailFinalizerBean() {
        final PeriodicJobsMailFinalizerBean periodicJobsMailFinalizerBean = new PeriodicJobsMailFinalizerBean();
        periodicJobsMailFinalizerBean.entityManager = env().getEntityManager();
        final Properties mailSessionProperties = new Properties();
        mailSessionProperties.setProperty("mail.from", mailFrom);
        periodicJobsMailFinalizerBean.mailSession = Session.getDefaultInstance(mailSessionProperties);
        periodicJobsMailFinalizerBean.jobStoreServiceConnector = jobStoreServiceConnector;
        periodicJobsMailFinalizerBean.weekResolverConnector = weekResolverConnector;
        return periodicJobsMailFinalizerBean;
    }

    /* Todo: Test for large mails
     */

    @Test
    public void deliver_recordCountExceedsLimit() throws AddressException {
        final int jobId = 42;
        final PeriodicJobsDataBlock block0 = new PeriodicJobsDataBlock();
        block0.setKey(new PeriodicJobsDataBlock.Key(jobId, 0, 0));
        block0.setSortkey("000000000");
        block0.setBytes(StringUtil.asBytes("0\n"));
        block0.setGroupHeader(StringUtil.asBytes("groupA\n"));
        final PeriodicJobsDataBlock block1 = new PeriodicJobsDataBlock();
        block1.setKey(new PeriodicJobsDataBlock.Key(jobId, 1, 0));
        block1.setSortkey("000000001");
        block1.setBytes(StringUtil.asBytes("1\n"));

        env().getPersistenceContext().run(() -> {
            env().getEntityManager().persist(block1);
            env().getEntityManager().persist(block0);
        });

        final PeriodicJobsDelivery delivery = new PeriodicJobsDelivery(jobId);
        delivery.setConfig(new PeriodicJobsHarvesterConfig(1, 1,
                new PeriodicJobsHarvesterConfig.Content()
                        .withTimeOfLastHarvest(new Date())
                        .withName("Deliver test")
                        .withSubmitterNumber("111111")
                        .withPickup(new MailPickup()
                                .withRecipients(recipients)
                                .withSubject(subject)
                                .withRecordLimit(1))));
        final Chunk chunk = new Chunk(jobId, 3, Chunk.Type.PROCESSED);
        final PeriodicJobsMailFinalizerBean periodicJobsMailFinalizerBean = newPeriodicJobsMailFinalizerBean();
        final Chunk result = env().getPersistenceContext().run(() ->
                periodicJobsMailFinalizerBean.deliver(chunk, delivery));
        List<Message> inbox = Mailbox.get("someone_out_there@outthere.dk");
        assertThat(result.getItems().get(0).getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat(result.getItems().get(0).getData(),
                is("IllegalStateException: Record count exceeded record limit of 1".getBytes(StandardCharsets.UTF_8)));
        assertThat("Inbox size", inbox.size(), is(0));
    }

}
