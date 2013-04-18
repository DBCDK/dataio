package dk.dbc.dataio.gui.client;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.assertEquals;
import org.openqa.selenium.Alert;

public class FlowCreationSeleniumIT {

    private static WebDriver driver;
    private static String APP_URL;

    @BeforeClass
    public static void setUpClass() {
        String jettyPort = System.getProperty("jetty.port");
        APP_URL = "http://localhost:" + jettyPort + "/dataio-gui/gui.html";
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
    public void testFlowCreationNavigationItemIsVisible() {
        WebElement element = driver.findElement(By.id(MainEntryPoint.GUIID_NAVIGATION_MENU_ITEM_FLOW_CREATION));
        assertEquals(true, element.isDisplayed());
    }

    @Test
    public void testFlowCreationNavigationItemIsClickable() throws Exception {
        navigateToFlowCreationContext();
        WebElement widget = driver.findElement(By.id(FlowCreationWidget.GUIID_FLOW_CREATION_WIDGET));
        assertEquals(true, widget.isDisplayed());
    }

    @Test
    public void testFlowCreationNameInputFieldIsVisible() {
        navigateToFlowCreationContext();
        WebElement element = driver.findElement(By.id(FlowCreationWidget.GUIID_FLOW_CREATION_NAME_TEXT_BOX));
        assertEquals(true, element.isDisplayed());
    }

    @Test
    public void testFlowCreationNameInputField_InsertAndRead() {
        final String fieldValue = "test of unicode content æøåÆØÅ";

        navigateToFlowCreationContext();
        WebElement element = driver.findElement(By.id(FlowCreationWidget.GUIID_FLOW_CREATION_NAME_TEXT_BOX));
        element.sendKeys(fieldValue);
        assertEquals(fieldValue, element.getAttribute("value"));
    }

    @Test
    public void testFlowCreationDescriptionInputFieldIsVisible() {
        navigateToFlowCreationContext();
        WebElement element = driver.findElement(By.id(FlowCreationWidget.GUIID_FLOW_CREATION_DESCRIPTION_TEXT_AREA));
        assertEquals(true, element.isDisplayed());
    }

    @Test
    public void testFlowCreationDescriptionInputField_InsertAndRead() {
        final String textWithMoreThan160Chars = "Dette er et stykke tekst som indeholder æøå og ÆØÅ. Formålet med teksten er hovedsagligt at være mere end 160 tegn lang, på en måde så der ikke er gentagelser i indholdet af teksten";
        final String sameTextWithExactly160Chars = textWithMoreThan160Chars.substring(0, 160);

        navigateToFlowCreationContext();
        WebElement element = driver.findElement(By.id(FlowCreationWidget.GUIID_FLOW_CREATION_DESCRIPTION_TEXT_AREA));
        element.sendKeys(textWithMoreThan160Chars);
        assertEquals(sameTextWithExactly160Chars, element.getAttribute("value"));
    }

    @Test
    public void testFlowCreationSaveButtonIsVisible() {
        navigateToFlowCreationContext();
        WebElement element = driver.findElement(By.id(FlowCreationWidget.GUIID_FLOW_CREATION_SAVE_BUTTON));
        assertEquals(true, element.isDisplayed());
    }

    @Test
    public void testFlowCreationSaveResultLabelIsNotVisibleAndEmptyAsDefault() throws Exception {
        navigateToFlowCreationContext();
        WebElement element = driver.findElement(By.id(FlowCreationWidget.GUIID_FLOW_CREATION_SAVE_RESULT_LABEL));
        assertEquals(false, element.isDisplayed());
        assertEquals("", element.getText());
    }

    @Test
    public void testFlowCreationSuccessfulSave_saveResultLabelContainsSuccessMessage() throws Exception {
        navigateToFlowCreationContext();
        insertTextInInputFieldsAndClickSaveButton();
        WebElement saveResultLabel = driver.findElement(By.id(FlowCreationWidget.GUIID_FLOW_CREATION_SAVE_RESULT_LABEL));
        assertEquals(FlowCreationWidget.SAVE_RESULT_LABEL_SUCCES_MESSAGE, saveResultLabel.getText());
    }

    @Test
    public void testFlowCreationNameInputFieldUpdate_clearsSaveResultLabel() throws Exception {
        navigateToFlowCreationContext();
        insertTextInInputFieldsAndClickSaveButton();
        WebElement saveResultLabel = driver.findElement(By.id(FlowCreationWidget.GUIID_FLOW_CREATION_SAVE_RESULT_LABEL));
        WebElement nameInputField = driver.findElement(By.id(FlowCreationWidget.GUIID_FLOW_CREATION_NAME_TEXT_BOX));
        nameInputField.sendKeys("a");
        assertEquals("", saveResultLabel.getText());
    }

    @Test
    public void testFlowCreationDescriptionInputFieldUpdate_clearsSaveResultLabel() throws Exception {
        navigateToFlowCreationContext();
        insertTextInInputFieldsAndClickSaveButton();
        WebElement saveResultLabel = driver.findElement(By.id(FlowCreationWidget.GUIID_FLOW_CREATION_SAVE_RESULT_LABEL));
        WebElement descriptionInputField = driver.findElement(By.id(FlowCreationWidget.GUIID_FLOW_CREATION_DESCRIPTION_TEXT_AREA));
        descriptionInputField.sendKeys("b");
        assertEquals("", saveResultLabel.getText());
    }

    @Test
    public void testSaveButton_EmptyNameInputField_DisplayErrorPopup() {
        navigateToFlowCreationContext();
        WebElement nameInputField = driver.findElement(By.id(FlowCreationWidget.GUIID_FLOW_CREATION_NAME_TEXT_BOX));
        nameInputField.sendKeys("a");
        WebElement saveButton = driver.findElement(By.id(FlowCreationWidget.GUIID_FLOW_CREATION_SAVE_BUTTON));
        saveButton.click();
        Alert alert = driver.switchTo().alert();
        String s = alert.getText();
        alert.accept();
        assertEquals(FlowCreationWidget.FLOW_CREATION_INPUT_FIELD_VALIDATION_ERROR, s);
    }

    @Test
    public void testSaveButton_EmptyDescriptionInputField_DisplayErrorPopup() {
        navigateToFlowCreationContext();
        WebElement descriptionInputField = driver.findElement(By.id(FlowCreationWidget.GUIID_FLOW_CREATION_DESCRIPTION_TEXT_AREA));
        descriptionInputField.sendKeys("b");
        WebElement saveButton = driver.findElement(By.id(FlowCreationWidget.GUIID_FLOW_CREATION_SAVE_BUTTON));
        saveButton.click();
        Alert alert = driver.switchTo().alert();
        String s = alert.getText();
        alert.accept();
        assertEquals(FlowCreationWidget.FLOW_CREATION_INPUT_FIELD_VALIDATION_ERROR, s);
    }

    private void navigateToFlowCreationContext() {
        WebElement element = driver.findElement(By.id(MainEntryPoint.GUIID_NAVIGATION_MENU_ITEM_FLOW_CREATION));
        element.click();
    }

    private void insertTextInInputFieldsAndClickSaveButton() {
        WebElement nameInputField = driver.findElement(By.id(FlowCreationWidget.GUIID_FLOW_CREATION_NAME_TEXT_BOX));
        nameInputField.sendKeys("a");
        WebElement descriptionInputField = driver.findElement(By.id(FlowCreationWidget.GUIID_FLOW_CREATION_DESCRIPTION_TEXT_AREA));
        descriptionInputField.sendKeys("b");
        WebElement saveButton = driver.findElement(By.id(FlowCreationWidget.GUIID_FLOW_CREATION_SAVE_BUTTON));
        saveButton.click();
    }
}
