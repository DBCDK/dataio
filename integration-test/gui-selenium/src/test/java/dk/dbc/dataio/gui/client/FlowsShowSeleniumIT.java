package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.gui.client.pages.flowsshow.FlowsShowViewImpl;
import dk.dbc.dataio.gui.util.ClientFactoryImpl;
import java.util.List;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;

public class FlowsShowSeleniumIT extends AbstractGuiSeleniumTest {
    @Test
    public void testFlowsShowEmptyList_NoContentIsShown() throws TimeoutException, Exception {
        navigateToFlowsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, FlowsShowViewImpl.GUIID_FLOWS_SHOW_WIDGET);
        table.waitAssertNoRows();
    }

    @Test
    public void testFlowsInsertOneRow_OneElementShown() {

        final String FLOW_NAME = "Flow-name";
        final String FLOW_DESCRIPTION = "Flow-description";
        final String FLOW_COMPONENT_NAME = "Flow-component-name";
        FlowComponentCreationSeleniumIT.createTestFlowComponent(webDriver, FLOW_COMPONENT_NAME);
        FlowCreationSeleniumIT.createTestFlow(webDriver, FLOW_NAME, FLOW_DESCRIPTION, FLOW_COMPONENT_NAME);
        navigateToFlowsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, FlowsShowViewImpl.GUIID_FLOWS_SHOW_WIDGET);
        table.waitAssertRows(1);
        List<String> rowData = table.getRow(0);
        assertThat(rowData.get(0), is(FLOW_NAME));
        assertThat(rowData.get(1), is(FLOW_DESCRIPTION));
        assertThat(rowData.get(2), is(FLOW_COMPONENT_NAME));
    }

    @Test
    public void testFlowsInsertTwoRows_TwoElementsShown() {
        final String FLOW_NAME_1 = "NamoUno";
        final String FLOW_DESCRIPTION_1 = "Description 11";
        final String FLOW_COMPONENT_NAME_1 = "FCompo 1";
        final String FLOW_NAME_2 = "NamoDuo";
        final String FLOW_DESCRIPTION_2 = "Description 22";
        final String FLOW_COMPONENT_NAME_2 = "FCompo 2";
        FlowComponentCreationSeleniumIT.createTestFlowComponent(webDriver, FLOW_COMPONENT_NAME_1);
        FlowComponentCreationSeleniumIT.createTestFlowComponent(webDriver, FLOW_COMPONENT_NAME_2);
        FlowCreationSeleniumIT.createTestFlow(webDriver, FLOW_NAME_1, FLOW_DESCRIPTION_1, FLOW_COMPONENT_NAME_1);
        FlowCreationSeleniumIT.createTestFlow(webDriver, FLOW_NAME_2, FLOW_DESCRIPTION_2, FLOW_COMPONENT_NAME_2);
        navigateToFlowsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, FlowsShowViewImpl.GUIID_FLOWS_SHOW_WIDGET);
        table.waitAssertRows(2);
        List<List<String>> rowData = table.get();
        assertThat(rowData.get(0).get(0), is(FLOW_NAME_2));
        assertThat(rowData.get(0).get(1), is(FLOW_DESCRIPTION_2));
        assertThat(rowData.get(0).get(2), is(FLOW_COMPONENT_NAME_1 + ", " + FLOW_COMPONENT_NAME_2));
        assertThat(rowData.get(1).get(0), is(FLOW_NAME_1));
        assertThat(rowData.get(1).get(1), is(FLOW_DESCRIPTION_1));
        assertThat(rowData.get(1).get(2), is(FLOW_COMPONENT_NAME_1));
    }

    private static void navigateToFlowsShowWidget(WebDriver webDriver) {
        NavigationPanelSeleniumIT.navigateTo(webDriver, ClientFactoryImpl.GUIID_MAIN_MENU_ITEM_FLOWS);
    }

}
