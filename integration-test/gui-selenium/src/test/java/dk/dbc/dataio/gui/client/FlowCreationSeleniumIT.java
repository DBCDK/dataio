package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentContentBuilder;
import dk.dbc.dataio.gui.client.components.DataEntry;
import dk.dbc.dataio.gui.client.components.SaveButton;
import dk.dbc.dataio.gui.client.pages.flow.flowcreate.FlowCreateViewImpl;
import dk.dbc.dataio.gui.util.ClientFactoryImpl;
import dk.dbc.dataio.integrationtest.ITUtil;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import javax.ws.rs.client.Client;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class FlowCreationSeleniumIT extends AbstractGuiSeleniumTest {
    private static ConstantsProperties texts = new ConstantsProperties("pages/flow/flowcreate/FlowCreateConstants_dk.properties");

    private static final long SAVE_TIMEOUT = 4;
    private static FlowStoreServiceConnector flowStoreServiceConnector;

    @BeforeClass
    public static void setUpClass() throws ClassNotFoundException, SQLException {
        String baseUrl = ITUtil.FLOW_STORE_BASE_URL;
        Client restClient = HttpClient.newClient();
        flowStoreServiceConnector = new FlowStoreServiceConnector(restClient, baseUrl);
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
        navigateToFlowCreationWidget(webDriver);
        assertTrue(findFlowCreationWidget(webDriver).isDisplayed());
    }

//    @Test
    public void testFlowCreationNameInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToFlowCreationWidget(webDriver);
        SeleniumUtil.assertFieldIsVisbleAndDataCanBeInsertedAndRead(findNameElement(webDriver));
    }

//    @Test
    public void testFlowCreationDescriptionInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToFlowCreationWidget(webDriver);
        SeleniumUtil.assertFieldIsVisbleAndDataCanBeInsertedAndReadWithMaxSize(findDescriptionElement(webDriver), 160);
    }

    @Test
    public void testFlowCreationFlowComponentSelectionFieldIsVisibleAndAnElementCanBeChosen() throws Exception {
        String flowComponentName = "testComponent";
        createTestFlowComponent(flowComponentName);
        navigateToFlowCreationWidget(webDriver);
        SeleniumUtil.assertDualListIsVisibleAndElementCanBeChosen(webDriver, findComponentSelectionElement(webDriver), flowComponentName);
   }

//    @Test
    public void testFlowCreationSaveButtonIsVisible() {
        navigateToFlowCreationWidget(webDriver);
        assertTrue(findSaveButtonElement(webDriver).isDisplayed());
    }

