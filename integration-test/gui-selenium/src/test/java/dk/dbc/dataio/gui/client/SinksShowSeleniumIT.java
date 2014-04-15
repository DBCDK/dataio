package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.gui.client.pages.sinksshow.SinksShowViewImpl;
import dk.dbc.dataio.gui.util.ClientFactoryImpl;
import org.junit.Test;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class SinksShowSeleniumIT extends AbstractGuiSeleniumTest {
    final String RESOURCE_NAME = "jdbc/flowStoreDb";

    @Test
    public void testSinksShowEmptyList_NoContentIsShown() throws TimeoutException, InterruptedException, Exception {
        navigateToSinksShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, SinksShowViewImpl.GUIID_SINKS_SHOW_WIDGET);
        table.waitAssertNoRows();
    }

    @Test
    public void testSinksInsertTwoRows_TwoElementsShown() {
        final String SINK_NAME_1 = "NamoUno";
        final String SINK_NAME_2 = "NamoDuo";
        final String BUTTON_NAME = "Rediger";
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

    private static void navigateToSinksShowWidget(WebDriver webDriver) {
        NavigationPanelSeleniumIT.navigateTo(webDriver, ClientFactoryImpl.GUIID_MENU_ITEM_SINKS_SHOW);
    }

}
