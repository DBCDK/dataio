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

package dk.dbc.dataio.gui.client.model;

import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.gui.client.modelBuilders.FlowComponentModelBuilder;
import dk.dbc.dataio.gui.client.modelBuilders.FlowModelBuilder;
import dk.dbc.dataio.gui.client.modelBuilders.SinkModelBuilder;
import dk.dbc.dataio.gui.client.modelBuilders.SubmitterModelBuilder;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class FlowBinderModelTest {

    @Test
    public void createModel_noArgs_returnsNewInstanceWithDefaultValues() {
        FlowBinderModel model = new FlowBinderModel();
        assertThat(model, is(notNullValue()));
        assertThat(model.getId(), is(0L));
        assertThat(model.getVersion(), is(0L));
        assertThat(model.getName(), is(""));
        assertThat(model.getDescription(), is(""));
        assertThat(model.getPackaging(), is(""));
        assertThat(model.getFormat(), is(""));
        assertThat(model.getCharset(), is(""));
        assertThat(model.getDestination(), is(""));
        assertThat(model.getPriority(), is(Priority.NORMAL.getValue()));
        assertThat(model.getRecordSplitter(), is(""));
        assertThat(model.getFlowModel().getFlowName(), is(""));  // FlowModel has been tested, therefore only name is checked
        assertThat(model.getSubmitterModels().size(), is(0));
        assertThat(model.getSinkModel().getSinkName(), is(""));  // FlowModel has been tested, therefore only name is checked
        assertThat(model.getQueueProvider(), is(""));
    }

    @Test
    public void isInputFieldsEmpty_emptyFlowNameInput_returnsTrue() {
        FlowBinderModel model = getTestModel();
        model.setName("");
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_emptyDescriptionInput_returnsTrue() {
        FlowBinderModel model = getTestModel();
        model.setDescription("");
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_emptyPackagingInput_returnsTrue() {
        FlowBinderModel model = getTestModel();
        model.setPackaging("");
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_emptyFormatInput_returnsTrue() {
        FlowBinderModel model = getTestModel();
        model.setFormat("");
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_emptyCharsetInput_returnsTrue() {
        FlowBinderModel model = getTestModel();
        model.setCharset("");
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_emptyDestinationInput_returnsTrue() {
        FlowBinderModel model = getTestModel();
        model.setDestination("");
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_nullPriorityInput_returnsFalse() {
        FlowBinderModel model = getTestModel();
        model.setPriority(null);
        assertThat(model.isInputFieldsEmpty(), is(false));
    }

    @Test
    public void isInputFieldsEmpty_emptyRecordSplitterInput_returnsTrue() {
        FlowBinderModel model = getTestModel();
        model.setRecordSplitter("");
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_nullFlowModelInput_returnsTrue() {
        FlowBinderModel model = getTestModel();
        model.setFlowModel(null);
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_emptyFlowModelInput_returnsFalse() {
        FlowBinderModel model = getTestModel();
        model.setFlowModel(new FlowModel());
        assertThat(model.isInputFieldsEmpty(), is(false));
    }

    @Test
    public void isInputFieldsEmpty_nullSubmitterModelsInput_returnsTrue() {
        FlowBinderModel model = getTestModel();
        model.setSubmitterModels(null);
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_emptySubmitterModelsInput_returnsTrue() {
        FlowBinderModel model = getTestModel();
        model.setSubmitterModels(new ArrayList<>());
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_nullSinkModelInput_returnsTrue() {
        FlowBinderModel model = getTestModel();
        model.setSinkModel(null);
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_emptySinkModelInput_returnsFalse() {
        FlowBinderModel model = getTestModel();
        model.setSinkModel(new SinkModel());
        assertThat(model.isInputFieldsEmpty(), is(false));
    }

    @Test
    public void isInputFieldsEmpty_updateSinkAndNullQueueProviderInput_returnsTrue() {
        FlowBinderModel model = getTestModel();
        model.setQueueProvider(null);
        SinkModel sinkModel = new SinkModelBuilder().setSinkType(SinkContent.SinkType.OPENUPDATE).build();
        model.setSinkModel(sinkModel);
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_updateSinkAndEmptyQueueProviderInput_returnsTrue() {
        FlowBinderModel model = getTestModel();
        model.setQueueProvider("");
        SinkModel sinkModel = new SinkModelBuilder().setSinkType(SinkContent.SinkType.OPENUPDATE).build();
        model.setSinkModel(sinkModel);
        assertThat(model.isInputFieldsEmpty(), is(true));
    }

    @Test
    public void isInputFieldsEmpty_nonUpdateSinkAndNullQueueProviderInput_returnsFalse() {
        FlowBinderModel model = getTestModel();
        model.setQueueProvider(null);
        SinkModel sinkModel = new SinkModelBuilder().setSinkType(SinkContent.SinkType.ES).build();
        model.setSinkModel(sinkModel);
        assertThat(model.isInputFieldsEmpty(), is(false));
    }

    @Test
    public void isInputFieldsEmpty_nonUpdateSinkAndEmptyQueueProviderInput_returnsFalse() {
        FlowBinderModel model = getTestModel();
        model.setQueueProvider("");
        SinkModel sinkModel = new SinkModelBuilder().setSinkType(SinkContent.SinkType.ES).build();
        model.setSinkModel(sinkModel);
        assertThat(model.isInputFieldsEmpty(), is(false));
    }

    @Test
    public void isInputFieldsEmpty_allInputFieldsSet_returnsFalse() {
        FlowBinderModel model = getTestModel();
        assertThat(model.isInputFieldsEmpty(), is(false));
    }

    @Test
    public void getDataioPatternMatches_validFlowBinderNameInput_returnsEmptyList() {
        FlowBinderModel model = getTestModel();
        model.setName("Valid flow binder name + 1_2_3");
        assertThat(model.getDataioPatternMatches().size(), is(0));
    }

    @Test
    public void getDataioPatternMatches_invalidFlowBinderNameInput_returnsList() {
        final FlowBinderModel model = getTestModel();
        final String expectedInvalidValues = "*<>*(#€)";
        model.setName("Invalid flow binder name" + expectedInvalidValues);

        final List<String> matches = model.getDataioPatternMatches();
        assertThat(matches.size(), is(expectedInvalidValues.length()));
        for (int i = 0; i < matches.size(); i++) {
            assertThat(matches.get(i), is(String.valueOf(expectedInvalidValues.charAt(i))));
        }
    }

    private FlowBinderModel getTestModel() {
        FlowComponentModel flowComponentModel = new FlowComponentModelBuilder().build();
        FlowModel flowModel = new FlowModelBuilder().setComponents(Collections.singletonList(flowComponentModel)).build();
        SubmitterModel submitterModel = new SubmitterModelBuilder().build();
        SinkModel sinkModel = new SinkModelBuilder().build();
        return new FlowBinderModel(11, 22, "Name", "Description", "Packaging", "Format", "Charset", "Destination", 4, "Record Splitter", flowModel, Collections.singletonList(submitterModel), sinkModel, "Queue Provider");
    }

}
