package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.gui.client.components.DioCellTable;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 *
 * This class eases Selenium test of GWT Tables, by offering wait, assert and get data.
 *
 * Basic assumptions:
 *
 * 1) The class assumes, that when data is added to a GWT table, complete rows are added at a time.
 *    It is therefore assumed, that whenever one cell in a row is present, all other cells in the
 *    row are also present
 *
 * 2) A GWT table contains the html tags:
 *      <table>
 *        <thead ...>...</thead>
 *        <colgroup>...</colgroup>
 *        <tbody ...>...</tbody>                            // The first tbody contains the table data
 *        <tbody ...>...</tbody>                            // The second tbody contains the no-data waiting animation
 *        <tfoot ...></tfoot>
 *      </table>
 *
 * 3) Before data has been loaded to the table, the GWT table looks like this:
 *      <table ...>
 *        <thead ...>...</thead>
 *        <colgroup>...</colgroup>
 *        <tbody style="display: none;"></tbody>            // The first tbody contains the table data (which is invisible and empty)
 *        <tbody>...</tbody>
 *        <tfoot ...></tfoot>
 *      </table>
 *
 * 4) After data has been loaded to the table, the GWT table looks like this:
 *      <table class="... dio-celltable-update-done">       // The class name dio-celltable-update-done has been added here
 *        <thead ...>...</thead>
 *        <colgroup>...</colgroup>
 *        <tbody ...>               // The first tbody contains the table data
 *          <tr ...>                // First row
 *            <td ...>...</td>      // First cell
 *            <td ...>...</td>      // Second cell
 *            <td ...>...</td>
 *          </tr>
 *          <tr>...</tr>            // Second row
 *          ...
 *        </tbody>
 *        <tbody ...>...</tbody>
 *        <tfoot ...></tfoot>
 *      </table>
 *
 */


public class SeleniumGWTTable {
    private static final long DEFAULT_TIMEOUT = 6;  // seconds
    private WebDriver webDriver;
    private String guiId;
    private long timeout;
    private long lastRowFound = 0;
    private List<List<Cell>> tableData;

    class Cell {
        private String cellContent;
        private String className;

        private Cell(String cellContent, String className) {
            this.cellContent = cellContent;
            this.className = className.toLowerCase();
        }

        @Override
        public int hashCode() {
            return 0;
        }

        String getCellContent() {
            return cellContent;
        }

        String getClassName() {
            return className;
        }

        boolean hasClassName(String className) {
            return Arrays.asList(this.className.split(" ")).contains(className.toLowerCase());  // Tests whether className is contained in this.className
        }
    }

    /**
     * Constructor
     *
     * @param webDriver The webdriver to use as the root
     * @param guiId The guiId for the html tag, that contains the table
     * @param timeout The timeout value to be used when waiting for data in seconds
     *
     * @throws TimeoutException
     */
    SeleniumGWTTable(WebDriver webDriver, String guiId, long timeout) throws TimeoutException {
        this.webDriver = webDriver;
        this.guiId = guiId;
        this.timeout = timeout;
        this.tableData = new ArrayList();
    }

    /**
     * Constructor
     *
     * @param webDriver The webdriver to use as the root
     * @param guiId The guiId for the html tag, that contains the table
     *
     * @throws TimeoutException
     */
    SeleniumGWTTable(WebDriver webDriver, String guiId) throws TimeoutException {
        this(webDriver, guiId, DEFAULT_TIMEOUT);
    }

    /**
     * Waits and asserts, that the table is empty (and is not in the process of being filled up).
     * If the table is never filled up (the animation logo never disappears) a TimeoutException is
     * thrown.
     *
     * @throws TimeoutException
     */
    public void waitAssertNoRows() throws TimeoutException {
         waitForTableDataSetup();  // First wait for the waiting animation to disappear
         assertThat("Unexpected data in table", getTableRowCount(), is(0));
     }

    /**
     * Waits for the next table row to be inserted.
     *
     * If not inserted within the timeout period, the method throws a TimeoutException.
     * If a row is being inserted within the timeout period, the data is ready to be fetched with the methods get() or getRow().
     *
     * @throws TimeoutException
     */
    public void waitAssertRows() throws TimeoutException {
         waitForTableDataSetup();  // First wait for the waiting animation to disappear
         waitAssertOneRow();
     }

