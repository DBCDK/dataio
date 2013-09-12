package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.gui.client.views.MainPanel;
import dk.dbc.dataio.gui.client.views.SubmitterCreateViewImpl;
import dk.dbc.dataio.integrationtest.ITUtil;
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
    public static final String SMALL_UNICODE_TEST_STRING = "test of unicode content æøåÆØÅ";

    private static final int SAVE_SUBMITTER_TIMOUT = 4;
    private static final String NAME = "name";
    private static final String NUMBER = "42";
    private static final String DESCRIPTTION = "desc";

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
        ITUtil.clearDbTables(conn, ITUtil.SUBMITTERS_TABLE_NAME);
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
        WebElement element = findNameElement(driver);
        assertEquals(true, element.isDisplayed());
    }

    public void testSubmitterCreationNameInputField_InsertAndRead() {
        navigateToSubmitterCreationContext();
        WebElement element = findNameElement(driver);
        element.sendKeys(SMALL_UNICODE_TEST_STRING);
        assertEquals(SMALL_UNICODE_TEST_STRING, element.getAttribute("value"));
    }

    public void testSubmitterCreationNumberInputFieldIsVisible() {
        navigateToSubmitterCreationContext();
        WebElement element = findNumberElement(driver);
        assertEquals(true, element.isDisplayed());
    }

    public void testSubmitterCreationNumberInputField_InsertAndRead() {
        navigateToSubmitterCreationContext();
        WebElement element = findNumberElement(driver);
        element.sendKeys(SMALL_UNICODE_TEST_STRING);
        assertEquals(SMALL_UNICODE_TEST_STRING, element.getAttribute("value"));
    }

    public void testSubmitterCreationDescriptionInputFieldIsVisible() {
        navigateToSubmitterCreationContext();
        WebElement element = findDescriptionElement(driver);
        assertEquals(true, element.isDisplayed());
    }

    public void testSubmitterCreationDescriptionInputField_InsertAndRead() {
        final String textWithMoreThan160Chars = "Dette er et stykke tekst som indeholder æøå og ÆØÅ. Formålet med teksten er hovedsagligt at være mere end 160 tegn lang, på en måde så der ikke er gentagelser i indholdet af teksten";
        final String sameTextWithExactly160Chars = textWithMoreThan160Chars.substring(0, 160);

        navigateToSubmitterCreationContext();
        WebElement element = findDescriptionElement(driver);
        element.sendKeys(textWithMoreThan160Chars);
        assertEquals(sameTextWithExactly160Chars, element.getAttribute("value"));
    }

    public void testSubmitterCreationSaveButtonIsVisible() {
        navigateToSubmitterCreationContext();
        WebElement element = findSaveButton(driver);
        assertEquals(true, element.isDisplayed());
    }

    public void testSubmitterCreationSaveResultLabelIsNotVisibleAndEmptyAsDefault() {
        navigateToSubmitterCreationContext();
        WebElement element = findSaveResultLabel(driver);
        assertEquals(false, element.isDisplayed());
        assertEquals("", element.getText());
    }

    @Test
    public void testSubmitterCreationSuccessfulSave_saveResultLabelContainsSuccessMessage() throws Exception {
        navigateToSubmitterCreationContext();
        insertTextInInputFieldsAndClickSaveButton();
        waitForSuccessfulSave(driver);
        WebElement saveResultLabel = findSaveResultLabel(driver);
        assertEquals(SubmitterCreateViewImpl.SAVE_RESULT_LABEL_SUCCES_MESSAGE, saveResultLabel.getText());
    }

    @Test
    public void testSubmitterCreationNameInputFieldUpdate_clearsSaveResultLabel() throws Exception {
        navigateToSubmitterCreationContext();
        insertTextInInputFieldsAndClickSaveButton();
        waitForSuccessfulSave(driver);
        findNameElement(driver).sendKeys(NAME);
        WebElement saveResultLabel = findSaveResultLabel(driver);
        assertEquals("", saveResultLabel.getText());
    }

    @Test
    public void testSubmitterCreationNumberInputFieldUpdate_clearsSaveResultLabel() throws Exception {
        navigateToSubmitterCreationContext();
        insertTextInInputFieldsAndClickSaveButton();
        waitForSuccessfulSave(driver);
        findNumberElement(driver).sendKeys(DESCRIPTTION);
        WebElement saveResultLabel = findSaveResultLabel(driver);
        assertEquals("", saveResultLabel.getText());
    }

    @Test
    public void testSubmitterCreationDescriptionInputFieldUpdate_clearsSaveResultLabel() throws Exception {
        navigateToSubmitterCreationContext();
        insertTextInInputFieldsAndClickSaveButton();
        waitForSuccessfulSave(driver);
        findDescriptionElement(driver).sendKeys(DESCRIPTTION);
        WebElement saveResultLabel = findSaveResultLabel(driver);
        assertEquals("", saveResultLabel.getText());
    }

    @Test
    public void testSaveButton_EmptyNameInputField_DisplayErrorPopup() {
        navigateToSubmitterCreationContext();
        findNumberElement(driver).sendKeys(NUMBER);
        findDescriptionElement(driver).sendKeys(DESCRIPTTION);
        findSaveButton(driver).click();
        Alert alert = driver.switchTo().alert();
        String s = alert.getText();
        alert.accept();
        assertEquals(SubmitterCreateViewImpl.SUBMITTER_CREATION_INPUT_FIELD_VALIDATION_ERROR, s);
    }

    @Test
    public void testSaveButton_EmptyDescriptionInputField_DisplayErrorPopup() {
        navigateToSubmitterCreationContext();
        findNameElement(driver).sendKeys(NAME);
        findNumberElement(driver).sendKeys(NUMBER);
        findSaveButton(driver).click();
        Alert alert = driver.switchTo().alert();
        String s = alert.getText();
        alert.accept();
        assertEquals(SubmitterCreateViewImpl.SUBMITTER_CREATION_INPUT_FIELD_VALIDATION_ERROR, s);
    }

    @Test
    public void testSaveButton_EmptyNumberInputField_DisplayErrorPopup() {
        navigateToSubmitterCreationContext();
        findNameElement(driver).sendKeys(NAME);
        findDescriptionElement(driver).sendKeys(DESCRIPTTION);
        findSaveButton(driver).click();
        Alert alert = driver.switchTo().alert();
        String s = alert.getText();
        alert.accept();
        assertEquals(SubmitterCreateViewImpl.SUBMITTER_CREATION_INPUT_FIELD_VALIDATION_ERROR, s);
    }

    @Test
    public void testSaveButton_numberInputFieldContainsNonNumericValue_DisplayErrorPopup() {
        navigateToSubmitterCreationContext();
        findNameElement(driver).sendKeys(NAME);
        findNumberElement(driver).sendKeys("fourty-two");
        findDescriptionElement(driver).sendKeys(DESCRIPTTION);
        findSaveButton(driver).click();
        Alert alert = driver.switchTo().alert();
        String s = alert.getText();
        alert.accept();
        assertEquals(SubmitterCreateViewImpl.SUBMITTER_CREATION_NUMBER_INPUT_FIELD_VALIDATION_ERROR, s);
    }

    private void navigateToSubmitterCreationContext() {
        WebElement element = driver.findElement(By.id(MainPanel.GUIID_NAVIGATION_MENU_ITEM_SUBMITTER_CREATION));
        element.click();
    }

    private static WebElement findNameElement(WebDriver webDriver) {
        return webDriver.findElement(By.id(SubmitterCreateViewImpl.GUIID_SUBMITTER_CREATION_NAME_TEXT_BOX));
    }

    private static WebElement findNumberElement(WebDriver webDriver) {
        return webDriver.findElement(By.id(SubmitterCreateViewImpl.GUIID_SUBMITTER_CREATION_NUMBER_TEXT_BOX));
    }

    private static WebElement findDescriptionElement(WebDriver webDriver) {
        return webDriver.findElement(By.id(SubmitterCreateViewImpl.GUIID_SUBMITTER_CREATION_DESCRIPTION_TEXT_AREA));
    }

    private static WebElement findSaveButton(WebDriver webDriver) {
        return webDriver.findElement(By.id(SubmitterCreateViewImpl.GUIID_SUBMITTER_CREATION_SAVE_BUTTON));
    }

    private static WebElement findSaveResultLabel(WebDriver webDriver) {
        return webDriver.findElement(By.id(SubmitterCreateViewImpl.GUIID_SUBMITTER_CREATION_SAVE_RESULT_LABEL));
    }

    private void insertTextInInputFieldsAndClickSaveButton() {
        findNameElement(driver).sendKeys("n");
        findNumberElement(driver).sendKeys("1");
        findDescriptionElement(driver).sendKeys("d");
        findSaveButton(driver).click();
    }

    private static void waitForSuccessfulSave(WebDriver webDriver) {
        WebDriverWait wait = new WebDriverWait(webDriver, SAVE_SUBMITTER_TIMOUT);
        wait.until(ExpectedConditions.textToBePresentInElement(By.id(SubmitterCreateViewImpl.GUIID_SUBMITTER_CREATION_SAVE_RESULT_LABEL), SubmitterCreateViewImpl.SAVE_RESULT_LABEL_SUCCES_MESSAGE));
    }

    public static boolean createTestSubmitter(WebDriver webDriver, String name, String number, String description) {
        webDriver.findElement(By.id(MainPanel.GUIID_NAVIGATION_MENU_ITEM_SUBMITTER_CREATION)).click();
        findNameElement(webDriver).sendKeys(name);
        findNumberElement(webDriver).sendKeys(number);
        findDescriptionElement(webDriver).sendKeys(description);
        findSaveButton(driver).click();

        waitForSuccessfulSave(webDriver);

        return findSaveResultLabel(webDriver).getText().equals(SubmitterCreateViewImpl.SAVE_RESULT_LABEL_SUCCES_MESSAGE);
    }
}
