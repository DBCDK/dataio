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

package dk.dbc.dataio.commons.utils.test.json;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.GatekeeperDestination;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.OpenUpdateSinkConfig;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.SubmitterContent;
import org.junit.Test;

public class JsonBuilderTest {

    private final JSONBContext jsonbContext = new JSONBContext();

    @Test
    public void FlowBinderContentJsonBuilderProducesValidJson() throws JSONBException {
        jsonbContext.unmarshall(new FlowBinderContentJsonBuilder().build(), FlowBinderContent.class);
    }

    @Test
    public void FlowBinderJsonBuilderProducesValidJson() throws JSONBException {
        jsonbContext.unmarshall(new FlowBinderJsonBuilder().build(), FlowBinder.class);
    }

    @Test
    public void FlowComponentContentJsonBuilderProducesValidJson() throws JSONBException {
        jsonbContext.unmarshall(new FlowComponentContentJsonBuilder().build(), FlowComponentContent.class);
    }

    @Test
    public void FlowComponentJsonBuilderProducesValidJson() throws JSONBException {
        jsonbContext.unmarshall(new FlowComponentJsonBuilder().build(), FlowComponent.class);
    }

    @Test
    public void FlowContentJsonBuilderProducesValidJson() throws JSONBException {
        jsonbContext.unmarshall(new FlowContentJsonBuilder().build(), FlowContent.class);
    }

    @Test
    public void FlowJsonBuilderProducesValidJson() throws JSONBException {
        jsonbContext.unmarshall(new FlowJsonBuilder().build(), Flow.class);
    }

    @Test
    public void JavaScriptJsonBuilderProducesValidJson() throws JSONBException {
        jsonbContext.unmarshall(new JavaScriptJsonBuilder().build(), JavaScript.class);
    }

    @Test
    public void JobSpecificationJsonBuilderProducesValidJson() throws JSONBException {
        jsonbContext.unmarshall(new JobSpecificationJsonBuilder().build(), JobSpecification.class);
    }

    @Test
    public void SinkContentJsonBuilderProducesValidJson() throws JSONBException {
        jsonbContext.unmarshall(new SinkContentJsonBuilder().build(), SinkContent.class);
    }

    @Test
    public void SinkContentJsonBuilderWithTypeProducesValidJson() throws JSONBException {
        jsonbContext.unmarshall(new SinkContentJsonBuilder()
                .setSinkType(SinkContent.SinkType.OPENUPDATE).build(), SinkContent.class);
    }

    @Test
    public void SinkContentJsonBuilderWithTypeAndConfigProducesValidJson() throws JSONBException {
        jsonbContext.unmarshall(new SinkContentJsonBuilder()
                .setSinkType(SinkContent.SinkType.OPENUPDATE)
                .setSinkConfig(jsonbContext.marshall(new OpenUpdateSinkConfig()))
                .build(), SinkContent.class);
    }

    @Test
    public void SinkJsonBuilderProducesValidJson() throws JSONBException {
        jsonbContext.unmarshall(new SinkJsonBuilder().build(), Sink.class);
    }

    @Test
    public void SubmitterContentJsonBuilderProducesValidJson() throws JSONBException {
        jsonbContext.unmarshall(new SubmitterContentJsonBuilder().build(), SubmitterContent.class);
    }

    @Test
    public void SubmitterJsonBuilderProducesValidJson() throws JSONBException {
        jsonbContext.unmarshall(new SubmitterJsonBuilder().build(), Submitter.class);
    }

    @Test
    public void GatekeeperDestinationJsonBuilderProducesValidJson() throws JSONBException {
        jsonbContext.unmarshall(new GatekeeperDestinationJsonBuilder().build(), GatekeeperDestination.class);
    }
}
