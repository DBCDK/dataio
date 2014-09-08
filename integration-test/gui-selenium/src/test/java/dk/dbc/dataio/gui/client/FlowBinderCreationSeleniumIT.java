package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.SubmitterContent;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.SubmitterContentBuilder;
import dk.dbc.dataio.gui.client.components.DataEntry;
import dk.dbc.dataio.gui.client.components.SaveButton;
import dk.dbc.dataio.gui.client.pages.flowbinder.flowbindercreate.FlowbinderCreateViewImpl;
import dk.dbc.dataio.gui.util.ClientFactoryImpl;
import dk.dbc.dataio.integrationtest.ITUtil;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import javax.ws.rs.client.Client;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class FlowBinderCreationSeleniumIT extends AbstractGuiSeleniumTest {
    private static ConstantsProperties texts = new ConstantsProperties("pages/flowbinder/flowbindercreate/FlowbinderCreateConstants_dk.properties");

    private static final long SAVE_TIMEOUT = 4;

    private static FlowStoreServiceConnector flowStoreServiceConnector;
    private static final String SINK_CREATION_KNOWN_RESOURCE_NAME = "jdbc/flowStoreDb";

    @BeforeClass
    public static void setUpClass() throws ClassNotFoundException, SQLException {
        String baseUrl = ITUtil.FLOW_STORE_BASE_URL;
        Client restClient = HttpClient.newClient();
        flowStoreServiceConnector = new FlowStoreServiceConnector(restClient, baseUrl);
    }

    @Test
    public void testInitialVisibilityAndAccessabilityOfElements() throws IOException {
        testFlowbinderCreationNameInputFieldIsVisibleAndDataCanBeInsertedAndRead();
        testFlowbinderCreationDescriptionInputFieldIsVisibleAndDataCanBeInsertedAndRead();
        testFlowbinderCreationFrameInputFieldIsVisibleAndDataCanBeInsertedAndRead();
        testFlowbinderCreationContentFormatInputFieldIsVisibleAndDataCanBeInsertedAndRead();
        testFlowbinderCreationCharacterSetInputFieldIsVisibleAndDataCanBeInsertedAndRead();
        testFlowbinderCreationDestinationInputFieldIsVisibleAndDataCanBeInsertedAndRead();
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
    public void testFlowbinderCreationSubmitterDualListIsVisibleAndAnElementCanBeChosen() throws Exception{
        Submitter submitter = createTestSubmitter("submitter1", 123456L);
        navigateToFlowbinderCreationWidget(webDriver);
        SeleniumUtil.assertDualListIsVisibleAndElementCanBeChosen(webDriver, findSubmitterPanelElement(webDriver), createSubmitterDisplayName(submitter));
    }

    // This can, for some reason, not be included with the other visibility tests
    @Test
    public void testFlowbinderCreationFlowListIsVisibleAndAnElementCanBeSelected() throws Exception {
        String flowName = "flowName";
        FlowComponent flowComponent = createTestFlowComponent("flowComponent");
        createTestFlow(flowName, "flowDescription", java.util.Arrays.asList(flowComponent));
        navigateToFlowbinderCreationWidget(webDriver);
        SeleniumUtil.assertListBoxIsVisibleAndAnElementCanBeSelected(webDriver, findFlowListElement(webDriver), flowName);
    }

    // This can, for some reason, not be included with the other visibility tests
    @Test
    public void testFlowbinderCreationSinkListIsVisibleAndAnElementCanBeSelected() throws Exception{
        Sink sink = createTestSink("oneSinkName", SINK_CREATION_KNOWN_RESOURCE_NAME);
        navigateToFlowbinderCreationWidget(webDriver);
        SeleniumUtil.assertListBoxIsVisibleAndAnElementCanBeSelected(webDriver, findSinkListElement(webDriver), sink.getContent().getName());
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
    public void testFlowbinderCreationSuccessfulSave_saveResultLabelContainsSuccessMessage() throws Exception{
        populateAllInputFieldsAndClickSaveAndWaitForSuccess();
    }

    @Test
    public void testSaveButton_emptyNameInputField_displayErrorPopup() throws Exception{
        populateAllInputFields();
        findNameTextElement(webDriver).clear();
        findSaveButtonElement(webDriver).click();
        assertThat(SeleniumUtil.getAlertStringAndAccept(webDriver), is(texts.translate("error_InputFieldValidationError")));
    }

    @Test
    public void testSaveButton_emptyDescriptionInputField_displayErrorPopup() throws Exception{
        populateAllInputFields();
        findDescriptionTextElement(webDriver).clear();
        findSaveButtonElement(webDriver).click();
        assertThat(SeleniumUtil.getAlertStringAndAccept(webDriver), is(texts.translate("error_InputFieldValidationError")));
    }

    @Test
    public void testSaveButton_emptyFrameInputField_displayErrorPopup() throws Exception{
        populateAllInputFields();
        findFrameTextElement(webDriver).clear();
        findSaveButtonElement(webDriver).click();
        assertThat(SeleniumUtil.getAlertStringAndAccept(webDriver), is(texts.translate("error_InputFieldValidationError")));
    }

    @Test
    public void testSaveButton_emptyContentFormatInputField_displayErrorPopup() throws Exception{
        populateAllInputFields();
        findContentFormatTextElement(webDriver).clear();
        findSaveButtonElement(webDriver).click();
        assertThat(SeleniumUtil.getAlertStringAndAccept(webDriver), is(texts.translate("error_InputFieldValidationError")));
    }

    @Test
    public void testSaveButton_emptyCharacterSetInputField_displayErrorPopup() throws Exception{
        populateAllInputFields();
        findCharacterSetTextElement(webDriver).clear();
        findSaveButtonElement(webDriver).click();
        assertThat(SeleniumUtil.getAlertStringAndAccept(webDriver), is(texts.translate("error_InputFieldValidationError")));
    }

    @Test
    public void testSaveButton_emptyDestinationInputField_displayErrorPopup() throws Exception{
        populateAllInputFields();
        findDestinationTextElement(webDriver).clear();
        findSaveButtonElement(webDriver).click();
        assertThat(SeleniumUtil.getAlertStringAndAccept(webDriver), is(texts.translate("error_InputFieldValidationError")));
    }

    @Test
    public void testSaveButton_emptySubmitterInputField_displayErrorPopup() throws Exception{
        Sink sink = createTestSink("sinkName45", SINK_CREATION_KNOWN_RESOURCE_NAME);
        FlowComponent flowComponent = createTestFlowComponent("flowComponent12");
        Flow flow = createTestFlow("flowName12", "description", java.util.Arrays.asList(flowComponent));
        navigateToFlowbinderCreationWidget(webDriver);
        populateAllTextInputFieldsWhenInFlowbinderCreationWidget();
        selectSinkWhenInFlowbinderCreationWidget(sink.getContent().getName());
        selectFlowWhenInFlowbinderCreationWidget(flow.getContent().getName());
        findSaveButtonElement(webDriver).click();
        assertThat(SeleniumUtil.getAlertStringAndAccept(webDriver), is(texts.translate("error_InputFieldValidationError")));
    }

    @Test
    public void testSaveButton_emptyFlowInputField_displayErrorPopup() throws Exception{
        Sink sink = createTestSink("sinkName45", SINK_CREATION_KNOWN_RESOURCE_NAME);
        Submitter submitter = createTestSubmitter("defaultSubmitter", 123456L);
        navigateToFlowbinderCreationWidget(webDriver);
        populateAllTextInputFieldsWhenInFlowbinderCreationWidget();
        selectSinkWhenInFlowbinderCreationWidget(sink.getContent().getName());
        selectSubmitterWhenInFlowbinderCreationWidget(createSubmitterDisplayName(submitter));
        findSaveButtonElement(webDriver).click();
        assertThat(SeleniumUtil.getAlertStringAndAccept(webDriver), is(texts.translate("error_InputFieldValidationError")));
    }

    @Test
    public void testSaveButton_emptySinkInputField_displayErrorPopup() throws Exception{
        Submitter submitter = createTestSubmitter("defaultSubmitter", 123456L);
        FlowComponent flowComponent = createTestFlowComponent("flowComponent12");
        Flow flow = createTestFlow("flowName12", "description", java.util.Arrays.asList(flowComponent));
        navigateToFlowbinderCreationWidget(webDriver);
        populateAllTextInputFieldsWhenInFlowbinderCreationWidget();
        selectSubmitterWhenInFlowbinderCreationWidget(createSubmitterDisplayName(submitter));
        selectFlowWhenInFlowbinderCreationWidget(flow.getContent().getName());
        findSaveButtonElement(webDriver).click();
        assertThat(SeleniumUtil.getAlertStringAndAccept(webDriver), is(texts.translate("error_InputFieldValidationError")));
    }

    @Test
    public void testFlowBinderCreationNameInputFieldUpdate_clearsSaveResultLabel() throws Exception{
        populateAllInputFieldsAndClickSaveAndWaitForSuccess();
        findNameTextElement(webDriver).sendKeys("other stuff");
        assertThat(findSaveResultLabelElement(webDriver).getText(), is(""));
    }

    @Test
    public void testFlowBinderCreationDescriptionInputFieldUpdate_clearsSaveResultLabel() throws Exception{
        populateAllInputFieldsAndClickSaveAndWaitForSuccess();
        findDescriptionTextElement(webDriver).sendKeys("other stuff");
        assertThat(findSaveResultLabelElement(webDriver).getText(), is(""));
    }

    @Test
    public void testFlowBinderCreationFrameInputFieldUpdate_clearsSaveResultLabel() throws Exception{
        populateAllInputFieldsAndClickSaveAndWaitForSuccess();
        findFrameTextElement(webDriver).sendKeys("other stuff");
        assertThat(findSaveResultLabelElement(webDriver).getText(), is(""));
    }

    @Test
    public void testFlowBinderCreationContentFormatInputFieldUpdate_clearsSaveResultLabel() throws Exception{
        populateAllInputFieldsAndClickSaveAndWaitForSuccess();
        findContentFormatTextElement(webDriver).sendKeys("other stuff");
        assertThat(findSaveResultLabelElement(webDriver).getText(), is(""));
    }

    @Test
    public void testFlowBinderCreationCharacterSetInputFieldUpdate_clearsSaveResultLabel() throws Exception{
        populateAllInputFieldsAndClickSaveAndWaitForSuccess();
        findCharacterSetTextElement(webDriver).sendKeys("other stuff");
        assertThat(findSaveResultLabelElement(webDriver).getText(), is(""));
    }

    @Test
    public void testFlowBinderCreationDestinationInputFieldUpdate_clearsSaveResultLabel() throws Exception{
        populateAllInputFieldsAndClickSaveAndWaitForSuccess();
        findDestinationTextElement(webDriver).sendKeys("other stuff");
        assertThat(findSaveResultLabelElement(webDriver).getText(), is(""));
    }

    @Test
    public void testFlowBinderCreationSubmitterInputFieldUpdate_clearsSaveResultLabel() throws Exception{
        Submitter submitter = createTestSubmitter("anotherSubmitter", 42L);
        populateAllInputFieldsAndClickSaveAndWaitForSuccess();
        SeleniumUtil.selectItemInDualList(findSubmitterPanelElement(webDriver), createSubmitterDisplayName(submitter));
        assertThat(findSaveResultLabelElement(webDriver).getText(), is(""));
    }

    @Test
    public void testFlowBinderCreationFlowInputFieldUpdate_clearsSaveResultLabel() throws Exception{
        final String flowName = "anotherFlow";
        FlowComponent flowComponent = createTestFlowComponent("anotherFlowComponent");
        createTestFlow(flowName, "flowDescription", java.util.Arrays.asList(flowComponent));
        populateAllInputFieldsAndClickSaveAndWaitForSuccess();
        SeleniumUtil.selectItemInListBox(findFlowListElement(webDriver), flowName);
        assertThat(findSaveResultLabelElement(webDriver).getText(), is(""));
    }

    @Test
    public void testFlowBinderCreationSinkInputFieldUpdate_clearsSaveResultLabel() throws Exception{
        Sink sink = createTestSink("anotherSink", SINK_CREATION_KNOWN_RESOURCE_NAME);
        populateAllInputFieldsAndClickSaveAndWaitForSuccess();
        SeleniumUtil.selectItemInListBox(findSinkListElement(webDriver), sink.getContent().getName());
        assertThat(findSaveResultLabelElement(webDriver).getText(), is(""));
    }

    @Test
    public void testFlowBinderCreationLeaveAndGetBack_clearsAllFields() throws Exception{
        // To assure, that two sinks will be created
        createTestSink("anExtraSinkName", SINK_CREATION_KNOWN_RESOURCE_NAME);
        populateAllInputFields();
        assertAllInputFields("Name", "Description", "Frame", "ContentFormat", "CharacterSet", "Destination", "Default Record Splitter", Arrays.asList("123456 (defaultSubmitter)"), "flowName12", "sinkName45");
        navigateAwayFromFlowbinderCreationWidget(webDriver);
        navigateToFlowbinderCreationWidget(webDriver);
        assertAllInputFields("", "", "", "", "", "", "Default Record Splitter", new ArrayList(), "flowName12", "anExtraSinkName");
    }

    /**
     * The following is private helper methods
     */
    private void populateAllInputFields() throws Exception{
        Submitter submitter = createTestSubmitter("defaultSubmitter", 123456L);
        Sink sink = createTestSink("sinkName45", SINK_CREATION_KNOWN_RESOURCE_NAME);
        FlowComponent flowComponent = createTestFlowComponent("flowComponent12");
        Flow flow = createTestFlow("flowName12", "description", java.util.Arrays.asList(flowComponent));
        navigateToFlowbinderCreationWidget(webDriver);
        populateAllTextInputFieldsWhenInFlowbinderCreationWidget();
        selectSinkWhenInFlowbinderCreationWidget(sink.getContent().getName());
        selectSubmitterWhenInFlowbinderCreationWidget(createSubmitterDisplayName(submitter));
        selectFlowWhenInFlowbinderCreationWidget(flow.getContent().getName());
    }

    private void populateAllTextInputFieldsWhenInFlowbinderCreationWidget() {
        findNameTextElement(webDriver).sendKeys("Name");
        findDescriptionTextElement(webDriver).sendKeys("Description");
        findFrameTextElement(webDriver).sendKeys("Frame");
        findContentFormatTextElement(webDriver).sendKeys("ContentFormat");
        findCharacterSetTextElement(webDriver).sendKeys("CharacterSet");
        findDestinationTextElement(webDriver).sendKeys("Destination");
    }

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

    private void selectFlowWhenInFlowbinderCreationWidget(String flow) {
        SeleniumUtil.selectItemInListBox(findFlowListElement(webDriver), flow);
    }

    private static Sink createTestSink(String sinkName, String resource) throws Exception{
        SinkContent sinkContent = new SinkContentBuilder()
                .setName(sinkName)
                .setResource(resource)
                .build();

        return flowStoreServiceConnector.createSink(sinkContent);
    }

    private void selectSinkWhenInFlowbinderCreationWidget(String sink) {
        SeleniumUtil.selectItemInListBox(findSinkListElement(webDriver), sink);
    }

    private static Submitter createTestSubmitter(String submitterName, Long submitterNumber) throws FlowStoreServiceConnectorException {
        SubmitterContent submitterContent = new SubmitterContentBuilder()
                .setName(submitterName)
                .setNumber(submitterNumber)
                .setDescription("Description")
                .build();

        return flowStoreServiceConnector.createSubmitter(submitterContent);
    }
    private static Flow createTestFlow(String flowName, String flowDescription, List<FlowComponent> flowComponents) throws Exception{
        FlowContent flowContent = new FlowContentBuilder()
                .setName(flowName)
                .setDescription(flowDescription)
                .setComponents(flowComponents)
                .build();

        return flowStoreServiceConnector.createFlow(flowContent);
    }

    private static FlowComponent createTestFlowComponent(String flowComponentName) throws Exception{
        FlowComponentContent flowComponentContent = new FlowComponentContentBuilder()
                .setName(flowComponentName)
                .build();

        return flowStoreServiceConnector.createFlowComponent(flowComponentContent);
    }

    private static String createSubmitterDisplayName(Submitter submitter){
        return submitter.getContent().getNumber() + " (" + submitter.getContent().getName() + ")";
    }

    private void selectSubmitterWhenInFlowbinderCreationWidget(String submitter) {
        SeleniumUtil.selectItemInDualList(findSubmitterPanelElement(webDriver), submitter);
    }

    private void populateAllInputFieldsAndClickSaveAndWaitForSuccess() throws Exception{
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
