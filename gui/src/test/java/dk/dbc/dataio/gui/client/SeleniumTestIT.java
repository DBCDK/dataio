/*                                                                                                                                                          
 * To change this template, choose Tools | Templates                                                                                                        
 * and open the template in the editor.                                                                                                                     
 */
package dk.dbc.dataio.gui.client;

import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import static org.junit.Assert.*;
import org.openqa.selenium.firefox.FirefoxDriver;


public class SeleniumTestIT {

    private static WebDriver driver;
    private static String jettyPort;
    private static String APP_URL;

    @BeforeClass
	public static void setUpClass() {
        jettyPort = System.getProperty("jetty.port");
        APP_URL = "http://localhost:"+jettyPort+"/dataio-gui/welcomeGWT.html";
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
	public void testFlowCreationNavigationButtonIsVisible() throws InterruptedException, Exception {
        WebElement element = driver.findElement(By.id(MainEntryPoint.GUIID_NAVIGATION_MENU_BUTTON_CREATION));
        assertTrue(element.isDisplayed());
    }

    /*
    @Test
	public void testFlowDescriptionMustMaximumContain160Chars() throws Exception {
        final String textWithMoreThan160Chars = "Dette er et stykke tekst som indeholder æøå og ÆØÅ. Formålet med teksten er hovedsagligt at være mere end 160 tegn lang, på en måde så der ikke er gentagelser i indholdet af teksten";
        final String sameTextWithExactly160Chars = textWithMoreThan160Chars.substring(0, 160);
        assertEquals(160, sameTextWithExactly160Chars.length());

        WebElement flowCreationButton = driver.findElement(By.id(PrototypeEntryPoint.GUIID_NAVIGATION_MENU_BUTTON_FLOW_CREATION));
        flowCreationButton.click();
        WebElement flowDescriptionTextArea = driver.findElement(By.id(FlowCreator.GUIID_FLOW_DESCRIPTION_TEXT_AREA));
        flowDescriptionTextArea.sendKeys(textWithMoreThan160Chars);
        String actualTextInTextArea = flowDescriptionTextArea.getAttribute("value");
        assertEquals(sameTextWithExactly160Chars, actualTextInTextArea);
    }
    */
}
