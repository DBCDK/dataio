package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.gui.client.pages.submittersshow.SubmittersShowViewImpl;
import dk.dbc.dataio.gui.util.ClientFactoryImpl;
import java.util.List;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;

public class SubmittersShowSeleniumIT extends AbstractGuiSeleniumTest {
    @Test
    public void testSubmittersShowEmptyList_NoContentIsShown() throws TimeoutException, InterruptedException, Exception {
        navigateToSubmittersShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, SubmittersShowViewImpl.GUIID_SUBMITTERS_SHOW_WIDGET);
        table.waitAssertNoRows();
    }

    @Test
    public void testSubmittersInsertOneRow_OneElementShown() {
        final String SUBMITTER_NUMBER = "111";
        final String SUBMITTER_NAME = "Submitter-name";
        final String SUBMITTER_DESCRIPTION = "Submitter-description";
        SubmitterCreationSeleniumIT.createTestSubmitter(webDriver, SUBMITTER_NAME, SUBMITTER_NUMBER, SUBMITTER_DESCRIPTION);
        navigateToSubmittersShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, SubmittersShowViewImpl.GUIID_SUBMITTERS_SHOW_WIDGET);
        table.waitAssertRows(1);
        List<String> rowData = table.getRow(0);
        assertThat(rowData.get(0), is(SUBMITTER_NUMBER));
        assertThat(rowData.get(1), is(SUBMITTER_NAME));
        assertThat(rowData.get(2), is(SUBMITTER_DESCRIPTION));
    }

    @Test
    public void testSubmittersInsertTwoRows_TwoElementsShown() {
        final String SUBMITTER_NUMBER_1 = "1111";
        final String SUBMITTER_NAME_1 = "NamoUno";
        final String SUBMITTER_DESCRIPTION_1 = "DesiUno";
        final String SUBMITTER_NUMBER_2 = "2222";
        final String SUBMITTER_NAME_2 = "NamoDuo";
        final String SUBMITTER_DESCRIPTION_2 = "DesiDuo";
        SubmitterCreationSeleniumIT.createTestSubmitter(webDriver, SUBMITTER_NAME_1, SUBMITTER_NUMBER_1, SUBMITTER_DESCRIPTION_1);
        SubmitterCreationSeleniumIT.createTestSubmitter(webDriver, SUBMITTER_NAME_2, SUBMITTER_NUMBER_2, SUBMITTER_DESCRIPTION_2);
        navigateToSubmittersShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, SubmittersShowViewImpl.GUIID_SUBMITTERS_SHOW_WIDGET);
        table.waitAssertRows(2);
        List<List<String>> rowData = table.get();
        assertThat(rowData.get(0).get(0), is(SUBMITTER_NUMBER_1));
        assertThat(rowData.get(0).get(1), is(SUBMITTER_NAME_1));
        assertThat(rowData.get(0).get(2), is(SUBMITTER_DESCRIPTION_1));
        assertThat(rowData.get(1).get(0), is(SUBMITTER_NUMBER_2));
        assertThat(rowData.get(1).get(1), is(SUBMITTER_NAME_2));
        assertThat(rowData.get(1).get(2), is(SUBMITTER_DESCRIPTION_2));
    }

    private static void navigateToSubmittersShowWidget(WebDriver webDriver) {
        NavigationPanelSeleniumIT.navigateTo(webDriver, ClientFactoryImpl.GUIID_MENU_ITEM_SUBMITTERS_SHOW);
    }

}
