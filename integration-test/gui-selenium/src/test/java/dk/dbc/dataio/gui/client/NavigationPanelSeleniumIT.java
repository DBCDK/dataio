package dk.dbc.dataio.gui.client;

import static dk.dbc.dataio.gui.client.SeleniumUtil.findElementInCurrentView;
import dk.dbc.dataio.gui.client.views.Menu;
import dk.dbc.dataio.gui.client.views.NavigationPanel;
import dk.dbc.dataio.integrationtest.ITUtil;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;

public class NavigationPanelSeleniumIT {
    private static ConstantsProperties texts = new ConstantsProperties("MenuConstants_dk.properties");

    private WebDriver driver;
    private static String appUrl;
    private static Connection conn;

        @BeforeClass
    public static void setUpClass() throws ClassNotFoundException, SQLException {
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
        ITUtil.clearAllDbTables(conn);
        driver.quit();
    }

    @Test
    public void testNavigationMenuPanelVisible() {
        WebElement navigationPanelElement = findNavigationPanelElement(driver);
        assertTrue(navigationPanelElement.isDisplayed());
    }

    @Test
    public void testMainMenuItemsVisible() {
        assertTrue(findNavigationElement(driver, Menu.GUIID_MAIN_MENU_ITEM_SUBMITTERS).isDisplayed());
        assertTrue(findNavigationElement(driver, Menu.GUIID_MAIN_MENU_ITEM_FLOWS).isDisplayed());
        assertTrue(findNavigationElement(driver, Menu.GUIID_MAIN_MENU_ITEM_SINKS).isDisplayed());
    }

    @Test
    public void testSubmitterMenuItemsVisible() {
        findNavigationElement(driver, Menu.GUIID_MAIN_MENU_ITEM_SUBMITTERS).click();
        assertTrue(findNavigationElement(driver, Menu.GUIID_SUB_MENU_ITEM_SUBMITTER_CREATION).isDisplayed());
    }

    @Test
    public void testFlowsMenuItemsVisible() {
        findNavigationElement(driver, Menu.GUIID_MAIN_MENU_ITEM_FLOWS).click();
        assertTrue(findNavigationElement(driver, Menu.GUIID_SUB_MENU_ITEM_FLOW_CREATION).isDisplayed());
        assertTrue(findNavigationElement(driver, Menu.GUIID_SUB_MENU_ITEM_FLOW_COMPONENT_CREATION).isDisplayed());
        assertTrue(findNavigationElement(driver, Menu.GUIID_SUB_MENU_ITEM_FLOW_COMPONENTS_SHOW).isDisplayed());
        assertTrue(findNavigationElement(driver, Menu.GUIID_SUB_MENU_ITEM_FLOWBINDER_CREATION).isDisplayed());
    }

    @Test
    public void testSinksMenuItemsVisible() {
        findNavigationElement(driver, Menu.GUIID_MAIN_MENU_ITEM_SINKS).click();
        assertTrue(findNavigationElement(driver, Menu.GUIID_SUB_MENU_ITEM_SINK_CREATION).isDisplayed());
    }


    // Utility methods
    private static WebElement findNavigationPanelElement(WebDriver webDriver) {
        return findElementInCurrentView(webDriver, NavigationPanel.GUIID_NAVIGATION_MENU_PANEL);
    }

    private static WebElement findNavigationElement(WebDriver webDriver, String menuId) {
        return webDriver.findElement(By.id(menuId));
    }

    public static void navigateTo(WebDriver webDriver, String menuId) {
        WebElement menuElement = webDriver.findElement(By.id(menuId));
        assertTrue(menuElement != null);
        if (!menuElement.isDisplayed()) {  // The menu in question is not displayed, so we need to make it visible
            switch (menuId) {
                case Menu.GUIID_SUB_MENU_ITEM_SUBMITTER_CREATION:
                    findNavigationElement(webDriver, Menu.GUIID_MAIN_MENU_ITEM_SUBMITTERS).click();
                    break;
                case Menu.GUIID_SUB_MENU_ITEM_FLOW_CREATION:
                case Menu.GUIID_SUB_MENU_ITEM_FLOW_COMPONENT_CREATION:
                case Menu.GUIID_SUB_MENU_ITEM_FLOW_COMPONENTS_SHOW:
                case Menu.GUIID_SUB_MENU_ITEM_FLOWBINDER_CREATION:
                    findNavigationElement(webDriver, Menu.GUIID_MAIN_MENU_ITEM_FLOWS).click();
                    break;
                case Menu.GUIID_SUB_MENU_ITEM_SINK_CREATION:
                    findNavigationElement(webDriver, Menu.GUIID_MAIN_MENU_ITEM_SINKS).click();
                    break;
            }
        }
        menuElement.click();  // Now the element is visble, click on it
    }

}
