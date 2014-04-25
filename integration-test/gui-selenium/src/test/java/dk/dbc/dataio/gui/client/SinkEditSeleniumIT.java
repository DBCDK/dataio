package dk.dbc.dataio.gui.client;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.test.json.SinkContentJsonBuilder;
import dk.dbc.dataio.gui.client.components.DataEntry;
import dk.dbc.dataio.gui.client.components.SaveButton;
import dk.dbc.dataio.gui.client.pages.sinkcreateedit.SinkCreateEditViewImpl;
import dk.dbc.dataio.gui.client.pages.sinksshow.SinksShowViewImpl;
import dk.dbc.dataio.integrationtest.ITUtil;
import org.junit.Test;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WebDriver;

import javax.ws.rs.client.Client;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static dk.dbc.dataio.gui.client.SinksShowSeleniumIT.navigateToSinksShowWidget;
import static dk.dbc.dataio.gui.client.SinksShowSeleniumIT.locateAndClickEditButtonForElement;
import static dk.dbc.dataio.integrationtest.ITUtil.clearAllDbTables;
import static dk.dbc.dataio.integrationtest.ITUtil.newDbConnection;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * Created by sma on 16/04/14.
 */

public class SinkEditSeleniumIT extends AbstractGuiSeleniumTest {
    private static ConstantsProperties texts = new ConstantsProperties("pages/sinkcreateedit/SinkCreateEditConstants_dk.properties");

    final String SINK_NAME_1 = "NamoUno";
    final String SINK_NAME_2 = "NamoDuo";
    final String SINK_NAME_3 = "NamoTrio";
    final String RESOURCE_NAME_FLOWSTORE = "jdbc/flowStoreDb";
    final String RESOURCE_NAME_INFLIGHT = "jdbc/dataio/sinks/esInFlight";

    private static Client restClient;
    private static Connection dbConnection;
    private static String baseUrl;

    private static final int SAVE_SINK_TIMOUT = 4;

    @Test
    public void testInitialVisibilityAndAccessibilityOfElements() {

        //Assert that the sink name and sink resource name is shown in the input fields.
        assertSinkEditSinkNameAndResourceFieldsAreVisibleAndDataIsInserted();

        //Assert that the save button is visible.
        assertSinkEditSaveButtonIsVisible();

        //Assert that the save result label is not visible and empty.
        assertSinkEditSaveResultLabelIsNotVisibleAndEmptyAsDefault();
    }

    @Test
    public void testSinkEdit_WhenMultipleSinksTheCorrectSinkIsLocatedForEdit(){

        //Create 3 new sinks.
        SinkCreationSeleniumIT.createTestSink(webDriver, SINK_NAME_1, RESOURCE_NAME_FLOWSTORE);
        SinkCreationSeleniumIT.createTestSink(webDriver, SINK_NAME_2, RESOURCE_NAME_INFLIGHT);
        SinkCreationSeleniumIT.createTestSink(webDriver, SINK_NAME_3, RESOURCE_NAME_FLOWSTORE);

        //Navigate to the sink show window.
        navigateToSinksShowWidget(webDriver);

        //Assert that 3 sinks can be located in the table
        assertTrue(getTableSize() == 3);

        //Navigate to the first row, locate the edit button and click.
        locateAndClickEditButtonForElement(0);

        //Assert that the sinks are sorted in alphabetically order after name.
        //Therefore: Assert that the first sink found is the second created.
        assertAllInputFields(SINK_NAME_2, RESOURCE_NAME_INFLIGHT);

        //Navigate to the sink show window.
        navigateToSinksShowWidget(webDriver);

        //Navigate to the second row, locate the edit button and click.
        locateAndClickEditButtonForElement(1);

        //Assert that the sink found matches the values from the third sink created.
        assertAllInputFields(SINK_NAME_3, RESOURCE_NAME_FLOWSTORE);

        //Navigate to the sink show window.
        navigateToSinksShowWidget(webDriver);

        //Navigate to the third row, locate the edit button and click.
        locateAndClickEditButtonForElement(2);

        //Assert that the sink found matches the values from the first sink created.
        assertAllInputFields(SINK_NAME_1, RESOURCE_NAME_FLOWSTORE);
    }

