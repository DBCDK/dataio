package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.gui.client.views.Menu;
import dk.dbc.dataio.gui.client.views.NavigationPanel;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static dk.dbc.dataio.gui.client.SeleniumUtil.findElementInCurrentView;
import static org.junit.Assert.assertTrue;

public class NavigationPanelSeleniumIT extends AbstractGuiSeleniumTest {
    private static ConstantsProperties texts = new ConstantsProperties("MenuConstants_dk.properties");

    @Test
    public void testNavigationMenuPanelVisible() {
        WebElement navigationPanelElement = findNavigationPanelElement(webDriver);
        assertTrue(navigationPanelElement.isDisplayed());
    }

    @Test
    public void testMainMenuItemsVisible() {
        assertTrue(findNavigationElement(webDriver, Menu.GUIID_MAIN_MENU_ITEM_SUBMITTERS).isDisplayed());
        assertTrue(findNavigationElement(webDriver, Menu.GUIID_MAIN_MENU_ITEM_FLOWS).isDisplayed());
        assertTrue(findNavigationElement(webDriver, Menu.GUIID_MAIN_MENU_ITEM_SINKS).isDisplayed());
    }

    @Test
    public void testSubmitterMenuItemsVisible() {
        findNavigationElement(webDriver, Menu.GUIID_MAIN_MENU_ITEM_SUBMITTERS).click();
        assertTrue(findNavigationElement(webDriver, Menu.GUIID_SUB_MENU_ITEM_SUBMITTER_CREATION).isDisplayed());
    }

    @Test
    public void testFlowsMenuItemsVisible() {
        findNavigationElement(webDriver, Menu.GUIID_MAIN_MENU_ITEM_FLOWS).click();
        assertTrue(findNavigationElement(webDriver, Menu.GUIID_SUB_MENU_ITEM_FLOW_CREATION).isDisplayed());
        assertTrue(findNavigationElement(webDriver, Menu.GUIID_SUB_MENU_ITEM_FLOW_COMPONENT_CREATION).isDisplayed());
        assertTrue(findNavigationElement(webDriver, Menu.GUIID_SUB_MENU_ITEM_FLOW_COMPONENTS_SHOW).isDisplayed());
        assertTrue(findNavigationElement(webDriver, Menu.GUIID_SUB_MENU_ITEM_FLOWBINDER_CREATION).isDisplayed());
    }

    @Test
    public void testSinksMenuItemsVisible() {
        findNavigationElement(webDriver, Menu.GUIID_MAIN_MENU_ITEM_SINKS).click();
        assertTrue(findNavigationElement(webDriver, Menu.GUIID_SUB_MENU_ITEM_SINK_CREATION).isDisplayed());
    }


    // Utility methods
    private static WebElement findNavigationPanelElement(WebDriver webDriver) {
        return findElementInCurrentView(webDriver, NavigationPanel.GUIID_NAVIGATION_MENU_PANEL);
    }

    private static WebElement findNavigationElement(WebDriver webDriver, String menuId) {
        return webDriver.findElement(By.id(menuId));
    }

    public static void navigateTo(WebDriver webDriver, String menuId) {
        WebElement menuElement = webDriver.findElement(By.id(menuId));
        assertTrue(menuElement != null);
        if (!menuElement.isDisplayed()) {  // The menu in question is not displayed, so we need to make it visible
            switch (menuId) {
                case Menu.GUIID_SUB_MENU_ITEM_SUBMITTER_CREATION:
                    findNavigationElement(webDriver, Menu.GUIID_MAIN_MENU_ITEM_SUBMITTERS).click();
                    break;
                case Menu.GUIID_SUB_MENU_ITEM_FLOW_CREATION:
                case Menu.GUIID_SUB_MENU_ITEM_FLOW_COMPONENT_CREATION:
                case Menu.GUIID_SUB_MENU_ITEM_FLOW_COMPONENTS_SHOW:
                case Menu.GUIID_SUB_MENU_ITEM_FLOWBINDER_CREATION:
                    findNavigationElement(webDriver, Menu.GUIID_MAIN_MENU_ITEM_FLOWS).click();
                    break;
                case Menu.GUIID_SUB_MENU_ITEM_SINK_CREATION:
                    findNavigationElement(webDriver, Menu.GUIID_MAIN_MENU_ITEM_SINKS).click();
                    break;
            }
        }
        menuElement.click();  // Now the element is visble, click on it
    }

}
