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
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class FlowComponentsShowSeleniumIT {
//    private static ConstantsProperties texts = new ConstantsProperties("Constants_dk.properties");

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
    public void testInitialVisibilityOfElements() throws IOException {
        testFlowComponentsShowNavigationItemIsVisibleAndClickable();
        testFlowComponentsShowEmptyList_NoContentIsShown();
    }

    public void testFlowComponentsShowNavigationItemIsVisibleAndClickable() {
        assertTrue("Navigation element 'Flowkomponenter' is not shown", findFlowComponentCreateNavigationElement(driver).isDisplayed());
        findFlowComponentCreateNavigationElement(driver).click();
        assertTrue("Flow Component View is not shown", findFlowComponentsShowWidget(driver).isDisplayed());
    }

    public void testFlowComponentsShowEmptyList_NoContentIsShown() {
        navigateToFlowComponentsShowWidget(driver);
        assertTrue("Flow Component table is not empty as expected", findFlowComponentsShowTableElements(driver).isEmpty());
    }

    @Test
    public void testFlowComponentsShowNotEmptyList_ElementsShown() {
        final String COMPONENT_NAME = "Flow-component-name";
        FlowComponentCreationSeleniumIT.createTestFlowComponent(driver, COMPONENT_NAME);
        navigateToFlowComponentsShowWidget(driver);
        waitForTableDataAndAssert(driver, 2, 0, COMPONENT_NAME);
    }
    
//    @Test
//    public void testxxx() {
//        navigateToFlowComponentsShowWidget(driver);
//        tableData = driver.getTable();
//    }
    
    private static void navigateToFlowComponentsShowWidget(WebDriver webDriver) {
        findFlowComponentCreateNavigationElement(webDriver).click();
    }

    private static WebElement findFlowComponentCreateNavigationElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, MainPanel.GUIID_NAVIGATION_MENU_ITEM_FLOW_COMPONENTS_SHOW);
    }

    private static WebElement findFlowComponentsShowWidget(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowComponentsShowViewImpl.GUIID_FLOW_COMPONENTS_SHOW_WIDGET);
    }

    private static List<WebElement> findFlowComponentsShowTableElements(WebDriver webDriver) {
        return driver.findElement(By.id(FlowComponentsShowViewImpl.GUIID_FLOW_COMPONENTS_SHOW_WIDGET)).findElements(By.cssSelector("table>tbody>tr[__gwt_row=\"0\"]>td>div"));
    }
    
    private static void waitForTableDataAndAssert(WebDriver webDriver, long timeToWait, long row, String expectedText) {
        WebDriverWait wait = new WebDriverWait(webDriver, timeToWait);
        String cssSelector = "#" + FlowComponentsShowViewImpl.GUIID_FLOW_COMPONENTS_SHOW_WIDGET;
        cssSelector += ">table>tbody>tr[__gwt_row=\"" + row + "\"]>td>div";
        wait.until(ExpectedConditions.textToBePresentInElement(By.cssSelector(cssSelector), expectedText));
    }
    
}
