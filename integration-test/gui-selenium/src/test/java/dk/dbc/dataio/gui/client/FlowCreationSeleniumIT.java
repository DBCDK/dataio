package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.gui.client.views.FlowCreateViewImpl;
import dk.dbc.dataio.gui.client.views.MainPanel;
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.is;

public class FlowCreationSeleniumIT {

    private static final long SAVE_TIMEOUT = 4;
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
        ITUtil.clearDbTables(conn, ITUtil.FLOWS_TABLE_NAME, ITUtil.FLOW_COMPONENTS_TABLE_NAME);
        driver.quit();
    }

    @Test
    public void testInitialVisibililtyAndAccessabilityOfElements() {
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
    public void testFlowCreationNavigationItemIsClickable() {
        navigateToFlowCreationWidget(driver);
        assertTrue(findFlowCreationWidget(driver).isDisplayed());
    }

//    @Test
    public void testFlowCreationNameInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToFlowCreationWidget(driver);
        SeleniumUtil.assertFieldIsVisbleAndDataCanBeInsertedAndRead(findNameElement(driver));
    }

//    @Test
    public void testFlowCreationDescriptionInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToFlowCreationWidget(driver);
        SeleniumUtil.assertFieldIsVisbleAndDataCanBeInsertedAndReadWithMaxSize(findDescriptionElement(driver), 160);
    }

//    @Test
    public void testFlowCreationFlowComponentSelectionFieldIsVisibleAndAnElementCanBeChosen() {
        String flowComponentName = "testComponent";
        FlowComponentCreationSeleniumIT.createTestFlowComponent(driver, flowComponentName);
        navigateToFlowCreationWidget(driver);
        SeleniumUtil.assertDualListIsVisibleAndElementCanBeChosen(driver, findComponentSelectionElement(driver), flowComponentName);
    }

//    @Test
    public void testFlowCreationSaveButtonIsVisible() {
        navigateToFlowCreationWidget(driver);
        assertTrue(findSaveButtonElement(driver).isDisplayed());
    }

