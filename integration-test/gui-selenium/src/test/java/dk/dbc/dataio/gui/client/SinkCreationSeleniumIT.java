package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.gui.client.components.DataEntry;
import dk.dbc.dataio.gui.client.components.SaveButton;
import dk.dbc.dataio.gui.client.pages.sinkcreate.SinkCreateViewImpl;
import dk.dbc.dataio.gui.util.ClientFactoryImpl;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class SinkCreationSeleniumIT extends AbstractGuiSeleniumTest {
    private static ConstantsProperties texts = new ConstantsProperties("pages/sinkcreate/SinkCreateConstants_dk.properties");

    public static final String SINK_CREATION_KNOWN_RESOURCE_NAME = "jdbc/flowStoreDb";
    private static final String SINK_NAME = "name";
    private static final String RESOURCE_NAME = "resource";
    private static final int SAVE_SINK_TIMOUT = 4;

    @Test
    public void testInitialVisibilityAndAccessabilityOfElements() {
        testSinkCreationSinkNameInputFieldIsVisibleAndDataCanBeInsertedAndRead();
        testSinkCreationResourceNameInputFieldIsVisibleAndDataCanBeInsertedAndRead();
        testSinkCreationSaveButtonIsVisible();
        testSinkCreationSaveResultLabelIsNotVisibleAndEmptyAsDefault();
    }

    public void testSinkCreationSinkNameInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToSinkCreationWidget(webDriver);
        SeleniumUtil.assertFieldIsVisbleAndDataCanBeInsertedAndRead(findSinkNameElement(webDriver));
    }

    public void testSinkCreationResourceNameInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToSinkCreationWidget(webDriver);
        SeleniumUtil.assertFieldIsVisbleAndDataCanBeInsertedAndRead(findResourceNameElement(webDriver));
    }

    public void testSinkCreationSaveButtonIsVisible() {
        navigateToSinkCreationWidget(webDriver);
        assertTrue(findSaveButton(webDriver).isDisplayed());
    }

    public void testSinkCreationSaveResultLabelIsNotVisibleAndEmptyAsDefault() {
        navigateToSinkCreationWidget(webDriver);
        WebElement element = findSaveResultLabel(webDriver);
        assertFalse(element.isDisplayed());
        assertThat(element.getText(), is(""));
    }

    @Test
    public void testSinkCreationSuccessfulSave_saveResultLabelContainsSuccessMessage() throws Exception {
        navigateToSinkCreationWidget(webDriver);
        insertKnownTextInInputFieldsAndClickSaveButtonAndWaitForSuccessfullSave();
    }

    @Test
    public void testSinkCreationSinkNameInputFieldUpdate_clearsSaveResultLabel() throws Exception {
        navigateToSinkCreationWidget(webDriver);
        insertKnownTextInInputFieldsAndClickSaveButtonAndWaitForSuccessfullSave();
        findSinkNameElement(webDriver).sendKeys(SINK_NAME);
        assertThat(findSaveResultLabel(webDriver).getText(), is(""));
    }

    @Test
    public void testSinkCreationResourceNameInputFieldUpdate_clearsSaveResultLabel() throws Exception {
        navigateToSinkCreationWidget(webDriver);
        insertKnownTextInInputFieldsAndClickSaveButtonAndWaitForSuccessfullSave();
        findResourceNameElement(webDriver).sendKeys(RESOURCE_NAME);
        assertThat(findSaveResultLabel(webDriver).getText(), is(""));
    }

    @Test
    public void testSaveButton_EmptySinkNameInputField_DisplayErrorPopup() {
        navigateToSinkCreationWidget(webDriver);
        findResourceNameElement(webDriver).sendKeys(RESOURCE_NAME);
        findSaveButton(webDriver).click();
        String s = SeleniumUtil.getAlertStringAndAccept(webDriver);
        assertThat(s, is(texts.translate("error_InputFieldValidationError")));
    }

    @Test
    public void testSaveButton_EmptyResourceNameInputField_DisplayErrorPopup() {
        navigateToSinkCreationWidget(webDriver);
        findSinkNameElement(webDriver).sendKeys(SINK_NAME);
        findSaveButton(webDriver).click();
        String s = SeleniumUtil.getAlertStringAndAccept(webDriver);
        assertThat(s, is(texts.translate("error_InputFieldValidationError")));
    }

    @Test
    public void testSinkCreationUnknownResourceName_DisplayErrorPopup() throws Exception {
        navigateToSinkCreationWidget(webDriver);
        findSinkNameElement(webDriver).sendKeys("unknownresource-name");
        findResourceNameElement(webDriver).sendKeys("unknownresource");
        findSaveButton(webDriver).click();
        String s = SeleniumUtil.getAlertStringAndAccept(webDriver);
        assertThat(s, is("Error: " + texts.translate("error_ResourceNameNotValid")));  // Todo: Generalisering af fejlhåndtering
    }

    @Test
    public void testSinkCreationSinkNameAlreadyExist_DisplayErrorPopup() throws Exception {
        navigateToSinkCreationWidget(webDriver);
        insertKnownTextInInputFieldsAndClickSaveButtonAndWaitForSuccessfullSave();
        findSaveButton(webDriver).click();  // Click enters the same data once again => Same Sink name
        String s = SeleniumUtil.getAlertStringAndAccept(webDriver);
        assertThat(s, is("Error: " + texts.translate("error_ProxyKeyViolationError")));  // Todo: Generalisering af fejlhåndtering
    }

    /**
     * The following is private static helper methods.
     */
    private static void navigateToSinkCreationWidget(WebDriver webDriver) {
        NavigationPanelSeleniumIT.navigateTo(webDriver, ClientFactoryImpl.GUIID_MENU_ITEM_SINK_CREATE);
    }

    private static WebElement findSinkCreationNavigationElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, ClientFactoryImpl.GUIID_MENU_ITEM_SINK_CREATE);
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

    private void insertKnownTextInInputFieldsAndClickSaveButtonAndWaitForSuccessfullSave() {
        findSinkNameElement(webDriver).sendKeys("succesfull-name");
        findResourceNameElement(webDriver).sendKeys(SINK_CREATION_KNOWN_RESOURCE_NAME);
        findSaveButton(webDriver).click();
        SeleniumUtil.waitAndAssert(webDriver, SAVE_SINK_TIMOUT, SinkCreateViewImpl.GUIID_SINK_CREATION_SAVE_BUTTON_PANEL, SaveButton.SAVE_BUTTON_RESULT_LABEL_CLASS, texts.translate("status_SinkSuccessfullySaved"));
    }

    /**
     * The following is public static helper methods.
     */
    public static void createTestSink(WebDriver webDriver, String sinkName, String resourceName) {
        navigateToSinkCreationWidget(webDriver);
        findSinkNameElement(webDriver).clear();
        findSinkNameElement(webDriver).sendKeys(sinkName);
        findResourceNameElement(webDriver).clear();
        findResourceNameElement(webDriver).sendKeys(resourceName);
        findSaveButton(webDriver).click();

        SeleniumUtil.waitAndAssert(webDriver, SAVE_SINK_TIMOUT, SinkCreateViewImpl.GUIID_SINK_CREATION_SAVE_BUTTON_PANEL, SaveButton.SAVE_BUTTON_RESULT_LABEL_CLASS, texts.translate("status_SinkSuccessfullySaved"));
    }
}