    /**
     * Waits for the next number of table rows to be inserted.
     *
     * If not inserted within the timeout period, the method throws a TimeoutException.
     * If the rows are being inserted within the timeout period, the data is ready to be fetched with the methods get() or getRow().
     *
     * @param count The number of rows to wait for
     *
     * @throws TimeoutException
     */
    public void waitAssertRows(long count) throws TimeoutException {
        waitForTableDataSetup();  // First wait for the waiting animation to disappear
        for (long i=0; i<count; i++) {
            waitAssertOneRow();
        }
    }

    /**
     * Gets the number of rows in the table
     *
     * @return Number of rows in the table
     */
    public int getSize() {
        return getTableRowCount();
    }

    /**
     * Fetches the complete table data, fetched with the corresponding waitXXX methods
     *
     * @return The complete table data as fetched with the corresponding waitXXX methods
     */
    public List<List<Cell>> get() {
        return tableData;
    }

    /**
     *
     * Strips any Html tags from the String given as input.
     *
     * @param htmlString
     * @return The String object without html tags.
     */
    private String stripHtml(String htmlString){
        return htmlString.replaceAll("\\<.*?>","");
    }


    /**
     * Fetches one row of table data pointed out by a row index
     *
     * @param row The row index
     * @return The row data
     */
    public List<Cell> getRow(int row) {
         return tableData.get(row);
     }

    /**
     * Fetches a Column Header as a WebElement (pointed out by the index passes as a parameter in the call)
     *
     * @param index Which column header to fetch (zero based index)
     * @return The column header
     */
    public WebElement findColumnHeader(int index) {
        final String xpathSelector = ".//*[@id='" + guiId + "']/*/table/thead/tr/th";
        return webDriver.findElements(By.xpath(xpathSelector)).get(index);
    }

    // Private methods

    /**
     * Gets the number of rows in the table
     *
     * @return Number of rows in the table
     */
    private int getTableRowCount() {
         final String xpathSelector = ".//*[@id='" + guiId + "']/*/table/tbody[1]/tr";
         List<WebElement> elements = webDriver.findElements(By.xpath(xpathSelector));
         return elements.size();
     }

    /**
     * Waits for the next table row to be inserted.
     *
     * If not inserted within the timeout period, the method throws a TimeoutException.
     * If a row is being inserted within the timeout period, the data is ready to be fetched with the methods get() or getRow().
     *
     * @throws TimeoutException
     */
    private void waitAssertOneRow() throws TimeoutException {
         WebElement trElement = waitAndFindNextTrTagContainingData();
         List<Cell> currentRow = new ArrayList<>();
         for(WebElement tdElement: trElement.findElements(By.tagName("td"))) {
             Cell tableCell = new Cell(stripHtml(SeleniumUtil.getCoveredText(tdElement.findElement(By.tagName("div")))),
                     tdElement.getAttribute("class"));
             currentRow.add(tableCell);
         }
         tableData.add(currentRow);
    }

    /**
     * Waits for the next valid data TR to appear, and returns the WebElement for it
     * @return WebElement for the data detected
     * @throws TimeoutException if no data is detected
     */
    private WebElement waitAndFindNextTrTagContainingData() throws TimeoutException {
        WebDriverWait wait = new WebDriverWait(webDriver, timeout);
        final String xpathSelector = ".//*[@id='" + guiId + "']/*/table/tbody[1]/tr[" + ++lastRowFound + "]";
        // Explanation:
        // Sample xpathSelector: ".//*[@id='flowcomponentsshowwidget']/table/tbody[1]/tr[1]"
        //  - finds the first (number 1) table row (tr) as a WebElement under the tree defined by the guiId: 'flowcomponentsshowwidget'
        return wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xpathSelector)));
    }

    /**
     * Waits for the initial data to be inserted in the table (whether there is data to display or not)
     * Technically speaken, the function waits for the "waiting-for-data" animation to disappear.
     * @throws TimeoutException if no data is detected
     */
    private void waitForTableDataSetup() throws TimeoutException {
        WebDriverWait wait = new WebDriverWait(webDriver, timeout);
        final String xpathUpdateDone = ".//*[@id='" + guiId + "']/*/table[contains(@class,'" + DioCellTable.DIO_CELLTABLE_UPDATE_DONE + "')]";
        // Explanation: The function waits for a situation, where the table is no more waiting for data.
        // This is happening when the activity signals an updateDone() to the DioCellTable component
        // - resulting in the class DIO_CELLTABLE_UPDATE_DONE to be added to the table class
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xpathUpdateDone)));
    }
}
