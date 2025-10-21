package dk.dbc.dataio.harvester.ticklerepo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.TickleRepoHarvesterConfig;
import dk.dbc.ticklerepo.dto.Batch;
import dk.dbc.ticklerepo.dto.DataSet;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class JobSpecificationTemplateTest {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void template() throws HarvesterException {
        DataSet dataset = new DataSet().withAgencyId(123456);
        Batch batch = new Batch().withId(42);
        TickleRepoHarvesterConfig.Content content = new TickleRepoHarvesterConfig.Content()
                .withDestination("-destination-")
                .withFormat("-format-")
                .withType(JobSpecification.Type.TEST);
        TickleRepoHarvesterConfig config = new TickleRepoHarvesterConfig(1, 2, content);

        JobSpecification template = JobSpecificationTemplate.create(config, dataset, batch, 0);
        assertThat("template", template, is(notNullValue()));
        assertThat("template packaging", template.getPackaging(), is("addi-xml"));
        assertThat("template format", template.getFormat(), is(config.getContent().getFormat()));
        assertThat("template charset", template.getCharset(), is("utf8"));
        assertThat("template destination", template.getDestination(), is(config.getContent().getDestination()));
        assertThat("template submitter", template.getSubmitterId(), is((long) dataset.getAgencyId()));
        assertThat("template MailForNotificationAboutVerification", template.getMailForNotificationAboutVerification(), is(JobSpecification.EMPTY_MAIL_FOR_NOTIFICATION_ABOUT_VERIFICATION));
        assertThat("template MailForNotificationAboutProcessing", template.getMailForNotificationAboutProcessing(), is(JobSpecification.EMPTY_MAIL_FOR_NOTIFICATION_ABOUT_PROCESSING));
        assertThat("template initials", template.getResultmailInitials(), is(JobSpecification.EMPTY_RESULT_MAIL_INITIALS));
        assertThat("template data file", template.getDataFile(), is("placeholder"));
        assertThat("template type", template.getType(), is(config.getContent().getType()));
        assertThat("template ancestry", template.getAncestry(), is(notNullValue()));
        assertThat("template ancestry token", template.getAncestry().getHarvesterToken(), is(config.getHarvesterToken(batch.getId())));
    }

    @Test
    public void templateBasedOnBatchMetadata() throws HarvesterException, JsonProcessingException {
        JobSpecification jobSpecification = new JobSpecification()
                .withMailForNotificationAboutVerification("verification@test.com")
                .withMailForNotificationAboutProcessing("processing@test.com")
                .withResultmailInitials("ABC")
                .withAncestry(new JobSpecification.Ancestry().withDatafile("testFile"));
        DataSet dataset = new DataSet().withAgencyId(123456);
        Batch batch = new Batch().withId(42).withMetadata(mapper.writeValueAsString(jobSpecification));
        TickleRepoHarvesterConfig config = new TickleRepoHarvesterConfig(1, 2, new TickleRepoHarvesterConfig.Content().withDestination("-destination-").withFormat("-format-").withType(JobSpecification.Type.TEST).withNotificationsEnabled(true));

        JobSpecification template = JobSpecificationTemplate.create(config, dataset, batch, 0);
        assertThat("template", template, is(notNullValue()));
        assertThat("template mailForNotificationAboutVerification", template.getMailForNotificationAboutVerification(), is(jobSpecification.getMailForNotificationAboutVerification()));
        assertThat("template mailForNotificationAboutProcessing", template.getMailForNotificationAboutProcessing(), is(jobSpecification.getMailForNotificationAboutProcessing()));
        assertThat("template resultMailInitials", template.getResultmailInitials(), is(jobSpecification.getResultmailInitials()));
        assertThat("template ancestry", template.getAncestry(), is(notNullValue()));
        assertThat("template ancestry datafile", template.getAncestry().getDatafile(), is(jobSpecification.getAncestry().getDatafile()));
        assertThat("template ancestry token", template.getAncestry().getHarvesterToken(), is(config.getHarvesterToken(batch.getId())));
    }

    @Test
    public void templateWhenNotificationsAreNotEnabled() throws HarvesterException, JsonProcessingException {
        JobSpecification jobSpecification = new JobSpecification().withMailForNotificationAboutVerification("verification@test.com").withMailForNotificationAboutProcessing("processing@test.com").withResultmailInitials("ABC").withAncestry(new JobSpecification.Ancestry().withDatafile("testFile"));
        DataSet dataset = new DataSet().withAgencyId(123456);
        Batch batch = new Batch().withId(42).withMetadata(mapper.writeValueAsString(jobSpecification));
        TickleRepoHarvesterConfig.Content content = new TickleRepoHarvesterConfig.Content()
                .withDestination("-destination-")
                .withFormat("-format-")
                .withType(JobSpecification.Type.TEST);
        TickleRepoHarvesterConfig config = new TickleRepoHarvesterConfig(1, 2, content);
        JobSpecification template = JobSpecificationTemplate.create(config, dataset, batch, 0);
        assertThat("template", template, is(notNullValue()));
        assertThat("template MailForNotificationAboutVerification", template.getMailForNotificationAboutVerification(), is(JobSpecification.EMPTY_MAIL_FOR_NOTIFICATION_ABOUT_VERIFICATION));
        assertThat("template MailForNotificationAboutProcessing", template.getMailForNotificationAboutProcessing(), is(JobSpecification.EMPTY_MAIL_FOR_NOTIFICATION_ABOUT_PROCESSING));
        assertThat("template initials", template.getResultmailInitials(), is(JobSpecification.EMPTY_RESULT_MAIL_INITIALS));
        assertThat("template ancestry", template.getAncestry(), is(notNullValue()));
        assertThat("template ancestry datafile", template.getAncestry().getDatafile(), is(nullValue()));
        assertThat("template ancestry token", template.getAncestry().getHarvesterToken(), is(config.getHarvesterToken(batch.getId())));
    }
}
