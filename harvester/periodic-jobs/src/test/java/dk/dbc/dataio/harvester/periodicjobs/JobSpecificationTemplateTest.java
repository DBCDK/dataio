package dk.dbc.dataio.harvester.periodicjobs;

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class JobSpecificationTemplateTest {
    @Test
    public void template() throws HarvesterException {
        PeriodicJobsHarvesterConfig config = new PeriodicJobsHarvesterConfig(1, 2,
                new PeriodicJobsHarvesterConfig.Content()
                        .withDestination("-destination-")
                        .withFormat("-format-")
                        .withSubmitterNumber("123456")
        );

        JobSpecification template = JobSpecificationTemplate.create(config);
        assertThat("template", template,
                is(notNullValue()));
        assertThat("template packaging", template.getPackaging(),
                is("addi-xml"));
        assertThat("template format", template.getFormat(),
                is(config.getContent().getFormat()));
        assertThat("template charset", template.getCharset(),
                is("utf8"));
        assertThat("template destination", template.getDestination(),
                is(config.getContent().getDestination()));
        assertThat("template submitter", template.getSubmitterId(),
                is(123456L));
        assertThat("template MailForNotificationAboutVerification",
                template.getMailForNotificationAboutVerification(),
                is("placeholder"));
        assertThat("template MailForNotificationAboutProcessing",
                template.getMailForNotificationAboutProcessing(),
                is("placeholder"));
        assertThat("template initials", template.getResultmailInitials(),
                is("placeholder"));
        assertThat("template data file", template.getDataFile(),
                is("placeholder"));
        assertThat("template type", template.getType(),
                is(JobSpecification.Type.PERIODIC));
        assertThat("template ancestry", template.getAncestry(),
                is(new JobSpecification.Ancestry()
                        .withHarvesterToken(config.getHarvesterToken())));
    }
}
