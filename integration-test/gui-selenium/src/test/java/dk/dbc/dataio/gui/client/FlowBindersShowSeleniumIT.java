package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.SubmitterContent;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.SubmitterContentBuilder;
import dk.dbc.dataio.gui.client.pages.flowbinder.show.FlowBindersShowViewImpl;
import dk.dbc.dataio.gui.util.ClientFactoryImpl;
import dk.dbc.dataio.integrationtest.ITUtil;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.WebDriver;

import javax.ws.rs.client.Client;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class FlowBindersShowSeleniumIT extends AbstractGuiSeleniumTest {
    private static ConstantsProperties flowBinderCreationTexts = new ConstantsProperties("flowbinder/modify/Texts_dk.properties");

    private final static String SUBMITTER_NAME = "SubmitterName";
    private final static String SUBMITTER_DESCRIPTION = "SubmitterDescription";
    private final static String SINK_NAME = "SinkName";
    private final static String FLOW_NAME = "FlowName";
    private final static String FLOW_DESCRIPTION = "FlowDescription";
    private final static String FLOW_COMPONENT_NAME = "FlowComponentName";
    private final static String FLOW_BINDER_NAME = "FloBinderName";
    private final static String FLOW_BINDER_DESCRIPTION = "FloBinderDescription";
    private final static String FLOW_BINDER_FRAME_FORMAT = "FloBinderFrameFormat";
    private final static String FLOW_BINDER_CONTENT_FORMAT = "FloBinderContentFormat";
    private final static String FLOW_BINDER_CHAR_SET = "FloBinderCharSet";
    private final static String FLOW_BINDER_DESTINATION = "FloBinderDestination";
    private final static String FLOW_BINDER_RECORD_SPLITTER = "FloBinderRecordSplitter";
    private static final String SINK_CREATION_KNOWN_RESOURCE_NAME = "jdbc/flowStoreDb";

    private static FlowStoreServiceConnector flowStoreServiceConnector;

    @BeforeClass
    public static void setUpClass() throws ClassNotFoundException, SQLException {
        String baseUrl = ITUtil.FLOW_STORE_BASE_URL;
        Client restClient = HttpClient.newClient();
        flowStoreServiceConnector = new FlowStoreServiceConnector(restClient, baseUrl);
    }

    @Test
    public void testFlowBindersShowEmptyList_NoContentIsShown() {
        navigateToFlowBindersShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, FlowBindersShowViewImpl.GUIID_FLOW_BINDERS_SHOW_WIDGET);
        table.waitAssertNoRows();
    }

    @Test
    public void testFlowBindersInsertTwoRows_TwoElementsShown() throws Exception{
        // Create necessary elements:
        Submitter submitter11 = createTestSubmitter(11);  // Submitter #11
        Submitter submitter12 = createTestSubmitter(12);  // Submitter #12
        FlowComponent flowComponent13 = createTestFlowComponent(13);    // FlowComponent #13
        FlowComponent flowComponent110 = createTestFlowComponent(110);  // FlowComponent #110
        Flow flow14 = createTestFlow(14, java.util.Arrays.asList(flowComponent13));   // Flow #14, containing FlowComponent #13
        Flow flow18 = createTestFlow(18, java.util.Arrays.asList(flowComponent110));  // Flow #18, containing FlowComponent #110
        Sink sink15 = createTestSink(15);  // Sink #15
        Sink sink19 = createTestSink(19);  // Sink #19
        createTestFlowBinder(16, Arrays.asList(submitter11.getId()), flow14.getId(), sink15.getId());  // Flowbinder #16 containing Submitters (#11), Flow #14 and Sink #15
        createTestFlowBinder(17, Arrays.asList(submitter11.getId(), submitter12.getId()), flow18.getId(), sink19.getId());  // Flowbinder #17 containing Submitter(s) (#11, #12), Flow #18 and Sink #19

        navigateToFlowBindersShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, FlowBindersShowViewImpl.GUIID_FLOW_BINDERS_SHOW_WIDGET);
        table.waitAssertRows(2);
        List<List<SeleniumGWTTable.Cell>> rowData = table.get();
        assertThat(rowData.get(0).get(0).getCellContent(), is(subjectNameString(FLOW_BINDER_NAME, 16)));  // Navn
        assertThat(rowData.get(0).get(1).getCellContent(), is(subjectNameString(FLOW_BINDER_DESCRIPTION, 16)));  // Beskrivelse
        assertThat(rowData.get(0).get(2).getCellContent(), is(subjectNameString(FLOW_BINDER_FRAME_FORMAT, 16)));  // Rammeformat
        assertThat(rowData.get(0).get(3).getCellContent(), is(subjectNameString(FLOW_BINDER_CONTENT_FORMAT, 16)));  //Indholdsformat
        assertThat(rowData.get(0).get(4).getCellContent(), is(subjectNameString(FLOW_BINDER_CHAR_SET, 16)));  // Tegnsæt
        assertThat(rowData.get(0).get(5).getCellContent(), is(subjectNameString(FLOW_BINDER_DESTINATION, 16)));  // Destination
        assertThat(rowData.get(0).get(6).getCellContent(), is(subjectNameString(FLOW_BINDER_RECORD_SPLITTER, 16)));  // Recordsplitter
        assertThat(rowData.get(0).get(7).getCellContent(), is(submitterPairString(11)));  // Submittere
        assertThat(rowData.get(0).get(8).getCellContent(), is(subjectNameString(FLOW_NAME, 14)));  // Flow
        assertThat(rowData.get(0).get(9).getCellContent(), is(subjectNameString(SINK_NAME, 15)));  // Sink
        assertThat(rowData.get(1).get(0).getCellContent(), is(subjectNameString(FLOW_BINDER_NAME, 17)));  // Navn
        assertThat(rowData.get(1).get(1).getCellContent(), is(subjectNameString(FLOW_BINDER_DESCRIPTION, 17)));  // Beskrivelse
        assertThat(rowData.get(1).get(2).getCellContent(), is(subjectNameString(FLOW_BINDER_FRAME_FORMAT, 17)));  // Rammeformat
        assertThat(rowData.get(1).get(3).getCellContent(), is(subjectNameString(FLOW_BINDER_CONTENT_FORMAT, 17)));  //Indholdsformat
        assertThat(rowData.get(1).get(4).getCellContent(), is(subjectNameString(FLOW_BINDER_CHAR_SET, 17)));  // Tegnsæt
        assertThat(rowData.get(1).get(5).getCellContent(), is(subjectNameString(FLOW_BINDER_DESTINATION, 17)));  // Destination
        assertThat(rowData.get(1).get(6).getCellContent(), is(subjectNameString(FLOW_BINDER_RECORD_SPLITTER, 17)));  // Recordsplitter
        assertThat(rowData.get(1).get(7).getCellContent(), is(submitterPairString(11) + ", " + submitterPairString(12)));  // Submittere
        assertThat(rowData.get(1).get(8).getCellContent(), is(subjectNameString(FLOW_NAME, 18)));  // Flow
        assertThat(rowData.get(1).get(9).getCellContent(), is(subjectNameString(SINK_NAME, 19)));  // Sink

    }

    private static void navigateToFlowBindersShowWidget(WebDriver webDriver) {
        NavigationPanelSeleniumIT.navigateTo(webDriver, ClientFactoryImpl.GUIID_MENU_ITEM_FLOW_BINDERS_SHOW);
    }


    private static String subjectNameString(String name, long number) {
        return name + "_" + Long.toString(number);
    }

    private static String submitterPairString(int number) {
        return Long.toString(number) + " (" + subjectNameString(SUBMITTER_NAME, number) + ")";
    }

    private static Submitter createTestSubmitter(int number) throws Exception{
        SubmitterContent submitterContent = new SubmitterContentBuilder()
                .setName(subjectNameString(SUBMITTER_NAME, number))
                .setNumber(new Long(number))
                .setDescription(subjectNameString(SUBMITTER_DESCRIPTION, number))
                .build();
        return flowStoreServiceConnector.createSubmitter(submitterContent);
    }

    private static Sink createTestSink(int number) throws Exception{
        SinkContent sinkContent = new SinkContentBuilder()
                .setName(subjectNameString(SINK_NAME, number))
                .setResource(SINK_CREATION_KNOWN_RESOURCE_NAME)
                .build();
        return flowStoreServiceConnector.createSink(sinkContent);
    }

    private static Flow createTestFlow(int flowNumber, List<FlowComponent> flowComponents) throws Exception{
        FlowContent flowContent = new FlowContentBuilder()
                .setName(subjectNameString(FLOW_NAME, flowNumber))
                .setDescription(subjectNameString(FLOW_DESCRIPTION, flowNumber))
                .setComponents(flowComponents)
                .build();
        return flowStoreServiceConnector.createFlow(flowContent);
    }

    private static FlowComponent createTestFlowComponent(int number) throws Exception{
        FlowComponentContent flowComponentContent = new FlowComponentContentBuilder()
                .setName(subjectNameString(FLOW_COMPONENT_NAME, number))
                .build();

        return flowStoreServiceConnector.createFlowComponent(flowComponentContent);
    }

    private FlowBinder createTestFlowBinder(long flowBinder, List<Long> submitters, long flow, long sink) throws FlowStoreServiceConnectorException {
        FlowBinderContent flowBinderContent = new FlowBinderContentBuilder()
                .setName(subjectNameString(FLOW_BINDER_NAME, flowBinder))
                .setDescription(subjectNameString(FLOW_BINDER_DESCRIPTION, flowBinder))
                .setPackaging(subjectNameString(FLOW_BINDER_FRAME_FORMAT, flowBinder))
                .setFormat(subjectNameString(FLOW_BINDER_CONTENT_FORMAT, flowBinder))
                .setCharset(subjectNameString(FLOW_BINDER_CHAR_SET, flowBinder))
                .setDestination(subjectNameString(FLOW_BINDER_DESTINATION, flowBinder))
                .setRecordSplitter(subjectNameString(FLOW_BINDER_RECORD_SPLITTER, flowBinder))
                .setSubmitterIds(submitters)
                .setFlowId(flow)
                .setSinkId(sink)
                .build();
        return flowStoreServiceConnector.createFlowBinder(flowBinderContent);
    }
}