    @Test
    public void testSaveButton_EmptySinkNameInputField_DisplayErrorPopup() {

        //Create new sink
        SinkCreationSeleniumIT.createTestSink(webDriver, SINK_NAME_1, RESOURCE_NAME_FLOWSTORE);

        //Navigate to the sink show window.
        navigateToSinksShowWidget(webDriver);

        //Assert that 1 sink can be located in the table
        assertTrue(getTableSize() == 1);

        //Navigate to the first row, locate the edit button and click.
        locateAndClickEditButtonForElement(0);

        //Retrieve the sink resource from the current view
        findResourceNameElement(webDriver).getAttribute("value");

        //Clear the sink name
        findSinkNameElement(webDriver).clear();

        //Attempt saving
        findSaveButton(webDriver).click();

        //Assert error
        String s = SeleniumUtil.getAlertStringAndAccept(webDriver);
        assertThat(s, is(texts.translate("error_InputFieldValidationError")));
    }

    @Test
    public void testSaveButton_EmptyResourceNameInputField_DisplayErrorPopup() {

        //Create new sink
        SinkCreationSeleniumIT.createTestSink(webDriver, SINK_NAME_1, RESOURCE_NAME_FLOWSTORE);

        //Navigate to the sink show window.
        navigateToSinksShowWidget(webDriver);

        //Assert that 1 sink can be located in the table
        assertTrue(getTableSize() == 1);

        //Navigate to the first row, locate the edit button and click.
        locateAndClickEditButtonForElement(0);

        //Retrieve the sink name from the current view
        findSinkNameElement(webDriver).getAttribute("value");

        //Clear the sink resource
        findResourceNameElement(webDriver).clear();

        //Attempt saving
        findSaveButton(webDriver).click();

        //Assert error
        String s = SeleniumUtil.getAlertStringAndAccept(webDriver);
        assertThat(s, is(texts.translate("error_InputFieldValidationError")));
    }

    @Test
    public void testSinkEditUnknownResourceName_DisplayErrorPopup() throws Exception {

        //Create new sink
        SinkCreationSeleniumIT.createTestSink(webDriver, SINK_NAME_1, RESOURCE_NAME_FLOWSTORE);

        //Navigate to the sink show window.
        navigateToSinksShowWidget(webDriver);

        //Assert that 1 sink can be located in the table
        assertTrue(getTableSize() == 1);

        //Navigate to the first row, locate the edit button and click.
        locateAndClickEditButtonForElement(0);

        //Retrieve the sink name from the current view
        findSinkNameElement(webDriver).getAttribute("value");

        //Clear the sink resource
        findResourceNameElement(webDriver).clear();

        //Set an unknown resource
        findResourceNameElement(webDriver).sendKeys("unknownresource");

        //Attempt saving
        findSaveButton(webDriver).click();

        //Assert error
        String s = SeleniumUtil.getAlertStringAndAccept(webDriver);
        assertThat(s, is("Error: " + texts.translate("error_ResourceNameNotValid")));  // Todo: Generalisering af fejlhåndtering
    }

    @Test
    public void testSinkEditSinkNameAlreadyExistForAnotherSink_DisplayErrorPopup() throws Exception {

        //Create 2 new sinks.
        SinkCreationSeleniumIT.createTestSink(webDriver, SINK_NAME_1, RESOURCE_NAME_FLOWSTORE);
        SinkCreationSeleniumIT.createTestSink(webDriver, SINK_NAME_2, RESOURCE_NAME_INFLIGHT);

        //Navigate to the sink show window.
        navigateToSinksShowWidget(webDriver);

        //Assert that 2 sinks can be located in the database
        assertTrue(getTableSize() == 2);

        //Navigate to the first sink created (second row as the sinks are sorted alphabetically), locate the edit button and click.
        locateAndClickEditButtonForElement(1);

        //Change the sink name to the name of the second sink created.
        findSinkNameElement(webDriver).clear();
        findSinkNameElement(webDriver).sendKeys(SINK_NAME_2);

        //Attempt saving the sink with the new name.
        findSaveButton(webDriver).click();
        String s = SeleniumUtil.getAlertStringAndAccept(webDriver);

        //Assert that an error is thrown as the sink name belonged to another existing sink.
        assertThat(s, is("Error: " + texts.translate("error_ProxyKeyViolationError")));  // Todo: Generalisering af fejlhåndtering
    }

    @Test
    public void testSinkEdit_EditSinkResourceWithoutChangingSinkName() throws Exception{

        //Create new sink
        SinkCreationSeleniumIT.createTestSink(webDriver, SINK_NAME_1, RESOURCE_NAME_FLOWSTORE);

        //Navigate to the sink show window.
        navigateToSinksShowWidget(webDriver);

        //Assert that 1 sink can be located in the table
        assertTrue(getTableSize() == 1);

        //Navigate to the first row, locate the edit button and click.
        locateAndClickEditButtonForElement(0);

        //Change the resource name from jdbc/flowStoreDb to jdbc/dataio/sinks/esInFlight and save the sink with the new resource name.
        RetrieveTextFromInputFieldsAndClickSaveButtonAndWaitForSuccessfulSave_updateResultLabelContainsSuccessMessage(null, RESOURCE_NAME_INFLIGHT);
    }

