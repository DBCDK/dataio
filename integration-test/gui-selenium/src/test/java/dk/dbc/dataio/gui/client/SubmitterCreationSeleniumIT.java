package dk.dbc.dataio.gui.client;

import static dk.dbc.dataio.gui.client.NavigationPanelSeleniumIT.navigateTo;
import dk.dbc.dataio.gui.client.components.DataEntry;
import dk.dbc.dataio.gui.client.components.SaveButton;
import dk.dbc.dataio.gui.client.pages.submittercreate.SubmitterCreateViewImpl;
import dk.dbc.dataio.gui.util.ClientFactoryImpl;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class SubmitterCreationSeleniumIT extends AbstractGuiSeleniumTest {
    private static ConstantsProperties texts = new ConstantsProperties("pages/submittercreate/SubmitterCreateConstants_dk.properties");

    private static final int SAVE_SUBMITTER_TIMOUT = 4;
    private static final String NAME = "name";
    private static final String NUMBER = "42";
    private static final String DESCRIPTTION = "desc";

    @Test
    public void testInitialVisibilityAndAccessabilityOfElements() {
        testSubmitterCreationNameInputFieldIsVisibleAndDataCanBeInsertedAndRead();
        testSubmitterCreationNumberInputFieldIsVisibleAndDataCanBeInsertedAndRead();
        testSubmitterCreationDescriptionInputFieldIsVisibleAndDataCanBeInsertedAndRead();
        testSubmitterCreationSaveButtonIsVisible();
        testSubmitterCreationSaveResultLabelIsNotVisibleAndEmptyAsDefault();
    }

    public void testSubmitterCreationNameInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToSubmitterCreationWidget(webDriver);
        SeleniumUtil.assertFieldIsVisbleAndDataCanBeInsertedAndRead(findNameElement(webDriver));
    }

    public void testSubmitterCreationNumberInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToSubmitterCreationWidget(webDriver);
        SeleniumUtil.assertFieldIsVisbleAndDataCanBeInsertedAndRead(findNumberElement(webDriver));
    }

    public void testSubmitterCreationDescriptionInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToSubmitterCreationWidget(webDriver);
        SeleniumUtil.assertFieldIsVisbleAndDataCanBeInsertedAndReadWithMaxSize(findDescriptionElement(webDriver), 160);
    }

    public void testSubmitterCreationSaveButtonIsVisible() {
        navigateToSubmitterCreationWidget(webDriver);
        assertTrue(findSaveButton(webDriver).isDisplayed());
    }

    public void testSubmitterCreationSaveResultLabelIsNotVisibleAndEmptyAsDefault() {
        navigateToSubmitterCreationWidget(webDriver);
        WebElement element = findSaveResultLabel(webDriver);
        assertFalse(element.isDisplayed());
        assertThat(element.getText(), is(""));
    }

    @Test
    public void testSubmitterCreationSuccessfulSave_saveResultLabelContainsSuccessMessage() throws Exception {
        navigateToSubmitterCreationWidget(webDriver);
        insertTextInInputFieldsAndClickSaveButtonAndWaitForSuccessfullSave();
    }

    @Test
    public void testSubmitterCreationNameInputFieldUpdate_clearsSaveResultLabel() throws Exception {
        navigateToSubmitterCreationWidget(webDriver);
        insertTextInInputFieldsAndClickSaveButtonAndWaitForSuccessfullSave();
        findNameElement(webDriver).sendKeys(NAME);
        assertThat(findSaveResultLabel(webDriver).getText(), is(""));
    }

    @Test
    public void testSubmitterCreationNumberInputFieldUpdate_clearsSaveResultLabel() throws Exception {
        navigateToSubmitterCreationWidget(webDriver);
        insertTextInInputFieldsAndClickSaveButtonAndWaitForSuccessfullSave();
        findNumberElement(webDriver).sendKeys(DESCRIPTTION);
        assertThat(findSaveResultLabel(webDriver).getText(), is(""));
    }

    @Test
    public void testSubmitterCreationDescriptionInputFieldUpdate_clearsSaveResultLabel() throws Exception {
        navigateToSubmitterCreationWidget(webDriver);
        insertTextInInputFieldsAndClickSaveButtonAndWaitForSuccessfullSave();
        findDescriptionElement(webDriver).sendKeys(DESCRIPTTION);
        assertThat(findSaveResultLabel(webDriver).getText(), is(""));
    }

    @Test
    public void testSaveButton_EmptyNameInputField_DisplayErrorPopup() {
        navigateToSubmitterCreationWidget(webDriver);
        findNumberElement(webDriver).sendKeys(NUMBER);
        findDescriptionElement(webDriver).sendKeys(DESCRIPTTION);
        findSaveButton(webDriver).click();
        String s = SeleniumUtil.getAlertStringAndAccept(webDriver);
        assertThat(s, is(texts.translate("error_InputFieldValidationError")));
    }

    @Test
    public void testSaveButton_EmptyDescriptionInputField_DisplayErrorPopup() {
        navigateToSubmitterCreationWidget(webDriver);
        findNameElement(webDriver).sendKeys(NAME);
        findNumberElement(webDriver).sendKeys(NUMBER);
        findSaveButton(webDriver).click();
        String s = SeleniumUtil.getAlertStringAndAccept(webDriver);
        assertThat(s, is(texts.translate("error_InputFieldValidationError")));
    }

    @Test
    public void testSaveButton_EmptyNumberInputField_DisplayErrorPopup() {
        navigateToSubmitterCreationWidget(webDriver);
        findNameElement(webDriver).sendKeys(NAME);
        findDescriptionElement(webDriver).sendKeys(DESCRIPTTION);
        findSaveButton(webDriver).click();
        String s = SeleniumUtil.getAlertStringAndAccept(webDriver);
        assertThat(s, is(texts.translate("error_InputFieldValidationError")));
    }

    @Test
    public void testSaveButton_numberInputFieldContainsNonNumericValue_DisplayErrorPopup() {
        navigateToSubmitterCreationWidget(webDriver);
        findNameElement(webDriver).sendKeys(NAME);
        findNumberElement(webDriver).sendKeys("fourty-two");
        findDescriptionElement(webDriver).sendKeys(DESCRIPTTION);
        findSaveButton(webDriver).click();
        String s = SeleniumUtil.getAlertStringAndAccept(webDriver);
        assertThat(s, is(texts.translate("error_NumberInputFieldValidationError")));
    }

    /**
     * The following is private static helper methods.
     */
    private static void navigateToSubmitterCreationWidget(WebDriver webDriver) {
        navigateTo(webDriver, ClientFactoryImpl.GUIID_MENU_ITEM_SUBMITTER_CREATE);
    }

    private static WebElement findSubmitterCreationWidget(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, SubmitterCreateViewImpl.GUIID_SUBMITTER_CREATION_WIDGET);
    }

    private static WebElement findNameElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, SubmitterCreateViewImpl.GUIID_SUBMITTER_CREATION_NAME_PANEL, DataEntry.DATA_ENTRY_INPUT_BOX_CLASS);
    }

    private static WebElement findNumberElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, SubmitterCreateViewImpl.GUIID_SUBMITTER_CREATION_NUMBER_PANEL, DataEntry.DATA_ENTRY_INPUT_BOX_CLASS);
    }

    private static WebElement findDescriptionElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, SubmitterCreateViewImpl.GUIID_SUBMITTER_CREATION_DESCRIPTION_PANEL, DataEntry.DATA_ENTRY_INPUT_BOX_CLASS);
    }

    private static WebElement findSaveButton(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, SubmitterCreateViewImpl.GUIID_SUBMITTER_CREATION_SAVE_BUTTON_PANEL, SaveButton.SAVE_BUTTON_BUTTON_CLASS);
    }

    private static WebElement findSaveResultLabel(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, SubmitterCreateViewImpl.GUIID_SUBMITTER_CREATION_SAVE_BUTTON_PANEL, SaveButton.SAVE_BUTTON_RESULT_LABEL_CLASS);
    }

    private void insertTextInInputFieldsAndClickSaveButtonAndWaitForSuccessfullSave() {
        findNameElement(webDriver).sendKeys("n");
        findNumberElement(webDriver).sendKeys("1");
        findDescriptionElement(webDriver).sendKeys("d");
        findSaveButton(webDriver).click();
        SeleniumUtil.waitAndAssert(webDriver, SAVE_SUBMITTER_TIMOUT, SubmitterCreateViewImpl.GUIID_SUBMITTER_CREATION_SAVE_BUTTON_PANEL, SaveButton.SAVE_BUTTON_RESULT_LABEL_CLASS, texts.translate("status_SubmitterSuccessfullySaved"));
    }

    /**
     * The following is public static helper methods.
     */
    public static void createTestSubmitter(WebDriver webDriver, String name, String number, String description) {
        navigateToSubmitterCreationWidget(webDriver);
        findNameElement(webDriver).clear();
        findNameElement(webDriver).sendKeys(name);
        findNumberElement(webDriver).clear();
        findNumberElement(webDriver).sendKeys(number);
        findDescriptionElement(webDriver).clear();
        findDescriptionElement(webDriver).sendKeys(description);
        findSaveButton(webDriver).click();
        SeleniumUtil.waitAndAssert(webDriver, SAVE_SUBMITTER_TIMOUT, SubmitterCreateViewImpl.GUIID_SUBMITTER_CREATION_SAVE_BUTTON_PANEL, SaveButton.SAVE_BUTTON_RESULT_LABEL_CLASS, texts.translate("status_SubmitterSuccessfullySaved"));
    }
}
