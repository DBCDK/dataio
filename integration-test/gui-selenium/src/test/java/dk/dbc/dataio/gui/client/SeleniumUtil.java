package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.gui.client.components.DualList;
import java.util.List;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import static org.junit.Assert.assertTrue;
import org.openqa.selenium.Alert;
import org.openqa.selenium.support.ui.Select;

public class SeleniumUtil {

    public static WebElement findElementInCurrentView(WebDriver webDriver, final String elementId) {
        return webDriver.findElement(By.id(elementId));
    }

    public static void assertFieldIsVisbleAndDataCanBeInsertedAndRead(WebElement element) {
        assertTrue(element.isDisplayed());

        final String fieldValue = "test of unicode content æøåÆØÅ";
        element.sendKeys(fieldValue);
        assertThat(element.getAttribute("value"), is(fieldValue));
    }

    public static void assertFieldIsVisbleAndDataCanBeInsertedAndReadWithMaxSize(WebElement element, int maxSizeOfText) {
        final String testSubText = "æøå ÆØÅ ";

        assertTrue(element.isDisplayed());

        StringBuilder sb = new StringBuilder();
        // ensure to make text larger than what can be read.
        for (int i = 0; i < maxSizeOfText / testSubText.length() + 2; i++) {
            sb.append(testSubText);
        }
        String testText = sb.toString();
        assertTrue(testText.length() > maxSizeOfText);

        element.sendKeys(testText);
        assertThat(element.getAttribute("value"), is(testText.substring(0, maxSizeOfText)));
    }

    public static void assertListBoxIsVisibleAndAnElementCanBeSelected(WebDriver webDriver, WebElement listElement, String itemName) {
        assertTrue(listElement.isDisplayed());

        final Select list = new Select(listElement);
        assertTrue(list.getOptions().size() > 0);
        list.selectByVisibleText(itemName);
        assertThat(list.getFirstSelectedOption().getText(), is(itemName));
    }

    public static void assertDualListIsVisibleAndElementCanBeChosen(WebDriver webDriver, WebElement dualListElement, String itemName) {
        assertTrue(dualListElement.isDisplayed());

        WebElement buttonLeft2Right = dualListElement.findElement(By.cssSelector("." + DualList.DUAL_LIST_ADDITEM_CLASS + ""));
        Select list = new Select(dualListElement.findElement(By.tagName("select")));
        list.selectByIndex(0);
        buttonLeft2Right.click();

        List<WebElement> selectedItems = dualListElement.findElements(By.cssSelector("." + DualList.DUAL_LIST_RIGHT_SELECTION_PANE_CLASS + " option"));
        assertThat(selectedItems.get(0).getText(), is(itemName));
    }

    public static void selectItemInListBox(WebElement listBoxElement, String listItem) {
        final Select list = new Select(listBoxElement);
        list.selectByVisibleText(listItem);
    }

    public static void selectItemInDualList(WebElement dualListElement, String listItem) {
        Select list = new Select(dualListElement.findElement(By.tagName("select")));
        list.selectByVisibleText(listItem);
        dualListElement.findElement(By.cssSelector("." + DualList.DUAL_LIST_ADDITEM_CLASS + "")).click();
    }

    public static String getAlertStringAndAccept(WebDriver webDriver) {
        final Alert alert = webDriver.switchTo().alert();
        final String s = alert.getText();
        alert.accept();
        return s;
    }
}
