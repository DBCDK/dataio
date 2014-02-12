package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.gui.client.views.FlowComponentsShowViewImpl;
import dk.dbc.dataio.gui.util.ClientFactoryImpl;
import java.util.List;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;

public class FlowComponentsShowSeleniumIT extends AbstractGuiSeleniumTest {
    @Test
    public void testFlowComponentsShowEmptyList_NoContentIsShown() throws TimeoutException, Exception {
        navigateToFlowComponentsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, FlowComponentsShowViewImpl.GUIID_FLOW_COMPONENTS_SHOW_WIDGET);
        table.waitAssertNoRows();
    }

    @Test
    public void testFlowComponentsInsertOneRow_OneElementShown() {
        final String COMPONENT_NAME = "Flow-component-name";
        FlowComponentCreationSeleniumIT.createTestFlowComponent(webDriver, COMPONENT_NAME);
        navigateToFlowComponentsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, FlowComponentsShowViewImpl.GUIID_FLOW_COMPONENTS_SHOW_WIDGET);
        table.waitAssertRows();
        List<String> rowData = table.getRow(0);
        assertThat(rowData.get(0), is(COMPONENT_NAME));
        assertThat(rowData.get(1), is("invocationMethod"));
    }

    @Test
    public void testFlowComponentsInsertTwoRows_TwoElementsShown() {
        final String COMPONENT_NAME_1 = "FlowCoOne";
        final String COMPONENT_NAME_2 = "FlowCoTwo";
        FlowComponentCreationSeleniumIT.createTestFlowComponent(webDriver, COMPONENT_NAME_1);
        FlowComponentCreationSeleniumIT.createTestFlowComponent(webDriver, COMPONENT_NAME_2);
        navigateToFlowComponentsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, FlowComponentsShowViewImpl.GUIID_FLOW_COMPONENTS_SHOW_WIDGET);
        table.waitAssertRows(2);
        List<List<String>> rowData = table.get();
        assertThat(rowData.get(0).get(0), is(COMPONENT_NAME_1));
        assertThat(rowData.get(0).get(1), is("invocationMethod"));
        assertThat(rowData.get(1).get(0), is(COMPONENT_NAME_2));
        assertThat(rowData.get(1).get(1), is("invocationMethod"));
    }

    private static void navigateToFlowComponentsShowWidget(WebDriver webDriver) {
        NavigationPanelSeleniumIT.navigateTo(webDriver, ClientFactoryImpl.GUIID_SUB_MENU_ITEM_FLOW_COMPONENTS_SHOW);
    }

}
