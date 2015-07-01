package dk.dbc.dataio.gui.server.modelmappers;

import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.gui.client.model.FlowBinderModel;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.model.SubmitterModel;
import dk.dbc.dataio.gui.client.modelBuilders.SubmitterModelBuilder;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * FlowModelMapper unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class FlowBinderModelMapperTest {

    private static final long DEFAULT_FLOW_BINDER_ID = 100L;
    private static final long DEFAULT_FLOW_BINDER_VERSION = 104L;
    private static final long DEFAULT_FLOW_ID = 101L;
    private static final long DEFAULT_SUBMITTER_ID = 102L;
    private static final long DEFAULT_SINK_ID = 103L;

    // Default Flow Binder Content
    private static final FlowBinderContent defaultFlowBinderContent = new FlowBinderContent(
            "flow binder name", "flow binder description", "packaging", "format", "charset", "destination", "recordsplitter", true,
            DEFAULT_FLOW_ID,
            Collections.singletonList(DEFAULT_SUBMITTER_ID),
            DEFAULT_SINK_ID);
    private static final FlowBinder defaultFlowBinder = new FlowBinder(DEFAULT_FLOW_BINDER_ID, 2L, defaultFlowBinderContent);

    // Default Flow Binder Model
    private static final FlowModel defaultFlowModel = new FlowModel(DEFAULT_FLOW_ID, 4L, "flow name", "flow description", Collections.singletonList(new FlowComponentModel()));
    private static final SubmitterModel defaultSubmitterModel = new SubmitterModelBuilder().setId(DEFAULT_SUBMITTER_ID).build();
    private static final SinkModel defaultSinkModel = new SinkModel(DEFAULT_SINK_ID, 7L, "sink name", "sink resource", "sink description");
    private static final FlowBinderModel defaultFlowBinderModel = new FlowBinderModel(
            DEFAULT_FLOW_BINDER_ID,
            DEFAULT_FLOW_BINDER_VERSION,
            "flow binder name", "flow binder description", "packaging", "format", "charset", "destination", "recordsplitter", true,
            defaultFlowModel,
            Collections.singletonList(defaultSubmitterModel),
            defaultSinkModel
            );


    // FlowBinderModelMapper.toModel()

    @Test(expected = NullPointerException.class)
    public void toModel_nullFlowBinderInput_throws() {
        FlowBinderModelMapper.toModel(null, new FlowModel(), Collections.singletonList(new SubmitterModel()), new SinkModel());
    }

    @Test(expected = NullPointerException.class)
    public void toModel_nullFlowInput_throws() {
        FlowBinderModelMapper.toModel(defaultFlowBinder, null, Collections.singletonList(new SubmitterModel()), new SinkModel());
    }

    @Test(expected = NullPointerException.class)
    public void toModel_nullSubmitterInput_throws() {
        FlowBinderModelMapper.toModel(defaultFlowBinder, new FlowModel(), null, new SinkModel());
    }

    @Test(expected = NullPointerException.class)
    public void toModel_nullSinkInput_throws() {
        FlowBinderModelMapper.toModel(defaultFlowBinder, new FlowModel(), Collections.singletonList(new SubmitterModel()), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void toModel_validInputNoFlowIdInFlowBinderContent_throws() {
        FlowBinderContent flowBinderContent = new FlowBinderContent("flow binder name", "flow binder description", "packaging", "format", "charset", "destination", "recordsplitter", true, 0L, Collections.singletonList(4L), 5L);
        FlowBinder flowBinder = new FlowBinder(1L, 2L, flowBinderContent);

        FlowBinderModelMapper.toModel(flowBinder, new FlowModel(), Collections.singletonList(new SubmitterModel()), new SinkModel());
    }

    @Test(expected = NullPointerException.class)
    public void toModel_validInputNoSubmitterIdsInFlowBinderContent_throws() {
        FlowBinderContent flowBinderContent = new FlowBinderContent("flow binder name", "flow binder description", "packaging", "format", "charset", "destination", "recordsplitter", true, 3L, null, 5L);
        FlowBinder flowBinder = new FlowBinder(1L, 2L, flowBinderContent);

        FlowBinderModelMapper.toModel(flowBinder, new FlowModel(), Collections.singletonList(new SubmitterModel()), new SinkModel());
    }

    @Test(expected = IllegalArgumentException.class)
    public void toModel_validInputNoSinkIdInFlowBinderContent_throws() {
        FlowBinderContent flowBinderContent = new FlowBinderContent("flow binder name", "flow binder description", "packaging", "format", "charset", "destination", "recordsplitter", true, 3L, Collections.singletonList(4L), 0L);
        FlowBinder flowBinder = new FlowBinder(1L, 2L, flowBinderContent);

        FlowBinderModelMapper.toModel(flowBinder, new FlowModel(), Collections.singletonList(new SubmitterModel()), new SinkModel());
    }

    @Test
    public void toFlowBinderContent_invalidFlowBinderName_throwsIllegalArgumentException() {
        final String flowBinderName = "*%(Illegal)_&Name - €";
        final String expectedIllegalCharacters = "[*], [%], [(], [)], [&], [€]";

        final FlowBinderModel flowBinderModel = new FlowBinderModel(
                DEFAULT_FLOW_BINDER_ID,
                DEFAULT_FLOW_BINDER_VERSION,
                flowBinderName, "flow binder description", "packaging", "format", "charset", "destination", "recordsplitter", true,
                defaultFlowModel,
                Collections.singletonList(defaultSubmitterModel),
                defaultSinkModel);

        try {
            FlowBinderModelMapper.toFlowBinderContent(flowBinderModel);
        } catch(IllegalArgumentException e) {
            assertThat(e.getMessage().contains(expectedIllegalCharacters), is (true));
        }
    }

    @Test
    public void toModel_validInput_returnsValidModel() {

        FlowBinderModel flowBinderModel = FlowBinderModelMapper.toModel(defaultFlowBinder, defaultFlowModel, Collections.singletonList(defaultSubmitterModel), defaultSinkModel);

        assertThat(flowBinderModel.getId(), is(DEFAULT_FLOW_BINDER_ID));
        assertThat(flowBinderModel.getName(), is(defaultFlowBinder.getContent().getName()));
        assertThat(flowBinderModel.getDescription(), is(defaultFlowBinder.getContent().getDescription()));
        assertThat(flowBinderModel.getPackaging(), is(defaultFlowBinder.getContent().getPackaging()));
        assertThat(flowBinderModel.getFormat(), is(defaultFlowBinder.getContent().getFormat()));
        assertThat(flowBinderModel.getCharset(), is(defaultFlowBinder.getContent().getCharset()));
        assertThat(flowBinderModel.getDestination(), is(defaultFlowBinder.getContent().getDestination()));
        assertThat(flowBinderModel.getRecordSplitter(), is(defaultFlowBinder.getContent().getRecordSplitter()));
        assertThat(flowBinderModel.getFlowModel().getId(), is(DEFAULT_FLOW_ID));
        assertThat(flowBinderModel.getSubmitterModels().size(), is(1));
        assertThat(flowBinderModel.getSubmitterModels().get(0).getId(), is(DEFAULT_SUBMITTER_ID));
        assertThat(flowBinderModel.getSinkModel().getId(), is(DEFAULT_SINK_ID));
    }


    // FlowBinderModelMapper.toFlowBinderContent()

    @Test(expected = NullPointerException.class)
    public void toFlowBinderContent_nullInput_throwsNullPointerException() {
        FlowBinderModelMapper.toFlowBinderContent(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void toFlowBinderContent_validInputNullFlow_throwsIllegalArgumentException() {
        FlowBinderModel model = new FlowBinderModel(defaultFlowBinderModel);
        model.setFlowModel(null);

        FlowBinderModelMapper.toFlowBinderContent(model);
    }

    @Test(expected = IllegalArgumentException.class)
    public void toFlowBinderContent_validInputNullSubmitters_throwsIllegalArgumentException() {
        FlowBinderModel model = new FlowBinderModel(defaultFlowBinderModel);
        model.setSubmitterModels(null);

        FlowBinderModelMapper.toFlowBinderContent(model);
    }

    @Test(expected = IllegalArgumentException.class)
    public void toFlowBinderContent_validInputNoSubmitters_throwsIllegalArgumentException() {
        FlowBinderModel model = new FlowBinderModel(defaultFlowBinderModel);
        model.setSubmitterModels(new ArrayList<SubmitterModel>());

        FlowBinderModelMapper.toFlowBinderContent(model);
    }

    @Test(expected = IllegalArgumentException.class)
    public void toFlowBinderContent_validInputNullSink_throwsIllegalArgumentException() {
        FlowBinderModel model = new FlowBinderModel(defaultFlowBinderModel);
        model.setSinkModel(null);

        FlowBinderModelMapper.toFlowBinderContent(model);
    }

    @Test
    public void toFlowBinderContent_validInput_returnsValidFlowBinderContent() {

        FlowBinderContent content = FlowBinderModelMapper.toFlowBinderContent(defaultFlowBinderModel);

        assertThat(content.getName(), is(defaultFlowBinderModel.getName()));
        assertThat(content.getDescription(), is(defaultFlowBinderModel.getDescription()));
        assertThat(content.getPackaging(), is(defaultFlowBinderModel.getPackaging()));
        assertThat(content.getFormat(), is(defaultFlowBinderModel.getFormat()));
        assertThat(content.getCharset(), is(defaultFlowBinderModel.getCharset()));
        assertThat(content.getDestination(), is(defaultFlowBinderModel.getDestination()));
        assertThat(content.getRecordSplitter(), is(defaultFlowBinderModel.getRecordSplitter()));
        assertThat(content.getFlowId(), is(defaultFlowModel.getId()));
        assertThat(content.getSubmitterIds().size(), is(1));
        assertThat(content.getSubmitterIds().get(0), is(defaultSubmitterModel.getId()));
        assertThat(content.getSinkId(), is(defaultSinkModel.getId()));
    }

}
