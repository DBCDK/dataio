package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.gui.client.pages.sinksshow.SinksShowViewImpl;
import dk.dbc.dataio.gui.util.ClientFactoryImpl;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class SinksShowSeleniumIT extends AbstractGuiSeleniumTest {
    final String SINK_NAME_1 = "NamoUno";
    final String SINK_NAME_2 = "NamoDuo";
    final String BUTTON_NAME = "Rediger";
    final String RESOURCE_NAME = "jdbc/flowStoreDb";

    @Test
    public void testSinksShowEmptyList_NoContentIsShown() throws TimeoutException, InterruptedException, Exception {
        navigateToSinksShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, SinksShowViewImpl.GUIID_SINKS_SHOW_WIDGET);
        table.waitAssertNoRows();
    }

    @Test
    public void testSinksShowInsertTwoRows_TwoElementsShown() {

        SinkCreationSeleniumIT.createTestSink(webDriver, SINK_NAME_1, RESOURCE_NAME);
        SinkCreationSeleniumIT.createTestSink(webDriver, SINK_NAME_2, RESOURCE_NAME);
        navigateToSinksShowWidget(webDriver);

        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, SinksShowViewImpl.GUIID_SINKS_SHOW_WIDGET);
        table.waitAssertRows(2);

        List<List<String>> rowData = table.get();
        assertNotNull(rowData);

        for (List <String> row : rowData){
            assertNotNull(row );
            assertTrue(row.size() == 3);
        }

        assertThat(rowData.get(0).get(0), is(SINK_NAME_2));
        assertThat(rowData.get(0).get(1), is(RESOURCE_NAME));
        assertThat(rowData.get(0).get(2), is(BUTTON_NAME));

        assertThat(rowData.get(1).get(0), is(SINK_NAME_1));
        assertThat(rowData.get(1).get(1), is(RESOURCE_NAME));
        assertThat(rowData.get(1).get(2), is(BUTTON_NAME));
    }

    @Test
    public void testSinksShowClickEditButton_NavigateToSinkCreationEditWidget(){

        //Create new sink
        SinkCreationSeleniumIT.createTestSink(webDriver, SINK_NAME_1, RESOURCE_NAME);

        //Navigate to the sink show window.
        navigateToSinksShowWidget(webDriver);

        //Navigate to the first row, locate the edit button and click.
        locateAndClickEditButtonForElement(0);

        //Assert that the SinkCreateEditView is opened.
        assertTrue(webDriver.getCurrentUrl().contains("#EditSink"));
    }

    /**
     * The following is public static helper methods.
     */
    public static void navigateToSinksShowWidget(WebDriver webDriver) {
        NavigationPanelSeleniumIT.navigateTo(webDriver, ClientFactoryImpl.GUIID_MENU_ITEM_SINKS_SHOW);
    }

    public static void locateAndClickEditButtonForElement(int index){
        WebElement element = SeleniumUtil.findElementInCurrentView(webDriver, SinksShowViewImpl.GUUID_SHOW_SINK_TABLE_EDIT, SinksShowViewImpl.CLASS_SINK_SHOW_WIDGET_EDIT_BUTTON, index);
        element.findElement(By.tagName("button")).click();
    }
}
