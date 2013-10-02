package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.gui.client.components.TextAreaEntry;
import dk.dbc.dataio.gui.client.components.TextEntry;
import dk.dbc.dataio.gui.client.views.MainPanel;
import dk.dbc.dataio.gui.client.views.SubmitterCreateViewImpl;
import dk.dbc.dataio.integrationtest.ITUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.is;

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
        testSubmitterCreationNameInputFieldIsVisibleAndDataCanBeInsertedAndRead();
        testSubmitterCreationNumberInputFieldIsVisibleAndDataCanBeInsertedAndRead();
        testSubmitterCreationDescriptionInputFieldIsVisibleAndDataCanBeInsertedAndRead();
        testSubmitterCreationSaveButtonIsVisible();
        testSubmitterCreationSaveResultLabelIsNotVisibleAndEmptyAsDefault();
    }

    public void testSubmitterCreationNavigationItemIsVisible() {
        assertTrue(findSubmitterCreationNavigationElement(driver).isDisplayed());
    }

    public void testSubmitterCreationNavigationItemIsClickable() {
        navigateToSubmitterCreationWidget(driver);
        assertTrue(findSubmitterCreationWidget(driver).isDisplayed());
    }

    public void testSubmitterCreationNameInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToSubmitterCreationWidget(driver);
        SeleniumUtil.assertFieldIsVisbleAndDataCanBeInsertedAndRead(findNameElement(driver));
    }

    public void testSubmitterCreationNumberInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToSubmitterCreationWidget(driver);
        SeleniumUtil.assertFieldIsVisbleAndDataCanBeInsertedAndRead(findNumberElement(driver));
    }

    public void testSubmitterCreationDescriptionInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToSubmitterCreationWidget(driver);
        SeleniumUtil.assertFieldIsVisbleAndDataCanBeInsertedAndReadWithMaxSize(findDescriptionElement(driver), 160);
    }

    public void testSubmitterCreationSaveButtonIsVisible() {
        navigateToSubmitterCreationWidget(driver);
        assertTrue(findSaveButton(driver).isDisplayed());
    }

    public void testSubmitterCreationSaveResultLabelIsNotVisibleAndEmptyAsDefault() {
        navigateToSubmitterCreationWidget(driver);
        WebElement element = findSaveResultLabel(driver);
        assertFalse(element.isDisplayed());
        assertThat(element.getText(), is(""));
    }

    @Test
    public void testSubmitterCreationSuccessfulSave_saveResultLabelContainsSuccessMessage() throws Exception {
        navigateToSubmitterCreationWidget(driver);
        insertTextInInputFieldsAndClickSaveButtonAndWaitForSuccessfullSave();
    }

    @Test
    public void testSubmitterCreationNameInputFieldUpdate_clearsSaveResultLabel() throws Exception {
        navigateToSubmitterCreationWidget(driver);
        insertTextInInputFieldsAndClickSaveButtonAndWaitForSuccessfullSave();
        findNameElement(driver).sendKeys(NAME);
        assertThat(findSaveResultLabel(driver).getText(), is(""));
    }

    @Test
    public void testSubmitterCreationNumberInputFieldUpdate_clearsSaveResultLabel() throws Exception {
        navigateToSubmitterCreationWidget(driver);
        insertTextInInputFieldsAndClickSaveButtonAndWaitForSuccessfullSave();
        findNumberElement(driver).sendKeys(DESCRIPTTION);
        assertThat(findSaveResultLabel(driver).getText(), is(""));
    }

    @Test
    public void testSubmitterCreationDescriptionInputFieldUpdate_clearsSaveResultLabel() throws Exception {
        navigateToSubmitterCreationWidget(driver);
        insertTextInInputFieldsAndClickSaveButtonAndWaitForSuccessfullSave();
        findDescriptionElement(driver).sendKeys(DESCRIPTTION);
        assertThat(findSaveResultLabel(driver).getText(), is(""));
    }

    @Test
    public void testSaveButton_EmptyNameInputField_DisplayErrorPopup() {
        navigateToSubmitterCreationWidget(driver);
        findNumberElement(driver).sendKeys(NUMBER);
        findDescriptionElement(driver).sendKeys(DESCRIPTTION);
        findSaveButton(driver).click();
        String s = SeleniumUtil.getAlertStringAndAccept(driver);
        assertThat(s, is(SubmitterCreateViewImpl.SUBMITTER_CREATION_INPUT_FIELD_VALIDATION_ERROR));
    }

    @Test
    public void testSaveButton_EmptyDescriptionInputField_DisplayErrorPopup() {
        navigateToSubmitterCreationWidget(driver);
        findNameElement(driver).sendKeys(NAME);
        findNumberElement(driver).sendKeys(NUMBER);
        findSaveButton(driver).click();
        String s = SeleniumUtil.getAlertStringAndAccept(driver);
        assertThat(s, is(SubmitterCreateViewImpl.SUBMITTER_CREATION_INPUT_FIELD_VALIDATION_ERROR));
    }

    @Test
    public void testSaveButton_EmptyNumberInputField_DisplayErrorPopup() {
        navigateToSubmitterCreationWidget(driver);
        findNameElement(driver).sendKeys(NAME);
        findDescriptionElement(driver).sendKeys(DESCRIPTTION);
        findSaveButton(driver).click();
        String s = SeleniumUtil.getAlertStringAndAccept(driver);
        assertThat(s, is(SubmitterCreateViewImpl.SUBMITTER_CREATION_INPUT_FIELD_VALIDATION_ERROR));
    }

    @Test
    public void testSaveButton_numberInputFieldContainsNonNumericValue_DisplayErrorPopup() {
        navigateToSubmitterCreationWidget(driver);
        findNameElement(driver).sendKeys(NAME);
        findNumberElement(driver).sendKeys("fourty-two");
        findDescriptionElement(driver).sendKeys(DESCRIPTTION);
        findSaveButton(driver).click();
        String s = SeleniumUtil.getAlertStringAndAccept(driver);
        assertThat(s, is(SubmitterCreateViewImpl.SUBMITTER_CREATION_NUMBER_INPUT_FIELD_VALIDATION_ERROR));
    }

    /**
     * The following is private static helper methods.
     */
    private static void navigateToSubmitterCreationWidget(WebDriver webDriver) {
        findSubmitterCreationNavigationElement(webDriver).click();
    }

    private static WebElement findSubmitterCreationNavigationElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, MainPanel.GUIID_NAVIGATION_MENU_ITEM_SUBMITTER_CREATION);
    }

    private static WebElement findSubmitterCreationWidget(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, SubmitterCreateViewImpl.GUIID_SUBMITTER_CREATION_WIDGET);
    }

    private static WebElement findNameElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, SubmitterCreateViewImpl.GUIID_SUBMITTER_CREATION_NAME_PANEL, TextEntry.TEXT_ENTRY_TEXT_BOX_CLASS);
    }

    private static WebElement findNumberElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, SubmitterCreateViewImpl.GUIID_SUBMITTER_CREATION_NUMBER_PANEL, TextEntry.TEXT_ENTRY_TEXT_BOX_CLASS);
    }

    private static WebElement findDescriptionElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, SubmitterCreateViewImpl.GUIID_SUBMITTER_CREATION_DESCRIPTION_PANEL, TextAreaEntry.TEXT_AREA_ENTRY_TEXT_BOX_CLASS);
    }

    private static WebElement findSaveButton(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, SubmitterCreateViewImpl.GUIID_SUBMITTER_CREATION_SAVE_BUTTON);
    }

    private static WebElement findSaveResultLabel(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, SubmitterCreateViewImpl.GUIID_SUBMITTER_CREATION_SAVE_RESULT_LABEL);
    }

    private void insertTextInInputFieldsAndClickSaveButtonAndWaitForSuccessfullSave() {
        findNameElement(driver).sendKeys("n");
        findNumberElement(driver).sendKeys("1");
        findDescriptionElement(driver).sendKeys("d");
        findSaveButton(driver).click();
        SeleniumUtil.waitAndAssert(driver, SAVE_SUBMITTER_TIMOUT, findSaveResultLabel(driver), SubmitterCreateViewImpl.SAVE_RESULT_LABEL_SUCCES_MESSAGE);
    }

    /**
     * The following is public static helper methods.
     */
    public static boolean createTestSubmitter(WebDriver webDriver, String name, String number, String description) {
        navigateToSubmitterCreationWidget(webDriver);
        findNameElement(webDriver).clear();
        findNameElement(webDriver).sendKeys(name);
        findNumberElement(webDriver).clear();
        findNumberElement(webDriver).sendKeys(number);
        findDescriptionElement(webDriver).clear();
        findDescriptionElement(webDriver).sendKeys(description);
        findSaveButton(webDriver).click();

        return SeleniumUtil.waitAndAssert(webDriver, SAVE_SUBMITTER_TIMOUT, findSaveResultLabel(webDriver), SubmitterCreateViewImpl.SAVE_RESULT_LABEL_SUCCES_MESSAGE);
    }
}
