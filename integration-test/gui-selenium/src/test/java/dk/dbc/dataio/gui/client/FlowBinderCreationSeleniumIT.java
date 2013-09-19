package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.gui.client.components.DualList;
import dk.dbc.dataio.gui.client.views.FlowbinderCreateViewImpl;
import dk.dbc.dataio.gui.client.views.MainPanel;
import dk.dbc.dataio.integrationtest.ITUtil;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import static org.hamcrest.CoreMatchers.is;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.Select;

public class FlowBinderCreationSeleniumIT {

    private static WebDriver driver;
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
        //ITUtil.clearDbTables(conn, ITUtil.FLOW_BINDERS_TABLE_NAME, ITUtil.FLOW_BINDERS_SEARCH_INDEX_TABLE_NAME, ITUtil.FLOW_BINDERS_SUBMITTER_JOIN_TABLE_NAME);
        driver.quit();
    }

    @Test
    public void testInitialVisibilityAndAccessabilityOfElements() throws IOException {
        testFlowBinderCreationNavigationItemIsVisibleAndClickable();
        //testFlowbinderCreationNameInputFieldIsVisibleAndDataCanBeInsertedAndRead();
        testFlowbinderCreationDescriptionInputFieldIsVisibleAndDataCanBeInsertedAndRead();
        testFlowbinderCreationFrameInputFieldIsVisibleAndDataCanBeInsertedAndRead();
        testFlowbinderCreationContentFormatInputFieldIsVisibleAndDataCanBeInsertedAndRead();
        testFlowbinderCreationCharacterSetInputFieldIsVisibleAndDataCanBeInsertedAndRead();
        testFlowbinderCreationSinkInputFieldIsVisibleAndDataCanBeInsertedAndRead();
    }

    public void testFlowBinderCreationNavigationItemIsVisibleAndClickable() {
        WebElement element = driver.findElement(By.id(MainPanel.GUIID_NAVIGATION_MENU_ITEM_FLOWBINDER_CREATION));
        assertEquals(true, element.isDisplayed());
        element.click();

        WebElement widget = driver.findElement(By.id(FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_WIDGET));
        assertEquals(true, widget.isDisplayed());
    }

    @Test
    public void testFlowbinderCreationNameInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToFlowbinderCreationContext();
        assertFieldIsVisbleAndDataCanBeInsertedAndReadWithMaxSize(findNameTextElement(), 160);
    }

    public void testFlowbinderCreationDescriptionInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToFlowbinderCreationContext();
        assertFieldIsVisbleAndDataCanBeInsertedAndReadWithMaxSize(findDescriptionTextElement(), 160);
    }

    public void testFlowbinderCreationFrameInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToFlowbinderCreationContext();
        assertFieldIsVisbleAndDataCanBeInsertedAndRead(findFrameTextElement());
    }

    public void testFlowbinderCreationContentFormatInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToFlowbinderCreationContext();
        assertFieldIsVisbleAndDataCanBeInsertedAndRead(findContentFormatTextElement());
    }

    public void testFlowbinderCreationCharacterSetInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToFlowbinderCreationContext();
        assertFieldIsVisbleAndDataCanBeInsertedAndRead(findCharacterSetTextElement());
    }

    public void testFlowbinderCreationSinkInputFieldIsVisibleAndDataCanBeInsertedAndRead() {
        navigateToFlowbinderCreationContext();
        assertFieldIsVisbleAndDataCanBeInsertedAndRead(findSinkTextElement());
    }

    @Test
    public void testFlowbinderCreationSubmitterDualListIsVisibleAndAnElementCanBeChosen() {
        String submitterName = "submitter1";
        SubmitterCreationSeleniumIT.createTestSubmitter(driver, submitterName, "123456", "Description");
        navigateToFlowbinderCreationContext();
        assertDualListIsVisibleAndElementCanBeChosen(driver, findSubmitterPanelElement(), submitterName);
    }

    @Ignore
    @Test
    public void test() {
        FlowCreationSeleniumIT.createTestFlow(driver, "name", "description", "flowComponent1");

        navigateToFlowbinderCreationContext();

        try {
            Thread.sleep(10000);
        } catch (InterruptedException ex) {
        }
}

    // done:
    // test recordsplitter text box : visibility/read/NOT write
    // test submitter panel : visibility/select
    //
    //
    // todo:
    // test flow list box : visibility/select
    // test save button : visibility/clickable
    // test save result label : visibility after click with success content
    // test save result label : initially NOT visible
    // test popup error : missing name
    // test popup error : missing description
    // test popup error : missing frame
    // test popup error : missing content format
    // test popup error : missing character set
    // test popup error : missing sink
    // test popup error : missing flow
    // test popup error : missing submitter
    // test removal of success status label after save : name changed
    // test removal of success status label after save : description changed
    // test removal of success status label after save : frame changed
    // test removal of success status label after save : content format changed
    // test removal of success status label after save : character set changed
    // test removal of success status label after save : sink changed
    // test removal of success status label after save : flow changed
    // test removal of success status label after save : submitter changed
    //
    //
    /**
     * The following is private helper methods
     */
    private void navigateToFlowbinderCreationContext() {
        WebElement element = driver.findElement(By.id(MainPanel.GUIID_NAVIGATION_MENU_ITEM_FLOWBINDER_CREATION));
        element.click();
    }

    private WebElement findNameTextElement() {
        return findElementInCurrentView(driver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_NAME_TEXT_BOX);
    }

    private WebElement findDescriptionTextElement() {
        return findElementInCurrentView(driver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_DESCRIPTION_TEXT_AREA);
    }

    private WebElement findFrameTextElement() {
        return findElementInCurrentView(driver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_FRAME_TEXT_BOX);
    }

    private WebElement findContentFormatTextElement() {
        return findElementInCurrentView(driver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_CONTENTFORMAT_TEXT_BOX);
    }

    private WebElement findCharacterSetTextElement() {
        return findElementInCurrentView(driver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_CHARACTER_SET_TEXT_BOX);
    }

    private WebElement findSinkTextElement() {
        return findElementInCurrentView(driver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_SINK_TEXT_BOX);
    }

    private WebElement findRecordSplitterTextElement() {
        return findElementInCurrentView(driver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_RECORD_SPLITTER_TEXT_BOX);
    }

    private WebElement findSubmitterPanelElement() {
        return findElementInCurrentView(driver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_SUBMITTERS_SELECTION_PANEL);
    }

    private WebElement findFlowListElement() {
        return findElementInCurrentView(driver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_FLOW_LIST_BOX);
    }

    private WebElement findSaveButtonElement() {
        return findElementInCurrentView(driver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_SAVE_BUTTON);
    }

    private WebElement findSaveResultLabelElement() {
        return findElementInCurrentView(driver, FlowbinderCreateViewImpl.GUIID_FLOWBINDER_CREATION_SAVE_RESULT_LABEL);
    }

    /**
     * The following is static public helper methods - they should probably be
     * moved to a helper-class.
     */
    public static WebElement findElementInCurrentView(WebDriver webDriver, final String elementId) {
        return webDriver.findElement(By.id(elementId));
    }

    public static void assertFieldIsVisbleAndDataCanBeInsertedAndRead(WebElement element) {
        assertEquals(true, element.isDisplayed());

        final String fieldValue = "test of unicode content æøåÆØÅ";
        element.sendKeys(fieldValue);
        assertEquals(fieldValue, element.getAttribute("value"));
    }

    public static void assertFieldIsVisbleAndDataCanBeInsertedAndReadWithMaxSize(WebElement element, int maxSizeOfText) {
        final String testSubText = "æøå ÆØÅ ";

        assertEquals(true, element.isDisplayed());

        StringBuilder sb = new StringBuilder();
        // ensure to make text larger than what can be read.
        for (int i = 0; i < maxSizeOfText / testSubText.length() + 2; i++) {
            sb.append(testSubText);
        }
        String testText = sb.toString();
        assertThat(testText.length() > maxSizeOfText, is(true));

        element.sendKeys(testText);
        assertThat(element.getAttribute("value"), is(testText.substring(0, maxSizeOfText)));
    }

    public static void assertDualListIsVisibleAndElementCanBeChosen(WebDriver webDriver, WebElement dualListElement, String submitterName) {
        assertThat(dualListElement.isDisplayed(), is(true));

        WebElement buttonLeft2Right = dualListElement.findElement(By.cssSelector("." + DualList.DUAL_LIST_ADDITEM_CLASS + ""));
        Select list = new Select(dualListElement.findElement(By.tagName("select")));
        list.selectByIndex(0);
        buttonLeft2Right.click();

        List<WebElement> selectedItems = dualListElement.findElements(By.cssSelector("." + DualList.DUAL_LIST_RIGHT_SELECTION_PANE_CLASS + " option"));
        assertThat(selectedItems.get(0).getText(), is(submitterName));
    }
}