//    @Test
    public void testFlowCreationSaveResultLabelIsNotVisibleAndEmptyAsDefault() {
        navigateToFlowCreationWidget(driver);
        WebElement element = findSaveResultElement(driver);
        assertFalse(element.isDisplayed());
        assertThat(element.getText(), is(""));
    }

    @Test
    public void testFlowCreationSuccessfulSave_saveResultLabelContainsSuccessMessage() {
        String flowComponentName = "testComponent";
        FlowComponentCreationSeleniumIT.createTestFlowComponent(driver, flowComponentName);
        navigateToFlowCreationWidget(driver);
        insertTextInInputFieldsAndClickSaveButtonAndWaitForSuccess(flowComponentName);
    }

    @Test
    public void testFlowCreationNameInputFieldUpdate_clearsSaveResultLabel() {
        String flowComponentName = "testComponent";
        FlowComponentCreationSeleniumIT.createTestFlowComponent(driver, flowComponentName);
        navigateToFlowCreationWidget(driver);
        insertTextInInputFieldsAndClickSaveButtonAndWaitForSuccess(flowComponentName);
        findNameElement(driver).sendKeys("a");
        assertThat(findSaveResultElement(driver).getText(), is(""));
    }

    @Test
    public void testFlowCreationDescriptionInputFieldUpdate_clearsSaveResultLabel() {
        String flowComponentName = "testComponent";
        FlowComponentCreationSeleniumIT.createTestFlowComponent(driver, flowComponentName);
        navigateToFlowCreationWidget(driver);
        insertTextInInputFieldsAndClickSaveButtonAndWaitForSuccess(flowComponentName);
        findDescriptionElement(driver).sendKeys("b");
        assertThat(findSaveResultElement(driver).getText(), is(""));
    }

    @Test
    public void testFlowCreationFlowComponentSelectionUpdate_clearsSaveResultLabel() {
        String flowComponentName1 = "testComponent";
        FlowComponentCreationSeleniumIT.createTestFlowComponent(driver, flowComponentName1);
        String flowComponentName2 = "anotherTestComponent";
        FlowComponentCreationSeleniumIT.createTestFlowComponent(driver, flowComponentName2);

        navigateToFlowCreationWidget(driver);
        insertTextInInputFieldsAndClickSaveButtonAndWaitForSuccess(flowComponentName1);
        SeleniumUtil.selectItemInDualList(findComponentSelectionElement(driver), flowComponentName2);
        assertThat(findSaveResultElement(driver).getText(), is(""));
    }

    @Test
    public void testSaveButton_EmptyNameInputField_DisplayErrorPopup() {
        String flowComponentName = "testComponent";
        FlowComponentCreationSeleniumIT.createTestFlowComponent(driver, flowComponentName);
        navigateToFlowCreationWidget(driver);
        findNameElement(driver).sendKeys("a");
        SeleniumUtil.selectItemInDualList(findComponentSelectionElement(driver), flowComponentName);
        findSaveButtonElement(driver).click();
        String s = SeleniumUtil.getAlertStringAndAccept(driver);
        assertThat(s, is(FlowCreateViewImpl.FLOW_CREATION_INPUT_FIELD_VALIDATION_ERROR));
    }

    @Test
    public void testSaveButton_EmptyDescriptionInputField_DisplayErrorPopup() {
        String flowComponentName = "testComponent";
        FlowComponentCreationSeleniumIT.createTestFlowComponent(driver, flowComponentName);
        navigateToFlowCreationWidget(driver);
        findDescriptionElement(driver).sendKeys("b");
        SeleniumUtil.selectItemInDualList(findComponentSelectionElement(driver), flowComponentName);
        findSaveButtonElement(driver).click();
        String s = SeleniumUtil.getAlertStringAndAccept(driver);
        assertThat(s, is(FlowCreateViewImpl.FLOW_CREATION_INPUT_FIELD_VALIDATION_ERROR));
    }

    @Test
    public void testSaveButton_NoSelectedFlowComponent_DisplayErrorPopup() {
        String flowComponentName = "testComponent";
        FlowComponentCreationSeleniumIT.createTestFlowComponent(driver, flowComponentName);
        navigateToFlowCreationWidget(driver);
        findNameElement(driver).sendKeys("a");
        findDescriptionElement(driver).sendKeys("b");
        findSaveButtonElement(driver).click();
        String s = SeleniumUtil.getAlertStringAndAccept(driver);
        assertThat(s, is(FlowCreateViewImpl.FLOW_CREATION_INPUT_FIELD_VALIDATION_ERROR));
    }

    /**
     * The following is private helper methods
     */
    private void insertTextInInputFieldsAndClickSaveButtonAndWaitForSuccess(String flowComponentName) {
        findNameElement(driver).sendKeys("a");
        findDescriptionElement(driver).sendKeys("b");
        SeleniumUtil.selectItemInDualList(findComponentSelectionElement(driver), flowComponentName);
        findSaveButtonElement(driver).click();
        SeleniumUtil.waitAndAssert(driver, SAVE_TIMEOUT, findSaveResultElement(driver), FlowCreateViewImpl.SAVE_RESULT_LABEL_SUCCES_MESSAGE);
    }

    private static void navigateToFlowCreationWidget(WebDriver webDriver) {
        findFlowCreationNavigationElement(webDriver).click();
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
    /**
     * Creates a new Flow with the given values - NOTE: It is the callers
     * responsibility to create a flow-component beforehand with the given name.
     */
    public static boolean createTestFlow(WebDriver webDriver, String name, String description, String flowComponentName) {
        findFlowCreationNavigationElement(webDriver).click();

        findNameElement(webDriver).clear();
        findNameElement(webDriver).sendKeys(name);

        SeleniumUtil.selectItemInDualList(findComponentSelectionElement(webDriver), flowComponentName);

        findDescriptionElement(webDriver).clear();
        findDescriptionElement(webDriver).sendKeys(description);

        findSaveButtonElement(webDriver).click();

        return SeleniumUtil.waitAndAssert(webDriver, SAVE_TIMEOUT, findSaveResultElement(webDriver), FlowCreateViewImpl.SAVE_RESULT_LABEL_SUCCES_MESSAGE);
    }
}
