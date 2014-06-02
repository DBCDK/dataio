package dk.dbc.dataio.gui.client;

import dk.dbc.commons.jdbc.util.JDBCUtil;
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
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;

import javax.ws.rs.client.Client;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static dk.dbc.dataio.integrationtest.ITUtil.clearAllDbTables;
import static dk.dbc.dataio.integrationtest.ITUtil.newDbConnection;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class FlowsShowSeleniumIT extends AbstractGuiSeleniumTest {

    private static Connection dbConnection;
    private static FlowStoreServiceConnector flowStoreServiceConnector;

    @BeforeClass
    public static void setUpClass() throws ClassNotFoundException, SQLException {
        String baseUrl = ITUtil.FLOW_STORE_BASE_URL;
        Client restClient = HttpClient.newClient();
        dbConnection = newDbConnection("flow_store");
        flowStoreServiceConnector = new FlowStoreServiceConnector(restClient, baseUrl);
    }

    @AfterClass
    public static void tearDownClass() throws SQLException {
        JDBCUtil.closeConnection(dbConnection);
    }

    @After
    public void tearDown() throws SQLException {
        clearAllDbTables(dbConnection);
    }

    @Test
    public void testFlowsShowEmptyList_NoContentIsShown() throws TimeoutException, Exception {
        navigateToFlowsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, FlowsShowViewImpl.GUIID_FLOWS_SHOW_WIDGET);
        table.waitAssertNoRows();
    }

    @Test
    public void testFlowsInsertOneRow_OneElementShown() throws Exception{
        FlowComponent flowComponent = createTestFlowComponent("Flow-component-name");
        Flow flow = createTestFlow("Flow-name", "Flow-description", java.util.Arrays.asList(flowComponent));
        navigateToFlowsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, FlowsShowViewImpl.GUIID_FLOWS_SHOW_WIDGET);
        table.waitAssertRows(1);

        List<String> rowData = table.getRow(0);
        assertThat(rowData.get(0), is(flow.getContent().getName()));
        assertThat(rowData.get(1), is(flow.getContent().getDescription()));
        assertThat(rowData.get(2), is(flow.getContent().getComponents().get(0).getContent().getName()));
    }

    @Test
    public void testFlowsInsertTwoRows_TwoElementsShown() throws Exception{
        FlowComponent flowComponentA = createTestFlowComponent("FCompo 1");
        FlowComponent flowComponentB = createTestFlowComponent("FCompo 2");
        Flow flowA = createTestFlow("NamoUno", "Description 11", java.util.Arrays.asList(flowComponentA));
        Flow flowB = createTestFlow("NamoDuo", "Description 22", java.util.Arrays.asList(flowComponentB));
        navigateToFlowsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, FlowsShowViewImpl.GUIID_FLOWS_SHOW_WIDGET);
        table.waitAssertRows(2);
        List<List<String>> rowData = table.get();

        assertThat(rowData.get(0).get(0), is(flowB.getContent().getName()));
        assertThat(rowData.get(0).get(1), is(flowB.getContent().getDescription()));
        assertThat(rowData.get(0).get(2), is(flowB.getContent().getComponents().get(0).getContent().getName()));
        assertThat(rowData.get(1).get(0), is(flowA.getContent().getName()));
        assertThat(rowData.get(1).get(1), is(flowA.getContent().getDescription()));
        assertThat(rowData.get(1).get(2), is(flowA.getContent().getComponents().get(0).getContent().getName()));
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
