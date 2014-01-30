package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.gui.client.views.JobsShowViewImpl;
import dk.dbc.dataio.gui.client.views.Menu;
import java.util.List;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;

public class JobsShowSeleniumIT extends AbstractGuiSeleniumTest {
    @Ignore
    @Test
    public void testJobsShowEmptyList_NoContentIsShown() throws TimeoutException, Exception {
        navigateToJobsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        table.waitAssertNoRows();
    }

    @Ignore
    @Test
    public void testJobsInsertTwoRows_TwoElementsShown() {
        // Create two jobs
        navigateToJobsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        table.waitAssertRows(2);
        List<List<String>> rowData = table.get();
//        assertThat(rowData.get(0).get(0), is(...));
//        assertThat(rowData.get(0).get(1), is(...));
        // ...
//        assertThat(rowData.get(1).get(0), is(...));
//        assertThat(rowData.get(1).get(1), is(...));
        // ...
    }

    private static void navigateToJobsShowWidget(WebDriver webDriver) {
        NavigationPanelSeleniumIT.navigateTo(webDriver, Menu.GUIID_MAIN_MENU_ITEM_JOBS);
    }

}