    @Test
    public void testSinkEdit_EditSinkNameWithoutChangingSinkResource() throws Exception{

        //Create new sink
        SinkCreationSeleniumIT.createTestSink(webDriver, SINK_NAME_1, RESOURCE_NAME_FLOWSTORE);

        //Navigate to the sink show window.
        navigateToSinksShowWidget(webDriver);

        //Assert that 1 sink can be located in the table
        assertTrue(getTableSize() == 1);

        //Navigate to the first row, locate the edit button and click.
        locateAndClickEditButtonForElement(0);

        //Change the sink name and save the sink.
        RetrieveTextFromInputFieldsAndClickSaveButtonAndWaitForSuccessfulSave_updateResultLabelContainsSuccessMessage(SINK_NAME_2, null);
    }

    @Test
    public void testSinkEdit_SinkNameAndResourceHasNotChanged() throws Exception {

        //Create new sink
        SinkCreationSeleniumIT.createTestSink(webDriver, SINK_NAME_1, RESOURCE_NAME_FLOWSTORE);

        //Navigate to the sink show window.
        navigateToSinksShowWidget(webDriver);

        //Assert that 1 sink can be located in the table
        assertTrue(getTableSize() == 1);

        //Navigate to the first row, locate the edit button and click.
        locateAndClickEditButtonForElement(0);

         //Save the existing sink without any changes.
        RetrieveTextFromInputFieldsAndClickSaveButtonAndWaitForSuccessfulSave_updateResultLabelContainsSuccessMessage(null, null);
    }

    @Test
    public void testSinkEdit_TestDoubleEdit() throws Exception{

        //Create new sink
        SinkCreationSeleniumIT.createTestSink(webDriver, SINK_NAME_1, RESOURCE_NAME_FLOWSTORE);

        //Navigate to the sink show window.
        navigateToSinksShowWidget(webDriver);

        //Assert that 1 sink can be located in the table
        assertTrue(getTableSize() == 1);

        //Navigate to the first row, locate the edit button and click.
        locateAndClickEditButtonForElement(0);

        //Change the sink name and save the sink.
        RetrieveTextFromInputFieldsAndClickSaveButtonAndWaitForSuccessfulSave_updateResultLabelContainsSuccessMessage(SINK_NAME_2, null);

        //Save the sink a second time to make sure the version numbers are updating correctly within the same view.
        RetrieveTextFromInputFieldsAndClickSaveButtonAndWaitForSuccessfulSave_updateResultLabelContainsSuccessMessage(null, null);
    }

    @Test
    public void testSinkEdit_testSaveVersionFromBeforeCurrent() throws Exception{

        //Setup connection to flow store service
        setUpFlowStoreConnection();

        //Create new sink through flow store
        long sinkId = ITUtil.createSink(restClient, baseUrl, new SinkContentJsonBuilder().setName(SINK_NAME_1).setResource(RESOURCE_NAME_FLOWSTORE).build());

        //Navigate to the sink show window.
        navigateToSinksShowWidget(webDriver);

        //Assert that 1 sink can be located in the table
        assertTrue(getTableSize() == 1);

        //Navigate to the first row, locate the edit button and click.
        locateAndClickEditButtonForElement(0);

        //Edit the sink through flow store and save
        ITUtil.updateSink(restClient, baseUrl, new SinkContentJsonBuilder().setName(SINK_NAME_3).setResource(RESOURCE_NAME_FLOWSTORE).build(), sinkId, 1L);

        //Edit the sink opened for edit through Selenium
        findSinkNameElement(webDriver).clear();
        findSinkNameElement(webDriver).sendKeys(SINK_NAME_2);

        //Attempt saving the sink with the new name.
        findSaveButton(webDriver).click();

        //Assert that an error is thrown. The sink opened for edit cannot be saved since the version is outdated.
        String s = SeleniumUtil.getAlertStringAndAccept(webDriver);
        assertThat(s, containsString("Error: "));  // Todo: Generalisering af fejlhåndtering
        assertThat(s, containsString("version"));
        assertThat(s, containsString("must be larger than or equal to 2"));

        //Close flow store connection
        TearDownFlowStoreConnection();
    }

