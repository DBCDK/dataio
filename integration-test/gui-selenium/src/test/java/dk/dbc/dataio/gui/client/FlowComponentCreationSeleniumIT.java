package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.gui.client.views.FlowComponentCreateViewImpl;
import dk.dbc.dataio.gui.client.views.MainPanel;
import dk.dbc.dataio.integrationtest.ITUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;

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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class FlowComponentCreationSeleniumIT {
    private static final String PROJECTS_PATH = "projects";
    private static final String SVN_PROJECT_NAME = "main";
    private static final String SVN_DEPENDENCY_NAME = "jscommon";
    private static final String SVN_TRUNK_PATH = "trunk";
    private static final String JAVASCRIPT_FILE = "main.js";
    private static final String JAVASCRIPT_USE_MODULE = "main.use.js";
    private static final String INVOCATION_METHOD = "funA";

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
        populateSvnRepository();
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
    public void testInitialVisibililtyAndAccessabilityOfElements() throws IOException {
        testFlowComponentCreationNavigationItemIsVisibleAndClickable();
        testFlowComponentCreationNameInputFieldIsVisibleAndDataCanBeInsertedAndRead();
        testFlowComponentCreationSvnProjectInputFieldIsVisibleAndDataCanBeInsertedAndRead();
        testFlowComponentCreationSvnRevisionSelectionFieldIsVisible();
        testFlowComponentCreationScriptNameSelectionFieldIsVisible();
        testFlowComponentCreationInvocationMethodSelectionFieldIsVisible();
        testFlowComponentCreationSaveButtonIsVisible();
        testFlowComponentCreationSaveResultLabelIsVisibleAndEmpty();
    }

    public void testFlowComponentCreationNavigationItemIsVisibleAndClickable() {
        WebElement element = driver.findElement(By.id(MainPanel.GUIID_NAVIGATION_MENU_ITEM_FLOW_COMPONENT_CREATION));
        assertEquals(true, element.isDisplayed());
        element.click();

        WebElement widget = driver.findElement(By.id(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_WIDGET));
        assertEquals(true, widget.isDisplayed());
    }

    public void testFlowComponentCreationNameInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToFlowComponentCreationContext();
        WebElement element = findComponentNameElement();
        assertEquals(true, element.isDisplayed());

        final String fieldValue = "test of unicode content æøåÆØÅ";
        element.sendKeys(fieldValue);
        assertEquals(fieldValue, element.getAttribute("value"));
    }

    public void testFlowComponentCreationSvnProjectInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToFlowComponentCreationContext();
        final WebElement element = findComponentSvnProjectElement();
        assertEquals(true, element.isDisplayed());

        final String fieldValue = SVN_PROJECT_NAME;
        element.sendKeys(fieldValue);
        assertEquals(fieldValue, element.getAttribute("value"));
    }

    public void testFlowComponentCreationSvnRevisionSelectionFieldIsVisible() {
        navigateToFlowComponentCreationContext();
        final WebElement element = findComponentSvnRevisionElement();
        assertEquals(true, element.isDisplayed());
    }

    public void testFlowComponentCreationScriptNameSelectionFieldIsVisible() {
        navigateToFlowComponentCreationContext();
        final WebElement element = findComponentScriptNameElement();
        assertEquals(true, element.isDisplayed());
    }

    public void testFlowComponentCreationInvocationMethodSelectionFieldIsVisible() {
        navigateToFlowComponentCreationContext();
        final WebElement element = findComponentInvocationMethodElement();
        assertEquals(true, element.isDisplayed());
    }

    public void testFlowComponentCreationSaveButtonIsVisible() {
        navigateToFlowComponentCreationContext();
        WebElement element = findComponentSaveButtonElement();
        assertEquals(true, element.isDisplayed());
    }

    public void testFlowComponentCreationSaveResultLabelIsVisibleAndEmpty() {
        navigateToFlowComponentCreationContext();
        WebElement element = findComponentSaveResultLabelElement();
        assertEquals(false, element.isDisplayed());
        assertEquals("", element.getText());
    }

    @Test
    public void testFlowComponentCreationValidSvnProjectNamePopulatesListBoxes() {
        navigateToFlowComponentCreationContext();
        final WebElement svnProject = insertSvnProjectNameThatExistsInSvnRepository();
        findComponentNameElement().sendKeys("test");
        waitForListBoxToFillOut(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_SVN_REVISION_LIST_BOX, 30);
        /*
        WebDriverWait wait = new WebDriverWait(driver, 30);
        wait.until(new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver webDriver) {
                return new Select(findComponentSvnRevisionElement()).getOptions().size() > 1;
            }
        });
        */
        final Select svnRevision = new Select(findComponentSvnRevisionElement());
        assertThat(svnRevision.getOptions().size(), is(2));

        waitForListBoxToFillOut(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_SCRIPT_NAME_LIST_BOX, 30);
        final Select scriptName = new Select(findComponentScriptNameElement());
        assertThat(scriptName.getOptions().size(), is(1));
        assertThat(scriptName.getFirstSelectedOption().getText(), is(JAVASCRIPT_FILE));

        waitForListBoxToFillOut(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_INVOCATION_METHOD_LIST_BOX, 30);
        final Select invocationMethod = new Select(findComponentInvocationMethodElement());
        assertThat(invocationMethod.getOptions().size(), is(2));
        assertThat(invocationMethod.getFirstSelectedOption().getText(), is(INVOCATION_METHOD));
    }

    @Ignore  // Er midlertidig slået fra - afventer opdateret Seleniumtest 
    @Test
    public void testSaveButton_EmptyComponentNameInputField_DisplayErrorPopup() throws IOException {
        //File javascriptFile = createTemporaryJavascriptFile();
        //avigateToFlowComponentCreationContext();
        //findFileUploadElement().sendKeys(javascriptFile.getAbsolutePath());
        //findInvocationMethodElement().sendKeys("f");
        //findSaveButtonElement().click();

        Alert alert = driver.switchTo().alert();
        String s = alert.getText();
        alert.accept();
        assertEquals(FlowComponentCreateViewImpl.FLOW_COMPONENT_CREATION_INPUT_FIELD_VALIDATION_ERROR, s);
    }

    @Ignore  // Er midlertidig slået fra - afventer opdateret Seleniumtest 
    @Test
    public void testSaveButton_MissingFileForUpload_DisplayErrorPopup() throws IOException {
        navigateToFlowComponentCreationContext();
        findComponentNameElement().sendKeys("testComponent");
        //findInvocationMethodElement().sendKeys("f");
        //findSaveButtonElement().click();

        Alert alert = driver.switchTo().alert();
        String s = alert.getText();
        alert.accept();
        assertEquals(FlowComponentCreateViewImpl.FLOW_COMPONENT_CREATION_INPUT_FIELD_VALIDATION_ERROR, s);
    }

    @Ignore  // Er midlertidig slået fra - afventer opdateret Seleniumtest 
    @Test
    public void testSaveButton_EmptyInvocationMethodInputField_DisplayErrorPopup() throws IOException {
        //File javascriptFile = createTemporaryJavascriptFile();
        //navigateToFlowComponentCreationContext();
        //findComponentNameElement().sendKeys("testComponent");
        //findFileUploadElement().sendKeys(javascriptFile.getAbsolutePath());
        //findSaveButtonElement().click();

        Alert alert = driver.switchTo().alert();
        String s = alert.getText();
        alert.accept();
        assertEquals(FlowComponentCreateViewImpl.FLOW_COMPONENT_CREATION_INPUT_FIELD_VALIDATION_ERROR, s);
    }

    @Ignore  // Er midlertidig slået fra - afventer opdateret Seleniumtest 
    @Test
    public void testFlowComponentCreationSuccessfulSave_saveResultLabelContainsSuccessMessage() throws IOException {
        navigateToFlowComponentCreationContext();
        //insertInputIntoInputElementsAndClickSaveButton();

        WebDriverWait wait = new WebDriverWait(driver, 10);
        wait.until(ExpectedConditions.textToBePresentInElement(By.id(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_SAVE_RESULT_LABEL), FlowComponentCreateViewImpl.SAVE_RESULT_LABEL_SUCCES_MESSAGE));
        //WebElement saveResultLabel = findSaveResultLabelElement();
        //assertEquals(FlowComponentCreateViewImpl.SAVE_RESULT_LABEL_SUCCES_MESSAGE, saveResultLabel.getText());
    }

    @Ignore
    @Test
    public void testFlowComponentCreationNameInputFieldUpdate_clearsSaveResultLabel() throws IOException {
        navigateToFlowComponentCreationContext();
        //insertInputIntoInputElementsAndClickSaveButton();
        findComponentNameElement().sendKeys("a");
        //assertEquals("", findSaveResultLabelElement().getText());
    }

    @Ignore
    @Test
    public void testFlowComponentCreationFileUploadUpdate_clearsSaveResultLabel() throws IOException {
        navigateToFlowComponentCreationContext();
        //insertInputIntoInputElementsAndClickSaveButton();
        //findFileUploadElement().sendKeys("b");
        //assertEquals("", findSaveResultLabelElement().getText());
    }

    @Ignore
    @Test
    public void testFlowComponentCreationInvocationMethodInputfieldUpdate_clearsSaveResultLabel() throws IOException {
        navigateToFlowComponentCreationContext();
        //insertInputIntoInputElementsAndClickSaveButton();
        //findInvocationMethodElement().sendKeys("c");
        //assertEquals("", findSaveResultLabelElement().getText());
    }

    private void navigateToFlowComponentCreationContext() {
        WebElement element = driver.findElement(By.id(MainPanel.GUIID_NAVIGATION_MENU_ITEM_FLOW_COMPONENT_CREATION));
        element.click();
    }

    private WebElement findComponentNameElement() {
        return driver.findElement(By.id(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_NAME_TEXT_BOX));
    }

    private WebElement findComponentSvnProjectElement() {
        return driver.findElement(By.id(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_SVN_PROJECT_TEXT_BOX));
    }

    private WebElement findComponentSvnRevisionElement() {
        return driver.findElement(By.id(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_SVN_REVISION_LIST_BOX));
    }

    private WebElement findComponentScriptNameElement() {
        return driver.findElement(By.id(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_SCRIPT_NAME_LIST_BOX));
    }

    private WebElement findComponentInvocationMethodElement() {
        return driver.findElement(By.id(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_INVOCATION_METHOD_LIST_BOX));
    }

    private WebElement findComponentSaveButtonElement() {
        return driver.findElement(By.id(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_SAVE_BUTTON));
    }

    private WebElement findComponentSaveResultLabelElement() {
        return driver.findElement(By.id(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_SAVE_RESULT_LABEL));
    }

    private WebElement findElement(final String elementId) {
        return driver.findElement(By.id(elementId));
    }

    private WebElement insertSvnProjectNameThatExistsInSvnRepository() {
        final WebElement element = findComponentSvnProjectElement();
        element.sendKeys(SVN_PROJECT_NAME);
        return element;
    }

    /*
    private void insertInputIntoInputElementsAndClickSaveButton() throws IOException {
        findComponentNameElement().sendKeys("testComponent");
        findFileUploadElement().sendKeys(createTemporaryJavascriptFile().getAbsolutePath());
        findInvocationMethodElement().sendKeys("f");
        findSaveButtonElement().click();
    }

    private File createTemporaryJavascriptFile() throws IOException {
        final String javascript = "function f(s) { return s.toUpperCase(); }";
        final String javascriptFileName = "test.js";

        File javascriptFile = tempFolder.newFile(javascriptFileName);
        try (BufferedWriter writer = Files.newBufferedWriter(javascriptFile.toPath(), Charset.forName("UTF-8"))) {
            writer.write(javascript);
        }
        return javascriptFile;
    }

    public static void addFlowComponent(WebDriver driver, TemporaryFolder tempFolder, String componentName, String javascript, String invocationMethod) throws IOException {
        final String javascriptFileName = "test-"+System.currentTimeMillis()+".js";
        File javascriptFile = tempFolder.newFile(javascriptFileName);
        try (BufferedWriter writer = Files.newBufferedWriter(javascriptFile.toPath(), Charset.forName("UTF-8"))) {
            writer.write(javascript);
        }
        driver.findElement(By.id(MainPanel.GUIID_NAVIGATION_MENU_ITEM_FLOW_COMPONENT_CREATION)).click();
        driver.findElement(By.id(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_NAME_TEXT_BOX)).clear();
        driver.findElement(By.id(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_NAME_TEXT_BOX)).sendKeys(componentName);
        driver.findElement(By.id(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_JAVASCRIPT_FILE_UPLOAD)).sendKeys(javascriptFile.getAbsolutePath());
        driver.findElement(By.id(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_INVOCATION_METHOD_LIST_BOX)).clear();
        driver.findElement(By.id(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_INVOCATION_METHOD_LIST_BOX)).sendKeys(invocationMethod);
        driver.findElement(By.id(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_SAVE_BUTTON)).click();
        
        WebDriverWait wait = new WebDriverWait(driver, 10);
        wait.until(ExpectedConditions.textToBePresentInElement(By.id(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_SAVE_RESULT_LABEL), FlowComponentCreateViewImpl.SAVE_RESULT_LABEL_SUCCES_MESSAGE));
    }

    public static void clearFlowComponentDBTable(Connection conn) throws SQLException {
        ITUtil.clearDbTables(conn, ITUtil.FLOW_COMPONENTS_TABLE_NAME);
    }
    */

    private WebElement changeFocusTo(WebElement element) {
        new Actions(driver).moveToElement(element).perform();
        return element;
    }

    private void waitForListBoxToFillOut(final String elementId, final int duration) {
        final WebDriverWait wait = new WebDriverWait(driver, duration);
        wait.until(new ListBoxFilledOutCondition(elementId));
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

        public ListBoxFilledOutCondition(final String elementId) {
            this.elementId = elementId;
        }

        @Override
        public Boolean apply(WebDriver webDriver) {
            return new Select(findElement(elementId)).getOptions().size() > 1;
        }
    }
}
