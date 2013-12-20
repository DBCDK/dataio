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
 * 2) A GWT table contains the html tags:
 *      <table>
 *        <tbody ...>...</tbody>
 *        <tbody ...>...</tbody>
 *      </table>
 * 3) There are two tbody tags within the table tag (one with data and one with a waiting animation)
 * 4) The animation tbody looks like this (both when there is data, and when there is no data):
 *      <tbody ...>
 *        <tr>                                // Note a: No attributes set at all
 *          <td ...><div>...</div></td>
 *        </tr>
 *      </tbody>
 * 5) When there is no data in the table, the data tbody looks like this:
 *      <tbody ...></tbody>                   // Note b: The tbody is empty
 * 6) When there is data in the table, the data tbody looks like this:
 *     <tbody ...>
 *       <tr class="..." ...>                 // Note c: Here the class attribute is set to something
 *         <td ...><div ...>...</div></td>    // The div contains the cell data
 *         <td ...><div ...>...</div></td>    // The div contains the cell data
 *         ...
 *       </tr>
 *       <tr class="..." ...>
 *         <td ...><div ...>...</div></td>    // The div contains the cell data
 *         <td ...><div ...>...</div></td>    // The div contains the cell data
 *         ...
 *       </tr>
 *       ...
 *     </tbody>
 *
 */

public class SeleniumGWTTable {
    private static final long DEFAULT_TIMEOUT = 1;  // seconds
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
     * Waits for the next table row to be inserted.
     *
     * If not inserted within the timeout period, the method throws a TimeoutException.
     * If a row is being inserted within the timeout period, the data is ready to be fetched with the methods get() or getRow().
     *
     * @throws TimeoutException
     */
     void waitAssertRows() throws TimeoutException {
         WebElement trElement = waitAndFindNextTrTagContainingData();
         List<String> currentRow = new ArrayList();
         for(WebElement tdElement: trElement.findElements(By.tagName("td"))) {
             String text = tdElement.findElement(By.tagName("div")).getText();
             currentRow.add(text);
         }
         tableData.add(currentRow);
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
    void waitAssertRows(long count) throws TimeoutException {
        for (long i=0; i<count; i++) {
            waitAssertRows();
        }
    }

    /**
     * Fetches the complete table data, fetched with the corresponding waitXXX methods
     *
     * @return The complete table data as fetched with the corresponding waitXXX methods
     */
     List<List<String>> get() {
        return tableData;
    }

    /**
     * Fetches one row of table data pointed out by a row index
     *
     * @param row The row index
     * @return The row data
     */
     List<String> getRow(int row) {
         return tableData.get(row);
     }


    // Private methods

    private WebElement waitAndFindNextTrTagContainingData() throws TimeoutException {
        WebDriverWait wait = new WebDriverWait(webDriver, timeout);
        final String xpathSelector = ".//*[@id='" + guiId + "']//table/tbody/tr[@class][" + ++lastRowFound + "]";
        // Explanation: According to Note c in Assumption 6, a valid row with data is identified by a tr, where class is set to 'something'
        // Sample xpathSelector: ".//*[@id='flowcomponentsshowwidget']/table/tbody/tr[@class][1]"
        //  - finds the first (number 1) table row (tr) as a WebElement under the tree defined by the guiId: 'flowcomponentsshowwidget'
        return wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xpathSelector)));
    }

}
