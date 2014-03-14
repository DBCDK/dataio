package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.gui.client.components.DataEntry;
import dk.dbc.dataio.gui.client.components.SaveButton;
import dk.dbc.dataio.gui.client.pages.flowbindercreate.FlowbinderCreateViewImpl;
import dk.dbc.dataio.gui.util.ClientFactoryImpl;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class FlowBinderCreationSeleniumIT extends AbstractGuiSeleniumTest {
    private static ConstantsProperties texts = new ConstantsProperties("pages/flowbindercreate/FlowbinderCreateConstants_dk.properties");

    private static final long SAVE_TIMEOUT = 4;

    @Test
    public void testInitialVisibilityAndAccessabilityOfElements() throws IOException {
        testFlowbinderCreationNameInputFieldIsVisibleAndDataCanBeInsertedAndRead();
        testFlowbinderCreationDescriptionInputFieldIsVisibleAndDataCanBeInsertedAndRead();
        testFlowbinderCreationFrameInputFieldIsVisibleAndDataCanBeInsertedAndRead();
        testFlowbinderCreationContentFormatInputFieldIsVisibleAndDataCanBeInsertedAndRead();
        testFlowbinderCreationCharacterSetInputFieldIsVisibleAndDataCanBeInsertedAndRead();
        testFlowbinderCreationDestinationInputFieldIsVisibleAndDataCanBeInsertedAndRead();
        //testFlowbinderCreationSubmitterDualListIsVisibleAndAnElementCanBeChosen();
        //testFlowbinderCreationFlowListIsVisibleAndAnElementCanBeSelected();
        //testFlowbinderCreationSinkListIsVisibleAndAnElementCanBeSelected();
        testFlowbinderCreationSaveButtonIsVisible();
        testFlowbinderCreationSaveResultLabeINotVisibleAndEmptyByDefault();
    }

    public void testFlowbinderCreationNameInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToFlowbinderCreationWidget(webDriver);
        SeleniumUtil.assertFieldIsVisbleAndDataCanBeInsertedAndReadWithMaxSize(findNameTextElement(webDriver), 160);
    }

    public void testFlowbinderCreationDescriptionInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToFlowbinderCreationWidget(webDriver);
        SeleniumUtil.assertFieldIsVisbleAndDataCanBeInsertedAndReadWithMaxSize(findDescriptionTextElement(webDriver), 160);
    }

    public void testFlowbinderCreationFrameInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToFlowbinderCreationWidget(webDriver);
        SeleniumUtil.assertFieldIsVisbleAndDataCanBeInsertedAndRead(findFrameTextElement(webDriver));
    }

    public void testFlowbinderCreationContentFormatInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToFlowbinderCreationWidget(webDriver);
        SeleniumUtil.assertFieldIsVisbleAndDataCanBeInsertedAndRead(findContentFormatTextElement(webDriver));
    }

    public void testFlowbinderCreationCharacterSetInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToFlowbinderCreationWidget(webDriver);
        SeleniumUtil.assertFieldIsVisbleAndDataCanBeInsertedAndRead(findCharacterSetTextElement(webDriver));
    }

    public void testFlowbinderCreationDestinationInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToFlowbinderCreationWidget(webDriver);
        SeleniumUtil.assertFieldIsVisbleAndDataCanBeInsertedAndRead(findDestinationTextElement(webDriver));
    }

    // This can, for some reason, not be included with the other visibility tests
    @Test
    public void testFlowbinderCreationSubmitterDualListIsVisibleAndAnElementCanBeChosen() {
        final String submitterName = "submitter1";
        final String submitterNumber = "123456";
        final String expectedDisplayName = submitterNumber + " (" + submitterName + ")";
        SubmitterCreationSeleniumIT.createTestSubmitter(webDriver, submitterName, submitterNumber, "Description");
        navigateToFlowbinderCreationWidget(webDriver);
        SeleniumUtil.assertDualListIsVisibleAndElementCanBeChosen(webDriver, findSubmitterPanelElement(webDriver), expectedDisplayName);
    }

    // This can, for some reason, not be included with the other visibility tests
    @Test
    public void testFlowbinderCreationFlowListIsVisibleAndAnElementCanBeSelected() {
        String flowComponentName = "flowComponent";
        String flowName = "flowName";
        FlowComponentCreationSeleniumIT.createTestFlowComponent(webDriver, flowComponentName);
        FlowCreationSeleniumIT.createTestFlow(webDriver, flowName, "description", flowComponentName);
        navigateToFlowbinderCreationWidget(webDriver);
        SeleniumUtil.assertListBoxIsVisibleAndAnElementCanBeSelected(webDriver, findFlowListElement(webDriver), flowName);
    }

    // This can, for some reason, not be included with the other visibility tests
    @Test
    public void testFlowbinderCreationSinkListIsVisibleAndAnElementCanBeSelected() {
        String sinkName = "oneSinkName";
        final String resourceName = SinkCreationSeleniumIT.SINK_CREATION_KNOWN_RESOURCE_NAME;
        SinkCreationSeleniumIT.createTestSink(webDriver, sinkName, resourceName);
        navigateToFlowbinderCreationWidget(webDriver);
        SeleniumUtil.assertListBoxIsVisibleAndAnElementCanBeSelected(webDriver, findSinkListElement(webDriver), sinkName);
    }

    public void testFlowbinderCreationSaveButtonIsVisible() {
        navigateToFlowbinderCreationWidget(webDriver);
        assertTrue(findSaveButtonElement(webDriver).isDisplayed());
    }

    public void testFlowbinderCreationSaveResultLabeINotVisibleAndEmptyByDefault() {
        navigateToFlowbinderCreationWidget(webDriver);
        WebElement element = findSaveResultLabelElement(webDriver);
        assertFalse(element.isDisplayed());
        assertThat(element.getText(), is(""));
    }

    @Test
    public void testFlowbinderCreationSuccessfulSave_saveResultLabelContainsSuccessMessage() {
        populateAllInputFieldsAndClickSaveAndWaitForSuccess();
    }

    @Test
    public void testSaveButton_emptyNameInputField_displayErrorPopup() {
        populateAllInputFields();
        findNameTextElement(webDriver).clear();
        findSaveButtonElement(webDriver).click();
        assertThat(SeleniumUtil.getAlertStringAndAccept(webDriver), is(texts.translate("error_InputFieldValidationError")));
    }

    @Test
    public void testSaveButton_emptyDescriptionInputField_displayErrorPopup() {
        populateAllInputFields();
        findDescriptionTextElement(webDriver).clear();
        findSaveButtonElement(webDriver).click();
        assertThat(SeleniumUtil.getAlertStringAndAccept(webDriver), is(texts.translate("error_InputFieldValidationError")));
    }

    @Test
    public void testSaveButton_emptyFrameInputField_displayErrorPopup() {
        populateAllInputFields();
        findFrameTextElement(webDriver).clear();
        findSaveButtonElement(webDriver).click();
        assertThat(SeleniumUtil.getAlertStringAndAccept(webDriver), is(texts.translate("error_InputFieldValidationError")));
    }

    @Test
    public void testSaveButton_emptyContentFormatInputField_displayErrorPopup() {
        populateAllInputFields();
        findContentFormatTextElement(webDriver).clear();
        findSaveButtonElement(webDriver).click();
        assertThat(SeleniumUtil.getAlertStringAndAccept(webDriver), is(texts.translate("error_InputFieldValidationError")));
    }

    @Test
    public void testSaveButton_emptyCharacterSetInputField_displayErrorPopup() {
        populateAllInputFields();
        findCharacterSetTextElement(webDriver).clear();
        findSaveButtonElement(webDriver).click();
        assertThat(SeleniumUtil.getAlertStringAndAccept(webDriver), is(texts.translate("error_InputFieldValidationError")));
    }

    @Test
    public void testSaveButton_emptyDestinationInputField_displayErrorPopup() {
        populateAllInputFields();
        findDestinationTextElement(webDriver).clear();
        findSaveButtonElement(webDriver).click();
        assertThat(SeleniumUtil.getAlertStringAndAccept(webDriver), is(texts.translate("error_InputFieldValidationError")));
    }

    @Test
    public void testSaveButton_emptySubmitterInputField_displayErrorPopup() {
        String sink = createDefaultSink("sinkName45");
        String flow = createDefaultFlow();
        navigateToFlowbinderCreationWidget(webDriver);
        populateAllTextInputFieldsWhenInFlowbinderCreationWidget();
        selectSinkWhenInFlowbinderCreationWidget(sink);
        selectFlowWhenInFlowbinderCreationWidget(flow);
        findSaveButtonElement(webDriver).click();
        assertThat(SeleniumUtil.getAlertStringAndAccept(webDriver), is(texts.translate("error_InputFieldValidationError")));
    }

    @Test
    public void testSaveButton_emptyFlowInputField_displayErrorPopup() {
        String sink = createDefaultSink("sinkName45");
        String submitter = createDefaultSubmitter();
        navigateToFlowbinderCreationWidget(webDriver);
        populateAllTextInputFieldsWhenInFlowbinderCreationWidget();
        selectSinkWhenInFlowbinderCreationWidget(sink);
        selectSubmitterWhenInFlowbinderCreationWidget(submitter);
        findSaveButtonElement(webDriver).click();
        assertThat(SeleniumUtil.getAlertStringAndAccept(webDriver), is(texts.translate("error_InputFieldValidationError")));
    }

    @Test
    public void testSaveButton_emptySinkInputField_displayErrorPopup() {
        String submitter = createDefaultSubmitter();
        String flow = createDefaultFlow();
        navigateToFlowbinderCreationWidget(webDriver);
        populateAllTextInputFieldsWhenInFlowbinderCreationWidget();
        selectSubmitterWhenInFlowbinderCreationWidget(submitter);
        selectFlowWhenInFlowbinderCreationWidget(flow);
        findSaveButtonElement(webDriver).click();
        assertThat(SeleniumUtil.getAlertStringAndAccept(webDriver), is(texts.translate("error_InputFieldValidationError")));
    }

    @Test
    public void testFlowBinderCreationNameInputFieldUpdate_clearsSaveResultLabel() {
        populateAllInputFieldsAndClickSaveAndWaitForSuccess();
        findNameTextElement(webDriver).sendKeys("other stuff");
        assertThat(findSaveResultLabelElement(webDriver).getText(), is(""));
    }

    @Test
    public void testFlowBinderCreationDescriptionInputFieldUpdate_clearsSaveResultLabel() {
        populateAllInputFieldsAndClickSaveAndWaitForSuccess();
        findDescriptionTextElement(webDriver).sendKeys("other stuff");
        assertThat(findSaveResultLabelElement(webDriver).getText(), is(""));
    }

    @Test
    public void testFlowBinderCreationFrameInputFieldUpdate_clearsSaveResultLabel() {
        populateAllInputFieldsAndClickSaveAndWaitForSuccess();
        findFrameTextElement(webDriver).sendKeys("other stuff");
        assertThat(findSaveResultLabelElement(webDriver).getText(), is(""));
    }

    @Test
    public void testFlowBinderCreationContentFormatInputFieldUpdate_clearsSaveResultLabel() {
        populateAllInputFieldsAndClickSaveAndWaitForSuccess();
        findContentFormatTextElement(webDriver).sendKeys("other stuff");
        assertThat(findSaveResultLabelElement(webDriver).getText(), is(""));
    }

    @Test
    public void testFlowBinderCreationCharacterSetInputFieldUpdate_clearsSaveResultLabel() {
        populateAllInputFieldsAndClickSaveAndWaitForSuccess();
        findCharacterSetTextElement(webDriver).sendKeys("other stuff");
        assertThat(findSaveResultLabelElement(webDriver).getText(), is(""));
    }

    @Test
    public void testFlowBinderCreationDestinationInputFieldUpdate_clearsSaveResultLabel() {
        populateAllInputFieldsAndClickSaveAndWaitForSuccess();
        findDestinationTextElement(webDriver).sendKeys("other stuff");
        assertThat(findSaveResultLabelElement(webDriver).getText(), is(""));
    }

    @Test
    public void testFlowBinderCreationSubmitterInputFieldUpdate_clearsSaveResultLabel() {
        final String submitterName = "anotherSubmitter";
        final String submitterNumber = "42";
        final String displayName = submitterNumber + " (" + submitterName + ")";
        SubmitterCreationSeleniumIT.createTestSubmitter(webDriver, submitterName, submitterNumber, "Description");
        populateAllInputFieldsAndClickSaveAndWaitForSuccess();
        SeleniumUtil.selectItemInDualList(findSubmitterPanelElement(webDriver), displayName);
        assertThat(findSaveResultLabelElement(webDriver).getText(), is(""));
    }

    @Test
    public void testFlowBinderCreationFlowInputFieldUpdate_clearsSaveResultLabel() {
        final String flowComponentName = "anotherFlowComponent";
        final String flowName = "anotherFlow";
        FlowComponentCreationSeleniumIT.createTestFlowComponent(webDriver, flowComponentName);
        FlowCreationSeleniumIT.createTestFlow(webDriver, flowName, "description", flowComponentName);
        populateAllInputFieldsAndClickSaveAndWaitForSuccess();
        SeleniumUtil.selectItemInListBox(findFlowListElement(webDriver), flowName);
        assertThat(findSaveResultLabelElement(webDriver).getText(), is(""));
    }

    @Test
    public void testFlowBinderCreationSinkInputFieldUpdate_clearsSaveResultLabel() {
        final String sinkName = "anotherSink";
        SinkCreationSeleniumIT.createTestSink(webDriver, sinkName, SinkCreationSeleniumIT.SINK_CREATION_KNOWN_RESOURCE_NAME);
        populateAllInputFieldsAndClickSaveAndWaitForSuccess();
        SeleniumUtil.selectItemInListBox(findSinkListElement(webDriver), sinkName);
        assertThat(findSaveResultLabelElement(webDriver).getText(), is(""));
    }

    @Test
    public void testFlowBinderCreationLeaveAndGetBack_clearsAllFields() {
        createDefaultSink("anExtraSinkName");  // To assure, that two sinks will be created
        populateAllInputFields();
        assertAllInputFields("Name", "Description", "Frame", "ContentFormat", "CharacterSet", "Destination", "Default Record Splitter", Arrays.asList("123456 (defaultSubmitter)"), "flowName12", "sinkName45");
        navigateAwayFromFlowbinderCreationWidget(webDriver);
        navigateToFlowbinderCreationWidget(webDriver);
        assertAllInputFields("", "", "", "", "", "", "Default Record Splitter", new ArrayList(), "flowName12", "anExtraSinkName");
    }



    /**
     * The following is private helper methods
     */
    private void populateAllInputFields() {
        String submitter = createDefaultSubmitter();
        String sink = createDefaultSink("sinkName45");
        String flow = createDefaultFlow();
        navigateToFlowbinderCreationWidget(webDriver);
        populateAllTextInputFieldsWhenInFlowbinderCreationWidget();
        selectSinkWhenInFlowbinderCreationWidget(sink);
        selectSubmitterWhenInFlowbinderCreationWidget(submitter);
        selectFlowWhenInFlowbinderCreationWidget(flow);
    }

    private void populateAllTextInputFieldsWhenInFlowbinderCreationWidget() {
        // navigateToFlowbinderCreationWidget(driver);
        findNameTextElement(webDriver).sendKeys("Name");
        findDescriptionTextElement(webDriver).sendKeys("Description");
        findFrameTextElement(webDriver).sendKeys("Frame");
        findContentFormatTextElement(webDriver).sendKeys("ContentFormat");
        findCharacterSetTextElement(webDriver).sendKeys("CharacterSet");
        findDestinationTextElement(webDriver).sendKeys("Destination");
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

    private void assertAllInputFields(String name, String description, String frame, String contentFormat, String charSet, String destination, String recordSplitter, List<String> submitter, String flow, String sink) {
        assertThat(findNameTextElement(webDriver).getAttribute("value"), is(name));
        assertThat(findDescriptionTextElement(webDriver).getAttribute("value"), is(description));
        assertThat(findFrameTextElement(webDriver).getAttribute("value"), is(frame));
        assertThat(findContentFormatTextElement(webDriver).getAttribute("value"), is(contentFormat));
        assertThat(findCharacterSetTextElement(webDriver).getAttribute("value"), is(charSet));
        assertThat(findDestinationTextElement(webDriver).getAttribute("value"), is(destination));
        assertThat(findRecordSplitterTextElement(webDriver).getAttribute("value"), is(recordSplitter));
        assertThat(SeleniumUtil.getSelectedItemsInDualList(findSubmitterPanelElement(webDriver)), is(submitter));
        assertThat(SeleniumUtil.getSelectedItemInListBox(findFlowListElement(webDriver)), is(flow));
        assertThat(SeleniumUtil.getSelectedItemInListBox(findSinkListElement(webDriver)), is(sink));
    }

    private String createDefaultFlow() {
        final String flowComponentName = "flowComponent12";
        final String flowName = "flowName12";
        FlowComponentCreationSeleniumIT.createTestFlowComponent(webDriver, flowComponentName);
        FlowCreationSeleniumIT.createTestFlow(webDriver, flowName, "description", flowComponentName);
        return flowName;
    }

    private void selectFlowWhenInFlowbinderCreationWidget(String flow) {
        SeleniumUtil.selectItemInListBox(findFlowListElement(webDriver), flow);
    }

    private String createDefaultSink(String sinkName) {
        final String resourceName = SinkCreationSeleniumIT.SINK_CREATION_KNOWN_RESOURCE_NAME;
        SinkCreationSeleniumIT.createTestSink(webDriver, sinkName, resourceName);
        return sinkName;
    }

    private void selectSinkWhenInFlowbinderCreationWidget(String sink) {
        SeleniumUtil.selectItemInListBox(findSinkListElement(webDriver), sink);
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
        final String defaultSubmitterNumber = "123456";
        final String displayName = defaultSubmitterNumber + " (" + defaultSubmitterName + ")";
        SubmitterCreationSeleniumIT.createTestSubmitter(webDriver, defaultSubmitterName, defaultSubmitterNumber, "Description");
        return displayName;
    }

    private void selectSubmitterWhenInFlowbinderCreationWidget(String submitter) {
        SeleniumUtil.selectItemInDualList(findSubmitterPanelElement(webDriver), submitter);
    }

    private void populateAllInputFieldsAndClickSaveAndWaitForSuccess() {
        populateAllInputFields();
        findSaveButtonElement(webDriver).click();
        SeleniumUtil.waitAndAssert(webDriver, SAVE_TIMEOUT, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_SAVE_PANEL, SaveButton.SAVE_BUTTON_RESULT_LABEL_CLASS, texts.translate("status_SaveSuccess"));
    }

    private static void navigateToFlowbinderCreationWidget(WebDriver webDriver) {
        NavigationPanelSeleniumIT.navigateTo(webDriver, ClientFactoryImpl.GUIID_MENU_ITEM_FLOWBINDER_CREATE);
    }

    private static void navigateAwayFromFlowbinderCreationWidget(WebDriver webDriver) {
        NavigationPanelSeleniumIT.navigateTo(webDriver, ClientFactoryImpl.GUIID_MENU_ITEM_FLOW_BINDERS_SHOW);
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

    private static WebElement findDestinationTextElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_DESTINATION_PANEL, DataEntry.DATA_ENTRY_INPUT_BOX_CLASS);
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

    private static WebElement findSinkListElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_SINK_PANEL, DataEntry.DATA_ENTRY_INPUT_BOX_CLASS);
    }

    private static WebElement findSaveButtonElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_SAVE_PANEL, SaveButton.SAVE_BUTTON_BUTTON_CLASS);
    }

    private static WebElement findSaveResultLabelElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_SAVE_PANEL, SaveButton.SAVE_BUTTON_RESULT_LABEL_CLASS);
    }

    /**
     * The following is public static helper methods.
     */
    /**
     * Creates a new Flow Binder with the given values
     * NOTE: It is the callers responsibility to create a flow, a sink and a
     * list of submitters beforehand with the given name.
     */
    public static void createTestFlowBinder(WebDriver webDriver, String name, String description, String frameFormat,
                                                                 String contentFormat, String charSet, String destination,
                                                                 List<String> submitters, String flow, String sink) {
        navigateToFlowbinderCreationWidget(webDriver);

        findNameTextElement(webDriver).clear();
        findNameTextElement(webDriver).sendKeys(name);

        findDescriptionTextElement(webDriver).clear();
        findDescriptionTextElement(webDriver).sendKeys(description);

        findFrameTextElement(webDriver).clear();
        findFrameTextElement(webDriver).sendKeys(frameFormat);

        findContentFormatTextElement(webDriver).clear();
        findContentFormatTextElement(webDriver).sendKeys(contentFormat);

        findCharacterSetTextElement(webDriver).clear();
        findCharacterSetTextElement(webDriver).sendKeys(charSet);

        findDestinationTextElement(webDriver).clear();
        findDestinationTextElement(webDriver).sendKeys(destination);

        for (String submitter: submitters) {
            SeleniumUtil.selectItemInDualList(findSubmitterPanelElement(webDriver), submitter);
        }

        SeleniumUtil.selectItemInListBox(findFlowListElement(webDriver), flow);

        SeleniumUtil.selectItemInListBox(findSinkListElement(webDriver), sink);

        findSaveButtonElement(webDriver).click();

        SeleniumUtil.waitAndAssert(webDriver, SAVE_TIMEOUT, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_SAVE_PANEL, SaveButton.SAVE_BUTTON_RESULT_LABEL_CLASS, texts.translate("status_SaveSuccess"));
    }
}
