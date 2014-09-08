package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentContentBuilder;
import dk.dbc.dataio.gui.client.components.DataEntry;
import dk.dbc.dataio.gui.client.components.SaveButton;
import dk.dbc.dataio.gui.client.pages.flowcomponent.flowcomponentcreateedit.FlowComponentCreateEditViewImpl;
import dk.dbc.dataio.gui.util.ClientFactoryImpl;
import dk.dbc.dataio.integrationtest.ITUtil;
import org.junit.BeforeClass;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import javax.ws.rs.client.Client;
import java.sql.SQLException;

import static dk.dbc.dataio.gui.client.FlowComponentsShowSeleniumIT.locateAndClickEditButtonForElement;
import static dk.dbc.dataio.gui.client.FlowComponentsShowSeleniumIT.navigateToFlowComponentsShowWidget;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class FlowComponentEditSeleniumIT extends AbstractGuiSeleniumTest {
    private static ConstantsProperties texts = new ConstantsProperties("pages/flowcomponent/flowcomponentcreateedit/FlowComponentCreateEditConstants_dk.properties");

    final String FLOW_COMPONENT_NAME = "FlowComponentName";
    final String SVN_PROJECT = "datawell-convert";
    final Long   SVN_REVISION = 123L;
    final String JAVASCRIPT_NAME = "JavaScriptName";
    final String INVOCATION_METHOD = "InvocationMethod";

    private static FlowStoreServiceConnector flowStoreServiceConnector;

    @BeforeClass
    public static void setUpClass() throws ClassNotFoundException, SQLException {
        Client restClient = HttpClient.newClient();
        String baseUrl = ITUtil.FLOW_STORE_BASE_URL;
        flowStoreServiceConnector = new FlowStoreServiceConnector(restClient, baseUrl);
    }

    /**
     * The following is private helper methods.
     */

    private void populateAllInputFields() {
        findFlowComponentNameElement(webDriver).sendKeys(FLOW_COMPONENT_NAME);
        findSvnProjectElement(webDriver).sendKeys(SVN_PROJECT);
        findSvnRevisionElement(webDriver).sendKeys(SVN_REVISION.toString());
        findJavaScriptNameElement(webDriver).sendKeys(JAVASCRIPT_NAME);
        findInvocationMethodElement(webDriver).sendKeys(INVOCATION_METHOD);
    }

    private void assertAllInputFields(String flowComponentName, String svnProject, String svnRevision, String javaScript, String invocationMethod) {
        assertThat(findFlowComponentNameElement(webDriver).getAttribute("value"), is(flowComponentName));
        assertThat(findSvnProjectElement(webDriver).getAttribute("value"), is(svnProject));
        assertThat(findSvnRevisionElement(webDriver).getAttribute("value"), is(svnRevision));
        assertThat(findJavaScriptNameElement(webDriver).getAttribute("value"), is(javaScript));
        assertThat(findInvocationMethodElement(webDriver).getAttribute("value"), is(invocationMethod));
    }

    private static FlowComponent createTestFlowComponent(String flowComponentName, String svnProject, Long svnRevision, String javaScriptName, String invocationMethod) throws Exception {
        FlowComponentContent flowComponentContent = new FlowComponentContentBuilder()
                .setName(flowComponentName)
                .setSvnProjectForInvocationJavascript(svnProject)
                .setSvnRevision(svnRevision)
                .setInvocationJavascriptName(javaScriptName)
                .setInvocationMethod(invocationMethod)
                .build();
        return flowStoreServiceConnector.createFlowComponent(flowComponentContent);
    }

    private static FlowComponent updateTestFlowComponent(String flowComponentName, String svnProject, Long svnRevision, String javaScriptName, String invocationMethod, long id, long version) throws Exception {
        FlowComponentContent flowComponentContent = new FlowComponentContentBuilder()
                .setName(flowComponentName)
                .setSvnProjectForInvocationJavascript(svnProject)
                .setSvnRevision(svnRevision)
                .setInvocationJavascriptName(javaScriptName)
                .setInvocationMethod(invocationMethod)
                .build();
        return flowStoreServiceConnector.updateFlowComponent(flowComponentContent, id, version);
    }

    private void assertFlowComponentEditFieldsAreVisibleAndDataIsInserted() throws Exception {

        //Create new flow component
        FlowComponent flowComponent = createTestFlowComponent(FLOW_COMPONENT_NAME, SVN_PROJECT, SVN_REVISION, JAVASCRIPT_NAME, INVOCATION_METHOD);

        //Navigate to the flow components show window.
        navigateToFlowComponentsShowWidget(webDriver);

        //Navigate to the first row, locate the edit button and click.
        locateAndClickEditButtonForElement(0);

        //Assert that all the fields are shown
        assertTrue(findFlowComponentNameElement(webDriver).isDisplayed());
        assertTrue(findSvnProjectElement(webDriver).isDisplayed());
        assertTrue(findSvnRevisionElement(webDriver).isDisplayed());
        assertTrue(findJavaScriptNameElement(webDriver).isDisplayed());
        assertTrue(findInvocationMethodElement(webDriver).isDisplayed());
        //Assert that all the fields show the correct values.
        assertThat(findFlowComponentNameElement(webDriver).getAttribute("value"), is(flowComponent.getContent().getName()));
        assertThat(findSvnProjectElement(webDriver).getAttribute("value"), is(flowComponent.getContent().getSvnProjectForInvocationJavascript()));
        assertThat(findSvnRevisionElement(webDriver).getAttribute("value"), is(Long.toString(flowComponent.getContent().getSvnRevision())));
        assertThat(findJavaScriptNameElement(webDriver).getAttribute("value"), is(flowComponent.getContent().getInvocationJavascriptName()));
        assertThat(findInvocationMethodElement(webDriver).getAttribute("value"), is(flowComponent.getContent().getInvocationMethod()));
    }

    private void assertFlowComponentEditSaveButtonIsVisible() {
        assertTrue(findSaveButton(webDriver).isDisplayed());
    }

    private void assertFlowComponentEditSaveResultLabelIsNotVisibleAndEmptyAsDefault() {

        WebElement element = findSaveResultLabel(webDriver);
        assertNotNull(element);
        assertFalse(element.isDisplayed());
        assertThat(element.getText(), is(""));
    }

    /**
     * The following is private static helper methods.
     */

    private static void navigateAwayFromFlowComponentCreationWidget(WebDriver webDriver) {
        NavigationPanelSeleniumIT.navigateTo(webDriver, ClientFactoryImpl.GUIID_MENU_ITEM_FLOW_COMPONENTS_SHOW);
    }

    private static WebElement findFlowComponentNameElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowComponentCreateEditViewImpl.GUIID_FLOW_COMPONENT_CREATION_EDIT_NAME_PANEL, DataEntry.DATA_ENTRY_INPUT_BOX_CLASS);
    }

    private static WebElement findSvnProjectElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowComponentCreateEditViewImpl.GUIID_FLOW_COMPONENT_CREATION_EDIT_PROJECT_PANEL, DataEntry.DATA_ENTRY_INPUT_BOX_CLASS);
    }

    private static WebElement findSvnRevisionElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowComponentCreateEditViewImpl.GUIID_FLOW_COMPONENT_CREATION_EDIT_SVN_REVISION_PANEL, DataEntry.DATA_ENTRY_INPUT_BOX_CLASS);
    }

    private static WebElement findJavaScriptNameElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowComponentCreateEditViewImpl.GUIID_FLOW_COMPONENT_CREATION_EDIT_SCRIPT_NAME_PANEL, DataEntry.DATA_ENTRY_INPUT_BOX_CLASS);
    }

    private static WebElement findInvocationMethodElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowComponentCreateEditViewImpl.GUIID_FLOW_COMPONENT_CREATION_EDIT_INVOCATION_METHOD_PANEL, DataEntry.DATA_ENTRY_INPUT_BOX_CLASS);
    }

    private static WebElement findSaveButton(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowComponentCreateEditViewImpl.GUIID_FLOW_COMPONENT_CREATION_EDIT_SAVE_BUTTON_PANEL, SaveButton.SAVE_BUTTON_BUTTON_CLASS);
    }

    private static WebElement findSaveResultLabel(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowComponentCreateEditViewImpl.GUIID_FLOW_COMPONENT_CREATION_EDIT_SAVE_BUTTON_PANEL, SaveButton.SAVE_BUTTON_RESULT_LABEL_CLASS);
    }
}
