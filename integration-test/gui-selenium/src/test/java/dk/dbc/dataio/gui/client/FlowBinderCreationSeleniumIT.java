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
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;


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
        WebElement element = driver.findElement(By.id(MainPanel.GUIID_NAVIGATION_MENU_ITEM_FLOWBINDER_CREATION));
        assertEquals(true, element.isDisplayed());
        element.click();

        WebElement widget = driver.findElement(By.id(FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_WIDGET));
        assertEquals(true, widget.isDisplayed());
    }

    public void testFlowbinderCreationNameInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToFlowbinderCreationContext();
        SeleniumUtil.assertFieldIsVisbleAndDataCanBeInsertedAndReadWithMaxSize(findNameTextElement(), 160);
    }

    public void testFlowbinderCreationDescriptionInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToFlowbinderCreationContext();
        SeleniumUtil.assertFieldIsVisbleAndDataCanBeInsertedAndReadWithMaxSize(findDescriptionTextElement(), 160);
    }

    public void testFlowbinderCreationFrameInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToFlowbinderCreationContext();
        SeleniumUtil.assertFieldIsVisbleAndDataCanBeInsertedAndRead(findFrameTextElement());
    }

    public void testFlowbinderCreationContentFormatInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToFlowbinderCreationContext();
        SeleniumUtil.assertFieldIsVisbleAndDataCanBeInsertedAndRead(findContentFormatTextElement());
    }

    public void testFlowbinderCreationCharacterSetInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToFlowbinderCreationContext();
        SeleniumUtil.assertFieldIsVisbleAndDataCanBeInsertedAndRead(findCharacterSetTextElement());
    }

    public void testFlowbinderCreationSinkInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToFlowbinderCreationContext();
        SeleniumUtil.assertFieldIsVisbleAndDataCanBeInsertedAndRead(findSinkTextElement());
    }

    // This can, for some reason, not be included with the other visibility tests
    @Test
    public void testFlowbinderCreationSubmitterDualListIsVisibleAndAnElementCanBeChosen() {
        String submitterName = "submitter1";
        SubmitterCreationSeleniumIT.createTestSubmitter(driver, submitterName, "123456", "Description");
        navigateToFlowbinderCreationContext();
        SeleniumUtil.assertDualListIsVisibleAndElementCanBeChosen(driver, findSubmitterPanelElement(), submitterName);
    }

    // This can, for some reason, not be included with the other visibility tests
    @Test
    public void testFlowbinderCreationFlowListIsVisibleAndAnElementCanBeSelected() {
        String flowComponentName = "flowComponent";
        String flowName = "flowName";
        FlowComponentCreationSeleniumIT.createTestFlowComponent(driver, flowComponentName);
        FlowCreationSeleniumIT.createTestFlow(driver, flowName, "description", flowComponentName);

        navigateToFlowbinderCreationContext();
        SeleniumUtil.assertListBoxIsVisibleAndAnElementCanBeSelected(driver, findFlowListElement(), flowName);
    }

    public void testFlowbinderCreationSaveButtonIsVisible() {
        navigateToFlowbinderCreationContext();
        assertThat(findSaveButtonElement().isDisplayed(), is(true));
    }

    public void testFlowbinderCreationSaveResultLableIsNotVisibleAndEmptyByDefault() {
        navigateToFlowbinderCreationContext();
        WebElement element = findSaveResultLabelElement();
        assertEquals(false, element.isDisplayed());
        assertEquals("", element.getText());
    }

    @Test
    public void testFlowbinderCreationSuccessfulSave_saveResultLabelContainsSuccessMessage() {
        populateAllInputFieldsAndClickSave();
        assertThat(findSaveResultLabelElement().getText(), is(FlowbinderCreateViewImpl.FLOWBINDER_CREATION_SAVE_SUCCESS));
    }

    @Test
    public void testSaveButton_emptyNameInputField_displayErrorPopup() {
        navigateToFlowbinderCreationContext();
        populateAllInputFields();
        findNameTextElement().clear();
        findSaveButtonElement().click();
        assertThat(SeleniumUtil.getAlertStringAndAccept(driver), is(FlowbinderCreateViewImpl.FLOWBINDER_CREATION_INPUT_FIELD_VALIDATION_ERROR));
    }

    @Test
    public void testSaveButton_emptyDescriptionInputField_displayErrorPopup() {
        navigateToFlowbinderCreationContext();
        populateAllInputFields();
        findDescriptionTextElement().clear();
        findSaveButtonElement().click();
        assertThat(SeleniumUtil.getAlertStringAndAccept(driver), is(FlowbinderCreateViewImpl.FLOWBINDER_CREATION_INPUT_FIELD_VALIDATION_ERROR));
    }

    @Test
    public void testSaveButton_emptyFrameInputField_displayErrorPopup() {
        navigateToFlowbinderCreationContext();
        populateAllInputFields();
        findFrameTextElement().clear();
        findSaveButtonElement().click();
        assertThat(SeleniumUtil.getAlertStringAndAccept(driver), is(FlowbinderCreateViewImpl.FLOWBINDER_CREATION_INPUT_FIELD_VALIDATION_ERROR));
    }

    @Test
    public void testSaveButton_emptyContentFormatInputField_displayErrorPopup() {
        navigateToFlowbinderCreationContext();
        populateAllInputFields();
        findContentFormatTextElement().clear();
        findSaveButtonElement().click();
        assertThat(SeleniumUtil.getAlertStringAndAccept(driver), is(FlowbinderCreateViewImpl.FLOWBINDER_CREATION_INPUT_FIELD_VALIDATION_ERROR));
    }

    @Test
    public void testSaveButton_emptyCharacterSetInputField_displayErrorPopup() {
        navigateToFlowbinderCreationContext();
        populateAllInputFields();
        findCharacterSetTextElement().clear();
        findSaveButtonElement().click();
        assertThat(SeleniumUtil.getAlertStringAndAccept(driver), is(FlowbinderCreateViewImpl.FLOWBINDER_CREATION_INPUT_FIELD_VALIDATION_ERROR));
    }

    @Test
    public void testSaveButton_emptySinkInputField_displayErrorPopup() {
        navigateToFlowbinderCreationContext();
        populateAllInputFields();
        findSinkTextElement().clear();
        findSaveButtonElement().click();
        assertThat(SeleniumUtil.getAlertStringAndAccept(driver), is(FlowbinderCreateViewImpl.FLOWBINDER_CREATION_INPUT_FIELD_VALIDATION_ERROR));
    }

    @Test
    public void testSaveButton_emptyFlowInputField_displayErrorPopup() {
        navigateToFlowbinderCreationContext();
        populateSubmitterSelectionField();
        populateAllTextInputFields();
        findSaveButtonElement().click();
        assertThat(SeleniumUtil.getAlertStringAndAccept(driver), is(FlowbinderCreateViewImpl.FLOWBINDER_CREATION_INPUT_FIELD_VALIDATION_ERROR));
    }

    @Test
    public void testSaveButton_emptySubmitterInputField_displayErrorPopup() {
        navigateToFlowbinderCreationContext();
        populateFlowSelectionField();
        populateAllTextInputFields();
        findSaveButtonElement().click();
        assertThat(SeleniumUtil.getAlertStringAndAccept(driver), is(FlowbinderCreateViewImpl.FLOWBINDER_CREATION_INPUT_FIELD_VALIDATION_ERROR));
    }

    @Test
    public void testFlowBinderCreationNameInputFieldUpdate_clearsSaveResultLabel() {
        navigateToFlowbinderCreationContext();
        populateAllInputFieldsAndClickSave();
        findNameTextElement().sendKeys("other stuff");
        assertThat(findSaveResultLabelElement().getText(), is(""));
    }

    @Test
    public void testFlowBinderCreationDescriptionInputFieldUpdate_clearsSaveResultLabel() {
        navigateToFlowbinderCreationContext();
        populateAllInputFieldsAndClickSave();
        findDescriptionTextElement().sendKeys("other stuff");
        assertThat(findSaveResultLabelElement().getText(), is(""));
    }

    @Test
    public void testFlowBinderCreationFrameInputFieldUpdate_clearsSaveResultLabel() {
        navigateToFlowbinderCreationContext();
        populateAllInputFieldsAndClickSave();
        findFrameTextElement().sendKeys("other stuff");
        assertThat(findSaveResultLabelElement().getText(), is(""));
    }

    @Test
    public void testFlowBinderCreationContentFormatInputFieldUpdate_clearsSaveResultLabel() {
        navigateToFlowbinderCreationContext();
        populateAllInputFieldsAndClickSave();
        findContentFormatTextElement().sendKeys("other stuff");
        assertThat(findSaveResultLabelElement().getText(), is(""));
    }

    @Test
    public void testFlowBinderCreationCharacterSetInputFieldUpdate_clearsSaveResultLabel() {
        navigateToFlowbinderCreationContext();
        populateAllInputFieldsAndClickSave();
        findCharacterSetTextElement().sendKeys("other stuff");
        assertThat(findSaveResultLabelElement().getText(), is(""));
    }

    @Test
    public void testFlowBinderCreationFlowInputFieldUpdate_clearsSaveResultLabel() {
        final String flowComponentName = "anotherFlowComponent";
        final String flowName = "anotherFlow";
        FlowComponentCreationSeleniumIT.createTestFlowComponent(driver, flowComponentName);
        FlowCreationSeleniumIT.createTestFlow(driver, flowName, "description", flowComponentName);
        navigateToFlowbinderCreationContext();
        populateAllInputFieldsAndClickSave();
        SeleniumUtil.selectItemInListBox(findFlowListElement(), flowName);
        assertThat(findSaveResultLabelElement().getText(), is(""));
    }

    @Test
    public void testFlowBinderCreationSubmitterInputFieldUpdate_clearsSaveResultLabel() {
        final String submitterName = "anotherSubmitter";
        SubmitterCreationSeleniumIT.createTestSubmitter(driver, submitterName, "42", "Description");
        navigateToFlowbinderCreationContext();
        populateAllInputFieldsAndClickSave();
        SeleniumUtil.selectItemInDualList(findSubmitterPanelElement(), submitterName);
        assertThat(findSaveResultLabelElement().getText(), is(""));
    }

    private void populateAllInputFields() {
        populateSubmitterSelectionField();
        populateAllTextInputFields();
        populateFlowSelectionField();
    }

    private void populateAllTextInputFields() {
        navigateToFlowbinderCreationContext();
        findNameTextElement().sendKeys("Name");
        findDescriptionTextElement().sendKeys("Description");
        findFrameTextElement().sendKeys("Frame");
        findContentFormatTextElement().sendKeys("ContentFormat");
        findCharacterSetTextElement().sendKeys("CharacterSet");
        findSinkTextElement().sendKeys("Sink");
    }

    private void populateFlowSelectionField() {
        String flowComponentName = "flowComponent12";
        String flowName = "flowName12";
        FlowComponentCreationSeleniumIT.createTestFlowComponent(driver, flowComponentName);
        FlowCreationSeleniumIT.createTestFlow(driver, flowName, "description", flowComponentName);
        navigateToFlowbinderCreationContext();
        SeleniumUtil.selectItemInListBox(findFlowListElement(), flowName);
    }

    private void populateSubmitterSelectionField() {
        String submitterName = "submitter12";
        SubmitterCreationSeleniumIT.createTestSubmitter(driver, submitterName, "123456", "Description");
        navigateToFlowbinderCreationContext();
        SeleniumUtil.selectItemInDualList(findSubmitterPanelElement(), submitterName);
    }

    private void populateAllInputFieldsAndClickSave() {
        populateAllInputFields();

        findSaveButtonElement().click();

        // todo: Split into own method
        WebDriverWait wait = new WebDriverWait(driver, 4);
        wait.until(ExpectedConditions.textToBePresentInElement(By.id(FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_SAVE_RESULT_LABEL), FlowbinderCreateViewImpl.FLOWBINDER_CREATION_SAVE_SUCCESS));
    }

    /**
     * The following is private helper methods
     */
    private void navigateToFlowbinderCreationContext() {
        driver.findElement(By.id(MainPanel.GUIID_NAVIGATION_MENU_ITEM_FLOWBINDER_CREATION)).click();
    }

    private WebElement findNameTextElement() {
        return SeleniumUtil.findElementInCurrentView(driver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_NAME_TEXT_BOX);
    }

    private WebElement findDescriptionTextElement() {
        return SeleniumUtil.findElementInCurrentView(driver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_DESCRIPTION_TEXT_AREA);
    }

    private WebElement findFrameTextElement() {
        return SeleniumUtil.findElementInCurrentView(driver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_FRAME_TEXT_BOX);
    }

    private WebElement findContentFormatTextElement() {
        return SeleniumUtil.findElementInCurrentView(driver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_CONTENTFORMAT_TEXT_BOX);
    }

    private WebElement findCharacterSetTextElement() {
        return SeleniumUtil.findElementInCurrentView(driver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_CHARACTER_SET_TEXT_BOX);
    }

    private WebElement findSinkTextElement() {
        return SeleniumUtil.findElementInCurrentView(driver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_SINK_TEXT_BOX);
    }

    private WebElement findRecordSplitterTextElement() {
        return SeleniumUtil.findElementInCurrentView(driver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_RECORD_SPLITTER_TEXT_BOX);
    }

    private WebElement findSubmitterPanelElement() {
        return SeleniumUtil.findElementInCurrentView(driver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_SUBMITTERS_SELECTION_PANEL);
    }

    private WebElement findFlowListElement() {
        return SeleniumUtil.findElementInCurrentView(driver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_FLOW_LIST_BOX);
    }

    private WebElement findSaveButtonElement() {
        return SeleniumUtil.findElementInCurrentView(driver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_SAVE_BUTTON);
    }

    private WebElement findSaveResultLabelElement() {
        return SeleniumUtil.findElementInCurrentView(driver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_SAVE_RESULT_LABEL);
    }

}
