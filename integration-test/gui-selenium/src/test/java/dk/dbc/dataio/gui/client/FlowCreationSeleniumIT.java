package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.gui.client.views.FlowCreateViewImpl;
import dk.dbc.dataio.gui.client.views.MainPanel;
import dk.dbc.dataio.integrationtest.ITUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.is;

public class FlowCreationSeleniumIT {

    private static WebDriver driver;
    private static String APP_URL;
    private static Connection conn;
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

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
        ITUtil.clearDbTables(conn, ITUtil.FLOWS_TABLE_NAME, ITUtil.FLOW_COMPONENTS_TABLE_NAME);
        driver.quit();
    }

    @Test
    public void testInitialVisibililtyAndAccessabilityOfElements() throws Exception {
        testFlowCreationNavigationItemIsVisible();
        testFlowCreationNavigationItemIsClickable();
        testFlowCreationNameInputFieldIsVisibleAndDataCanBeInsertedAndRead();
        testFlowCreationDescriptionInputFieldIsVisibleAndDataCanBeInsertedAndRead();
        testFlowCreationFlowComponentSelectionFieldIsVisibleAndAnElementCanBeChosen();
        testFlowCreationSaveButtonIsVisible();
        testFlowCreationSaveResultLabelIsNotVisibleAndEmptyAsDefault();
    }

//    @Test
    public void testFlowCreationNavigationItemIsVisible() {
        assertTrue(findFlowCreationNavigationElement(driver).isDisplayed());
    }

//    @Test
    public void testFlowCreationNavigationItemIsClickable() throws Exception {
        navigateToFlowCreationWidget();
        assertTrue(findFlowCreationWidget(driver).isDisplayed());
    }

//    @Test
    public void testFlowCreationNameInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToFlowCreationWidget();
        SeleniumUtil.assertFieldIsVisbleAndDataCanBeInsertedAndRead(findNameElement(driver));
    }

    //    @Test
    public void testFlowCreationDescriptionInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToFlowCreationWidget();
        SeleniumUtil.assertFieldIsVisbleAndDataCanBeInsertedAndReadWithMaxSize(findDescriptionElement(driver), 160);
    }

//    @Test
    public void testFlowCreationFlowComponentSelectionFieldIsVisibleAndAnElementCanBeChosen() {
        String flowComponentName = "testComponent";
        FlowComponentCreationSeleniumIT.createTestFlowComponent(driver, flowComponentName);
        navigateToFlowCreationWidget();
        SeleniumUtil.assertDualListIsVisibleAndElementCanBeChosen(driver, findComponentSelectionElement(driver), flowComponentName);
    }

//    @Test
    public void testFlowCreationSaveButtonIsVisible() {
        navigateToFlowCreationWidget();
        assertTrue(findSaveButtonElement(driver).isDisplayed());
    }

