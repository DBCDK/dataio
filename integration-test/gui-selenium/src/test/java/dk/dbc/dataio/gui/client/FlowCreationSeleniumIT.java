package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.gui.client.views.FlowCreateViewImpl;
import dk.dbc.dataio.gui.client.views.MainPanel;
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
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class FlowCreationSeleniumIT {

    private static WebDriver driver;
    private static String APP_URL;
    private static Connection conn;

    @BeforeClass
    public static void setUpClass() throws ClassNotFoundException, SQLException {
        String glassfishPort = System.getProperty("glassfish.port");
        APP_URL = "http://localhost:" + glassfishPort + "/gui/gui.html";

        Class.forName("org.h2.Driver");
        conn = DriverManager.getConnection("jdbc:h2:tcp://localhost:" + System.getProperty("h2.port") + "/mem:flow_store", "root", "root");
        conn.setAutoCommit(true);
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
        clearDbTables();
        driver.quit();
    }

    @Test
    public void testFlowCreationNavigationItemIsVisible() {
        WebElement element = driver.findElement(By.id(MainPanel.GUIID_NAVIGATION_MENU_ITEM_FLOW_CREATION));
        assertEquals(true, element.isDisplayed());
    }

    @Test
    public void testFlowCreationNavigationItemIsClickable() throws Exception {
        navigateToFlowCreationContext();
        WebElement widget = driver.findElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_WIDGET));
        assertEquals(true, widget.isDisplayed());
    }

    @Test
    public void testFlowCreationNameInputFieldIsVisible() {
        navigateToFlowCreationContext();
        WebElement element = driver.findElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_NAME_TEXT_BOX));
        assertEquals(true, element.isDisplayed());
    }

    @Test
    public void testFlowCreationNameInputField_InsertAndRead() {
        final String fieldValue = "test of unicode content æøåÆØÅ";

        navigateToFlowCreationContext();
        WebElement element = driver.findElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_NAME_TEXT_BOX));
        element.sendKeys(fieldValue);
        assertEquals(fieldValue, element.getAttribute("value"));
    }

    @Test
    public void testFlowCreationDescriptionInputFieldIsVisible() {
        navigateToFlowCreationContext();
        WebElement element = driver.findElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_DESCRIPTION_TEXT_AREA));
        assertEquals(true, element.isDisplayed());
    }

    @Test
    public void testFlowCreationDescriptionInputField_InsertAndRead() {
        final String textWithMoreThan160Chars = "Dette er et stykke tekst som indeholder æøå og ÆØÅ. Formålet med teksten er hovedsagligt at være mere end 160 tegn lang, på en måde så der ikke er gentagelser i indholdet af teksten";
        final String sameTextWithExactly160Chars = textWithMoreThan160Chars.substring(0, 160);

        navigateToFlowCreationContext();
        WebElement element = driver.findElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_DESCRIPTION_TEXT_AREA));
        element.sendKeys(textWithMoreThan160Chars);
        assertEquals(sameTextWithExactly160Chars, element.getAttribute("value"));
    }

    @Test
    public void testFlowCreationSaveButtonIsVisible() {
        navigateToFlowCreationContext();
        WebElement element = driver.findElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_SAVE_BUTTON));
        assertEquals(true, element.isDisplayed());
    }

    @Test
    public void testFlowCreationSaveResultLabelIsNotVisibleAndEmptyAsDefault() throws Exception {
        navigateToFlowCreationContext();
        WebElement element = driver.findElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_SAVE_RESULT_LABEL));
        assertEquals(false, element.isDisplayed());
        assertEquals("", element.getText());
    }

    @Test
    public void testFlowCreationSuccessfulSave_saveResultLabelContainsSuccessMessage() throws Exception {
        navigateToFlowCreationContext();
        insertTextInInputFieldsAndClickSaveButton();
        WebDriverWait wait = new WebDriverWait(driver, 4);
        wait.until(ExpectedConditions.textToBePresentInElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_SAVE_RESULT_LABEL), FlowCreateViewImpl.SAVE_RESULT_LABEL_SUCCES_MESSAGE));
        WebElement saveResultLabel = driver.findElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_SAVE_RESULT_LABEL));
        assertEquals(FlowCreateViewImpl.SAVE_RESULT_LABEL_SUCCES_MESSAGE, saveResultLabel.getText());
    }

    @Test
    public void testFlowCreationNameInputFieldUpdate_clearsSaveResultLabel() throws Exception {
        navigateToFlowCreationContext();
        insertTextInInputFieldsAndClickSaveButton();
        WebElement saveResultLabel = driver.findElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_SAVE_RESULT_LABEL));
        WebElement nameInputField = driver.findElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_NAME_TEXT_BOX));
        nameInputField.sendKeys("a");
        assertEquals("", saveResultLabel.getText());
    }

    @Test
    public void testFlowCreationDescriptionInputFieldUpdate_clearsSaveResultLabel() throws Exception {
        navigateToFlowCreationContext();
        insertTextInInputFieldsAndClickSaveButton();
        WebElement saveResultLabel = driver.findElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_SAVE_RESULT_LABEL));
        WebElement descriptionInputField = driver.findElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_DESCRIPTION_TEXT_AREA));
        descriptionInputField.sendKeys("b");
        assertEquals("", saveResultLabel.getText());
    }

    @Test
    public void testSaveButton_EmptyNameInputField_DisplayErrorPopup() {
        navigateToFlowCreationContext();
        WebElement nameInputField = driver.findElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_NAME_TEXT_BOX));
        nameInputField.sendKeys("a");
        WebElement saveButton = driver.findElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_SAVE_BUTTON));
        saveButton.click();
        Alert alert = driver.switchTo().alert();
        String s = alert.getText();
        alert.accept();
        assertEquals(FlowCreateViewImpl.FLOW_CREATION_INPUT_FIELD_VALIDATION_ERROR, s);
    }

    @Test
    public void testSaveButton_EmptyDescriptionInputField_DisplayErrorPopup() {
        navigateToFlowCreationContext();
        WebElement descriptionInputField = driver.findElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_DESCRIPTION_TEXT_AREA));
        descriptionInputField.sendKeys("b");
        WebElement saveButton = driver.findElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_SAVE_BUTTON));
        saveButton.click();
        Alert alert = driver.switchTo().alert();
        String s = alert.getText();
        alert.accept();
        assertEquals(FlowCreateViewImpl.FLOW_CREATION_INPUT_FIELD_VALIDATION_ERROR, s);
    }

    private void navigateToFlowCreationContext() {
        WebElement element = driver.findElement(By.id(MainPanel.GUIID_NAVIGATION_MENU_ITEM_FLOW_CREATION));
        element.click();
    }

    private void insertTextInInputFieldsAndClickSaveButton() {
        WebElement nameInputField = driver.findElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_NAME_TEXT_BOX));
        nameInputField.sendKeys("a");
        WebElement descriptionInputField = driver.findElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_DESCRIPTION_TEXT_AREA));
        descriptionInputField.sendKeys("b");
        WebElement saveButton = driver.findElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_SAVE_BUTTON));
        saveButton.click();
    }

    private void clearDbTables() throws SQLException {
        PreparedStatement deleteStmt = conn.prepareStatement("DELETE FROM flows");
        try {
            deleteStmt.executeUpdate();
        } finally {
            deleteStmt.close();
        }
    }
}
