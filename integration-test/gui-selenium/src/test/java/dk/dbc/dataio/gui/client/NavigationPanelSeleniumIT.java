package dk.dbc.dataio.gui.client;

import static dk.dbc.dataio.gui.client.SeleniumUtil.findElementInCurrentView;
import dk.dbc.dataio.gui.client.views.Menu;
import dk.dbc.dataio.gui.client.views.NavigationPanel;
import static org.junit.Assert.assertTrue;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class NavigationPanelSeleniumIT extends AbstractGuiSeleniumTest {
    private static ConstantsProperties texts = new ConstantsProperties("MenuConstants_dk.properties");

    @Ignore("Temporary disabled because test fails with new Navigation Panel")
    @Test
    public void testNavigationMenuPanelVisible() {
        assertTrue(findNavigationPanelElement(webDriver).isDisplayed());
    }

    @Ignore("Temporary disabled because test fails with new Navigation Panel")
    @Test
    public void testMainMenuItemsVisible() {
        assertTrue(findMenuNavigationElement(webDriver, Menu.GUIID_MAIN_MENU_ITEM_SUBMITTERS).isDisplayed());
        assertTrue(findMenuNavigationElement(webDriver, Menu.GUIID_MAIN_MENU_ITEM_FLOWS).isDisplayed());
        assertTrue(findMenuNavigationElement(webDriver, Menu.GUIID_MAIN_MENU_ITEM_SINKS).isDisplayed());
    }

    @Ignore("Temporary disabled because test fails with new Navigation Panel")
    public void testSubmitterMenuItemsVisible() {
        findMainMenuFoldUnfoldElement(webDriver, Menu.GUIID_MAIN_MENU_ITEM_SUBMITTERS).click();
        assertTrue(findMenuNavigationElement(webDriver, Menu.GUIID_SUB_MENU_ITEM_SUBMITTER_CREATION).isDisplayed());

//        webDriver.get(applicationUrl);
//        assertTrue(findNavigationPanelElement(webDriver).isDisplayed());
    }

    @Ignore("Temporary disabled because test fails with new Navigation Panel")
    @Test
    public void testFlowsMenuItemsVisible() {
        findMainMenuFoldUnfoldElement(webDriver, Menu.GUIID_MAIN_MENU_ITEM_FLOWS).click();
        assertTrue(findMenuNavigationElement(webDriver, Menu.GUIID_SUB_MENU_ITEM_FLOW_CREATION).isDisplayed());
        assertTrue(findMenuNavigationElement(webDriver, Menu.GUIID_SUB_MENU_ITEM_FLOW_COMPONENT_CREATION).isDisplayed());
        assertTrue(findMenuNavigationElement(webDriver, Menu.GUIID_SUB_MENU_ITEM_FLOW_COMPONENTS_SHOW).isDisplayed());
        assertTrue(findMenuNavigationElement(webDriver, Menu.GUIID_SUB_MENU_ITEM_FLOWBINDER_CREATION).isDisplayed());
    }

    @Ignore("Temporary disabled because test fails with new Navigation Panel")
    @Test
    public void testSinksMenuItemsVisible() {
        findMainMenuFoldUnfoldElement(webDriver, Menu.GUIID_MAIN_MENU_ITEM_SINKS).click();
        assertTrue(findMenuNavigationElement(webDriver, Menu.GUIID_SUB_MENU_ITEM_SINK_CREATION).isDisplayed());
    }


    // Utility methods
    private static WebElement findNavigationPanelElement(WebDriver webDriver) {
        return findElementInCurrentView(webDriver, NavigationPanel.GUIID_NAVIGATION_MENU_PANEL);
    }

    private static WebElement findMainMenuFoldUnfoldElement(WebDriver webDriver, String menuId) {
        return webDriver.findElement(By.id(menuId)).findElement(By.tagName("img"));
    }

    private static WebElement findMenuNavigationElement(WebDriver webDriver, String menuId) {
        return webDriver.findElement(By.id(menuId));
    }

    public static void navigateTo(WebDriver webDriver, String menuId) {
        WebElement menuElement = webDriver.findElement(By.id(menuId));
        assertTrue(menuElement != null);
        if (!menuElement.isDisplayed()) {  // The menu in question is not displayed, so we need to make it visible
            switch (menuId) {
                case Menu.GUIID_SUB_MENU_ITEM_SUBMITTER_CREATION:
                    findMenuNavigationElement(webDriver, Menu.GUIID_MAIN_MENU_ITEM_SUBMITTERS).click();
                    break;
                case Menu.GUIID_SUB_MENU_ITEM_FLOW_CREATION:
                case Menu.GUIID_SUB_MENU_ITEM_FLOW_COMPONENT_CREATION:
                case Menu.GUIID_SUB_MENU_ITEM_FLOW_COMPONENTS_SHOW:
                case Menu.GUIID_SUB_MENU_ITEM_FLOWBINDER_CREATION:
                    findMenuNavigationElement(webDriver, Menu.GUIID_MAIN_MENU_ITEM_FLOWS).click();
                    break;
                case Menu.GUIID_SUB_MENU_ITEM_SINK_CREATION:
                    findMenuNavigationElement(webDriver, Menu.GUIID_MAIN_MENU_ITEM_SINKS).click();
                    break;
            }
        }
        menuElement.click();  // Now the element is visble, click on it
    }

}
