
package dk.dbc.dataio.sink.periodicjobs;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.harvester.types.MailPickup;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.mock_javamail.Mailbox;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
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

    @Before
    public void setupMocks() {
        Mailbox.clearAll();

        when(jobStoreServiceConnectorBean.getConnector())
                .thenReturn(jobStoreServiceConnector);
    }

    @Test
    public void deliver_onNonEmptyJobNoDatablocks() throws MessagingException {
        final int jobId = 42;
        final PeriodicJobsDelivery delivery = new PeriodicJobsDelivery(jobId);
        delivery.setConfig(new PeriodicJobsHarvesterConfig(1, 1,
                new PeriodicJobsHarvesterConfig.Content()
                        .withName("Deliver test")
                        .withSubmitterNumber("111111")
                        .withPickup(new MailPickup()
                                .withRecipients(recipients)
                                .withSubject(subject))));
        final Chunk chunk = new Chunk(jobId, 3, Chunk.Type.PROCESSED);
        final PeriodicJobsMailFinalizerBean periodicJobsMailFinalizerBean = newPeriodicJobsMailFinalizerBean();
        env().getPersistenceContext().run(() ->
                periodicJobsMailFinalizerBean.deliver(chunk, delivery));
        List<Message> inbox = Mailbox.get("someone_out_there@outthere.dk");
        assertThat("Inbox size", inbox.size(), is(0));
    }

    @Test
    public void deliver_onNonEmptyJob() throws MessagingException, IOException {
        final int jobId = 42;
        final PeriodicJobsDataBlock block0 = new PeriodicJobsDataBlock();
        block0.setKey(new PeriodicJobsDataBlock.Key(jobId, 0));
        block0.setSortkey("000000000");
        block0.setBytes(StringUtil.asBytes("0"));
        final PeriodicJobsDataBlock block1 = new PeriodicJobsDataBlock();
        block1.setKey(new PeriodicJobsDataBlock.Key(jobId, 1));
        block1.setSortkey("000000001");
        block1.setBytes(StringUtil.asBytes("1"));
        final PeriodicJobsDataBlock block2 = new PeriodicJobsDataBlock();
        block2.setKey(new PeriodicJobsDataBlock.Key(jobId, 2));
        block2.setSortkey("000000002");
        block2.setBytes(StringUtil.asBytes("2"));

        env().getPersistenceContext().run(() -> {
            env().getEntityManager().persist(block2);
            env().getEntityManager().persist(block1);
            env().getEntityManager().persist(block0);
        });

        final PeriodicJobsDelivery delivery = new PeriodicJobsDelivery(jobId);
        delivery.setConfig(new PeriodicJobsHarvesterConfig(1, 1,
                new PeriodicJobsHarvesterConfig.Content()
                        .withName("Deliver test")
                        .withSubmitterNumber("111111")
                        .withPickup(new MailPickup()
                                .withRecipients(recipients)
                                .withSubject(subject))));
        final Chunk chunk = new Chunk(jobId, 3, Chunk.Type.PROCESSED);
        final PeriodicJobsMailFinalizerBean periodicJobsMailFinalizerBean = newPeriodicJobsMailFinalizerBean();
        env().getPersistenceContext().run(() ->
                periodicJobsMailFinalizerBean.deliver(chunk, delivery));
        List<Message> inbox = Mailbox.get("someone_out_there@outthere.dk");
        assertThat("Inbox size", inbox.size(), is(1));
        Message receivedMail = inbox.get(0);
        assertThat("Recipients is ok", receivedMail.getAllRecipients(),
                is(new InternetAddress[]{new InternetAddress(recipients)}));
        assertThat("Mail content is intact", receivedMail.getContent(), is("012"));
    }

    @Test
    public void deliver_onEmptyJob() throws MessagingException, IOException {
        final int jobId = 42;

        final PeriodicJobsDelivery delivery = new PeriodicJobsDelivery(jobId);
        delivery.setConfig(new PeriodicJobsHarvesterConfig(1, 1,
                new PeriodicJobsHarvesterConfig.Content()
                        .withName("Deliver test")
                        .withSubmitterNumber("111111")
                        .withPickup(new MailPickup()
                                .withRecipients(recipients)
                                .withSubject(subject))));
        final Chunk chunk = new Chunk(jobId, 0, Chunk.Type.PROCESSED);
        final PeriodicJobsMailFinalizerBean periodicJobsMailFinalizerBean = newPeriodicJobsMailFinalizerBean();
        env().getPersistenceContext().run(() ->
                periodicJobsMailFinalizerBean.deliver(chunk, delivery));
        List<Message> inbox = Mailbox.get("someone_out_there@outthere.dk");
        assertThat("Inbox size", inbox.size(), is(1));
        Message receivedMail = inbox.get(0);
        assertThat("mail recipients", receivedMail.getAllRecipients(),
                is(new InternetAddress[] {new InternetAddress(recipients)}));
        assertThat("mail body", receivedMail.getContent(),
                is("Periodisk job fandt ingen nye poster"));
    }

    private PeriodicJobsMailFinalizerBean newPeriodicJobsMailFinalizerBean() {
        final PeriodicJobsMailFinalizerBean periodicJobsMailFinalizerBean = new PeriodicJobsMailFinalizerBean();
        periodicJobsMailFinalizerBean.entityManager = env().getEntityManager();
        final Properties mailSessionProperties = new Properties();
        mailSessionProperties.setProperty("mail.from", mailFrom);
        periodicJobsMailFinalizerBean.mailSession = Session.getDefaultInstance(mailSessionProperties);
        periodicJobsMailFinalizerBean.jobStoreServiceConnectorBean = jobStoreServiceConnectorBean;
        return periodicJobsMailFinalizerBean;
    }

    /* Todo: Test for large mails
     */
}
