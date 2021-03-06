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

package dk.dbc.dataio.jobstore.service.param;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.RecordSplitterConstants;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowContentBuilder;
import dk.dbc.dataio.jobstore.types.AccTestJobInputStream;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AddAccTestJobParamTest extends ParamBaseTest {

    private final FlowStoreServiceConnector mockedFlowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
    private final FlowBinder flowBinder = new FlowBinderBuilder().setContent(new FlowBinderContentBuilder().setName("FlowBinderName").setFlowId(42).setSinkId(42).build()).build();
    private final Flow flow = new FlowBuilder().setContent(new FlowContentBuilder().setName("accTestFlow").build()).build();
    private final Sink sink = AddJobParam.createDiffSink();
    private final RecordSplitterConstants.RecordSplitter typeOfDataPartitioner = RecordSplitterConstants.RecordSplitter.XML;
    private AccTestJobInputStream accTestJobInputStream;

    @Before
    public void setup() {
        jobSpecification.withType(JobSpecification.Type.ACCTEST);
        accTestJobInputStream = new AccTestJobInputStream(jobSpecification, flow, typeOfDataPartitioner);
    }

    @Test
    public void constructor_inputStreamArgIsNull_throws() {
        assertThat(() -> new AddAccTestJobParam(null, mockedFlowStoreServiceConnector), isThrowing(NullPointerException.class));
    }

    @Test
    public void constructor_flowStoreServiceConnectorArgIsNull_throws() {
        assertThat(() -> new AddAccTestJobParam(accTestJobInputStream, null), isThrowing(NullPointerException.class));
    }

    @Test
    public void constructor_allArgsAreValid_returnsAddAccTestJobParam() throws FlowStoreServiceConnectorException {

        when(mockedFlowStoreServiceConnector.getFlowBinder(
                jobSpecification.getPackaging(),
                jobSpecification.getFormat(),
                jobSpecification.getCharset(),
                jobSpecification.getSubmitterId(),
                jobSpecification.getDestination())).thenReturn(flowBinder);

        final AddAccTestJobParam addAccTestJobParam = new AddAccTestJobParam(accTestJobInputStream, mockedFlowStoreServiceConnector);

        assertThat(addAccTestJobParam.getTypeOfDataPartitioner(), is(typeOfDataPartitioner));
        assertThat(addAccTestJobParam.getFlow(), is(flow));
        assertThat(addAccTestJobParam.getSink(), is(sink));
        assertThat(addAccTestJobParam.getDiagnostics().size(), is(0));

        final FlowStoreReferences flowStoreReferences = addAccTestJobParam.getFlowStoreReferences();
        assertThat(flowStoreReferences.getReference(FlowStoreReferences.Elements.FLOW).getName(), is(flow.getContent().getName()));
        assertThat(flowStoreReferences.getReference(FlowStoreReferences.Elements.SINK).getName(), is(sink.getContent().getName()));
        assertThat(flowStoreReferences.getReference(FlowStoreReferences.Elements.FLOW_BINDER).getName(), is(flowBinder.getContent().getName()));
    }

    @Test
    public void constructor_flowBinderNotFound_returnsAddAccTestJobParam() throws FlowStoreServiceConnectorException {

        when(mockedFlowStoreServiceConnector.getFlowBinder(
                jobSpecification.getPackaging(),
                jobSpecification.getFormat(),
                jobSpecification.getCharset(),
                jobSpecification.getSubmitterId(),
                jobSpecification.getDestination())).thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("mesage", 404));

        final AddAccTestJobParam addAccTestJobParam = new AddAccTestJobParam(accTestJobInputStream, mockedFlowStoreServiceConnector);

        final List<Diagnostic> diagnostic = addAccTestJobParam.getDiagnostics();
        Assert.assertThat(diagnostic.size(), is(1));
        assertThat(diagnostic.get(0).getLevel(), is(Diagnostic.Level.FATAL));
    }
}
