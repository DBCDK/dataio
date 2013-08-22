package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.gui.client.components.DualList;
import dk.dbc.dataio.gui.client.views.FlowCreateViewImpl;
import dk.dbc.dataio.gui.client.views.MainPanel;
import dk.dbc.dataio.integrationtest.ITUtil;
import java.io.IOException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class FlowCreationSeleniumIT {

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
        ITUtil.clearDbTables(conn, ITUtil.FLOWS_TABLE_NAME);
        FlowComponentCreationSeleniumIT.clearFlowComponentDBTable(conn);
        driver.quit();
    }

    @Test
    public void testInitialVisibililtyAndAccessabilityOfElements() throws Exception {
        testFlowCreationNavigationItemIsVisible();
        testFlowCreationNavigationItemIsClickable();
        testFlowCreationNameInputFieldIsVisible();
        testFlowCreationDescriptionInputFieldIsVisible();
        testFlowCreationSaveButtonIsVisible();
        testFlowCreationSaveResultLabelIsNotVisibleAndEmptyAsDefault();
    }
    
//    @Test
    public void testFlowCreationNavigationItemIsVisible() {
        WebElement element = driver.findElement(By.id(MainPanel.GUIID_NAVIGATION_MENU_ITEM_FLOW_CREATION));
        assertEquals(true, element.isDisplayed());
    }

//    @Test
    public void testFlowCreationNavigationItemIsClickable() throws Exception {
        navigateToFlowCreationContext();
        WebElement widget = driver.findElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_WIDGET));
        assertEquals(true, widget.isDisplayed());
    }

//    @Test
    public void testFlowCreationNameInputFieldIsVisible() {
        navigateToFlowCreationContext();
        WebElement element = driver.findElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_NAME_TEXT_BOX));
        assertEquals(true, element.isDisplayed());
    }

    @Test
    public void testFlowCreationNameInputField_InsertAndRead() {
        final String fieldValue = "test of unicode content æøåÆØÅ";

        navigateToFlowCreationContext();
        WebElement element = driver.findElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_NAME_TEXT_BOX));
        element.sendKeys(fieldValue);
        assertEquals(fieldValue, element.getAttribute("value"));
    }

//    @Test
    public void testFlowCreationDescriptionInputFieldIsVisible() {
        navigateToFlowCreationContext();
        WebElement element = driver.findElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_DESCRIPTION_TEXT_AREA));
        assertEquals(true, element.isDisplayed());
    }

    @Test
    public void testFlowCreationDescriptionInputField_InsertAndRead() {
        final String textWithMoreThan160Chars = "Dette er et stykke tekst som indeholder æøå og ÆØÅ. Formålet med teksten er hovedsagligt at være mere end 160 tegn lang, på en måde så der ikke er gentagelser i indholdet af teksten";
        final String sameTextWithExactly160Chars = textWithMoreThan160Chars.substring(0, 160);

        navigateToFlowCreationContext();
        WebElement element = driver.findElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_DESCRIPTION_TEXT_AREA));
        element.sendKeys(textWithMoreThan160Chars);
        assertEquals(sameTextWithExactly160Chars, element.getAttribute("value"));
    }

    @Test
    public void testFlowCreationFlowComponentSelectionFieldIsVisible() {
        navigateToFlowCreationContext();
        WebElement element = driver.findElement(By.id(FlowCreateViewImpl.FLOW_CREATE_FLOW_COMPONENT_SELECTION_PANEL_ID));
        assertEquals(true, element.isDisplayed());
    }

    
    
    @Ignore
    @Test
    public void testFlowCreationFlowComponentSelectionField_InsertAndRead() throws IOException, InterruptedException {
        FlowComponentCreationSeleniumIT.addFlowComponent(driver, tempFolder, "Componentname 1", "Script 1", "Invocation Method 1");
        FlowComponentCreationSeleniumIT.addFlowComponent(driver, tempFolder, "Componentname 2", "Script 2", "Invocation Method 2");
        FlowComponentCreationSeleniumIT.addFlowComponent(driver, tempFolder, "Componentname 3", "Script 3", "Invocation Method 3");

        navigateToFlowCreationContext();
        WebElement componentSelection = driver.findElement(By.id(FlowCreateViewImpl.FLOW_CREATE_FLOW_COMPONENT_SELECTION_PANEL_ID));
        WebElement leftPane = componentSelection.findElement(By.className(DualList.DUAL_LIST_LEFT_SELECTION_PANE_CLASS));
//        WebElement buttonLeft2Right = componentSelection.findElement(By.className(DualList.DUAL_LIST_SELECTION_BUTTONS_PANE_CLASS)).findElement(By.xpath("//*[button='>']"));
//        WebElement buttonRight2Left = componentSelection.findElement(By.className(DualList.DUAL_LIST_SELECTION_BUTTONS_PANE_CLASS)).findElement(By.xpath("//*[button='<']"));
        WebElement buttonLeft2Right = driver.findElement(By.xpath("//*[button='>']"));
        WebElement buttonRight2Left = driver.findElement(By.xpath("//*[button='<']"));
        WebElement rightPane = componentSelection.findElement(By.className(DualList.DUAL_LIST_RIGHT_SELECTION_PANE_CLASS));
        

        Thread.sleep(1000);
//        leftPane.findElement(By.tagName("OPTION")).click();
        WebElement kurt = driver.findElement(By.tagName("OPTION"));
        kurt.click();
        System.out.println("tagname " + kurt.getText());

        
//IWebElement dropDownListBox = driver.findElement(By.Id("selection"));
//SelectElement clickThis = new SelectElement(dropDownListBox);
//clickThis.SelectByText("Germany");



        Thread.sleep(1000);
        driver.findElement(By.xpath("//*[button='>']")).click();
        //buttonLeft2Right.click();
        System.out.println("SLF: " + buttonLeft2Right.getText());
        System.out.println("SLF class: " + buttonLeft2Right.getClass());

        Thread.sleep(1000);
        leftPane.findElement(By.tagName("OPTION")).click();
        
        Thread.sleep(1000);
        buttonLeft2Right.click();
        
        Thread.sleep(5000);
                
//        element.sendKeys(textWithMoreThan160Chars);
//        assertEquals(sameTextWithExactly160Chars, element.getAttribute("value"));
    }

    
    
    
    
    
    
    
    
