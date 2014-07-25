package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentContentBuilder;
import dk.dbc.dataio.gui.client.pages.flowcomponentsshow.FlowComponentsShowViewImpl;
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

public class FlowComponentsShowSeleniumIT extends AbstractGuiSeleniumTest {

    private static FlowStoreServiceConnector flowStoreServiceConnector;

    @BeforeClass
    public static void setUpClass() throws ClassNotFoundException, SQLException {
        String baseUrl = ITUtil.FLOW_STORE_BASE_URL;
        Client restClient = HttpClient.newClient();
        flowStoreServiceConnector = new FlowStoreServiceConnector(restClient, baseUrl);
    }

    @Test
    public void testFlowComponentsShowEmptyList_NoContentIsShown() throws TimeoutException, Exception {
        navigateToFlowComponentsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, FlowComponentsShowViewImpl.GUIID_FLOW_COMPONENTS_SHOW_WIDGET);
        table.waitAssertNoRows();
    }

    @Test
    public void testFlowComponentsInsertTwoRows_TwoElementsShown() throws Exception{
        final String COMPONENT_NAME_1 = "FlowCoOne";
        final String COMPONENT_NAME_2 = "FlowCoTwo";

        createTestFlowComponent(COMPONENT_NAME_1);
        createTestFlowComponent(COMPONENT_NAME_2);
        navigateToFlowComponentsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, FlowComponentsShowViewImpl.GUIID_FLOW_COMPONENTS_SHOW_WIDGET);
        table.waitAssertRows(2);
        List<List<SeleniumGWTTable.Cell>> rowData = table.get();
        assertThat(rowData.get(0).get(0).getCellContent(), is(COMPONENT_NAME_1));
        assertThat(rowData.get(0).get(1).getCellContent(), is("invocationJavascriptName"));
        assertThat(rowData.get(0).get(2).getCellContent(), is("invocationMethod"));
        assertThat(rowData.get(0).get(3).getCellContent(), is("svnprojectforinvocationjavascript"));
        assertThat(rowData.get(0).get(4).getCellContent(), is("1"));
        assertThat(rowData.get(0).get(5).getCellContent(), is("moduleName"));
        assertThat(rowData.get(1).get(0).getCellContent(), is(COMPONENT_NAME_2));
        assertThat(rowData.get(1).get(1).getCellContent(), is("invocationJavascriptName"));
        assertThat(rowData.get(1).get(2).getCellContent(), is("invocationMethod"));
        assertThat(rowData.get(1).get(3).getCellContent(), is("svnprojectforinvocationjavascript"));
        assertThat(rowData.get(1).get(4).getCellContent(), is("1"));
        assertThat(rowData.get(1).get(5).getCellContent(), is("moduleName"));
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
