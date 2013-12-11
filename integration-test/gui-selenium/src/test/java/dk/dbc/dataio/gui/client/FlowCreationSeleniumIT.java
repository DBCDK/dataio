package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.gui.client.components.DataEntry;
import dk.dbc.dataio.gui.client.components.SaveButton;
import dk.dbc.dataio.gui.client.views.FlowCreateViewImpl;
import dk.dbc.dataio.gui.client.views.Menu;
import dk.dbc.dataio.integrationtest.ITUtil;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import static org.hamcrest.CoreMatchers.is;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;

public class FlowCreationSeleniumIT {
    private static ConstantsProperties texts = new ConstantsProperties("FlowCreateConstants_dk.properties");

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
        testFlowCreationNavigationItemIsClickable();
        testFlowCreationNameInputFieldIsVisibleAndDataCanBeInsertedAndRead();
        testFlowCreationDescriptionInputFieldIsVisibleAndDataCanBeInsertedAndRead();
//        testFlowCreationFlowComponentSelectionFieldIsVisibleAndAnElementCanBeChosen();
        testFlowCreationSaveButtonIsVisible();
        testFlowCreationSaveResultLabelIsNotVisibleAndEmptyAsDefault();
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

    @Test
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
        assertThat(s, is(texts.translate("error_InputFieldValidationError")));
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
        assertThat(s, is(texts.translate("error_InputFieldValidationError")));
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
        assertThat(s, is(texts.translate("error_InputFieldValidationError")));
    }

    /**
     * The following is private helper methods
     */
    private void insertTextInInputFieldsAndClickSaveButtonAndWaitForSuccess(String flowComponentName) {
        findNameElement(driver).sendKeys("a");
        findDescriptionElement(driver).sendKeys("b");
        SeleniumUtil.selectItemInDualList(findComponentSelectionElement(driver), flowComponentName);
        findSaveButtonElement(driver).click();
        SeleniumUtil.waitAndAssert(driver, SAVE_TIMEOUT, FlowCreateViewImpl.GUIID_FLOW_CREATION_FLOW_SAVE_PANEL, SaveButton.SAVE_BUTTON_RESULT_LABEL_CLASS, texts.translate("status_FlowSuccessfullySaved"));
    }

    private static void navigateToFlowCreationWidget(WebDriver webDriver) {
        NavigationPanelSeleniumIT.navigateTo(webDriver, Menu.GUIID_SUB_MENU_ITEM_FLOW_CREATION);
    }

    private static WebElement findFlowCreationWidget(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowCreateViewImpl.GUIID_FLOW_CREATION_WIDGET);
    }

    private static WebElement findNameElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowCreateViewImpl.GUIID_FLOW_CREATION_FLOW_NAME_PANEL, DataEntry.DATA_ENTRY_INPUT_BOX_CLASS);
    }

    private static WebElement findDescriptionElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowCreateViewImpl.GUIID_FLOW_CREATION_FLOW_DESCRIPTION_PANEL, DataEntry.DATA_ENTRY_INPUT_BOX_CLASS);
    }

    private static WebElement findComponentSelectionElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowCreateViewImpl.GUIID_FLOW_CREATION_FLOW_COMPONENT_SELECTION_PANEL);
    }

    private static WebElement findSaveButtonElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowCreateViewImpl.GUIID_FLOW_CREATION_FLOW_SAVE_PANEL, SaveButton.SAVE_BUTTON_BUTTON_CLASS);
    }

    private static WebElement findSaveResultElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowCreateViewImpl.GUIID_FLOW_CREATION_FLOW_SAVE_PANEL, SaveButton.SAVE_BUTTON_RESULT_LABEL_CLASS);
    }

    /**
     * The following is public static helper methods.
     */
    /**
     * Creates a new Flow with the given values - NOTE: It is the callers
     * responsibility to create a flow-component beforehand with the given name.
     */
    public static void createTestFlow(WebDriver webDriver, String name, String description, String flowComponentName) {
        navigateToFlowCreationWidget(webDriver);

        findNameElement(webDriver).clear();
        findNameElement(webDriver).sendKeys(name);

        SeleniumUtil.selectItemInDualList(findComponentSelectionElement(webDriver), flowComponentName);

        findDescriptionElement(webDriver).clear();
        findDescriptionElement(webDriver).sendKeys(description);

        findSaveButtonElement(webDriver).click();

        SeleniumUtil.waitAndAssert(webDriver, SAVE_TIMEOUT, FlowCreateViewImpl.GUIID_FLOW_CREATION_FLOW_SAVE_PANEL, SaveButton.SAVE_BUTTON_RESULT_LABEL_CLASS, texts.translate("status_FlowSuccessfullySaved"));
    }
}
