package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.gui.client.pages.flowbindersshow.FlowBindersShowViewImpl;
import dk.dbc.dataio.gui.util.ClientFactoryImpl;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.openqa.selenium.WebDriver;

public class FlowBindersShowSeleniumIT extends AbstractGuiSeleniumTest {
    private static ConstantsProperties flowBinderCreationTexts = new ConstantsProperties("pages/flowbindercreate/FlowbinderCreateConstants_dk.properties");

    private final static String SUBMITTER_NAME = "SubmitterName";
    private final static String SUBMITTER_DESCRIPTION = "SubmitterDescription";
    private final static String SINK_NAME = "SinkName";
    private final static String RESOURCE_NAME = "ResourceName";
    private final static String FLOW_NAME = "FlowName";
    private final static String FLOW_DESCRIPTION = "FlowDescription";
    private final static String FLOW_COMPONENT_NAME = "FlowComponentName";
    private final static String FLOW_BINDER_NAME = "FloBinderName";
    private final static String FLOW_BINDER_DESCRIPTION = "FloBinderDescription";
    private final static String FLOW_BINDER_FRAME_FORMAT = "FloBinderFrameFormat";
    private final static String FLOW_BINDER_CONTENT_FORMAT = "FloBinderContentFormat";
    private final static String FLOW_BINDER_CHAR_SET = "FloBinderCharSet";
    private final static String FLOW_BINDER_DESTINATION = "FloBinderDestination";


    @Test
    public void testFlowBindersShowEmptyList_NoContentIsShown() {
        navigateToFlowBindersShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, FlowBindersShowViewImpl.GUIID_FLOW_BINDERS_SHOW_WIDGET);
        table.waitAssertNoRows();
    }

    @Test
    public void testFlowBindersInsertTwoRows_TwoElementsShown() {
        // Create necessary elements:
        createTestSubmitter(webDriver, 1);  // Submitter #1
        createTestSubmitter(webDriver, 2);  // Submitter #2
        createTestFlowComponent(webDriver, 3);  // FlowComponent #3
        createTestFlowComponent(webDriver, 10);  // FlowComponent #10
        createTestFlow(webDriver, 4, 3);  // Flow #4, containing FlowComponent #3
        createTestFlow(webDriver, 8, 10);  // Flow #8, containing FlowComponent #10
        createTestSink(webDriver, 5);  // Sink #5
        createTestSink(webDriver, 9);  // Sink #9
        createTestFlowBinder(webDriver, 6, Arrays.asList(1), 4, 5);  // Flowbinder #6 containing Submitters (#1), Flow #4 and Sink #5
        createTestFlowBinder(webDriver, 7, Arrays.asList(2), 8, 9);  // Flowbinder #7 containing Submitter(s) (#2, #1), Flow #8 and Sink #9

        navigateToFlowBindersShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, FlowBindersShowViewImpl.GUIID_FLOW_BINDERS_SHOW_WIDGET);
        table.waitAssertRows(2);
        List<List<String>> rowData = table.get();
        assertThat(rowData.get(0).get(0), is(subjectNameString(FLOW_BINDER_NAME, 6)));  // Navn
        assertThat(rowData.get(0).get(1), is(subjectNameString(FLOW_BINDER_DESCRIPTION, 6)));  // Beskrivelse
        assertThat(rowData.get(0).get(2), is(subjectNameString(FLOW_BINDER_FRAME_FORMAT, 6)));  // Rammeformat
        assertThat(rowData.get(0).get(3), is(subjectNameString(FLOW_BINDER_CONTENT_FORMAT, 6)));  //Indholdsformat
        assertThat(rowData.get(0).get(4), is(subjectNameString(FLOW_BINDER_CHAR_SET, 6)));  // Tegnsæt
        assertThat(rowData.get(0).get(5), is(subjectNameString(FLOW_BINDER_DESTINATION, 6)));  // Destination
        assertThat(rowData.get(0).get(6), is(flowBinderCreationTexts.translate("label_DefaultRecordSplitter")));  // Recordsplitter
