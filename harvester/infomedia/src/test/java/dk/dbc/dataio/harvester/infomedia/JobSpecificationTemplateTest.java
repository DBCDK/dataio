/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.harvester.infomedia;

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.InfomediaHarvesterConfig;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class JobSpecificationTemplateTest {
    @Test
    public void template() throws HarvesterException {
        final InfomediaHarvesterConfig config = new InfomediaHarvesterConfig(1, 2,
                new InfomediaHarvesterConfig.Content()
                        .withDestination("-destination-")
                        .withFormat("-format-")
        );

        final JobSpecification template = JobSpecificationTemplate.create(config);
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
                is(190002L));
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
                is(JobSpecification.Type.INFOMEDIA));
        assertThat("template ancestry", template.getAncestry(),
                is(nullValue()));
    }
}