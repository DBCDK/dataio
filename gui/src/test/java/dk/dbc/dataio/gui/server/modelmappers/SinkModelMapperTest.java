package dk.dbc.dataio.gui.server.modelmappers;

import dk.dbc.dataio.commons.types.EsSinkConfig;
import dk.dbc.dataio.commons.types.OpenUpdateSinkConfig;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.modelBuilders.SinkModelBuilder;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

/**
 * SinkModelMapper unit tests
 * <p>
 * The test methods of this class uses the following naming convention:
 * <p>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
public class SinkModelMapperTest {
    // Default Sinks
    private static final SinkContent defaultSinkContent1 = new SinkContentBuilder().setName("sink content 1").setQueue("sink queue 1").build();
    private static final SinkContent defaultSinkContent2 = new SinkContentBuilder().setName("sink content 2").setQueue("sink queue 2").build();
    private static final OpenUpdateSinkConfig updateSC = new OpenUpdateSinkConfig().withUserId("uid").withPassword("pwd").withEndpoint("url").withAvailableQueueProviders(Collections.singletonList("avail"));
    private static final EsSinkConfig esSC = new EsSinkConfig().withUserId(1234).withDatabaseName("pwd");
    private static final SinkContent defaultSinkContentOUpdate = new SinkContentBuilder().setName("SC Up").setQueue("sink q").setDescription("desci").setSinkType(SinkContent.SinkType.OPENUPDATE).setSinkConfig(updateSC).build();
    private static final SinkContent defaultSinkContentES = new SinkContentBuilder().setName("SC Es").setQueue("sink q").setDescription("desci").setSinkType(SinkContent.SinkType.ES).setSinkConfig(updateSC).build();
    private static final Sink defaultSink1 = new SinkBuilder().setId(111L).setVersion(222L).setContent(defaultSinkContent1).build();
    private static final Sink defaultSink2 = new SinkBuilder().setId(333L).setVersion(444L).setContent(defaultSinkContent2).build();
    private static final Sink defaultSinkOU = new SinkBuilder().setId(555L).setVersion(666L).setContent(defaultSinkContentOUpdate).build();
    private static final Sink defaultSinkES = new SinkBuilder().setId(666L).setVersion(777L).setContent(defaultSinkContentES).build();
    private static final List<Sink> defaultSinkList = Arrays.asList(defaultSink1, defaultSink2);

    // Default SinkModels
    private static final SinkModel defaultSinkModelES = new SinkModelBuilder()
            .setSinkType(SinkContent.SinkType.ES)
            .setName("Sink Model Name 1")
            .setQueue("sink::queue1")
            .setDescription("Sink Model Description 1")
            .setSinkConfig(esSC)
            .build();
    private static final SinkModel defaultSinkModelOU = new SinkModelBuilder()
            .setSinkType(SinkContent.SinkType.OPENUPDATE)
            .setName("Name OU")
            .setDescription("Description OU")
            .setQueue("sink::queue_ou")
            .setSinkConfig(updateSC)
            .build();


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
        assertThat(model.getQueue(), is(defaultSink1.getContent().getQueue()));
        assertThat(model.getDescription(), is(defaultSink1.getContent().getDescription()));
    }

    @Test
    public void toModel_validOUInput_returnsValidOUModel() {
        // Activate Subject Under Test
        SinkModel model = SinkModelMapper.toModel(defaultSinkOU);

        // Verification
        assertThat(model.getId(), is(555L));
        assertThat(model.getVersion(), is(666L));
        assertThat(model.getSinkName(), is("SC Up"));
        assertThat(model.getQueue(), is("sink q"));
        assertThat(model.getDescription(), is("desci"));
        assertThat(model.getSinkType(), is(SinkContent.SinkType.OPENUPDATE));
        assertThat(model.getOpenUpdateUserId(), is("uid"));
        assertThat(model.getOpenUpdatePassword(), is("pwd"));
        assertThat(model.getOpenUpdateEndpoint(), is("url"));
    }

    @Test
    public void toModel_validESInput_returnsValidESModel() {
        // Activate Subject Under Test
        SinkModel model = SinkModelMapper.toModel(defaultSinkES);

        // Verification
        assertThat(model.getId(), is(defaultSinkES.getId()));
        assertThat(model.getVersion(), is(defaultSinkES.getVersion()));
        assertThat(model.getSinkName(), is(defaultSinkContentES.getName()));
        assertThat(model.getQueue(), is(defaultSinkContentES.getQueue()));
        assertThat(model.getDescription(), is(defaultSinkContentES.getDescription()));
        assertThat(model.getSinkType(), is(defaultSinkContentES.getSinkType()));
        assertThat(model.getSinkConfig(), is(defaultSinkContentES.getSinkConfig()));
    }

    @Test(expected = NullPointerException.class)
    public void toSinkContent_nullInput_throws() {
        // Activate Subject Under Test
        SinkModelMapper.toSinkContent(null);
    }

    @Test
    public void toSinkContent_validEsInput_returnsValidSinkContent() {
        // Activate Subject Under Test
        SinkContent sinkContent = SinkModelMapper.toSinkContent(defaultSinkModelES);

        // Verification
        assertThat(sinkContent.getName(), is(defaultSinkModelES.getSinkName()));
        assertThat(sinkContent.getQueue(), is(defaultSinkModelES.getQueue()));
        assertThat(sinkContent.getDescription(), is(defaultSinkModelES.getDescription()));
        assertThat(sinkContent.getSinkType(), is(SinkContent.SinkType.ES));
        final EsSinkConfig esSinkConfig = (EsSinkConfig) sinkContent.getSinkConfig();
        assertThat(esSinkConfig.getUserId(), is(defaultSinkModelES.getEsUserId()));
        assertThat(esSinkConfig.getDatabaseName(), is(defaultSinkModelES.getEsDatabase()));
    }

    @Test
    public void toSinkContent_validOpenUpdateInput_returnsValidSinkContent() {
        // Activate Subject Under Test
        SinkContent sinkContent = SinkModelMapper.toSinkContent(defaultSinkModelOU);

        // Verification
        assertThat(sinkContent.getName(), is(defaultSinkModelOU.getSinkName()));
        assertThat(sinkContent.getQueue(), is(defaultSinkModelOU.getQueue()));
        assertThat(sinkContent.getDescription(), is(defaultSinkModelOU.getDescription()));
        assertThat(sinkContent.getSinkType(), is(SinkContent.SinkType.OPENUPDATE));
        final OpenUpdateSinkConfig sinkConfig = (OpenUpdateSinkConfig) sinkContent.getSinkConfig();
        assertThat(sinkConfig.getUserId(), is(defaultSinkModelOU.getOpenUpdateUserId()));
        assertThat(sinkConfig.getPassword(), is(defaultSinkModelOU.getOpenUpdatePassword()));
        assertThat(sinkConfig.getEndpoint(), is(defaultSinkModelOU.getOpenUpdateEndpoint()));
        assertThat(sinkConfig.getAvailableQueueProviders(), is(defaultSinkModelOU.getOpenUpdateAvailableQueueProviders()));
    }

    @Test(expected = NullPointerException.class)
    public void toListOfSinkModels_nullInput_throws() {
        // Activate Subject Under Test
        SinkModelMapper.toListOfSinkModels(null);
    }

    @Test
    public void toListOfSinkModels_emptyInputList_returnsEmptyListOfSinkModels() {
        // Activate Subject Under Test
        List<SinkModel> sinkModels = SinkModelMapper.toListOfSinkModels(new ArrayList<>());

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

    @Test
    public void toSinkContent_invalidSinkName_throwsIllegalArgumentException() {
        final String sinkName = "*%(Illegal)_&Name - €";
        final String expectedIllegalCharacters = "[*], [%], [(], [)], [&], [€]";
        SinkModel model = new SinkModelBuilder().setName(sinkName).setSinkType(SinkContent.SinkType.DUMMY).build();
        try {
            SinkModelMapper.toSinkContent(model);
            fail("Illegal sink name not detected");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage().contains(expectedIllegalCharacters), is(true));
        }
    }
}