//    @Test
    public void testFlowCreationSaveButtonIsVisible() {
        navigateToFlowCreationContext();
        WebElement element = driver.findElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_SAVE_BUTTON));
        assertEquals(true, element.isDisplayed());
    }

//    @Test
    public void testFlowCreationSaveResultLabelIsNotVisibleAndEmptyAsDefault() throws Exception {
        navigateToFlowCreationContext();
        WebElement element = driver.findElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_SAVE_RESULT_LABEL));
        assertEquals(false, element.isDisplayed());
        assertEquals("", element.getText());
    }

    @Test
    public void testFlowCreationSuccessfulSave_saveResultLabelContainsSuccessMessage() throws Exception {
        navigateToFlowCreationContext();
        insertTextInInputFieldsAndClickSaveButton();
        WebDriverWait wait = new WebDriverWait(driver, 4);
        wait.until(ExpectedConditions.textToBePresentInElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_SAVE_RESULT_LABEL), FlowCreateViewImpl.SAVE_RESULT_LABEL_SUCCES_MESSAGE));
        WebElement saveResultLabel = driver.findElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_SAVE_RESULT_LABEL));
        assertEquals(FlowCreateViewImpl.SAVE_RESULT_LABEL_SUCCES_MESSAGE, saveResultLabel.getText());
    }

    @Test
    public void testFlowCreationNameInputFieldUpdate_clearsSaveResultLabel() throws Exception {
        navigateToFlowCreationContext();
        insertTextInInputFieldsAndClickSaveButton();
        WebElement saveResultLabel = driver.findElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_SAVE_RESULT_LABEL));
        WebElement nameInputField = driver.findElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_NAME_TEXT_BOX));
        nameInputField.sendKeys("a");
        assertEquals("", saveResultLabel.getText());
    }

    @Test
    public void testFlowCreationDescriptionInputFieldUpdate_clearsSaveResultLabel() throws Exception {
        navigateToFlowCreationContext();
        insertTextInInputFieldsAndClickSaveButton();
        WebElement saveResultLabel = driver.findElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_SAVE_RESULT_LABEL));
        WebElement descriptionInputField = driver.findElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_DESCRIPTION_TEXT_AREA));
        descriptionInputField.sendKeys("b");
        assertEquals("", saveResultLabel.getText());
    }

    @Test
    public void testSaveButton_EmptyNameInputField_DisplayErrorPopup() {
        navigateToFlowCreationContext();
        WebElement nameInputField = driver.findElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_NAME_TEXT_BOX));
        nameInputField.sendKeys("a");
        WebElement saveButton = driver.findElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_SAVE_BUTTON));
        saveButton.click();
        Alert alert = driver.switchTo().alert();
        String s = alert.getText();
        alert.accept();
        assertEquals(FlowCreateViewImpl.FLOW_CREATION_INPUT_FIELD_VALIDATION_ERROR, s);
    }

    @Test
    public void testSaveButton_EmptyDescriptionInputField_DisplayErrorPopup() {
        navigateToFlowCreationContext();
        WebElement descriptionInputField = driver.findElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_DESCRIPTION_TEXT_AREA));
        descriptionInputField.sendKeys("b");
        WebElement saveButton = driver.findElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_SAVE_BUTTON));
        saveButton.click();
        Alert alert = driver.switchTo().alert();
        String s = alert.getText();
        alert.accept();
        assertEquals(FlowCreateViewImpl.FLOW_CREATION_INPUT_FIELD_VALIDATION_ERROR, s);
    }

    private void navigateToFlowCreationContext() {
        WebElement element = driver.findElement(By.id(MainPanel.GUIID_NAVIGATION_MENU_ITEM_FLOW_CREATION));
        element.click();
    }

    private void insertTextInInputFieldsAndClickSaveButton() {
        WebElement nameInputField = driver.findElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_NAME_TEXT_BOX));
        nameInputField.sendKeys("a");
        WebElement descriptionInputField = driver.findElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_DESCRIPTION_TEXT_AREA));
        descriptionInputField.sendKeys("b");
        WebElement saveButton = driver.findElement(By.id(FlowCreateViewImpl.GUIID_FLOW_CREATION_SAVE_BUTTON));
        saveButton.click();
    }
}
