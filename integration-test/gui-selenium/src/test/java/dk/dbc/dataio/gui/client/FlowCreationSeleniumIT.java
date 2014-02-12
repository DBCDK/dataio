package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.gui.client.components.DataEntry;
import dk.dbc.dataio.gui.client.components.SaveButton;
import dk.dbc.dataio.gui.client.pages.flowcreate.FlowCreateViewImpl;
import dk.dbc.dataio.gui.util.ClientFactoryImpl;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class FlowCreationSeleniumIT extends AbstractGuiSeleniumTest {
    private static ConstantsProperties texts = new ConstantsProperties("pages/flowcreate/FlowCreateConstants_dk.properties");

    private static final long SAVE_TIMEOUT = 4;

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
    public void testFlowCreationFlowComponentSelectionFieldIsVisibleAndAnElementCanBeChosen() {
        String flowComponentName = "testComponent";
        FlowComponentCreationSeleniumIT.createTestFlowComponent(webDriver, flowComponentName);
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
    public void testFlowCreationSuccessfulSave_saveResultLabelContainsSuccessMessage() {
        String flowComponentName = "testComponent";
        FlowComponentCreationSeleniumIT.createTestFlowComponent(webDriver, flowComponentName);
        navigateToFlowCreationWidget(webDriver);
        insertTextInInputFieldsAndClickSaveButtonAndWaitForSuccess(flowComponentName);
    }

    @Test
    public void testFlowCreationNameInputFieldUpdate_clearsSaveResultLabel() {
        String flowComponentName = "testComponent";
        FlowComponentCreationSeleniumIT.createTestFlowComponent(webDriver, flowComponentName);
        navigateToFlowCreationWidget(webDriver);
        insertTextInInputFieldsAndClickSaveButtonAndWaitForSuccess(flowComponentName);
        findNameElement(webDriver).sendKeys("a");
        assertThat(findSaveResultElement(webDriver).getText(), is(""));
    }

    @Test
    public void testFlowCreationDescriptionInputFieldUpdate_clearsSaveResultLabel() {
        String flowComponentName = "testComponent";
        FlowComponentCreationSeleniumIT.createTestFlowComponent(webDriver, flowComponentName);
        navigateToFlowCreationWidget(webDriver);
        insertTextInInputFieldsAndClickSaveButtonAndWaitForSuccess(flowComponentName);
        findDescriptionElement(webDriver).sendKeys("b");
        assertThat(findSaveResultElement(webDriver).getText(), is(""));
    }

    @Test
    public void testFlowCreationFlowComponentSelectionUpdate_clearsSaveResultLabel() {
        String flowComponentName1 = "testComponent";
        FlowComponentCreationSeleniumIT.createTestFlowComponent(webDriver, flowComponentName1);
        String flowComponentName2 = "anotherTestComponent";
        FlowComponentCreationSeleniumIT.createTestFlowComponent(webDriver, flowComponentName2);

        navigateToFlowCreationWidget(webDriver);
        insertTextInInputFieldsAndClickSaveButtonAndWaitForSuccess(flowComponentName1);
        SeleniumUtil.selectItemInDualList(findComponentSelectionElement(webDriver), flowComponentName2);
        assertThat(findSaveResultElement(webDriver).getText(), is(""));
    }

    @Test
    public void testSaveButton_EmptyNameInputField_DisplayErrorPopup() {
        String flowComponentName = "testComponent";
        FlowComponentCreationSeleniumIT.createTestFlowComponent(webDriver, flowComponentName);
        navigateToFlowCreationWidget(webDriver);
        findNameElement(webDriver).sendKeys("a");
        SeleniumUtil.selectItemInDualList(findComponentSelectionElement(webDriver), flowComponentName);
        findSaveButtonElement(webDriver).click();
        String s = SeleniumUtil.getAlertStringAndAccept(webDriver);
        assertThat(s, is(texts.translate("error_InputFieldValidationError")));
    }

    @Test
    public void testSaveButton_EmptyDescriptionInputField_DisplayErrorPopup() {
        String flowComponentName = "testComponent";
        FlowComponentCreationSeleniumIT.createTestFlowComponent(webDriver, flowComponentName);
        navigateToFlowCreationWidget(webDriver);
        findDescriptionElement(webDriver).sendKeys("b");
        SeleniumUtil.selectItemInDualList(findComponentSelectionElement(webDriver), flowComponentName);
        findSaveButtonElement(webDriver).click();
        String s = SeleniumUtil.getAlertStringAndAccept(webDriver);
        assertThat(s, is(texts.translate("error_InputFieldValidationError")));
    }

    @Test
    public void testSaveButton_NoSelectedFlowComponent_DisplayErrorPopup() {
        String flowComponentName = "testComponent";
        FlowComponentCreationSeleniumIT.createTestFlowComponent(webDriver, flowComponentName);
        navigateToFlowCreationWidget(webDriver);
        findNameElement(webDriver).sendKeys("a");
        findDescriptionElement(webDriver).sendKeys("b");
        findSaveButtonElement(webDriver).click();
        String s = SeleniumUtil.getAlertStringAndAccept(webDriver);
        assertThat(s, is(texts.translate("error_InputFieldValidationError")));
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
        NavigationPanelSeleniumIT.navigateTo(webDriver, ClientFactoryImpl.GUIID_SUB_MENU_ITEM_FLOW_CREATION);
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
