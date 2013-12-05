package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.gui.client.views.MenuData;
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
import org.junit.Ignore;
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

    @Ignore
    @Test
    public void testMainMenuItemsVisible() {
        for (String mainMenuId: MenuData.findAllMainMenuIds()) {
            assertTrue(findNavigationElement(driver, mainMenuId).isDisplayed());
        }
    }
    
    
    // Private utility methods
    private static WebElement findNavigationPanelElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, NavigationPanel.GUIID_NAVIGATION_MENU_PANEL);
    }
    
    private static WebElement findNavigationElement(WebDriver webDriver, String menuId) {
        return webDriver.findElement(By.id(menuId));
    }
    
    public static void navigateTo(WebDriver webDriver, String menuId) {
        WebElement menuElement = webDriver.findElement(By.id(menuId));
        System.out.println("Navigate to: " + menuId);
        if (!menuElement.isDisplayed()) {  // The menu in question is not displayed, so we need to make it visible
            System.out.println("menuId is not displayed");
            if (MenuData.isSubMenuItem(menuId)) {  // Now we assume, that menuId is a Sub Menu (if this is not true, an exception is thrown)
                // Now find the parent Main Menu and click on it
                System.out.println("menuId is a sub menu, its parent menu is: " + MenuData.findMainMenuItem(menuId));
                webDriver.findElement(By.id(MenuData.findMainMenuItem(menuId))).click();
            }
        }
        menuElement.click();  // Now the element is visble, click on it
        System.out.println("return from navigate to");
    }
    
}
