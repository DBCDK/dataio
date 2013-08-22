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
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class FlowComponentCreationSeleniumIT {

    private static WebDriver driver;
    private static String APP_URL;
    private static Connection conn;
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @BeforeClass
    public static void setUpClass() throws ClassNotFoundException, SQLException {
        String glassfishPort = System.getProperty("glassfish.port");
        APP_URL = "http://localhost:" + glassfishPort + "/gui/gui.html";
        conn = ITUtil.newDbConnection();
    }

    @AfterClass
    public static void tearDownClass() throws SQLException {
        conn.close();
    }

    @Before
    public void setUp() {
        driver = new FirefoxDriver();
        driver.get(APP_URL);
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
        testFlowComponentCreationInvocationMethodInputFieldIsVisibleAndDataCanBeInsertedAndRead();
        testFlowComponentCreationFileUploadIsVisibleAndFileNameCanBeChosenAndFileNameCanBeRetrievedFromWidget();
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

    public void testFlowComponentCreationInvocationMethodInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToFlowComponentCreationContext();
        WebElement element = findInvocationMethodElement();
        assertEquals(true, element.isDisplayed());

        final String fieldValue = "test of unicode content æøåÆØÅ";
        element.sendKeys(fieldValue);
        assertEquals(fieldValue, element.getAttribute("value"));
    }

    public void testFlowComponentCreationFileUploadIsVisibleAndFileNameCanBeChosenAndFileNameCanBeRetrievedFromWidget() throws IOException {
        File javascript = createTemporaryJavascriptFile();
        navigateToFlowComponentCreationContext();
        WebElement element = findFileUploadElement();
        assertEquals(true, element.isDisplayed());

        element.sendKeys(javascript.getAbsolutePath());
        assertEquals(javascript.getAbsolutePath(), element.getAttribute("value"));
    }

    public void testFlowComponentCreationSaveButtonIsVisible() {
        navigateToFlowComponentCreationContext();
        WebElement element = findSaveButtonElement();
        assertEquals(true, element.isDisplayed());
    }

    public void testFlowComponentCreationSaveResultLabelIsVisibleAndEmpty() {
        navigateToFlowComponentCreationContext();
        WebElement element = findSaveResultLabelElement();
        assertEquals(false, element.isDisplayed());
        assertEquals("", element.getText());
    }

    @Test
    public void testSaveButton_EmptyComponentNameInputField_DisplayErrorPopup() throws IOException {
        File javascriptFile = createTemporaryJavascriptFile();
        navigateToFlowComponentCreationContext();
        findFileUploadElement().sendKeys(javascriptFile.getAbsolutePath());
        findInvocationMethodElement().sendKeys("f");
        findSaveButtonElement().click();

        Alert alert = driver.switchTo().alert();
        String s = alert.getText();
        alert.accept();
        assertEquals(FlowComponentCreateViewImpl.FLOW_COMPONENT_CREATION_INPUT_FIELD_VALIDATION_ERROR, s);
    }

    @Test
    public void testSaveButton_MissingFileForUpload_DisplayErrorPopup() throws IOException {
        navigateToFlowComponentCreationContext();
        findComponentNameElement().sendKeys("testComponent");
        findInvocationMethodElement().sendKeys("f");
        findSaveButtonElement().click();

        Alert alert = driver.switchTo().alert();
        String s = alert.getText();
        alert.accept();
        assertEquals(FlowComponentCreateViewImpl.FLOW_COMPONENT_CREATION_INPUT_FIELD_VALIDATION_ERROR, s);
    }

    @Test
    public void testSaveButton_EmptyInvocationMethodInputField_DisplayErrorPopup() throws IOException {
        File javascriptFile = createTemporaryJavascriptFile();
        navigateToFlowComponentCreationContext();
        findComponentNameElement().sendKeys("testComponent");
        findFileUploadElement().sendKeys(javascriptFile.getAbsolutePath());
        findSaveButtonElement().click();

        Alert alert = driver.switchTo().alert();
        String s = alert.getText();
        alert.accept();
        assertEquals(FlowComponentCreateViewImpl.FLOW_COMPONENT_CREATION_INPUT_FIELD_VALIDATION_ERROR, s);
    }

    @Test
    public void testFlowComponentCreationSuccessfulSave_saveResultLabelContainsSuccessMessage() throws IOException {
        navigateToFlowComponentCreationContext();
        insertInputIntoInputElementsAndClickSaveButton();

        WebDriverWait wait = new WebDriverWait(driver, 10);
        wait.until(ExpectedConditions.textToBePresentInElement(By.id(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_SAVE_RESULT_LABEL), FlowComponentCreateViewImpl.SAVE_RESULT_LABEL_SUCCES_MESSAGE));
        WebElement saveResultLabel = findSaveResultLabelElement();
        assertEquals(FlowComponentCreateViewImpl.SAVE_RESULT_LABEL_SUCCES_MESSAGE, saveResultLabel.getText());
    }

    @Ignore
    @Test
    public void testFlowComponentCreationNameInputFieldUpdate_clearsSaveResultLabel() throws IOException {
        navigateToFlowComponentCreationContext();
        insertInputIntoInputElementsAndClickSaveButton();
        findComponentNameElement().sendKeys("a");
        assertEquals("", findSaveResultLabelElement().getText());
    }

    @Ignore
    @Test
    public void testFlowComponentCreationFileUploadUpdate_clearsSaveResultLabel() throws IOException {
        navigateToFlowComponentCreationContext();
        insertInputIntoInputElementsAndClickSaveButton();
        findFileUploadElement().sendKeys("b");
        assertEquals("", findSaveResultLabelElement().getText());
    }

    @Ignore
    @Test
    public void testFlowComponentCreationInvocationMethodInputfieldUpdate_clearsSaveResultLabel() throws IOException {
        navigateToFlowComponentCreationContext();
        insertInputIntoInputElementsAndClickSaveButton();
        findInvocationMethodElement().sendKeys("c");
        assertEquals("", findSaveResultLabelElement().getText());
    }

    private void navigateToFlowComponentCreationContext() {
        WebElement element = driver.findElement(By.id(MainPanel.GUIID_NAVIGATION_MENU_ITEM_FLOW_COMPONENT_CREATION));
        element.click();
    }

    private WebElement findComponentNameElement() {
        return driver.findElement(By.id(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_NAME_TEXT_BOX));
    }

    private WebElement findFileUploadElement() {
        return driver.findElement(By.id(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_JAVASCRIPT_FILE_UPLOAD));
    }

    private WebElement findInvocationMethodElement() {
        return driver.findElement(By.id(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_INVOCATION_METHOD_TEXT_BOX));
    }

    private WebElement findSaveButtonElement() {
        return driver.findElement(By.id(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_SAVE_BUTTON));
    }

    private WebElement findSaveResultLabelElement() {
        return driver.findElement(By.id(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_SAVE_RESULT_LABEL));
    }

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
        driver.findElement(By.id(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_INVOCATION_METHOD_TEXT_BOX)).clear();
        driver.findElement(By.id(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_INVOCATION_METHOD_TEXT_BOX)).sendKeys(invocationMethod);
        driver.findElement(By.id(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_SAVE_BUTTON)).click();
        
        WebDriverWait wait = new WebDriverWait(driver, 10);
        wait.until(ExpectedConditions.textToBePresentInElement(By.id(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_SAVE_RESULT_LABEL), FlowComponentCreateViewImpl.SAVE_RESULT_LABEL_SUCCES_MESSAGE));
    }
    
    public static void clearFlowComponentDBTable(Connection conn) throws SQLException {
        ITUtil.clearDbTables(conn, ITUtil.FLOW_COMPONENTS_TABLE_NAME);
    } 
}