    /**
     * The following is private static helper methods.
     */
    private void assertSinkEditSinkNameAndResourceFieldsAreVisibleAndDataIsInserted(){

        //Create new sink.
        SinkCreationSeleniumIT.createTestSink(webDriver, SINK_NAME_1, RESOURCE_NAME_FLOWSTORE);

        //Navigate to the sink show window.
        navigateToSinksShowWidget(webDriver);

        //Navigate to the first row, locate the edit button and click.
        locateAndClickEditButtonForElement(0);

        //Assert that the sink name and sink resource name are visible and show the correct values.
        assertTrue(findSinkNameElement(webDriver).isDisplayed());
        assertTrue(findResourceNameElement(webDriver).isDisplayed());
        assertEquals(findSinkNameElement(webDriver).getAttribute("value"), SINK_NAME_1);
        assertEquals(findResourceNameElement(webDriver).getAttribute("value"), RESOURCE_NAME_FLOWSTORE);
    }


    private void assertSinkEditSaveButtonIsVisible() {
        assertTrue(findSaveButton(webDriver).isDisplayed());
    }

    private void assertSinkEditSaveResultLabelIsNotVisibleAndEmptyAsDefault() {

        WebElement element = findSaveResultLabel(webDriver);
        assertNotNull(element);
        assertFalse(element.isDisplayed());
        assertThat(element.getText(), is(""));
    }

    private static WebElement findSinkNameElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, SinkCreateEditViewImpl.GUIID_SINK_CREATION_EDIT_SINK_NAME_PANEL, DataEntry.DATA_ENTRY_INPUT_BOX_CLASS);
    }

    private static WebElement findResourceNameElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, SinkCreateEditViewImpl.GUIID_SINK_CREATION_EDIT_RESOURCE_NAME_PANEL, DataEntry.DATA_ENTRY_INPUT_BOX_CLASS);
    }

    private static WebElement findSaveButton(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, SinkCreateEditViewImpl.GUIID_SINK_CREATION_EDIT_SAVE_BUTTON_PANEL, SaveButton.SAVE_BUTTON_BUTTON_CLASS);
    }

    private static WebElement findSaveResultLabel(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, SinkCreateEditViewImpl.GUIID_SINK_CREATION_EDIT_SAVE_BUTTON_PANEL, SaveButton.SAVE_BUTTON_RESULT_LABEL_CLASS);
    }

    private void assertAllInputFields(String sinkName, String resourceName) {
        assertThat(findSinkNameElement(webDriver).getAttribute("value"), is(sinkName));
        assertThat(findResourceNameElement(webDriver).getAttribute("value"), is(resourceName));
    }

    private void updateSinkNameAndSinkResourceInView(String sinkName, String sinkResource){
        if(sinkName != null){
            findSinkNameElement(webDriver).clear();
            findSinkNameElement(webDriver).sendKeys(sinkName);
        }
        if(sinkResource != null){
            findResourceNameElement(webDriver).clear();
            findResourceNameElement(webDriver).sendKeys(sinkResource);
        }
    }

    private void RetrieveTextFromInputFieldsAndClickSaveButtonAndWaitForSuccessfulSave_updateResultLabelContainsSuccessMessage(String sinkName, String sinkResource) {
        updateSinkNameAndSinkResourceInView(sinkName, sinkResource);
        findSinkNameElement(webDriver).getAttribute("value");
        findResourceNameElement(webDriver).getAttribute("value");
        findSaveButton(webDriver).click();
        SeleniumUtil.waitAndAssert(webDriver, SAVE_SINK_TIMOUT, SinkCreateEditViewImpl.GUIID_SINK_CREATION_EDIT_SAVE_BUTTON_PANEL, SaveButton.SAVE_BUTTON_RESULT_LABEL_CLASS, texts.translate("status_SinkSuccessfullySaved"));
    }

    private int getTableSize(){
        List<WebElement> elements = SeleniumUtil.findElementsInCurrentView(webDriver, SinksShowViewImpl.GUUID_SHOW_SINK_TABLE_EDIT, SinksShowViewImpl.CLASS_SINK_SHOW_WIDGET_EDIT_BUTTON);
        return elements.size();
    }

    private void setUpFlowStoreConnection() throws ClassNotFoundException, SQLException {
        baseUrl = String.format("http://localhost:%s/flow-store", System.getProperty("glassfish.port"));
        restClient = HttpClient.newClient();
        dbConnection = newDbConnection("flow_store");
    }

    private void TearDownFlowStoreConnection() throws SQLException {
        clearAllDbTables(dbConnection);
        JDBCUtil.closeConnection(dbConnection);
    }
}