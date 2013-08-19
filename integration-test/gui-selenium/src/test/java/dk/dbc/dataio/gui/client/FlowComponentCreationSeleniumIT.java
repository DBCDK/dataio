package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.gui.client.views.FlowComponentCreateViewImpl;
import dk.dbc.dataio.gui.client.views.MainPanel;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
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

        Class.forName("org.h2.Driver");
        conn = DriverManager.getConnection("jdbc:h2:tcp://localhost:" + System.getProperty("h2.port") + "/mem:submitter_store", "root", "root");
        conn.setAutoCommit(true);
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
        clearDbTables();
        driver.quit();
    }

    // Note: Many of these tests should arguably be two separate tests, 
    // but I think that keeping them as one test is acceptable since 
    // we save one run of the WebDriver.

    @Test
    public void testFlowComponentCreationNavigationItemIsVisibleAndClickable() {
        WebElement element = driver.findElement(By.id(MainPanel.GUIID_NAVIGATION_MENU_ITEM_FLOW_COMPONENT_CREATION));
        assertEquals(true, element.isDisplayed());

        element.click();
        WebElement widget = driver.findElement(By.id(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_WIDGET));
        assertEquals(true, widget.isDisplayed());
    }

    @Test
    public void testFlowComponentCreationNameInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToFlowComponentCreationContext();
        WebElement element = driver.findElement(By.id(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_NAME_TEXT_BOX));
        assertEquals(true, element.isDisplayed());

        final String fieldValue = "test of unicode content æøåÆØÅ";
        element.sendKeys(fieldValue);
        assertEquals(fieldValue, element.getAttribute("value"));
    }

    @Test
    public void testFlowComponentCreationInvocationMethodInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToFlowComponentCreationContext();
        WebElement element = driver.findElement(By.id(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_INVOCATION_METHOD_TEXT_BOX));
        assertEquals(true, element.isDisplayed());

        final String fieldValue = "test of unicode content æøåÆØÅ";
        element.sendKeys(fieldValue);
        assertEquals(fieldValue, element.getAttribute("value"));
    }

    @Test
    public void testFlowComponentCreationFileUploadIsVisibleAndFileCanBeChosenAndRead() {
        navigateToFlowComponentCreationContext();
        WebElement element = driver.findElement(By.id(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_JAVASCRIPT_FILE_UPLOAD));
        assertEquals(true, element.isDisplayed());

        String filename = "/tmp/tmptmp/test.txt";
        element.sendKeys(filename);
        assertEquals(filename, element.getAttribute("value"));
    }

    @Test
    public void testFlowComponentCreation_missingComponentName_givesAlert() throws IOException {
        File javascriptFile = createTemporaryJavascriptFile();
        navigateToFlowComponentCreationContext();

        WebElement fileUpload = driver.findElement(By.id(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_JAVASCRIPT_FILE_UPLOAD));
        fileUpload.sendKeys(javascriptFile.getAbsolutePath());

        WebElement invocationMethod = driver.findElement(By.id(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_INVOCATION_METHOD_TEXT_BOX));
        invocationMethod.sendKeys("f");

        WebElement saveButton = driver.findElement(By.id(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_SAVE_BUTTON));
        saveButton.click();

        Alert alert = driver.switchTo().alert();
        String s = alert.getText();
        alert.accept();
        assertEquals(FlowComponentCreateViewImpl.FLOW_COMPONENT_CREATION_INPUT_FIELD_VALIDATION_ERROR, s);
    }

    @Test
    public void testFlowComponentCreation_missingFileForUpload_givesAlert() throws IOException {
        navigateToFlowComponentCreationContext();

        WebElement componentName = driver.findElement(By.id(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_NAME_TEXT_BOX));
        componentName.sendKeys("testComponent");

        WebElement invocationMethod = driver.findElement(By.id(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_INVOCATION_METHOD_TEXT_BOX));
        invocationMethod.sendKeys("f");

        WebElement saveButton = driver.findElement(By.id(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_SAVE_BUTTON));
        saveButton.click();

        Alert alert = driver.switchTo().alert();
        String s = alert.getText();
        alert.accept();
        assertEquals(FlowComponentCreateViewImpl.FLOW_COMPONENT_CREATION_INPUT_FIELD_VALIDATION_ERROR, s);
    }

    @Test
    public void testFlowComponentCreation_missingInvocationMethod_givesAlert() throws IOException {
        File javascriptFile = createTemporaryJavascriptFile();
        navigateToFlowComponentCreationContext();

        WebElement componentName = driver.findElement(By.id(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_NAME_TEXT_BOX));
        componentName.sendKeys("testComponent");

        WebElement fileUpload = driver.findElement(By.id(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_JAVASCRIPT_FILE_UPLOAD));
        fileUpload.sendKeys(javascriptFile.getAbsolutePath());

        WebElement saveButton = driver.findElement(By.id(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_SAVE_BUTTON));
        saveButton.click();

        Alert alert = driver.switchTo().alert();
        String s = alert.getText();
        alert.accept();
        assertEquals(FlowComponentCreateViewImpl.FLOW_COMPONENT_CREATION_INPUT_FIELD_VALIDATION_ERROR, s);
    }

    
    @Ignore("Not completly implemented")
    @Test
    public void testFlowComponentCreation_allInputCorrect_dataBaseIsUpdated() throws IOException {
        File javascriptFile = createTemporaryJavascriptFile();
        navigateToFlowComponentCreationContext();

        WebElement componentName = driver.findElement(By.id(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_NAME_TEXT_BOX));
        componentName.sendKeys("testComponent");

        WebElement fileUpload = driver.findElement(By.id(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_JAVASCRIPT_FILE_UPLOAD));
        fileUpload.sendKeys(javascriptFile.getAbsolutePath());

        WebElement invocationMethod = driver.findElement(By.id(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_INVOCATION_METHOD_TEXT_BOX));
        invocationMethod.sendKeys("f");

        WebElement saveButton = driver.findElement(By.id(FlowComponentCreateViewImpl.GUIID_FLOW_COMPONENT_CREATION_SAVE_BUTTON));
        saveButton.click();
    }
    
    private void clearDbTables() throws SQLException {
//        PreparedStatement deleteStmt = conn.prepareStatement("DELETE FROM flows");
//        try {
//            deleteStmt.executeUpdate();
//        } finally {
//            deleteStmt.close();
//        }
    }

    private void navigateToFlowComponentCreationContext() {
        WebElement element = driver.findElement(By.id(MainPanel.GUIID_NAVIGATION_MENU_ITEM_FLOW_COMPONENT_CREATION));
        element.click();
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
}
