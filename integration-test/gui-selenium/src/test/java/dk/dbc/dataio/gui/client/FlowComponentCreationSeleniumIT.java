package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.gui.client.components.DataEntry;
import dk.dbc.dataio.gui.client.components.SaveButton;
import dk.dbc.dataio.gui.client.views.FlowComponentCreateViewImpl;
import dk.dbc.dataio.gui.client.views.Menu;
import dk.dbc.dataio.integrationtest.ITUtil;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import static org.hamcrest.CoreMatchers.is;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;

public class FlowComponentCreationSeleniumIT {
    private static ConstantsProperties texts = new ConstantsProperties("FlowComponentCreateConstants_dk.properties");

    private static final String PROJECTS_PATH = "projects";
    private static final String SVN_PROJECT_NAME = "datawell-convert";
    private static final String SVN_DEPENDENCY_NAME = "jscommon";
    private static final String SVN_TRUNK_PATH = "trunk";
    private static final String JAVASCRIPT_FILE = "main.js";
    private static final String JAVASCRIPT_USE_MODULE = "main.use.js";
    private static final String INVOCATION_METHOD = "funA";
    private static final int SVN_TIMEOUT = 60;
    private static WebDriver driver;
    private static String appUrl;
    private static Connection conn;
    private static SVNURL svnRepoUrl;
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @BeforeClass
    public static void setUpClass() throws ClassNotFoundException, SQLException, SVNException, URISyntaxException, IOException {
        appUrl = "http://localhost:" + System.getProperty("glassfish.port") + "/gui/gui.html";
        conn = ITUtil.newDbConnection();
        svnRepoUrl = ITUtil.doSvnCreateFsRepository(Paths.get(System.getProperty("svn.local.dir")));
        // Disabled until we can get the FSFS repository working...
        //populateSvnRepository();
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
        ITUtil.clearDbTables(conn, ITUtil.FLOW_COMPONENTS_TABLE_NAME);
        driver.quit();
    }

    @Test
    public void testInitialVisibilityAndAccessabilityOfElements() throws IOException {
        testFlowComponentCreationNameInputFieldIsVisibleAndDataCanBeInsertedAndRead();
        testFlowComponentCreationSvnProjectInputFieldIsVisibleAndDataCanBeInsertedAndRead();
        testFlowComponentCreationSvnRevisionSelectionFieldIsVisible();
        testFlowComponentCreationScriptNameSelectionFieldIsVisible();
        testFlowComponentCreationInvocationMethodSelectionFieldIsVisible();
        testFlowComponentCreationSaveButtonIsVisible();
        testFlowComponentCreationSaveResultLabelIsVisibleAndEmpty();
    }

    public void testFlowComponentCreationNameInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToFlowComponentCreationWidget(driver);
        SeleniumUtil.assertFieldIsVisbleAndDataCanBeInsertedAndRead(findComponentNameElement(driver));
    }

    public void testFlowComponentCreationSvnProjectInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToFlowComponentCreationWidget(driver);
        SeleniumUtil.assertFieldIsVisbleAndDataCanBeInsertedAndReadWithValue(findComponentSvnProjectElement(driver), SVN_PROJECT_NAME);
    }

    public void testFlowComponentCreationSvnRevisionSelectionFieldIsVisible() {
        navigateToFlowComponentCreationWidget(driver);
        assertTrue(findComponentSvnRevisionElement(driver).isDisplayed());
    }

    public void testFlowComponentCreationScriptNameSelectionFieldIsVisible() {
        navigateToFlowComponentCreationWidget(driver);
        assertTrue(findComponentScriptNameElement(driver).isDisplayed());
    }

    public void testFlowComponentCreationInvocationMethodSelectionFieldIsVisible() {
        navigateToFlowComponentCreationWidget(driver);
        assertTrue(findComponentInvocationMethodElement(driver).isDisplayed());
    }

    public void testFlowComponentCreationSaveButtonIsVisible() {
        navigateToFlowComponentCreationWidget(driver);
        assertTrue(findComponentSaveButtonElement(driver).isDisplayed());
    }

    public void testFlowComponentCreationSaveResultLabelIsVisibleAndEmpty() {
        navigateToFlowComponentCreationWidget(driver);
        WebElement element = findComponentSaveResultLabelElement(driver);
        assertFalse(element.isDisplayed());
        assertThat(element.getText(), is(""));
    }

    @Ignore // merge with testFlowComponentCreationSuccessfulSave_* test
    @Test
    public void testFlowComponentCreationValidSvnProjectNamePopulatesListBoxes() {
        navigateToFlowComponentCreationWidget(driver);
        insertSvnProjectNameThatExistsInSvnRepository();
        waitForListBoxToFillOut(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_INVOCATION_METHOD_PANEL, DataEntry.DATA_ENTRY_INPUT_BOX_CLASS, SVN_TIMEOUT);
        findComponentNameElement(driver).sendKeys("testComponent");

        final Select svnRevision = new Select(findComponentSvnRevisionElement(driver));
        assertThat(svnRevision.getOptions().size() > 0, is(true));

        /* When uncommented no ListBoxes are filled ???

         final Select scriptName = new Select(findComponentScriptNameElement());
         assertThat(scriptName.getOptions().size() > 0, is(true));
         //assertThat(scriptName.getFirstSelectedOption().getText(), is(JAVASCRIPT_FILE));

         final Select invocationMethod = new Select(findComponentInvocationMethodElement());
         assertThat(invocationMethod.getOptions().size() > 0, is(true));
         //assertThat(invocationMethod.getFirstSelectedOption().getText(), is(INVOCATION_METHOD));
         */
    }

    @Ignore
    @Test
    public void testFlowComponentCreationSuccessfulSave_saveResultLabelContainsSuccessMessage() throws Exception {
        navigateToFlowComponentCreationWidget(driver);
        insertSvnProjectNameThatExistsInSvnRepository();
        findComponentNameElement(driver).sendKeys("testComponent");
        waitForListBoxToFillOut(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_INVOCATION_METHOD_PANEL, DataEntry.DATA_ENTRY_INPUT_BOX_CLASS, SVN_TIMEOUT);
        findComponentSaveButtonElement(driver).click();
        SeleniumUtil.waitAndAssert(driver, 10, FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_SAVE_BUTTON_PANEL, SaveButton.SAVE_BUTTON_RESULT_LABEL_CLASS, texts.translate("status_FlowComponentSuccessfullySaved"));
    }

    @Ignore
    @Test
    public void testSaveButton_EmptyComponentNameInputField_DisplayErrorPopup() throws IOException {
        navigateToFlowComponentCreationWidget(driver);
        insertSvnProjectNameThatExistsInSvnRepository();
        waitForListBoxToFillOut(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_INVOCATION_METHOD_PANEL, DataEntry.DATA_ENTRY_INPUT_BOX_CLASS, SVN_TIMEOUT);
        findComponentNameElement(driver).sendKeys("");
        findComponentSaveButtonElement(driver).click();
        String s = SeleniumUtil.getAlertStringAndAccept(driver);
        assertThat(s, is("Error: " + texts.translate("error_InputFieldValidationError")));
    }

    @Ignore
    @Test
    public void testFlowComponentCreationNameInputFieldUpdate_clearsSaveResultLabel() throws IOException {
        navigateToFlowComponentCreationWidget(driver);
        insertInputIntoInputElementsAndClickSaveButton();
        findComponentNameElement(driver).sendKeys("a");
        assertThat(findComponentSaveResultLabelElement(driver).getText(), is(""));
    }

    @Ignore
    @Test
    public void testFlowComponentCreationSvnProjectInputFieldUpdate_clearsSaveResultLabel() throws IOException {
        navigateToFlowComponentCreationWidget(driver);
        insertInputIntoInputElementsAndClickSaveButton();
        findComponentSvnProjectElement(driver).sendKeys("project");
        assertThat(findComponentSaveResultLabelElement(driver).getText(), is(""));
    }

    @Ignore
    @Test
    public void testFlowComponentCreationSvnRevisionSelectionFieldUpdate_clearsSaveResultLabel() throws IOException {
        navigateToFlowComponentCreationWidget(driver);
        insertInputIntoInputElementsAndClickSaveButton();
        new Select(findComponentSvnRevisionElement(driver)).selectByIndex(2);
        assertThat(findComponentSaveResultLabelElement(driver).getText(), is(""));
    }

    @Ignore
    @Test
    public void testFlowComponentCreationScriptFileSelectionFieldUpdate_clearsSaveResultLabel() throws IOException {
        navigateToFlowComponentCreationWidget(driver);
        insertInputIntoInputElementsAndClickSaveButton();
        new Select(findComponentScriptNameElement(driver)).selectByIndex(2);
        assertThat(findComponentSaveResultLabelElement(driver).getText(), is(""));
    }

    @Ignore
    @Test
    public void testFlowComponentCreationInvocationMethodSelectionFieldUpdate_clearsSaveResultLabel() throws IOException {
        navigateToFlowComponentCreationWidget(driver);
        insertInputIntoInputElementsAndClickSaveButton();
        new Select(findComponentInvocationMethodElement(driver)).selectByIndex(2);
        assertThat(findComponentSaveResultLabelElement(driver).getText(), is(""));
    }

    private static void navigateToFlowComponentCreationWidget(WebDriver webDriver) {
        NavigationPanelSeleniumIT.navigateTo(webDriver, Menu.GUIID_SUB_MENU_ITEM_FLOW_COMPONENT_CREATION);
    }

    private static WebElement findFlowComponentCreateNavigationElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, Menu.GUIID_SUB_MENU_ITEM_FLOW_COMPONENT_CREATION);
    }

    private static WebElement findFlowComponentCreationWidget(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_WIDGET);
    }

    private static WebElement findComponentNameElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_NAME_PANEL, DataEntry.DATA_ENTRY_INPUT_BOX_CLASS);
    }

    private WebElement findComponentSvnProjectElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_PROJECT_PANEL, DataEntry.DATA_ENTRY_INPUT_BOX_CLASS);
    }

    private WebElement findComponentSvnRevisionElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_SVN_REVISION_PANEL, DataEntry.DATA_ENTRY_INPUT_BOX_CLASS);
    }

    private WebElement findComponentScriptNameElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_SCRIPT_NAME_PANEL, DataEntry.DATA_ENTRY_INPUT_BOX_CLASS);
    }

    private WebElement findComponentInvocationMethodElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_INVOCATION_METHOD_PANEL, DataEntry.DATA_ENTRY_INPUT_BOX_CLASS);
    }

    private WebElement findComponentSaveButtonElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_SAVE_BUTTON_PANEL, SaveButton.SAVE_BUTTON_BUTTON_CLASS);
    }

    private WebElement findComponentSaveResultLabelElement(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_SAVE_BUTTON_PANEL, SaveButton.SAVE_BUTTON_RESULT_LABEL_CLASS);
    }

    private void insertSvnProjectNameThatExistsInSvnRepository() {
        final WebElement element = findComponentSvnProjectElement(driver);
        element.sendKeys(SVN_PROJECT_NAME);
        element.sendKeys(Keys.TAB);
    }

    private void insertInputIntoInputElementsAndClickSaveButton() throws IOException {
        insertSvnProjectNameThatExistsInSvnRepository();
        findComponentNameElement(driver).sendKeys("testComponent");
        waitForListBoxToFillOut(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_INVOCATION_METHOD_PANEL, DataEntry.DATA_ENTRY_INPUT_BOX_CLASS, SVN_TIMEOUT);
        findComponentSaveButtonElement(driver).click();
    }

    private void waitForListBoxToFillOut(final String elementId, final String elementClass, final int duration) {
        final WebDriverWait wait = new WebDriverWait(driver, duration);
        wait.until(new ListBoxFilledOutCondition(elementId, elementClass));
    }

    private static void populateSvnRepository() throws IOException, SVNException, URISyntaxException {
        final URL project = FlowComponentCreationSeleniumIT.class.getResource(String.format("/%s", PROJECTS_PATH));
        ITUtil.doSvnImport(svnRepoUrl, Paths.get(project.toURI()), "initial import");
        // Force second revision...
        final Path checkoutFolder = Paths.get(FlowComponentCreationSeleniumIT.class.getResource("/").toURI());
        ITUtil.doSvnCheckout(svnRepoUrl, checkoutFolder);
        appendToFile(Paths.get(checkoutFolder.toString(), SVN_PROJECT_NAME, SVN_TRUNK_PATH, JAVASCRIPT_FILE), "// some comment");
        appendToFile(Paths.get(checkoutFolder.toString(), SVN_DEPENDENCY_NAME, SVN_TRUNK_PATH, JAVASCRIPT_USE_MODULE), "// some other comment");
        ITUtil.doSvnCommit(checkoutFolder, "updated");
    }

    private static void appendToFile(final Path filename, final String data) throws IOException {
        try (PrintWriter output = new PrintWriter(new FileWriter(filename.toString(), true))) {
            output.print(data);
        }
    }

    private class ListBoxFilledOutCondition implements ExpectedCondition<Boolean> {

        private final String elementId;
        private final String elementClass;

        public ListBoxFilledOutCondition(final String elementId, final String elementClass) {
            this.elementId = elementId;
            this.elementClass = elementClass;
        }

        @Override
        public Boolean apply(WebDriver webDriver) {
            return new Select(SeleniumUtil.findElementInCurrentView(webDriver, elementId, elementClass)).getOptions().size() > 0;
        }
    }

    /**
     * The following are public static helper methods.
     */
    // notice: WebDriver is currently unused but is there for future reference.
    public static boolean createTestFlowComponent(WebDriver webDriver, String name) {
        final long resourceIdentifier = ITUtil.createFlowComponentWithName(name);
        return resourceIdentifier > 0L;
    }
}