//        assertThat(rowData.get(0).get(7), is(submitterPairString(1)));  // Submittere
        assertThat(rowData.get(0).get(7), is(subjectNameString(SUBMITTER_NAME, 1)));  // Submittere
        assertThat(rowData.get(0).get(8), is(subjectNameString(FLOW_NAME, 4)));  // Flow
        assertThat(rowData.get(0).get(9), is(subjectNameString(SINK_NAME, 5)));  // Sink
        assertThat(rowData.get(1).get(0), is(subjectNameString(FLOW_BINDER_NAME, 7)));  // Navn
        assertThat(rowData.get(1).get(1), is(subjectNameString(FLOW_BINDER_DESCRIPTION, 7)));  // Beskrivelse
        assertThat(rowData.get(1).get(2), is(subjectNameString(FLOW_BINDER_FRAME_FORMAT, 7)));  // Rammeformat
        assertThat(rowData.get(1).get(3), is(subjectNameString(FLOW_BINDER_CONTENT_FORMAT, 7)));  //Indholdsformat
        assertThat(rowData.get(1).get(4), is(subjectNameString(FLOW_BINDER_CHAR_SET, 7)));  // Tegnsæt
        assertThat(rowData.get(1).get(5), is(subjectNameString(FLOW_BINDER_DESTINATION, 7)));  // Destination
        assertThat(rowData.get(1).get(6), is(flowBinderCreationTexts.translate("label_DefaultRecordSplitter")));  // Recordsplitter
//        assertThat(rowData.get(1).get(7), is(submitterPairString(1) + ", " + submitterPairString(2)));  // Submittere
        assertThat(rowData.get(1).get(7), is(subjectNameString(SUBMITTER_NAME, 1) + ", " + subjectNameString(SUBMITTER_NAME, 2)));  // Submittere
        assertThat(rowData.get(1).get(8), is(subjectNameString(FLOW_NAME, 8)));  // Flow
        assertThat(rowData.get(1).get(9), is(subjectNameString(SINK_NAME, 9)));  // Sink

    }

    private static void navigateToFlowBindersShowWidget(WebDriver webDriver) {
        NavigationPanelSeleniumIT.navigateTo(webDriver, ClientFactoryImpl.GUIID_MENU_ITEM_FLOW_BINDERS_SHOW);
    }


    private static String subjectNameString(String name, int number) {
        return name + "_" + Long.toString(number);
    }

    private static String submitterPairString(int number) {
        return Long.toString(number) + " (" + subjectNameString(SUBMITTER_NAME, number) + ")";
    }

    private static void createTestSubmitter(WebDriver webDriver, int number) {
        SubmitterCreationSeleniumIT.createTestSubmitter(webDriver,
                                                        subjectNameString(SUBMITTER_NAME, number),
                                                        Long.toString(number),
                                                        subjectNameString(SUBMITTER_DESCRIPTION, number));
    }

    private static void createTestSink(WebDriver webDriver, int number) {
        SinkCreationSeleniumIT.createTestSink(webDriver,
                                              subjectNameString(SINK_NAME, number),
                                              SinkCreationSeleniumIT.SINK_CREATION_KNOWN_RESOURCE_NAME);
    }

    private void createTestFlow(WebDriver webDriver, int flowNumber, int flowComponentNumber) {
        FlowCreationSeleniumIT.createTestFlow(webDriver,
                                              subjectNameString(FLOW_NAME, flowNumber),
                                              subjectNameString(FLOW_DESCRIPTION, flowNumber),
                                              subjectNameString(FLOW_COMPONENT_NAME, flowComponentNumber));
    }

    private void createTestFlowComponent(WebDriver webDriver, int number) {
        FlowComponentCreationSeleniumIT.createTestFlowComponent(webDriver,
                                                                subjectNameString(FLOW_COMPONENT_NAME, number));
    }

    private void createTestFlowBinder(WebDriver webDriver, int flowBinder, List<Integer> submitters, int flow, int sink) {
        List<String> submitterNameList = new ArrayList<String>();
        for (Integer submitter: submitters) {
            submitterNameList.add(submitterPairString(submitter));
        }
        FlowBinderCreationSeleniumIT.createTestFlowBinder(webDriver,
                                                          subjectNameString(FLOW_BINDER_NAME, flowBinder),
                                                          subjectNameString(FLOW_BINDER_DESCRIPTION, flowBinder),
                                                          subjectNameString(FLOW_BINDER_FRAME_FORMAT, flowBinder),
                                                          subjectNameString(FLOW_BINDER_CONTENT_FORMAT, flowBinder),
                                                          subjectNameString(FLOW_BINDER_CHAR_SET, flowBinder),
                                                          subjectNameString(FLOW_BINDER_DESTINATION, flowBinder),
                                                          submitterNameList,
                                                          subjectNameString(FLOW_NAME, flow),
                                                          subjectNameString(SINK_NAME, sink));
    }
}
