package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.gui.client.views.FlowComponentsShowViewImpl;
import dk.dbc.dataio.gui.client.views.Menu;
import dk.dbc.dataio.integrationtest.ITUtil;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import static org.hamcrest.CoreMatchers.is;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.tmatesoft.svn.core.SVNException;

public class FlowComponentsShowSeleniumIT {
    private static WebDriver driver;
    private static String appUrl;
    private static Connection conn;

    @BeforeClass
    public static void setUpClass() throws ClassNotFoundException, SQLException, SVNException, URISyntaxException, IOException {
        appUrl = "http://localhost:" + System.getProperty("glassfish.port") + "/gui/gui.html";
        conn = ITUtil.newDbConnection();
    }

    @AfterClass
    public static void tearDownClass() throws SQLException {
        conn.close();
    }

    @Before
    public void setUp() {
        driver = new FirefoxDriver();
        driver.get(appUrl);
        driver.manage().timeouts().implicitlyWait(2, TimeUnit.SECONDS);
    }

    @After
    public void tearDown() throws SQLException {
        ITUtil.clearDbTables(conn, ITUtil.FLOW_COMPONENTS_TABLE_NAME);
        driver.quit();
    }


    @Test(expected = TimeoutException.class)
    public void testFlowComponentsShowEmptyList_NoContentIsShown() throws TimeoutException {
        navigateToFlowComponentsShowWidget(driver);
        SeleniumGWTTable table = new SeleniumGWTTable(driver, FlowComponentsShowViewImpl.GUIID_FLOW_COMPONENTS_SHOW_WIDGET);
        table.waitAssertRows();
    }

    @Test
    public void testFlowComponentsInsertOneRow_OneElementShown() {
        final String COMPONENT_NAME = "Flow-component-name";
        FlowComponentCreationSeleniumIT.createTestFlowComponent(driver, COMPONENT_NAME);
        navigateToFlowComponentsShowWidget(driver);
        SeleniumGWTTable table = new SeleniumGWTTable(driver, FlowComponentsShowViewImpl.GUIID_FLOW_COMPONENTS_SHOW_WIDGET);
        table.waitAssertRows();
        List<String> rowData = table.getRow(0);
        assertThat(rowData.get(0), is(COMPONENT_NAME));
        assertThat(rowData.get(1), is("invocationMethod"));
    }

    @Test
    public void testFlowComponentsInsertTwoRows_TwoElementsShown() {
        final String COMPONENT_NAME_1 = "FlowCoOne";
        final String COMPONENT_NAME_2 = "FlowCoTwo";
        FlowComponentCreationSeleniumIT.createTestFlowComponent(driver, COMPONENT_NAME_1);
        FlowComponentCreationSeleniumIT.createTestFlowComponent(driver, COMPONENT_NAME_2);
        navigateToFlowComponentsShowWidget(driver);
        SeleniumGWTTable table = new SeleniumGWTTable(driver, FlowComponentsShowViewImpl.GUIID_FLOW_COMPONENTS_SHOW_WIDGET);
        table.waitAssertRows(2);
        List<List<String>> rowData = table.get();
        assertThat(rowData.get(0).get(0), is(COMPONENT_NAME_1));
        assertThat(rowData.get(0).get(1), is("invocationMethod"));
        assertThat(rowData.get(1).get(0), is(COMPONENT_NAME_2));
        assertThat(rowData.get(1).get(1), is("invocationMethod"));
    }

    private static void navigateToFlowComponentsShowWidget(WebDriver webDriver) {
        NavigationPanelSeleniumIT.navigateTo(webDriver, Menu.GUIID_SUB_MENU_ITEM_FLOW_COMPONENTS_SHOW);
    }

    private static WebElement findFlowComponentCreateNavigationElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, Menu.GUIID_SUB_MENU_ITEM_FLOW_COMPONENTS_SHOW);
    }

    private static WebElement findFlowComponentsShowWidget(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowComponentsShowViewImpl.GUIID_FLOW_COMPONENTS_SHOW_WIDGET);
    }

}
