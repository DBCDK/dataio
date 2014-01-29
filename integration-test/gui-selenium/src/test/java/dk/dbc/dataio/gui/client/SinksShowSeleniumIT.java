package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.gui.client.views.Menu;
import dk.dbc.dataio.gui.client.views.SinksShowViewImpl;
import java.util.List;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;

public class SinksShowSeleniumIT extends AbstractGuiSeleniumTest {
    final String RESOURCE_NAME = "jdbc/flowStoreDb";

    @Test
    public void testSinksShowEmptyList_NoContentIsShown() throws TimeoutException, InterruptedException, Exception {
        navigateToSinksShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, SinksShowViewImpl.GUIID_SINKS_SHOW_WIDGET);
        table.waitAssertNoRows();
    }

    @Test
    public void testSinksInsertOneRow_OneElementShown() {
        final String SINK_NAME = "Sink-name";
        SinkCreationSeleniumIT.createTestSink(webDriver, SINK_NAME, RESOURCE_NAME);
        navigateToSinksShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, SinksShowViewImpl.GUIID_SINKS_SHOW_WIDGET);
        table.waitAssertRows(1);
        List<String> rowData = table.getRow(0);
        assertThat(rowData.get(0), is(SINK_NAME));
        assertThat(rowData.get(1), is(RESOURCE_NAME));
    }

    @Test
    public void testSinksInsertTwoRows_TwoElementsShown() {
        final String SINK_NAME_1 = "NamoUno";
        final String SINK_NAME_2 = "NamoDuo";
        SinkCreationSeleniumIT.createTestSink(webDriver, SINK_NAME_1, RESOURCE_NAME);
        SinkCreationSeleniumIT.createTestSink(webDriver, SINK_NAME_2, RESOURCE_NAME);
        navigateToSinksShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, SinksShowViewImpl.GUIID_SINKS_SHOW_WIDGET);
        table.waitAssertRows(2);
        List<List<String>> rowData = table.get();
        assertThat(rowData.get(0).get(0), is(SINK_NAME_2));
        assertThat(rowData.get(0).get(1), is(RESOURCE_NAME));
        assertThat(rowData.get(1).get(0), is(SINK_NAME_1));
        assertThat(rowData.get(1).get(1), is(RESOURCE_NAME));
    }

    private static void navigateToSinksShowWidget(WebDriver webDriver) {
        NavigationPanelSeleniumIT.navigateTo(webDriver, Menu.GUIID_MAIN_MENU_ITEM_SINKS);
    }

}
