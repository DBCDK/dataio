package dk.dbc.dataio.gui.client;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentContentBuilder;
import dk.dbc.dataio.gui.client.pages.flowcomponentsshow.FlowComponentsShowViewImpl;
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

public class FlowComponentsShowSeleniumIT extends AbstractGuiSeleniumTest {

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
    public void testFlowComponentsShowEmptyList_NoContentIsShown() throws TimeoutException, Exception {
        navigateToFlowComponentsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, FlowComponentsShowViewImpl.GUIID_FLOW_COMPONENTS_SHOW_WIDGET);
        table.waitAssertNoRows();
    }

    @Test
    public void testFlowComponentsInsertTwoRows_TwoElementsShown() throws Exception{
        createTestFlowComponent("FlowCoOne");
        createTestFlowComponent("FlowCoTwo");
        navigateToFlowComponentsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, FlowComponentsShowViewImpl.GUIID_FLOW_COMPONENTS_SHOW_WIDGET);
        table.waitAssertRows(2);
        List<List<String>> rowData = table.get();
        assertThat(rowData.get(0).get(0), is("FlowCoOne"));
        assertThat(rowData.get(0).get(1), is("invocationJavascriptName"));
        assertThat(rowData.get(0).get(2), is("invocationMethod"));
        assertThat(rowData.get(0).get(3), is("svnprojectforinvocationjavascript"));
        assertThat(rowData.get(0).get(4), is("1"));
        assertThat(rowData.get(0).get(5), is("moduleName"));
        assertThat(rowData.get(1).get(0), is("FlowCoTwo"));
        assertThat(rowData.get(1).get(1), is("invocationJavascriptName"));
        assertThat(rowData.get(1).get(2), is("invocationMethod"));
        assertThat(rowData.get(1).get(3), is("svnprojectforinvocationjavascript"));
        assertThat(rowData.get(1).get(4), is("1"));
        assertThat(rowData.get(1).get(5), is("moduleName"));
    }

    private static FlowComponent createTestFlowComponent(String flowComponentName) throws Exception{
        FlowComponentContent flowComponentContent = new FlowComponentContentBuilder()
                .setName(flowComponentName)
                .build();

        return flowStoreServiceConnector.createFlowComponent(flowComponentContent);
    }

    private static void navigateToFlowComponentsShowWidget(WebDriver webDriver) {
        NavigationPanelSeleniumIT.navigateTo(webDriver, ClientFactoryImpl.GUIID_MENU_ITEM_FLOW_COMPONENTS_SHOW);
    }

}
