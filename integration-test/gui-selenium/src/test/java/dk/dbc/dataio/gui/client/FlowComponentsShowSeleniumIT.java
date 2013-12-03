package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.gui.client.views.FlowComponentsShowViewImpl;
import dk.dbc.dataio.gui.client.views.MainPanel;
import dk.dbc.dataio.integrationtest.ITUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.tmatesoft.svn.core.SVNException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.is;

import org.openqa.selenium.TimeoutException;

public class FlowComponentsShowSeleniumIT {
    private static WebDriver driver;
    private static String appUrl;
    private static Connection conn;
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

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

    @Test
    public void testFlowComponentsShowNavigationItemIsVisibleAndClickable() {
        assertTrue("Navigation element 'Flowkomponenter' is not shown", findFlowComponentCreateNavigationElement(driver).isDisplayed());
        findFlowComponentCreateNavigationElement(driver).click();
        assertTrue("Flow Component View is not shown", findFlowComponentsShowWidget(driver).isDisplayed());
    }

    @Test(expected = TimeoutException.class)
    public void testFlowComponentsShowEmptyList_NoContentIsShown() throws TimeoutException {
        navigateToFlowComponentsShowWidget(driver);
        SeleniumGWTTable table = new SeleniumGWTTable(driver, FlowComponentsShowViewImpl.GUIID_FLOW_COMPONENTS_SHOW_WIDGET);
        table.waitAssertNextRowPresent();
    }

    @Test
    public void testFlowComponentsInsertOneRow_OneElementShown() {
        final String COMPONENT_NAME = "Flow-component-name";
        FlowComponentCreationSeleniumIT.createTestFlowComponent(driver, COMPONENT_NAME);
        navigateToFlowComponentsShowWidget(driver);
        SeleniumGWTTable table = new SeleniumGWTTable(driver, FlowComponentsShowViewImpl.GUIID_FLOW_COMPONENTS_SHOW_WIDGET);
        table.waitAssertNextRowPresent();
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
        table.waitAssertNextRowPresent(2);
        List<List<String>> rowData = table.get();
        assertThat(rowData.get(0).get(0), is(COMPONENT_NAME_1));
        assertThat(rowData.get(0).get(1), is("invocationMethod"));
        assertThat(rowData.get(1).get(0), is(COMPONENT_NAME_2));
        assertThat(rowData.get(1).get(1), is("invocationMethod"));
    }
    
    private static void navigateToFlowComponentsShowWidget(WebDriver webDriver) {
        findFlowComponentCreateNavigationElement(webDriver).click();
    }

    private static WebElement findFlowComponentCreateNavigationElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, MainPanel.GUIID_NAVIGATION_MENU_ITEM_FLOW_COMPONENTS_SHOW);
    }

    private static WebElement findFlowComponentsShowWidget(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowComponentsShowViewImpl.GUIID_FLOW_COMPONENTS_SHOW_WIDGET);
    }

}