//    @Test
    public void testFlowCreationSaveResultLabelIsNotVisibleAndEmptyAsDefault() throws Exception {
        navigateToFlowCreationWidget();
        WebElement element = findSaveResultElement(driver);
        assertFalse(element.isDisplayed());
        assertThat(element.getText(), is(""));
    }

    @Test
    public void testFlowCreationSuccessfulSave_saveResultLabelContainsSuccessMessage() throws Exception {
        navigateToFlowCreationWidget();
        insertTextInInputFieldsAndClickSaveButton();
        WebDriverWait wait = new WebDriverWait(driver, 4);
        wait.until(ExpectedConditions.textToBePresentInElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_SAVE_RESULT_LABEL), FlowCreateViewImpl.SAVE_RESULT_LABEL_SUCCES_MESSAGE));
        assertThat(findSaveResultElement(driver).getText(), is(FlowCreateViewImpl.SAVE_RESULT_LABEL_SUCCES_MESSAGE));
    }

    @Test
    public void testFlowCreationNameInputFieldUpdate_clearsSaveResultLabel() throws Exception {
        navigateToFlowCreationWidget();
        insertTextInInputFieldsAndClickSaveButton();
        findNameElement(driver).sendKeys("a");
        assertThat(findSaveResultElement(driver).getText(), is(""));
    }

    @Test
    public void testFlowCreationDescriptionInputFieldUpdate_clearsSaveResultLabel() throws Exception {
        navigateToFlowCreationWidget();
        insertTextInInputFieldsAndClickSaveButton();
        findDescriptionElement(driver).sendKeys("b");
        assertThat(findSaveResultElement(driver).getText(), is(""));
    }

    @Test
    public void testSaveButton_EmptyNameInputField_DisplayErrorPopup() {
        navigateToFlowCreationWidget();
        findNameElement(driver).sendKeys("a");
        findSaveButtonElement(driver).click();
        String s = SeleniumUtil.getAlertStringAndAccept(driver);
        assertThat(s, is(FlowCreateViewImpl.FLOW_CREATION_INPUT_FIELD_VALIDATION_ERROR));
    }

    @Test
    public void testSaveButton_EmptyDescriptionInputField_DisplayErrorPopup() {
        navigateToFlowCreationWidget();
        findDescriptionElement(driver).sendKeys("b");
        findSaveButtonElement(driver).click();
        String s = SeleniumUtil.getAlertStringAndAccept(driver);
        assertThat(s, is(FlowCreateViewImpl.FLOW_CREATION_INPUT_FIELD_VALIDATION_ERROR));
    }

    /**
     * The following is private helper methods
     */
    private void navigateToFlowCreationWidget() {
        findFlowCreationNavigationElement(driver).click();
    }

    private void insertTextInInputFieldsAndClickSaveButton() {
        findNameElement(driver).sendKeys("a");
        findDescriptionElement(driver).sendKeys("b");
        findSaveButtonElement(driver).click();
    }

    private static WebElement findFlowCreationNavigationElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, MainPanel.GUIID_NAVIGATION_MENU_ITEM_FLOW_CREATION);
    }

    private static WebElement findFlowCreationWidget(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowCreateViewImpl.GUIID_FLOW_CREATION_WIDGET);
    }

    private static WebElement findNameElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowCreateViewImpl.GUIID_FLOW_CREATION_NAME_TEXT_BOX);
    }

    private static WebElement findDescriptionElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowCreateViewImpl.GUIID_FLOW_CREATION_DESCRIPTION_TEXT_AREA);
    }

    private static WebElement findComponentSelectionElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowCreateViewImpl.GUIID_FLOW_CREATION_FLOW_COMPONENT_SELECTION_PANEL);
    }

    private static WebElement findSaveButtonElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowCreateViewImpl.GUIID_FLOW_CREATION_SAVE_BUTTON);
    }

    private static WebElement findSaveResultElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowCreateViewImpl.GUIID_FLOW_CREATION_SAVE_RESULT_LABEL);
    }

    /**
     * The following is public static helper methods.
     */
    private static void waitForSuccessfulSave(WebDriver webDriver) {
        WebDriverWait wait = new WebDriverWait(webDriver, 4);
        wait.until(ExpectedConditions.textToBePresentInElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_SAVE_RESULT_LABEL), FlowCreateViewImpl.SAVE_RESULT_LABEL_SUCCES_MESSAGE));
    }

    /**
     * Creates a new Flow with the given values - NOTE: It is the callers
     * responsibility to create a flow-component beforehand with the given name.
     */
    public static boolean createTestFlow(WebDriver webDriver, String name, String description, String flowComponentName) {
        findFlowCreationNavigationElement(webDriver).click();

        webDriver.findElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_NAME_TEXT_BOX)).clear();
        webDriver.findElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_NAME_TEXT_BOX)).sendKeys(name);

        // todo: Add selection of chosen flowComponent in duallist.
        webDriver.findElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_DESCRIPTION_TEXT_AREA)).clear();
        webDriver.findElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_DESCRIPTION_TEXT_AREA)).sendKeys(description);
        webDriver.findElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_SAVE_BUTTON)).click();

        waitForSuccessfulSave(webDriver);

        return webDriver.findElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_SAVE_RESULT_LABEL)).getText().equals(FlowCreateViewImpl.SAVE_RESULT_LABEL_SUCCES_MESSAGE);
    }
}
