package dk.dbc.dataio.gui.client;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.gui.client.pages.sinksshow.SinksShowViewImpl;
import dk.dbc.dataio.gui.util.ClientFactoryImpl;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import javax.ws.rs.client.Client;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static dk.dbc.dataio.integrationtest.ITUtil.clearAllDbTables;
import static dk.dbc.dataio.integrationtest.ITUtil.newDbConnection;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class SinksShowSeleniumIT extends AbstractGuiSeleniumTest {
    final String SINK_NAME_1 = "NamoUno";
    final String SINK_NAME_2 = "NamoDuo";
    final String BUTTON_NAME = "Rediger";
    final String RESOURCE_NAME = "jdbc/flowStoreDb";

    private static Connection dbConnection;
    private static FlowStoreServiceConnector flowStoreServiceConnector;

    @BeforeClass
    public static void setUpClass() throws ClassNotFoundException, SQLException {
        String baseUrl = String.format("http://localhost:%s/flow-store", System.getProperty("glassfish.port"));
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
    public void testSinksShowEmptyList_NoContentIsShown() throws Exception {
        navigateToSinksShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, SinksShowViewImpl.GUIID_SINKS_SHOW_WIDGET);
        table.waitAssertNoRows();
    }

    @Test
    public void testSinksShowInsertTwoRows_TwoElementsShown() throws Exception{

        Sink sink1 = createTestSink(SINK_NAME_1, RESOURCE_NAME);
        Sink sink2 = createTestSink(SINK_NAME_2, RESOURCE_NAME);

        navigateToSinksShowWidget(webDriver);

        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, SinksShowViewImpl.GUIID_SINKS_SHOW_WIDGET);
        table.waitAssertRows(2);

        List<List<String>> rowData = table.get();
        assertNotNull(rowData);

        for (List <String> row : rowData){
            assertNotNull(row );
            assertTrue(row.size() == 3);
        }

        assertThat(rowData.get(0).get(0), is(sink2.getContent().getName()));
        assertThat(rowData.get(0).get(1), is(sink2.getContent().getResource()));
        assertThat(rowData.get(0).get(2), is(BUTTON_NAME));

        assertThat(rowData.get(1).get(0), is(sink1.getContent().getName()));
        assertThat(rowData.get(1).get(1), is(sink1.getContent().getResource()));
        assertThat(rowData.get(1).get(2), is(BUTTON_NAME));
    }

    @Test
    public void testSinksShowClickEditButton_NavigateToSinkCreationEditWidget() throws Exception{

        //Create new sink
        createTestSink(SINK_NAME_1, RESOURCE_NAME);

        //Navigate to the sink show window.
        navigateToSinksShowWidget(webDriver);

        //Navigate to the first row, locate the edit button and click.
        locateAndClickEditButtonForElement(0);

        //Assert that the SinkCreateEditView is opened.
        assertTrue(webDriver.getCurrentUrl().contains("#EditSink"));
    }

    /**
     * The following is private static helper methods.
     */
    private static Sink createTestSink(String sinkName, String resource) throws Exception{
        SinkContent sinkContent = new SinkContentBuilder()
                .setName(sinkName)
                .setResource(resource)
                .build();

        return flowStoreServiceConnector.createSink(sinkContent);
    }

    /**
     * The following is public static helper methods.
     */
    public static void navigateToSinksShowWidget(WebDriver webDriver) {
        NavigationPanelSeleniumIT.navigateTo(webDriver, ClientFactoryImpl.GUIID_MENU_ITEM_SINKS_SHOW);
    }

    public static void locateAndClickEditButtonForElement(int index){
        WebElement element = SeleniumUtil.findElementInCurrentView(webDriver, SinksShowViewImpl.GUUID_SHOW_SINK_TABLE_EDIT, SinksShowViewImpl.CLASS_SINK_SHOW_WIDGET_EDIT_BUTTON, index);
        element.findElement(By.tagName("button")).click();
    }
}
