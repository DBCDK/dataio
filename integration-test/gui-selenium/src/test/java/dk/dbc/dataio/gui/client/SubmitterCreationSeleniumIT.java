package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.gui.client.views.MainPanel;
import dk.dbc.dataio.gui.client.views.SubmitterCreateViewImpl;
import dk.dbc.dataio.integrationtest.ITUtil;
import java.io.IOException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class SubmitterCreationSeleniumIT {

    private static WebDriver driver;
    private static String APP_URL;
    private static Connection conn;

    @BeforeClass
    public static void setUpClass() throws ClassNotFoundException, SQLException {
        String glassfishPort = System.getProperty("glassfish.port");
        APP_URL = "http://localhost:" + glassfishPort + "/gui/gui.html";
        conn = ITUtil.newDbConnection();
    }

    @AfterClass
    public static void tearDownClass() throws SQLException {
        conn.close();
    }

    @Before
    public void setUp() {
        driver = new FirefoxDriver();
        driver.get(APP_URL);
        driver.manage().timeouts().implicitlyWait(2, TimeUnit.SECONDS);
    }

    @After
    public void tearDown() throws SQLException {
        //ITUtil.clearDbTables(conn, ITUtil.SUBMITTERS_TABLE_NAME);
        driver.quit();
    }

    @Test
    public void testInitialVisibilityAndAccessabilityOfElements() {
        testSubmitterCreationNavigationItemIsVisible();
        testSubmitterCreationNavigationItemIsClickable();
        testSubmitterCreationNameInputFieldIsVisible();
        testSubmitterCreationNameInputField_InsertAndRead();
        testSubmitterCreationNumberInputFieldIsVisible();
        testSubmitterCreationNumberInputField_InsertAndRead();
        testSubmitterCreationDescriptionInputFieldIsVisible();
        testSubmitterCreationDescriptionInputField_InsertAndRead();
        testSubmitterCreationSaveButtonIsVisible();
        testSubmitterCreationSaveResultLabelIsNotVisibleAndEmptyAsDefault();
    }

    public void testSubmitterCreationNavigationItemIsVisible() {
        WebElement element = driver.findElement(By.id(MainPanel.GUIID_NAVIGATION_MENU_ITEM_SUBMITTER_CREATION));
        assertEquals(true, element.isDisplayed());
    }

    public void testSubmitterCreationNavigationItemIsClickable() {
        navigateToSubmitterCreationContext();
        WebElement widget = driver.findElement(By.id(SubmitterCreateViewImpl.GUIID_SUBMITTER_CREATION_WIDGET));
        assertEquals(true, widget.isDisplayed());
    }

    public void testSubmitterCreationNameInputFieldIsVisible() {
        navigateToSubmitterCreationContext();
        WebElement element = driver.findElement(By.id(SubmitterCreateViewImpl.GUIID_SUBMITTER_CREATION_NAME_TEXT_BOX));
        assertEquals(true, element.isDisplayed());
    }

    public void testSubmitterCreationNameInputField_InsertAndRead() {
        final String fieldValue = "test of unicode content æøåÆØÅ";

        navigateToSubmitterCreationContext();
        WebElement element = driver.findElement(By.id(SubmitterCreateViewImpl.GUIID_SUBMITTER_CREATION_NAME_TEXT_BOX));
        element.sendKeys(fieldValue);
        assertEquals(fieldValue, element.getAttribute("value"));
    }

    public void testSubmitterCreationNumberInputFieldIsVisible() {
        navigateToSubmitterCreationContext();
        WebElement element = driver.findElement(By.id(SubmitterCreateViewImpl.GUIID_SUBMITTER_CREATION_NUMBER_TEXT_BOX));
        assertEquals(true, element.isDisplayed());
    }

    public void testSubmitterCreationNumberInputField_InsertAndRead() {
        final String fieldValue = "test of unicode content æøåÆØÅ";

        navigateToSubmitterCreationContext();
        WebElement element = driver.findElement(By.id(SubmitterCreateViewImpl.GUIID_SUBMITTER_CREATION_NUMBER_TEXT_BOX));
        element.sendKeys(fieldValue);
        assertEquals(fieldValue, element.getAttribute("value"));
    }

    public void testSubmitterCreationDescriptionInputFieldIsVisible() {
        navigateToSubmitterCreationContext();
        WebElement element = driver.findElement(By.id(SubmitterCreateViewImpl.GUIID_SUBMITTER_CREATION_DESCRIPTION_TEXT_AREA));
        assertEquals(true, element.isDisplayed());
    }

    public void testSubmitterCreationDescriptionInputField_InsertAndRead() {
        final String textWithMoreThan160Chars = "Dette er et stykke tekst som indeholder æøå og ÆØÅ. Formålet med teksten er hovedsagligt at være mere end 160 tegn lang, på en måde så der ikke er gentagelser i indholdet af teksten";
        final String sameTextWithExactly160Chars = textWithMoreThan160Chars.substring(0, 160);

        navigateToSubmitterCreationContext();
        WebElement element = driver.findElement(By.id(SubmitterCreateViewImpl.GUIID_SUBMITTER_CREATION_DESCRIPTION_TEXT_AREA));
        element.sendKeys(textWithMoreThan160Chars);
        assertEquals(sameTextWithExactly160Chars, element.getAttribute("value"));
    }

    public void testSubmitterCreationSaveButtonIsVisible() {
        navigateToSubmitterCreationContext();
        WebElement element = driver.findElement(By.id(SubmitterCreateViewImpl.GUIID_SUBMITTER_CREATION_SAVE_BUTTON));
        assertEquals(true, element.isDisplayed());
    }

    public void testSubmitterCreationSaveResultLabelIsNotVisibleAndEmptyAsDefault() {
        navigateToSubmitterCreationContext();
        WebElement element = driver.findElement(By.id(SubmitterCreateViewImpl.GUIID_SUBMITTER_CREATION_SAVE_RESULT_LABEL));
        assertEquals(false, element.isDisplayed());
        assertEquals("", element.getText());
    }

    @Test
    public void testSubmitterCreationSuccessfulSave_saveResultLabelContainsSuccessMessage() throws Exception {
        navigateToSubmitterCreationContext();
        insertTextInInputFieldsAndClickSaveButton();
        WebDriverWait wait = new WebDriverWait(driver, 4);
        wait.until(ExpectedConditions.textToBePresentInElement(By.id(SubmitterCreateViewImpl.GUIID_SUBMITTER_CREATION_SAVE_RESULT_LABEL), SubmitterCreateViewImpl.SAVE_RESULT_LABEL_SUCCES_MESSAGE));
        WebElement saveResultLabel = driver.findElement(By.id(SubmitterCreateViewImpl.GUIID_SUBMITTER_CREATION_SAVE_RESULT_LABEL));
        assertEquals(SubmitterCreateViewImpl.SAVE_RESULT_LABEL_SUCCES_MESSAGE, saveResultLabel.getText());
    }

    @Test
    public void testSubmitterCreationNameInputFieldUpdate_clearsSaveResultLabel() throws Exception {
        navigateToSubmitterCreationContext();
        insertTextInInputFieldsAndClickSaveButton();
        WebElement saveResultLabel = driver.findElement(By.id(SubmitterCreateViewImpl.GUIID_SUBMITTER_CREATION_SAVE_RESULT_LABEL));
        WebElement nameInputField = driver.findElement(By.id(SubmitterCreateViewImpl.GUIID_SUBMITTER_CREATION_NAME_TEXT_BOX));
        nameInputField.sendKeys("a");
        assertEquals("", saveResultLabel.getText());
    }

    @Test
    public void testSubmitterCreationNumberInputFieldUpdate_clearsSaveResultLabel() throws Exception {
        navigateToSubmitterCreationContext();
        insertTextInInputFieldsAndClickSaveButton();
        WebElement saveResultLabel = driver.findElement(By.id(SubmitterCreateViewImpl.GUIID_SUBMITTER_CREATION_SAVE_RESULT_LABEL));
        WebElement numberInputField = driver.findElement(By.id(SubmitterCreateViewImpl.GUIID_SUBMITTER_CREATION_NUMBER_TEXT_BOX));
        numberInputField.sendKeys("b");
        assertEquals("", saveResultLabel.getText());
    }

    @Test
    public void testSubmitterCreationDescriptionInputFieldUpdate_clearsSaveResultLabel() throws Exception {
        navigateToSubmitterCreationContext();
        insertTextInInputFieldsAndClickSaveButton();
        WebElement saveResultLabel = driver.findElement(By.id(SubmitterCreateViewImpl.GUIID_SUBMITTER_CREATION_SAVE_RESULT_LABEL));
        WebElement descriptionInputField = driver.findElement(By.id(SubmitterCreateViewImpl.GUIID_SUBMITTER_CREATION_DESCRIPTION_TEXT_AREA));
        descriptionInputField.sendKeys("b");
        assertEquals("", saveResultLabel.getText());
    }

    // Error popup
    @Test
    public void testSaveButton_EmptyNameInputField_DisplayErrorPopup() {
        navigateToSubmitterCreationContext();
        WebElement nameInputField = driver.findElement(By.id(SubmitterCreateViewImpl.GUIID_SUBMITTER_CREATION_NAME_TEXT_BOX));
        nameInputField.sendKeys("a");
        WebElement saveButton = driver.findElement(By.id(SubmitterCreateViewImpl.GUIID_SUBMITTER_CREATION_SAVE_BUTTON));
        saveButton.click();
        Alert alert = driver.switchTo().alert();
        String s = alert.getText();
        alert.accept();
        assertEquals(SubmitterCreateViewImpl.SUBMITTER_CREATION_INPUT_FIELD_VALIDATION_ERROR, s);
    }

    @Test
    public void testSaveButton_EmptyDescriptionInputField_DisplayErrorPopup() {
        navigateToSubmitterCreationContext();
        WebElement descriptionInputField = driver.findElement(By.id(SubmitterCreateViewImpl.GUIID_SUBMITTER_CREATION_DESCRIPTION_TEXT_AREA));
        descriptionInputField.sendKeys("b");
        WebElement saveButton = driver.findElement(By.id(SubmitterCreateViewImpl.GUIID_SUBMITTER_CREATION_SAVE_BUTTON));
        saveButton.click();
        Alert alert = driver.switchTo().alert();
        String s = alert.getText();
        alert.accept();
        assertEquals(SubmitterCreateViewImpl.SUBMITTER_CREATION_INPUT_FIELD_VALIDATION_ERROR, s);
    }


    // Private methods
    private void navigateToSubmitterCreationContext() {
        WebElement element = driver.findElement(By.id(MainPanel.GUIID_NAVIGATION_MENU_ITEM_SUBMITTER_CREATION));
        element.click();
    }

    private void insertTextInInputFieldsAndClickSaveButton() {
        WebElement nameInputField = driver.findElement(By.id(SubmitterCreateViewImpl.GUIID_SUBMITTER_CREATION_NAME_TEXT_BOX));
        nameInputField.sendKeys("a");
        WebElement numberInputField = driver.findElement(By.id(SubmitterCreateViewImpl.GUIID_SUBMITTER_CREATION_NUMBER_TEXT_BOX));
        numberInputField.sendKeys("b");
        WebElement descriptionInputField = driver.findElement(By.id(SubmitterCreateViewImpl.GUIID_SUBMITTER_CREATION_DESCRIPTION_TEXT_AREA));
        descriptionInputField.sendKeys("c");
        WebElement saveButton = driver.findElement(By.id(SubmitterCreateViewImpl.GUIID_SUBMITTER_CREATION_SAVE_BUTTON));
        saveButton.click();
    }
}
