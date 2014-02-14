package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.gui.client.pages.flowcomponentsshow.FlowComponentsShowViewImpl;
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
        assertThat(rowData.get(0).get(1), is("invocationJavascriptName"));
        assertThat(rowData.get(0).get(2), is("invocationMethod"));
        assertThat(rowData.get(0).get(3), is("svnprojectforinvocationjavascript"));
        assertThat(rowData.get(0).get(4), is("1"));
        assertThat(rowData.get(0).get(5), is("moduleName"));
        assertThat(rowData.get(1).get(0), is(COMPONENT_NAME_2));
        assertThat(rowData.get(1).get(1), is("invocationJavascriptName"));
        assertThat(rowData.get(1).get(2), is("invocationMethod"));
        assertThat(rowData.get(1).get(3), is("svnprojectforinvocationjavascript"));
        assertThat(rowData.get(1).get(4), is("1"));
        assertThat(rowData.get(1).get(5), is("moduleName"));
    }

    private static void navigateToFlowComponentsShowWidget(WebDriver webDriver) {
        NavigationPanelSeleniumIT.navigateTo(webDriver, ClientFactoryImpl.GUIID_MENU_ITEM_FLOW_COMPONENTS_SHOW);
    }

}
