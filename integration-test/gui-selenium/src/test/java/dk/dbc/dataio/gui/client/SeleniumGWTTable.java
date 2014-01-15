package dk.dbc.dataio.gui.client;

import java.util.ArrayList;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

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
 *        <tbody style="display: none;"></tbody>            // The first tbody contains the table data (which is invisible)
 *        <tbody>                                           // The second tbody contains the no-data waiting animation
 *          <tr>
 *            <td ...>
 *              <div>
 *                <div ... style="... display: none;">
 *                  <div ... style="... display: none;"></div>
 *                </div>
 *                <div ...>
 *                  <div ...>
 *                    <img class="gwt-Image" .../>
 *                  </div>
 *                </div>
 *              </div>
 *            </td>
 *          </tr>
 *        </tbody>
 *        <tfoot ... style="display: none;"></tfoot>
 *      </table>
 *
 * 4) After data has been loaded to the table, the GWT table looks like this:
 *      <table>
 *        <thead ...>...</thead>
 *        <colgroup>...</colgroup>
 *        <tbody ...>                            // The first tbody contains the table data
 *          <tr ...>
 *            <td ...>...</td>
 *            <td ...>...</td>
 *            <td ...>...</td>
 *          </tr>
 *        </tbody>
 *        <tbody ... style="... display: none;"> // The second tbody contains the no-data waiting animation (which is invisible) - see also Note below
 *          <tr>
 *            <td ...>
 *              <div>
 *                <div ...>
 *                  <div ...></div>
 *                </div>
 *                <div ... style="... display: none;">  // See note below
 *                  <div ... style="... display: none;">  // See note below
 *                    <img class="gwt-Image" .../>
 *                  </div>
 *                </div>
 *              </div>
 *            </td>
 *          </tr>
 *        </tbody>
 *        <tfoot ... style="display: none;"></tfoot>
 *      </table>
 *   Note:
 *   Please note, that the invisiblity style code may occur in any combination. Experiments have
 *   shown, that sometimes, only the first one is present, sometimes only the last two... !!!
 *
 */


public class SeleniumGWTTable {
    private static final long DEFAULT_TIMEOUT = 6;  // seconds
    private WebDriver webDriver;
    private String guiId;
    private long timeout;
    private long lastRowFound = 0;
    private List<List<String>> tableData;

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
     * thrown, but if there is unexpected data in the table, an Exception is thrown.
     *
     * @throws TimeoutException
     * @throws Exception
     */
    public void waitAssertNoRows() throws Exception, TimeoutException {
         waitForTableDataSetup();  // First wait for the waiting animation to disappear
         if (getTableRowCount() != 0) {
             throw new Exception("Unexpected data in table");
         }
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
    public List<List<String>> get() {
        return tableData;
    }

    /**
     * Fetches one row of table data pointed out by a row index
     *
     * @param row The row index
     * @return The row data
     */
    public List<String> getRow(int row) {
         return tableData.get(row);
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
         List<String> currentRow = new ArrayList<>();
         for(WebElement tdElement: trElement.findElements(By.tagName("td"))) {
             String text = tdElement.findElement(By.tagName("div")).getText();
             currentRow.add(text);
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
        final String xpathTbodyInvisible = "//*[@id='" + guiId + "']/*/table/tbody[2][contains(@style,'display: none')]";
        final String xpathDivInvisible = "//*[@id='" + guiId + "']/*/table/tbody[2]/tr/td/*/div[contains(@style,'display: none')]/*/img[@class='gwt-Image']";
        final String xpathAnimationInvisible = "." + xpathTbodyInvisible + " | " + xpathDivInvisible;
        // Explanation: The function waits for a situation, where the table is no more waiting for data.
        // This is happening exactly at the moment, where the animation logo is no more displayed.
        // In other words, the function waits for the invisiblity of the animation logo
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xpathAnimationInvisible)));
    }
}
