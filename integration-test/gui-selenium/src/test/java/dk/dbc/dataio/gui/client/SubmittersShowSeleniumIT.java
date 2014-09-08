package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.SubmitterContent;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.test.model.SubmitterContentBuilder;
import dk.dbc.dataio.gui.client.pages.submitter.submittersshow.SubmittersShowViewImpl;
import dk.dbc.dataio.gui.util.ClientFactoryImpl;
import dk.dbc.dataio.integrationtest.ITUtil;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;

import javax.ws.rs.client.Client;
import java.sql.SQLException;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SubmittersShowSeleniumIT extends AbstractGuiSeleniumTest {

    private static FlowStoreServiceConnector flowStoreServiceConnector;

    @BeforeClass
    public static void setUpClass() throws ClassNotFoundException, SQLException {
        String baseUrl = ITUtil.FLOW_STORE_BASE_URL;
        Client restClient = HttpClient.newClient();
        flowStoreServiceConnector = new FlowStoreServiceConnector(restClient, baseUrl);
    }

    @Test
    public void testSubmittersShowEmptyList_NoContentIsShown() throws TimeoutException, InterruptedException, Exception {
        navigateToSubmittersShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, SubmittersShowViewImpl.GUIID_SUBMITTERS_SHOW_WIDGET);
        table.waitAssertNoRows();
    }

    @Test
    public void testSubmittersInsertOneRow_OneElementShown() throws Exception{
        Submitter submitter = createTestSubmitter(111L, "Submitter-name", "Submitter-description");
        navigateToSubmittersShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, SubmittersShowViewImpl.GUIID_SUBMITTERS_SHOW_WIDGET);
        table.waitAssertRows(1);
        List<SeleniumGWTTable.Cell> rowData = table.getRow(0);
        assertThat(rowData.get(0).getCellContent(), is((Long.toString(submitter.getContent().getNumber()))));
        assertThat(rowData.get(1).getCellContent(), is(submitter.getContent().getName()));
        assertThat(rowData.get(2).getCellContent(), is(submitter.getContent().getDescription()));
    }

    @Test
    public void testSubmittersInsertTwoRows_TwoElementsShown() throws Exception{
        Submitter submitterA = createTestSubmitter(111L, "NamoUno", "DesiUno");
        Submitter submitterB = createTestSubmitter(2222L, "NamoDuo", "DesiDuo");

        navigateToSubmittersShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, SubmittersShowViewImpl.GUIID_SUBMITTERS_SHOW_WIDGET);
        table.waitAssertRows(2);
        List<List<SeleniumGWTTable.Cell>> rowData = table.get();
        assertThat(rowData.get(0).get(0).getCellContent(), is(Long.toString(submitterA.getContent().getNumber())));
        assertThat(rowData.get(0).get(1).getCellContent(), is(submitterA.getContent().getName()));
        assertThat(rowData.get(0).get(2).getCellContent(), is(submitterA.getContent().getDescription()));
        assertThat(rowData.get(1).get(0).getCellContent(), is(Long.toString(submitterB.getContent().getNumber())));
        assertThat(rowData.get(1).get(1).getCellContent(), is(submitterB.getContent().getName()));
        assertThat(rowData.get(1).get(2).getCellContent(), is(submitterB.getContent().getDescription()));
    }

    /**
     * The following is private static helper methods.
     */
    private static Submitter createTestSubmitter(Long  submitterNumber, String submitterName , String submitterDescription) throws Exception{
        SubmitterContent submitterContent = new SubmitterContentBuilder()
                .setName(submitterName)
                .setNumber(submitterNumber)
                .setDescription(submitterDescription)
                .build();

        return flowStoreServiceConnector.createSubmitter(submitterContent);
    }

    private static void navigateToSubmittersShowWidget(WebDriver webDriver) {
        NavigationPanelSeleniumIT.navigateTo(webDriver, ClientFactoryImpl.GUIID_MENU_ITEM_SUBMITTERS_SHOW);
    }

}
