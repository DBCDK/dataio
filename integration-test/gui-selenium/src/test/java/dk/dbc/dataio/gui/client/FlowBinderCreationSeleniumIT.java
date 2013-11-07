package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.gui.client.components.DataEntry;
import dk.dbc.dataio.gui.client.components.SaveButton;
import dk.dbc.dataio.gui.client.views.FlowbinderCreateViewImpl;
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

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import org.junit.Ignore;

public class FlowBinderCreationSeleniumIT {

    private static final long SAVE_TIMEOUT = 4;
    private WebDriver driver;
    private static String appUrl;
    private static Connection conn;

    @BeforeClass
    public static void setUpClass() throws ClassNotFoundException, SQLException {
        appUrl = "http://localhost:" + System.getProperty("glassfish.port") + "/gui/gui.html";
        conn = ITUtil.newDbConnection();
    }

    @AfterClass
    public static void tearDownClass() throws SQLException {
        conn.close();
    }

    @Before
    public void setUp() {
        driver = new FirefoxDriver();
        driver.get(appUrl);
        driver.manage().timeouts().implicitlyWait(2, TimeUnit.SECONDS);
    }

    @After
    public void tearDown() throws SQLException {
        ITUtil.clearAllDbTables(conn);
        driver.quit();
    }

    @Ignore
    @Test
    public void testInitialVisibilityAndAccessabilityOfElements() throws IOException {
        testFlowBinderCreationNavigationItemIsVisibleAndClickable();
        testFlowbinderCreationNameInputFieldIsVisibleAndDataCanBeInsertedAndRead();
        testFlowbinderCreationDescriptionInputFieldIsVisibleAndDataCanBeInsertedAndRead();
        testFlowbinderCreationFrameInputFieldIsVisibleAndDataCanBeInsertedAndRead();
        testFlowbinderCreationContentFormatInputFieldIsVisibleAndDataCanBeInsertedAndRead();
        testFlowbinderCreationCharacterSetInputFieldIsVisibleAndDataCanBeInsertedAndRead();
        testFlowbinderCreationSinkInputFieldIsVisibleAndDataCanBeInsertedAndRead();
        //testFlowbinderCreationSubmitterDualListIsVisibleAndAnElementCanBeChosen();
        //testFlowbinderCreationFlowListIsVisibleAndAnElementCanBeSelected();
        testFlowbinderCreationSaveButtonIsVisible();
        testFlowbinderCreationSaveResultLableIsNotVisibleAndEmptyByDefault();
    }

    public void testFlowBinderCreationNavigationItemIsVisibleAndClickable() {
        WebElement element = findFlowbinderCreationContextElement(driver);
        assertTrue(element.isDisplayed());
        element.click();

        WebElement widget = findFlowbinderCreationWidget(driver);
        assertTrue(widget.isDisplayed());
    }

    public void testFlowbinderCreationNameInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToFlowbinderCreationWidget(driver);
        SeleniumUtil.assertFieldIsVisbleAndDataCanBeInsertedAndReadWithMaxSize(findNameTextElement(driver), 160);
    }

    public void testFlowbinderCreationDescriptionInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToFlowbinderCreationWidget(driver);
        SeleniumUtil.assertFieldIsVisbleAndDataCanBeInsertedAndReadWithMaxSize(findDescriptionTextElement(driver), 160);
    }

    public void testFlowbinderCreationFrameInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToFlowbinderCreationWidget(driver);
        SeleniumUtil.assertFieldIsVisbleAndDataCanBeInsertedAndRead(findFrameTextElement(driver));
    }

    public void testFlowbinderCreationContentFormatInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToFlowbinderCreationWidget(driver);
        SeleniumUtil.assertFieldIsVisbleAndDataCanBeInsertedAndRead(findContentFormatTextElement(driver));
    }

    public void testFlowbinderCreationCharacterSetInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToFlowbinderCreationWidget(driver);
        SeleniumUtil.assertFieldIsVisbleAndDataCanBeInsertedAndRead(findCharacterSetTextElement(driver));
    }

    public void testFlowbinderCreationSinkInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToFlowbinderCreationWidget(driver);
        SeleniumUtil.assertFieldIsVisbleAndDataCanBeInsertedAndRead(findSinkListElement(driver));
    }

    // This can, for some reason, not be included with the other visibility tests
    @Test
    public void testFlowbinderCreationSubmitterDualListIsVisibleAndAnElementCanBeChosen() {
        String submitterName = "submitter1";
        SubmitterCreationSeleniumIT.createTestSubmitter(driver, submitterName, "123456", "Description");
        navigateToFlowbinderCreationWidget(driver);
        SeleniumUtil.assertDualListIsVisibleAndElementCanBeChosen(driver, findSubmitterPanelElement(driver), submitterName);
    }

    // This can, for some reason, not be included with the other visibility tests
    @Test
    public void testFlowbinderCreationFlowListIsVisibleAndAnElementCanBeSelected() {
        String flowComponentName = "flowComponent";
        String flowName = "flowName";
        FlowComponentCreationSeleniumIT.createTestFlowComponent(driver, flowComponentName);
        FlowCreationSeleniumIT.createTestFlow(driver, flowName, "description", flowComponentName);

        navigateToFlowbinderCreationWidget(driver);
        SeleniumUtil.assertListBoxIsVisibleAndAnElementCanBeSelected(driver, findFlowListElement(driver), flowName);
    }

    public void testFlowbinderCreationSaveButtonIsVisible() {
        navigateToFlowbinderCreationWidget(driver);
        assertTrue(findSaveButtonElement(driver).isDisplayed());
    }

    public void testFlowbinderCreationSaveResultLableIsNotVisibleAndEmptyByDefault() {
        navigateToFlowbinderCreationWidget(driver);
        WebElement element = findSaveResultLabelElement(driver);
        assertFalse(element.isDisplayed());
        assertThat(element.getText(), is(""));
    }

    @Ignore
    @Test
    public void testFlowbinderCreationSuccessfulSave_saveResultLabelContainsSuccessMessage() {
        populateAllInputFieldsAndClickSaveAndWaitForSuccess();
    }

    @Ignore
    @Test
    public void testSaveButton_emptyNameInputField_displayErrorPopup() {
        populateAllInputFields();
        findNameTextElement(driver).clear();
        findSaveButtonElement(driver).click();
        assertThat(SeleniumUtil.getAlertStringAndAccept(driver), is(FlowbinderCreateViewImpl.FLOWBINDER_CREATION_INPUT_FIELD_VALIDATION_ERROR));
    }

    @Ignore
    @Test
    public void testSaveButton_emptyDescriptionInputField_displayErrorPopup() {
        populateAllInputFields();
        findDescriptionTextElement(driver).clear();
        findSaveButtonElement(driver).click();
        assertThat(SeleniumUtil.getAlertStringAndAccept(driver), is(FlowbinderCreateViewImpl.FLOWBINDER_CREATION_INPUT_FIELD_VALIDATION_ERROR));
    }

    @Ignore
    @Test
    public void testSaveButton_emptyFrameInputField_displayErrorPopup() {
        populateAllInputFields();
        findFrameTextElement(driver).clear();
        findSaveButtonElement(driver).click();
        assertThat(SeleniumUtil.getAlertStringAndAccept(driver), is(FlowbinderCreateViewImpl.FLOWBINDER_CREATION_INPUT_FIELD_VALIDATION_ERROR));
    }

    @Ignore
    @Test
    public void testSaveButton_emptyContentFormatInputField_displayErrorPopup() {
        populateAllInputFields();
        findContentFormatTextElement(driver).clear();
        findSaveButtonElement(driver).click();
        assertThat(SeleniumUtil.getAlertStringAndAccept(driver), is(FlowbinderCreateViewImpl.FLOWBINDER_CREATION_INPUT_FIELD_VALIDATION_ERROR));
    }

    @Ignore
    @Test
    public void testSaveButton_emptyCharacterSetInputField_displayErrorPopup() {
        populateAllInputFields();
        findCharacterSetTextElement(driver).clear();
        findSaveButtonElement(driver).click();
        assertThat(SeleniumUtil.getAlertStringAndAccept(driver), is(FlowbinderCreateViewImpl.FLOWBINDER_CREATION_INPUT_FIELD_VALIDATION_ERROR));
    }

    @Test
    public void testSaveButton_emptySinkInputField_displayErrorPopup() {
        String submitter = createDefaultSubmitter();
        String flow = createDefaultFlow();
        navigateToFlowbinderCreationWidget(driver);
        populateAllTextInputFieldsWhenInFlowbinderCreationWidget();
        selectSubmitterWhenInFlowbinderCreationWidget(submitter);
        selectFlowWhenInFlowbinderCreationWidget(flow);
        findSaveButtonElement(driver).click();
        assertThat(SeleniumUtil.getAlertStringAndAccept(driver), is(FlowbinderCreateViewImpl.FLOWBINDER_CREATION_INPUT_FIELD_VALIDATION_ERROR));
    }

    @Ignore
    @Test
    public void testSaveButton_emptySubmitterInputField_displayErrorPopup() {
        String sink = createDefaultSink();
        String flow = createDefaultFlow();
        navigateToFlowbinderCreationWidget(driver);
        populateAllTextInputFieldsWhenInFlowbinderCreationWidget();
        selectSinkWhenInFlowbinderCreationWidget(sink);
        selectFlowWhenInFlowbinderCreationWidget(flow);
        findSaveButtonElement(driver).click();
        assertThat(SeleniumUtil.getAlertStringAndAccept(driver), is(FlowbinderCreateViewImpl.FLOWBINDER_CREATION_INPUT_FIELD_VALIDATION_ERROR));
    }

    @Ignore
    @Test
    public void testSaveButton_emptyFlowInputField_displayErrorPopup() {
        String sink = createDefaultSink();
        String submitter = createDefaultSubmitter();
        navigateToFlowbinderCreationWidget(driver);
        populateAllTextInputFieldsWhenInFlowbinderCreationWidget();
        selectSinkWhenInFlowbinderCreationWidget(sink);
        selectSubmitterWhenInFlowbinderCreationWidget(submitter);
        findSaveButtonElement(driver).click();
        assertThat(SeleniumUtil.getAlertStringAndAccept(driver), is(FlowbinderCreateViewImpl.FLOWBINDER_CREATION_INPUT_FIELD_VALIDATION_ERROR));
    }

    @Ignore
    @Test
    public void testFlowBinderCreationNameInputFieldUpdate_clearsSaveResultLabel() {
        populateAllInputFieldsAndClickSaveAndWaitForSuccess();
        findNameTextElement(driver).sendKeys("other stuff");
        assertThat(findSaveResultLabelElement(driver).getText(), is(""));
    }

    @Ignore
    @Test
    public void testFlowBinderCreationDescriptionInputFieldUpdate_clearsSaveResultLabel() {
        populateAllInputFieldsAndClickSaveAndWaitForSuccess();
        findDescriptionTextElement(driver).sendKeys("other stuff");
        assertThat(findSaveResultLabelElement(driver).getText(), is(""));
    }

    @Ignore
    @Test
    public void testFlowBinderCreationFrameInputFieldUpdate_clearsSaveResultLabel() {
        populateAllInputFieldsAndClickSaveAndWaitForSuccess();
        findFrameTextElement(driver).sendKeys("other stuff");
        assertThat(findSaveResultLabelElement(driver).getText(), is(""));
    }

    @Ignore
    @Test
    public void testFlowBinderCreationContentFormatInputFieldUpdate_clearsSaveResultLabel() {
        populateAllInputFieldsAndClickSaveAndWaitForSuccess();
        findContentFormatTextElement(driver).sendKeys("other stuff");
        assertThat(findSaveResultLabelElement(driver).getText(), is(""));
    }

    @Ignore
    @Test
    public void testFlowBinderCreationCharacterSetInputFieldUpdate_clearsSaveResultLabel() {
        populateAllInputFieldsAndClickSaveAndWaitForSuccess();
        findCharacterSetTextElement(driver).sendKeys("other stuff");
        assertThat(findSaveResultLabelElement(driver).getText(), is(""));
    }

    @Ignore
    @Test
    public void testFlowBinderCreationSinkInputFieldUpdate_clearsSaveResultLabel() {
        final String sinkName = "anotherSink";
        SinkCreationSeleniumIT.createTestSink(driver, sinkName, SinkCreationSeleniumIT.SINK_CREATION_KNOWN_RESOURCE_NAME);
        populateAllInputFieldsAndClickSaveAndWaitForSuccess();
        SeleniumUtil.selectItemInListBox(findSinkListElement(driver), sinkName);
        assertThat(findSaveResultLabelElement(driver).getText(), is(""));
    }

    @Ignore
    @Test
    public void testFlowBinderCreationSubmitterInputFieldUpdate_clearsSaveResultLabel() {
        final String submitterName = "anotherSubmitter";
        SubmitterCreationSeleniumIT.createTestSubmitter(driver, submitterName, "42", "Description");
        populateAllInputFieldsAndClickSaveAndWaitForSuccess();
        SeleniumUtil.selectItemInDualList(findSubmitterPanelElement(driver), submitterName);
        assertThat(findSaveResultLabelElement(driver).getText(), is(""));
    }

    @Ignore
    @Test
    public void testFlowBinderCreationFlowInputFieldUpdate_clearsSaveResultLabel() {
        final String flowComponentName = "anotherFlowComponent";
        final String flowName = "anotherFlow";
        FlowComponentCreationSeleniumIT.createTestFlowComponent(driver, flowComponentName);
        FlowCreationSeleniumIT.createTestFlow(driver, flowName, "description", flowComponentName);
        populateAllInputFieldsAndClickSaveAndWaitForSuccess();
        SeleniumUtil.selectItemInListBox(findFlowListElement(driver), flowName);
        assertThat(findSaveResultLabelElement(driver).getText(), is(""));
    }

    /**
     * The following is private helper methods
     */
    private void populateAllInputFields() {
        String submitter = createDefaultSubmitter();
        String sink = createDefaultSink();
        String flow = createDefaultFlow();
        navigateToFlowbinderCreationWidget(driver);
        populateAllTextInputFieldsWhenInFlowbinderCreationWidget();
        selectSinkWhenInFlowbinderCreationWidget(sink);
        selectSubmitterWhenInFlowbinderCreationWidget(submitter);
        selectFlowWhenInFlowbinderCreationWidget(flow);
    }

    private void populateAllTextInputFieldsWhenInFlowbinderCreationWidget() {
        // navigateToFlowbinderCreationWidget(driver);
        findNameTextElement(driver).sendKeys("Name");
        findDescriptionTextElement(driver).sendKeys("Description");
        findFrameTextElement(driver).sendKeys("Frame");
        findContentFormatTextElement(driver).sendKeys("ContentFormat");
        findCharacterSetTextElement(driver).sendKeys("CharacterSet");
    }

    /*
    private void populateFlowSelectionField() {
        String flowComponentName = "flowComponent12";
        String flowName = "flowName12";
        FlowComponentCreationSeleniumIT.createTestFlowComponent(driver, flowComponentName);
        FlowCreationSeleniumIT.createTestFlow(driver, flowName, "description", flowComponentName);
        navigateToFlowbinderCreationContext(driver);
        SeleniumUtil.selectItemInListBox(findFlowListElement(driver), flowName);
    }
    */

    private String createDefaultFlow() {
        final String flowComponentName = "flowComponent12";
        final String flowName = "flowName12";
        FlowComponentCreationSeleniumIT.createTestFlowComponent(driver, flowComponentName);
        FlowCreationSeleniumIT.createTestFlow(driver, flowName, "description", flowComponentName);
        return flowName;
    }

    private void selectFlowWhenInFlowbinderCreationWidget(String flow) {
        SeleniumUtil.selectItemInListBox(findFlowListElement(driver), flow);
    }

    private String createDefaultSink() {
        final String sinkName = "sinkName45";
        final String resourceName = SinkCreationSeleniumIT.SINK_CREATION_KNOWN_RESOURCE_NAME;
        SinkCreationSeleniumIT.createTestSink(driver, sinkName, resourceName);
        return sinkName;
    }

    private void selectSinkWhenInFlowbinderCreationWidget(String sink) {
        SeleniumUtil.selectItemInListBox(findSinkListElement(driver), sink);
    }

    /*
    private void populateSubmitterSelectionField() {
        String submitterName = "submitter12";
        SubmitterCreationSeleniumIT.createTestSubmitter(driver, submitterName, "123456", "Description");
        navigateToFlowbinderCreationWidget(driver);
        SeleniumUtil.selectItemInDualList(findSubmitterPanelElement(driver), submitterName);
    }
    */

    private String createDefaultSubmitter() {
        final String defaultSubmitterName = "defaultSubmitter";
        SubmitterCreationSeleniumIT.createTestSubmitter(driver, defaultSubmitterName, "123456", "Description");
        return defaultSubmitterName;
    }

    private void selectSubmitterWhenInFlowbinderCreationWidget(String submitter) {
        SeleniumUtil.selectItemInDualList(findSubmitterPanelElement(driver), submitter);
    }

    private void populateAllInputFieldsAndClickSaveAndWaitForSuccess() {
        populateAllInputFields();
        findSaveButtonElement(driver).click();
        SeleniumUtil.waitAndAssert(driver, SAVE_TIMEOUT, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_SAVE_PANEL, SaveButton.SAVE_BUTTON_RESULT_LABEL_CLASS, FlowbinderCreateViewImpl.FLOWBINDER_CREATION_SAVE_SUCCESS);
    }

    private static void navigateToFlowbinderCreationWidget(WebDriver webDriver) {
        findFlowbinderCreationContextElement(webDriver).click();
    }

    private static WebElement findFlowbinderCreationContextElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, MainPanel.GUIID_NAVIGATION_MENU_ITEM_FLOWBINDER_CREATION);
    }

    private static WebElement findFlowbinderCreationWidget(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_WIDGET);
    }

    private static WebElement findNameTextElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_NAME_PANEL, DataEntry.DATA_ENTRY_INPUT_BOX_CLASS);
    }

    private static WebElement findDescriptionTextElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_DESCRIPTION_PANEL, DataEntry.DATA_ENTRY_INPUT_BOX_CLASS);
    }

    private static WebElement findFrameTextElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_FRAME_PANEL, DataEntry.DATA_ENTRY_INPUT_BOX_CLASS);
    }

    private static WebElement findContentFormatTextElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_CONTENTFORMAT_PANEL, DataEntry.DATA_ENTRY_INPUT_BOX_CLASS);
    }

    private static WebElement findCharacterSetTextElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_CHARACTER_SET_PANEL, DataEntry.DATA_ENTRY_INPUT_BOX_CLASS);
    }

    private static WebElement findSinkListElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_SINK_PANEL, DataEntry.DATA_ENTRY_INPUT_BOX_CLASS);
    }

    private static WebElement findRecordSplitterTextElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_RECORD_SPLITTER_PANEL, DataEntry.DATA_ENTRY_INPUT_BOX_CLASS);
    }

    private static WebElement findSubmitterPanelElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_SUBMITTERS_SELECTION_PANEL);
    }

    private static WebElement findFlowListElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_FLOW_PANEL, DataEntry.DATA_ENTRY_INPUT_BOX_CLASS);
    }

    private static WebElement findSaveButtonElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_SAVE_PANEL, SaveButton.SAVE_BUTTON_BUTTON_CLASS);
    }

    private static WebElement findSaveResultLabelElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_SAVE_PANEL, SaveButton.SAVE_BUTTON_RESULT_LABEL_CLASS);
    }
}
