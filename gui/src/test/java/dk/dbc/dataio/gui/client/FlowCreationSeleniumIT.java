package dk.dbc.dataio.gui.client;

import java.util.concurrent.TimeUnit;
import org.junit.After;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;

public class FlowCreationSeleniumIT {

    private static WebDriver driver;
    private static String jettyPort;
    private static String APP_URL;

    @BeforeClass
    public static void setUpClass() {
        jettyPort = System.getProperty("jetty.port");
        APP_URL = "http://localhost:" + jettyPort + "/dataio-gui/welcomeGWT.html";
    }

    @Before
    public void setUp() {
        driver = new FirefoxDriver();
        driver.get(APP_URL);
        driver.manage().timeouts().implicitlyWait(2, TimeUnit.SECONDS);
    }

    @After
    public void tearDown() {
        driver.quit();
    }

    @Test
    public void testFlowCreationNavigationItemIsVisble() {
        WebElement element = driver.findElement(By.id(MainEntryPoint.GUIID_NAVIGATION_MENU_ITEM_FLOW_CREATION));
        assertTrue(element.isDisplayed());
    }

    @Test
    public void testFlowCreationNavigationItemIsClickable() throws Exception {
        WebElement widget0 = driver.findElement(By.id(CreationPage.GUIID_WIDGET_FLOW_CREATION));
        assertEquals(widget0.isDisplayed(), true);
        WebElement element = driver.findElement(By.id(MainEntryPoint.GUIID_NAVIGATION_MENU_ITEM_FLOW_CREATION));
        element.click();
        WebElement widget = driver.findElement(By.id(CreationPage.GUIID_WIDGET_FLOW_CREATION));
        assertTrue(widget.isDisplayed());
        
    }

}
