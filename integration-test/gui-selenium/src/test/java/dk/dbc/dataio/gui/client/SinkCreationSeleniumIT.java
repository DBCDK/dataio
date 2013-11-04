package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.gui.client.components.DataEntry;
import dk.dbc.dataio.gui.client.components.SaveButton;
import dk.dbc.dataio.gui.client.views.MainPanel;
import dk.dbc.dataio.gui.client.views.SinkCreateViewImpl;
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

public class SinkCreationSeleniumIT {

    public static final String SMALL_UNICODE_TEST_STRING = "test of unicode content æøåÆØÅ";
    private static final int SAVE_SINK_TIMOUT = 4;
    private static final String SINK_NAME = "name";
    private static final String RESOURCE_NAME = "resource";
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
        ITUtil.clearDbTables(conn, ITUtil.SINKS_TABLE_NAME);
        driver.quit();
    }

    @Test
    public void testInitialVisibilityAndAccessabilityOfElements() {
        testSinkCreationNavigationItemIsVisible();
        testSinkCreationNavigationItemIsClickable();
        testSinkCreationSinkNameInputFieldIsVisibleAndDataCanBeInsertedAndRead();
        testSinkCreationResourceNameInputFieldIsVisibleAndDataCanBeInsertedAndRead();
        testSinkCreationSaveButtonIsVisible();
        testSinkCreationSaveResultLabelIsNotVisibleAndEmptyAsDefault();
    }

    public void testSinkCreationNavigationItemIsVisible() {
        assertTrue(findSinkCreationNavigationElement(driver).isDisplayed());
    }

    public void testSinkCreationNavigationItemIsClickable() {
        navigateToSinkCreationWidget(driver);
        assertTrue(findSinkCreationWidget(driver).isDisplayed());
    }

    public void testSinkCreationSinkNameInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToSinkCreationWidget(driver);
        SeleniumUtil.assertFieldIsVisbleAndDataCanBeInsertedAndRead(findSinkNameElement(driver));
    }

    public void testSinkCreationResourceNameInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToSinkCreationWidget(driver);
        SeleniumUtil.assertFieldIsVisbleAndDataCanBeInsertedAndRead(findResourceNameElement(driver));
    }

    public void testSinkCreationSaveButtonIsVisible() {
        navigateToSinkCreationWidget(driver);
        assertTrue(findSaveButton(driver).isDisplayed());
    }

    public void testSinkCreationSaveResultLabelIsNotVisibleAndEmptyAsDefault() {
        navigateToSinkCreationWidget(driver);
        WebElement element = findSaveResultLabel(driver);
        assertFalse(element.isDisplayed());
        assertThat(element.getText(), is(""));
    }

    @Test
    public void testSinkCreationSuccessfulSave_saveResultLabelContainsSuccessMessage() throws Exception {
        navigateToSinkCreationWidget(driver);
        insertTextInInputFieldsAndClickSaveButtonAndWaitForSuccessfullSave();
    }

    @Test
    public void testSinkCreationSinkNameInputFieldUpdate_clearsSaveResultLabel() throws Exception {
        navigateToSinkCreationWidget(driver);
        insertTextInInputFieldsAndClickSaveButtonAndWaitForSuccessfullSave();
        findSinkNameElement(driver).sendKeys(SINK_NAME);
        assertThat(findSaveResultLabel(driver).getText(), is(""));
    }

    @Test
    public void testSinkCreationResourceNameInputFieldUpdate_clearsSaveResultLabel() throws Exception {
        navigateToSinkCreationWidget(driver);
        insertTextInInputFieldsAndClickSaveButtonAndWaitForSuccessfullSave();
        findResourceNameElement(driver).sendKeys(RESOURCE_NAME);
        assertThat(findSaveResultLabel(driver).getText(), is(""));
    }

    @Test
    public void testSaveButton_EmptySinkNameInputField_DisplayErrorPopup() {
        navigateToSinkCreationWidget(driver);
        findResourceNameElement(driver).sendKeys(RESOURCE_NAME);
        findSaveButton(driver).click();
        String s = SeleniumUtil.getAlertStringAndAccept(driver);
        assertThat(s, is(SinkCreateViewImpl.SINK_CREATION_INPUT_FIELD_VALIDATION_ERROR));
    }

    @Test
    public void testSaveButton_EmptyResourceNameInputField_DisplayErrorPopup() {
        navigateToSinkCreationWidget(driver);
        findSinkNameElement(driver).sendKeys(SINK_NAME);
        findSaveButton(driver).click();
        String s = SeleniumUtil.getAlertStringAndAccept(driver);
        assertThat(s, is(SinkCreateViewImpl.SINK_CREATION_INPUT_FIELD_VALIDATION_ERROR));
    }

    /**
     * The following is private static helper methods.
     */
    private static void navigateToSinkCreationWidget(WebDriver webDriver) {
        findSinkCreationNavigationElement(webDriver).click();
    }

    private static WebElement findSinkCreationNavigationElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, MainPanel.GUIID_NAVIGATION_MENU_ITEM_SINK_CREATION);
    }

    private static WebElement findSinkCreationWidget(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, SinkCreateViewImpl.GUIID_SINK_CREATION_WIDGET);
    }

    private static WebElement findSinkNameElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, SinkCreateViewImpl.GUIID_SINK_CREATION_SINK_NAME_PANEL, DataEntry.DATA_ENTRY_INPUT_BOX_CLASS);
    }

    private static WebElement findResourceNameElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, SinkCreateViewImpl.GUIID_SINK_CREATION_RESOURCE_NAME_PANEL, DataEntry.DATA_ENTRY_INPUT_BOX_CLASS);
    }

    private static WebElement findSaveButton(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, SinkCreateViewImpl.GUIID_SINK_CREATION_SAVE_BUTTON_PANEL, SaveButton.SAVE_BUTTON_BUTTON_CLASS);
    }

    private static WebElement findSaveResultLabel(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, SinkCreateViewImpl.GUIID_SINK_CREATION_SAVE_BUTTON_PANEL, SaveButton.SAVE_BUTTON_RESULT_LABEL_CLASS);
    }

    private void insertTextInInputFieldsAndClickSaveButtonAndWaitForSuccessfullSave() {
        findSinkNameElement(driver).sendKeys("name");
        findResourceNameElement(driver).sendKeys("resource");
        findSaveButton(driver).click();
        SeleniumUtil.waitAndAssert(driver, SAVE_SINK_TIMOUT, SinkCreateViewImpl.GUIID_SINK_CREATION_SAVE_BUTTON_PANEL, SaveButton.SAVE_BUTTON_RESULT_LABEL_CLASS, SinkCreateViewImpl.SAVE_RESULT_LABEL_SUCCES_MESSAGE);
    }

    /**
     * The following is public static helper methods.
     */
    public static void createTestSink(WebDriver webDriver, String sinkName, String resourceName) {
        navigateToSinkCreationWidget(webDriver);
        findSinkNameElement(webDriver).clear();
        findSinkNameElement(webDriver).sendKeys(sinkName);
        findResourceNameElement(webDriver).clear();
        findResourceNameElement(webDriver).sendKeys(sinkName);
        findSaveButton(webDriver).click();

        SeleniumUtil.waitAndAssert(webDriver, SAVE_SINK_TIMOUT, SinkCreateViewImpl.GUIID_SINK_CREATION_SAVE_BUTTON_PANEL, SaveButton.SAVE_BUTTON_RESULT_LABEL_CLASS, SinkCreateViewImpl.SAVE_RESULT_LABEL_SUCCES_MESSAGE);
    }
}
