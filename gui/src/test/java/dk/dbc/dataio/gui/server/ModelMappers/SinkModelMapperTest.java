package dk.dbc.dataio.gui.server.ModelMappers;

import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.gui.client.model.SinkModel;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * SinkModelMapper unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class SinkModelMapperTest {
    // Default Sinks
    private static final SinkContent defaultSinkContent1 = new SinkContentBuilder().setName("sink content 1").setResource("sink resource 1").build();
    private static final SinkContent defaultSinkContent2 = new SinkContentBuilder().setName("sink content 2").setResource("sink resource 2").build();
    private static final Sink defaultSink1 = new SinkBuilder().setId(111L).setVersion(222L).setContent(defaultSinkContent1).build();
    private static final Sink defaultSink2 = new SinkBuilder().setId(333L).setVersion(444L).setContent(defaultSinkContent2).build();
    private static final List<Sink> defaultSinkList = Arrays.asList(defaultSink1, defaultSink2);
    // Default SinkModels
    private static final SinkModel defaultSinkModel1 = new SinkModel(555L, 666L, "Sink Model Name 1", "Sink Model Resource 1");


    @Test(expected = NullPointerException.class)
    public void toModel_nullInput_throws() {
        // Activate Subject Under Test
        SinkModelMapper.toModel(null);
    }

    @Test
    public void toModel_validInput_returnsValidModel() {
        // Activate Subject Under Test
        SinkModel model = SinkModelMapper.toModel(defaultSink1);

        // Verification
        assertThat(model.getId(), is(defaultSink1.getId()));
        assertThat(model.getVersion(), is(defaultSink1.getVersion()));
        assertThat(model.getSinkName(), is(defaultSink1.getContent().getName()));
        assertThat(model.getResourceName(), is(defaultSink1.getContent().getResource()));
    }

    @Test(expected = NullPointerException.class)
    public void toSinkContent_nullInput_throws() {
        // Activate Subject Under Test
        SinkModelMapper.toSinkContent(null);
    }

    @Test
    public void toSinkContent_validInput_returnsValidSinkContent() {
        // Activate Subject Under Test
        SinkContent sinkContent = SinkModelMapper.toSinkContent(defaultSinkModel1);

        // Verification
        assertThat(sinkContent.getName(), is(defaultSinkModel1.getSinkName()));
        assertThat(sinkContent.getResource(), is(defaultSinkModel1.getResourceName()));
    }

    @Test(expected = NullPointerException.class)
    public void toListOfSinkModels_nullInput_throws() {
        // Activate Subject Under Test
        SinkModelMapper.toListOfSinkModels(null);
    }

    @Test
    public void toListOfSinkModels_emptyInputList_returnsEmptyListOfSinkModels() {
        // Activate Subject Under Test
        List<SinkModel> sinkModels = SinkModelMapper.toListOfSinkModels(new ArrayList<Sink>());

        // Verification
        assertThat(sinkModels.size(), is(0));
    }

    @Test
    public void toListOfSinkModels_validListOfSinks_returnsValidListOfSinkModels() {
        // Activate Subject Under Test
        List<SinkModel> sinkModels = SinkModelMapper.toListOfSinkModels(defaultSinkList);

        // Verification
        assertThat(sinkModels.size(), is(2));
        assertThat(sinkModels.get(0).getId(), is(111L));
        assertThat(sinkModels.get(1).getId(), is(333L));
    }

}
