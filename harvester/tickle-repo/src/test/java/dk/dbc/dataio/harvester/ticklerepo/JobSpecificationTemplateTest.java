/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.harvester.ticklerepo;

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.TickleRepoHarvesterConfig;
import dk.dbc.ticklerepo.dto.Batch;
import dk.dbc.ticklerepo.dto.DataSet;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class JobSpecificationTemplateTest {
    @Test
    public void template() throws HarvesterException {
        final DataSet dataset = new DataSet().withAgencyId(123456);
        final Batch batch = new Batch().withId(42);
        final TickleRepoHarvesterConfig config = new TickleRepoHarvesterConfig(1, 2,
                new TickleRepoHarvesterConfig.Content()
                        .withDestination("-destination-")
                        .withFormat("-format-")
                        .withType(JobSpecification.Type.TEST)
        );

        final JobSpecification template = JobSpecificationTemplate.create(config, dataset, batch);
        assertThat("template", template, is(notNullValue()));
        assertThat("template packaging", template.getPackaging(), is("xml"));
        assertThat("template format", template.getFormat(), is(config.getContent().getFormat()));
        assertThat("template charset", template.getCharset(), is(StandardCharsets.UTF_8.name()));
        assertThat("template destination", template.getDestination(), is(config.getContent().getDestination()));
        assertThat("template submitter", template.getSubmitterId(), is((long) dataset.getAgencyId()));
        assertThat("template MailForNotificationAboutVerification", template.getMailForNotificationAboutVerification(), is("placeholder"));
        assertThat("template MailForNotificationAboutProcessing", template.getMailForNotificationAboutProcessing(), is("placeholder"));
        assertThat("template initials", template.getResultmailInitials(), is("placeholder"));
        assertThat("template data file", template.getDataFile(), is("placeholder"));
        assertThat("template type", template.getType(), is(config.getContent().getType()));
        assertThat("template ancestry", template.getAncestry(), is(notNullValue()));
        assertThat("template ancestry token", template.getAncestry().getHarvesterToken(), is(config.getHarvesterToken(batch.getId())));
    }
}