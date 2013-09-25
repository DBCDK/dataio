package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.gui.client.views.FlowbinderCreateViewImpl;
import dk.dbc.dataio.gui.client.views.MainPanel;
import dk.dbc.dataio.integrationtest.ITUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
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

public class FlowBinderCreationSeleniumIT {

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
        navigateToFlowbinderCreationContext(driver);
        SeleniumUtil.assertFieldIsVisbleAndDataCanBeInsertedAndReadWithMaxSize(findNameTextElement(driver), 160);
    }

    public void testFlowbinderCreationDescriptionInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToFlowbinderCreationContext(driver);
        SeleniumUtil.assertFieldIsVisbleAndDataCanBeInsertedAndReadWithMaxSize(findDescriptionTextElement(driver), 160);
    }

    public void testFlowbinderCreationFrameInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToFlowbinderCreationContext(driver);
        SeleniumUtil.assertFieldIsVisbleAndDataCanBeInsertedAndRead(findFrameTextElement(driver));
    }

    public void testFlowbinderCreationContentFormatInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToFlowbinderCreationContext(driver);
        SeleniumUtil.assertFieldIsVisbleAndDataCanBeInsertedAndRead(findContentFormatTextElement(driver));
    }

    public void testFlowbinderCreationCharacterSetInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToFlowbinderCreationContext(driver);
        SeleniumUtil.assertFieldIsVisbleAndDataCanBeInsertedAndRead(findCharacterSetTextElement(driver));
    }

    public void testFlowbinderCreationSinkInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToFlowbinderCreationContext(driver);
        SeleniumUtil.assertFieldIsVisbleAndDataCanBeInsertedAndRead(findSinkTextElement(driver));
    }

    // This can, for some reason, not be included with the other visibility tests
    @Test
    public void testFlowbinderCreationSubmitterDualListIsVisibleAndAnElementCanBeChosen() {
        String submitterName = "submitter1";
        SubmitterCreationSeleniumIT.createTestSubmitter(driver, submitterName, "123456", "Description");
        navigateToFlowbinderCreationContext(driver);
        SeleniumUtil.assertDualListIsVisibleAndElementCanBeChosen(driver, findSubmitterPanelElement(driver), submitterName);
    }

    // This can, for some reason, not be included with the other visibility tests
    @Test
    public void testFlowbinderCreationFlowListIsVisibleAndAnElementCanBeSelected() {
        String flowComponentName = "flowComponent";
        String flowName = "flowName";
        FlowComponentCreationSeleniumIT.createTestFlowComponent(driver, flowComponentName);
        FlowCreationSeleniumIT.createTestFlow(driver, flowName, "description", flowComponentName);

        navigateToFlowbinderCreationContext(driver);
        SeleniumUtil.assertListBoxIsVisibleAndAnElementCanBeSelected(driver, findFlowListElement(driver), flowName);
    }

    public void testFlowbinderCreationSaveButtonIsVisible() {
        navigateToFlowbinderCreationContext(driver);
        assertTrue(findSaveButtonElement(driver).isDisplayed());
    }

    public void testFlowbinderCreationSaveResultLableIsNotVisibleAndEmptyByDefault() {
        navigateToFlowbinderCreationContext(driver);
        WebElement element = findSaveResultLabelElement(driver);
        assertFalse(element.isDisplayed());
        assertThat(element.getText(), is(""));
    }

    @Test
    public void testFlowbinderCreationSuccessfulSave_saveResultLabelContainsSuccessMessage() {
        populateAllInputFieldsAndClickSave();
    }

    @Test
    public void testSaveButton_emptyNameInputField_displayErrorPopup() {
        navigateToFlowbinderCreationContext(driver);
        populateAllInputFields();
        findNameTextElement(driver).clear();
        findSaveButtonElement(driver).click();
        assertThat(SeleniumUtil.getAlertStringAndAccept(driver), is(FlowbinderCreateViewImpl.FLOWBINDER_CREATION_INPUT_FIELD_VALIDATION_ERROR));
    }

    @Test
    public void testSaveButton_emptyDescriptionInputField_displayErrorPopup() {
        navigateToFlowbinderCreationContext(driver);
        populateAllInputFields();
        findDescriptionTextElement(driver).clear();
        findSaveButtonElement(driver).click();
        assertThat(SeleniumUtil.getAlertStringAndAccept(driver), is(FlowbinderCreateViewImpl.FLOWBINDER_CREATION_INPUT_FIELD_VALIDATION_ERROR));
    }

    @Test
    public void testSaveButton_emptyFrameInputField_displayErrorPopup() {
        navigateToFlowbinderCreationContext(driver);
        populateAllInputFields();
        findFrameTextElement(driver).clear();
        findSaveButtonElement(driver).click();
        assertThat(SeleniumUtil.getAlertStringAndAccept(driver), is(FlowbinderCreateViewImpl.FLOWBINDER_CREATION_INPUT_FIELD_VALIDATION_ERROR));
    }

    @Test
    public void testSaveButton_emptyContentFormatInputField_displayErrorPopup() {
        navigateToFlowbinderCreationContext(driver);
        populateAllInputFields();
        findContentFormatTextElement(driver).clear();
        findSaveButtonElement(driver).click();
        assertThat(SeleniumUtil.getAlertStringAndAccept(driver), is(FlowbinderCreateViewImpl.FLOWBINDER_CREATION_INPUT_FIELD_VALIDATION_ERROR));
    }

    @Test
    public void testSaveButton_emptyCharacterSetInputField_displayErrorPopup() {
        navigateToFlowbinderCreationContext(driver);
        populateAllInputFields();
        findCharacterSetTextElement(driver).clear();
        findSaveButtonElement(driver).click();
        assertThat(SeleniumUtil.getAlertStringAndAccept(driver), is(FlowbinderCreateViewImpl.FLOWBINDER_CREATION_INPUT_FIELD_VALIDATION_ERROR));
    }

    @Test
    public void testSaveButton_emptySinkInputField_displayErrorPopup() {
        navigateToFlowbinderCreationContext(driver);
        populateAllInputFields();
        findSinkTextElement(driver).clear();
        findSaveButtonElement(driver).click();
        assertThat(SeleniumUtil.getAlertStringAndAccept(driver), is(FlowbinderCreateViewImpl.FLOWBINDER_CREATION_INPUT_FIELD_VALIDATION_ERROR));
    }

    @Test
    public void testSaveButton_emptyFlowInputField_displayErrorPopup() {
        navigateToFlowbinderCreationContext(driver);
        populateSubmitterSelectionField();
        populateAllTextInputFields();
        findSaveButtonElement(driver).click();
        assertThat(SeleniumUtil.getAlertStringAndAccept(driver), is(FlowbinderCreateViewImpl.FLOWBINDER_CREATION_INPUT_FIELD_VALIDATION_ERROR));
    }

    @Test
    public void testSaveButton_emptySubmitterInputField_displayErrorPopup() {
        navigateToFlowbinderCreationContext(driver);
        populateFlowSelectionField();
        populateAllTextInputFields();
        findSaveButtonElement(driver).click();
        assertThat(SeleniumUtil.getAlertStringAndAccept(driver), is(FlowbinderCreateViewImpl.FLOWBINDER_CREATION_INPUT_FIELD_VALIDATION_ERROR));
    }

    @Test
    public void testFlowBinderCreationNameInputFieldUpdate_clearsSaveResultLabel() {
        navigateToFlowbinderCreationContext(driver);
        populateAllInputFieldsAndClickSave();
        findNameTextElement(driver).sendKeys("other stuff");
        assertThat(findSaveResultLabelElement(driver).getText(), is(""));
    }

    @Test
    public void testFlowBinderCreationDescriptionInputFieldUpdate_clearsSaveResultLabel() {
        navigateToFlowbinderCreationContext(driver);
        populateAllInputFieldsAndClickSave();
        findDescriptionTextElement(driver).sendKeys("other stuff");
        assertThat(findSaveResultLabelElement(driver).getText(), is(""));
    }

    @Test
    public void testFlowBinderCreationFrameInputFieldUpdate_clearsSaveResultLabel() {
        navigateToFlowbinderCreationContext(driver);
        populateAllInputFieldsAndClickSave();
        findFrameTextElement(driver).sendKeys("other stuff");
        assertThat(findSaveResultLabelElement(driver).getText(), is(""));
    }

    @Test
    public void testFlowBinderCreationContentFormatInputFieldUpdate_clearsSaveResultLabel() {
        navigateToFlowbinderCreationContext(driver);
        populateAllInputFieldsAndClickSave();
        findContentFormatTextElement(driver).sendKeys("other stuff");
        assertThat(findSaveResultLabelElement(driver).getText(), is(""));
    }

    @Test
    public void testFlowBinderCreationCharacterSetInputFieldUpdate_clearsSaveResultLabel() {
        navigateToFlowbinderCreationContext(driver);
        populateAllInputFieldsAndClickSave();
        findCharacterSetTextElement(driver).sendKeys("other stuff");
        assertThat(findSaveResultLabelElement(driver).getText(), is(""));
    }

    @Test
    public void testFlowBinderCreationFlowInputFieldUpdate_clearsSaveResultLabel() {
        final String flowComponentName = "anotherFlowComponent";
        final String flowName = "anotherFlow";
        FlowComponentCreationSeleniumIT.createTestFlowComponent(driver, flowComponentName);
        FlowCreationSeleniumIT.createTestFlow(driver, flowName, "description", flowComponentName);
        navigateToFlowbinderCreationContext(driver);
        populateAllInputFieldsAndClickSave();
        SeleniumUtil.selectItemInListBox(findFlowListElement(driver), flowName);
        assertThat(findSaveResultLabelElement(driver).getText(), is(""));
    }

    @Test
    public void testFlowBinderCreationSubmitterInputFieldUpdate_clearsSaveResultLabel() {
        final String submitterName = "anotherSubmitter";
        SubmitterCreationSeleniumIT.createTestSubmitter(driver, submitterName, "42", "Description");
        navigateToFlowbinderCreationContext(driver);
        populateAllInputFieldsAndClickSave();
        SeleniumUtil.selectItemInDualList(findSubmitterPanelElement(driver), submitterName);
        assertThat(findSaveResultLabelElement(driver).getText(), is(""));
    }

    /**
     * The following is private helper methods
     */
    private void populateAllInputFields() {
        populateSubmitterSelectionField();
        populateAllTextInputFields();
        populateFlowSelectionField();
    }

    private void populateAllTextInputFields() {
        navigateToFlowbinderCreationContext(driver);
        findNameTextElement(driver).sendKeys("Name");
        findDescriptionTextElement(driver).sendKeys("Description");
        findFrameTextElement(driver).sendKeys("Frame");
        findContentFormatTextElement(driver).sendKeys("ContentFormat");
        findCharacterSetTextElement(driver).sendKeys("CharacterSet");
        findSinkTextElement(driver).sendKeys("Sink");
    }

    private void populateFlowSelectionField() {
        String flowComponentName = "flowComponent12";
        String flowName = "flowName12";
        FlowComponentCreationSeleniumIT.createTestFlowComponent(driver, flowComponentName);
        FlowCreationSeleniumIT.createTestFlow(driver, flowName, "description", flowComponentName);
        navigateToFlowbinderCreationContext(driver);
        SeleniumUtil.selectItemInListBox(findFlowListElement(driver), flowName);
    }

    private void populateSubmitterSelectionField() {
        String submitterName = "submitter12";
        SubmitterCreationSeleniumIT.createTestSubmitter(driver, submitterName, "123456", "Description");
        navigateToFlowbinderCreationContext(driver);
        SeleniumUtil.selectItemInDualList(findSubmitterPanelElement(driver), submitterName);
    }

    private void populateAllInputFieldsAndClickSave() {
        populateAllInputFields();
        findSaveButtonElement(driver).click();
        SeleniumUtil.waitAndAssert(driver, 4, findSaveResultLabelElement(driver), FlowbinderCreateViewImpl.FLOWBINDER_CREATION_SAVE_SUCCESS);
    }

    private static void navigateToFlowbinderCreationContext(WebDriver webDriver) {
        findFlowbinderCreationContextElement(webDriver).click();
    }

    private static WebElement findFlowbinderCreationContextElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, MainPanel.GUIID_NAVIGATION_MENU_ITEM_FLOWBINDER_CREATION);
    }

    private static WebElement findFlowbinderCreationWidget(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_WIDGET);
    }

    private static WebElement findNameTextElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_NAME_TEXT_BOX);
    }

    private static WebElement findDescriptionTextElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_DESCRIPTION_TEXT_AREA);
    }

    private static WebElement findFrameTextElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_FRAME_TEXT_BOX);
    }

    private static WebElement findContentFormatTextElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_CONTENTFORMAT_TEXT_BOX);
    }

    private static WebElement findCharacterSetTextElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_CHARACTER_SET_TEXT_BOX);
    }

    private static WebElement findSinkTextElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_SINK_TEXT_BOX);
    }

    private static WebElement findRecordSplitterTextElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_RECORD_SPLITTER_TEXT_BOX);
    }

    private static WebElement findSubmitterPanelElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_SUBMITTERS_SELECTION_PANEL);
    }

    private static WebElement findFlowListElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_FLOW_LIST_BOX);
    }

    private static WebElement findSaveButtonElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_SAVE_BUTTON);
    }

    private static WebElement findSaveResultLabelElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_SAVE_RESULT_LABEL);
    }
}
