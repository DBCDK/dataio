package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowContentBuilder;
import dk.dbc.dataio.gui.client.pages.flowsshow.FlowsShowViewImpl;
import dk.dbc.dataio.gui.util.ClientFactoryImpl;
import dk.dbc.dataio.integrationtest.ITUtil;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;

import javax.ws.rs.client.Client;
import java.sql.SQLException;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class FlowsShowSeleniumIT extends AbstractGuiSeleniumTest {

    private static FlowStoreServiceConnector flowStoreServiceConnector;

    @BeforeClass
    public static void setUpClass() throws ClassNotFoundException, SQLException {
        String baseUrl = ITUtil.FLOW_STORE_BASE_URL;
        Client restClient = HttpClient.newClient();
        flowStoreServiceConnector = new FlowStoreServiceConnector(restClient, baseUrl);
    }

    @Test
    public void testFlowsShowEmptyList_NoContentIsShown() throws TimeoutException, Exception {
        navigateToFlowsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, FlowsShowViewImpl.GUIID_FLOWS_SHOW_WIDGET);
        table.waitAssertNoRows();
    }

    @Test
    public void testFlowsInsertOneRow_OneElementShown() throws Exception{
        final String FLOW_NAME = "Flow-name";
        final String FLOW_DESCRIPTION = "Flow-description";
        final String FLOW_COMPONENT_NAME = "Flow-component-name";

        FlowComponent flowComponent = createTestFlowComponent(FLOW_COMPONENT_NAME);
        createTestFlow(FLOW_NAME, FLOW_DESCRIPTION, java.util.Arrays.asList(flowComponent));
        navigateToFlowsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, FlowsShowViewImpl.GUIID_FLOWS_SHOW_WIDGET);
        table.waitAssertRows(1);

        List<String> rowData = table.getRow(0);
        assertThat(rowData.get(0), is(FLOW_NAME));
        assertThat(rowData.get(1), is(FLOW_DESCRIPTION));
        assertThat(rowData.get(2), is(FLOW_COMPONENT_NAME));
    }

    @Test
    public void testFlowsInsertTwoRows_TwoElementsShown() throws Exception{
        final String FLOW_NAME_1 = "NamoUno";
        final String FLOW_DESCRIPTION_1 = "Description 11";
        final String FLOW_COMPONENT_NAME_1 = "FCompo 1";
        final String FLOW_NAME_2 = "NamoDuo";
        final String FLOW_DESCRIPTION_2 = "Description 22";
        final String FLOW_COMPONENT_NAME_2 = "FCompo 2";

        FlowComponent flowComponentA = createTestFlowComponent(FLOW_COMPONENT_NAME_1);
        FlowComponent flowComponentB = createTestFlowComponent(FLOW_COMPONENT_NAME_2);
        createTestFlow(FLOW_NAME_1, FLOW_DESCRIPTION_1, java.util.Arrays.asList(flowComponentA));
        createTestFlow(FLOW_NAME_2, FLOW_DESCRIPTION_2, java.util.Arrays.asList(flowComponentB));
        navigateToFlowsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, FlowsShowViewImpl.GUIID_FLOWS_SHOW_WIDGET);
        table.waitAssertRows(2);
        List<List<String>> rowData = table.get();

        assertThat(rowData.get(0).get(0), is(FLOW_NAME_2));
        assertThat(rowData.get(0).get(1), is(FLOW_DESCRIPTION_2));
        assertThat(rowData.get(0).get(2), is(FLOW_COMPONENT_NAME_2));
        assertThat(rowData.get(1).get(0), is(FLOW_NAME_1));
        assertThat(rowData.get(1).get(1), is(FLOW_DESCRIPTION_1));
        assertThat(rowData.get(1).get(2), is(FLOW_COMPONENT_NAME_1));
    }

    private static void navigateToFlowsShowWidget(WebDriver webDriver) {
        NavigationPanelSeleniumIT.navigateTo(webDriver, ClientFactoryImpl.GUIID_MENU_ITEM_FLOWS_SHOW);
    }

    private static Flow createTestFlow(String flowName, String flowDescription, List<FlowComponent> flowComponents) throws Exception{
        FlowContent flowContent = new FlowContentBuilder()
                .setName(flowName)
                .setDescription(flowDescription)
                .setComponents(flowComponents)
                .build();

        return flowStoreServiceConnector.createFlow(flowContent);
    }

    private static FlowComponent createTestFlowComponent(String flowComponentName) throws Exception{
        FlowComponentContent flowComponentContent = new FlowComponentContentBuilder()
                .setName(flowComponentName)
                .build();

        return flowStoreServiceConnector.createFlowComponent(flowComponentContent);
    }
}