//    @Test
    public void testFlowCreationSaveResultLabelIsNotVisibleAndEmptyAsDefault() {
        navigateToFlowCreationWidget(webDriver);
        WebElement element = findSaveResultElement(webDriver);
        assertFalse(element.isDisplayed());
        assertThat(element.getText(), is(""));
    }

    @Test
    public void testFlowCreationSuccessfulSave_saveResultLabelContainsSuccessMessage() throws Exception {
        String flowComponentName = "testComponent";
        createTestFlowComponent(flowComponentName);
        navigateToFlowCreationWidget(webDriver);
        insertTextInInputFieldsAndClickSaveButtonAndWaitForSuccess(flowComponentName);
    }

    @Test
    public void testFlowCreationNameInputFieldUpdate_clearsSaveResultLabel() throws Exception {
        String flowComponentName = "testComponent";
        createTestFlowComponent(flowComponentName);
        navigateToFlowCreationWidget(webDriver);
        insertTextInInputFieldsAndClickSaveButtonAndWaitForSuccess(flowComponentName);
        findNameElement(webDriver).sendKeys("a");
        assertThat(findSaveResultElement(webDriver).getText(), is(""));
    }

    @Test
    public void testFlowCreationDescriptionInputFieldUpdate_clearsSaveResultLabel() throws Exception {
        String flowComponentName = "testComponent";
        createTestFlowComponent(flowComponentName);
        navigateToFlowCreationWidget(webDriver);
        insertTextInInputFieldsAndClickSaveButtonAndWaitForSuccess(flowComponentName);
        findDescriptionElement(webDriver).sendKeys("b");
        assertThat(findSaveResultElement(webDriver).getText(), is(""));
    }

    @Test
    public void testFlowCreationFlowComponentSelectionUpdate_clearsSaveResultLabel() throws Exception {
        String flowComponentName1 = "testComponent";
        String flowComponentName2 = "anotherTestComponent";

        createTestFlowComponent(flowComponentName1);
        createTestFlowComponent(flowComponentName2);

        navigateToFlowCreationWidget(webDriver);
        insertTextInInputFieldsAndClickSaveButtonAndWaitForSuccess(flowComponentName1);
        SeleniumUtil.selectItemInDualList(findComponentSelectionElement(webDriver), flowComponentName2);
        assertThat(findSaveResultElement(webDriver).getText(), is(""));
    }

    @Test
    public void testSaveButton_EmptyNameInputField_DisplayErrorPopup() throws Exception {
        String flowComponentName = "testComponent";
        createTestFlowComponent(flowComponentName);
        navigateToFlowCreationWidget(webDriver);
        findNameElement(webDriver).sendKeys("a");
        SeleniumUtil.selectItemInDualList(findComponentSelectionElement(webDriver), flowComponentName);
        findSaveButtonElement(webDriver).click();
        String s = SeleniumUtil.getAlertStringAndAccept(webDriver);
        assertThat(s, is(texts.translate("error_InputFieldValidationError")));
    }

    @Test
    public void testSaveButton_EmptyDescriptionInputField_DisplayErrorPopup() throws Exception {
        String flowComponentName = "testComponent";
        createTestFlowComponent(flowComponentName);
        navigateToFlowCreationWidget(webDriver);
        findDescriptionElement(webDriver).sendKeys("b");
        SeleniumUtil.selectItemInDualList(findComponentSelectionElement(webDriver), flowComponentName);
        findSaveButtonElement(webDriver).click();
        String s = SeleniumUtil.getAlertStringAndAccept(webDriver);
        assertThat(s, is(texts.translate("error_InputFieldValidationError")));
    }

    @Test
    public void testSaveButton_NoSelectedFlowComponent_DisplayErrorPopup() throws Exception {
        String flowComponentName = "testComponent";
        createTestFlowComponent(flowComponentName);
        navigateToFlowCreationWidget(webDriver);
        findNameElement(webDriver).sendKeys("a");
        findDescriptionElement(webDriver).sendKeys("b");
        findSaveButtonElement(webDriver).click();
        String s = SeleniumUtil.getAlertStringAndAccept(webDriver);
        assertThat(s, is(texts.translate("error_InputFieldValidationError")));
    }

    @Test
    public void testFlowCreationLeaveAndGetBack_clearsAllFields() throws Exception {
        populateAllInputFields();
        assertAllInputFields("FlowName", "FlowDescription", Arrays.asList("FlowComponentName"));
        navigateAwayFromFlowCreationWidget(webDriver);
        navigateToFlowCreationWidget(webDriver);
        assertAllInputFields("", "", new ArrayList());
    }


    /**
     * The following is private helper methods
     */
    private void insertTextInInputFieldsAndClickSaveButtonAndWaitForSuccess(String flowComponentName) {
        findNameElement(webDriver).sendKeys("a");
        findDescriptionElement(webDriver).sendKeys("b");
        SeleniumUtil.selectItemInDualList(findComponentSelectionElement(webDriver), flowComponentName);
        findSaveButtonElement(webDriver).click();
        SeleniumUtil.waitAndAssert(webDriver, SAVE_TIMEOUT, FlowCreateViewImpl.GUIID_FLOW_CREATION_FLOW_SAVE_PANEL, SaveButton.SAVE_BUTTON_RESULT_LABEL_CLASS, texts.translate("status_FlowSuccessfullySaved"));
    }

    private static void navigateToFlowCreationWidget(WebDriver webDriver) {
        NavigationPanelSeleniumIT.navigateTo(webDriver, ClientFactoryImpl.GUIID_MENU_ITEM_FLOW_CREATE);
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

    private void navigateAwayFromFlowCreationWidget(WebDriver webDriver) {
        NavigationPanelSeleniumIT.navigateTo(webDriver, ClientFactoryImpl.GUIID_MENU_ITEM_FLOWS_SHOW);
    }

    private void populateAllInputFields() throws Exception {
        createTestFlowComponent("FlowComponentName");
        navigateToFlowCreationWidget(webDriver);
        findNameElement(webDriver).sendKeys("FlowName");
        findDescriptionElement(webDriver).sendKeys("FlowDescription");
        SeleniumUtil.selectItemInDualList(findComponentSelectionElement(webDriver), "FlowComponentName");
    }

    private void assertAllInputFields(String name, String description, List<String> flowComponents) {
        assertThat(findNameElement(webDriver).getAttribute("value"), is(name));
        assertThat(findDescriptionElement(webDriver).getAttribute("value"), is(description));
        assertThat(SeleniumUtil.getSelectedItemsInDualList(findComponentSelectionElement(webDriver)), is(flowComponents));
    }

    private static FlowComponent createTestFlowComponent(String flowComponentName) throws Exception{
        FlowComponentContent flowComponentContent = new FlowComponentContentBuilder()
                .setName(flowComponentName)
                .build();

        return flowStoreServiceConnector.createFlowComponent(flowComponentContent);
    }
}
