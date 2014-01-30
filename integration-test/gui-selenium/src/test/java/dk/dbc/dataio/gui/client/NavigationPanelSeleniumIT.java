package dk.dbc.dataio.gui.client;

import static dk.dbc.dataio.gui.client.SeleniumUtil.findElementInCurrentView;
import dk.dbc.dataio.gui.client.views.Menu;
import dk.dbc.dataio.gui.client.views.NavigationPanel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class NavigationPanelSeleniumIT extends AbstractGuiSeleniumTest {

    /**
     * When modifying the menu structure, do also remember to change the structure below,
     * since the test is based on this structure.
     */
    private final static class MenuItems {
        private final String[] mainItems;
        private final Map<String, String[]> subItems;
        /**
         * Constructor, where the expected menu structure is defined
         */
        public MenuItems() {
            // First initialize the list of main menu items
            mainItems = new String[] {
                Menu.GUIID_MAIN_MENU_ITEM_SUBMITTERS,
                Menu.GUIID_MAIN_MENU_ITEM_FLOWS,
                Menu.GUIID_MAIN_MENU_ITEM_SINKS,
                Menu.GUIID_MAIN_MENU_ITEM_JOBS,
            };
            // Then all of the sub menus
            subItems = new HashMap<>();
            // Submenus for Submitters Main Menu
            subItems.put(
                Menu.GUIID_MAIN_MENU_ITEM_SUBMITTERS,
                new String[] {
                    Menu.GUIID_SUB_MENU_ITEM_SUBMITTER_CREATION,
                }
            );
            // Submenus for Flows Main Menu
            subItems.put(
                Menu.GUIID_MAIN_MENU_ITEM_FLOWS,
                new String[] {
                    Menu.GUIID_SUB_MENU_ITEM_FLOW_CREATION,
                    Menu.GUIID_SUB_MENU_ITEM_FLOW_COMPONENT_CREATION,
                    Menu.GUIID_SUB_MENU_ITEM_FLOW_COMPONENTS_SHOW,
                    Menu.GUIID_SUB_MENU_ITEM_FLOWBINDER_CREATION,
                }
            );
            // Submenus for Sinks Main Menu
            subItems.put(
                Menu.GUIID_MAIN_MENU_ITEM_SINKS,
                new String[] {
                    Menu.GUIID_SUB_MENU_ITEM_SINK_CREATION,
                }
            );
            // Submenus for Jobs Main Menu
            subItems.put(
                Menu.GUIID_MAIN_MENU_ITEM_JOBS,
                new String[] {
                }
            );
        }

        /**
         * isMainMenuItem returns true, if the parameter is a Main Menu, false if not
         * @param item The menu item to test
         * @return true, if the item is a Main Menu, false if not
         */
        public boolean isMainMenuItem(String item) {
            return Arrays.asList(mainItems).contains(item);
        }

        /**
         * isSubMenuUnder returns the parent Main Menu item, if the parameter
         * is a Sub Menu, null if it is not a sub menu
         * @param item The menu item to test
         * @return The parent Main Menu, if the item is a Sub Menu, null if not
         */
        public String isSubMenuUnder(String item) {
            for (String key: subItems.keySet()) {
                if (Arrays.asList(subItems.get(key)).contains(item)) {
                    return key;
                }
            }
            return null;
        }

        /**
         * isSubMenuItem returns true, if the parameter is a Sub Menu, false if not
         * @param item The menu item to test
         * @return true, if the item is a Sub Menu, false if not
         */
        public boolean isSubMenuItem(String item) {
            return isSubMenuUnder(item) != null;
        }

        /**
         * Gets the list of Main Menus
         * @return The list of Main Menus
         */
        public String[] getMainMenus() {
            return mainItems;
        }

        /**
         * Gets the list of Sub Menus for a given Main Menu
         * @param mainMenu The Main Menu to fetch Sub Menus for
         * @return The list of Sub Menus
         */
        public String[] getSubMenus(String mainMenu) {
            return subItems.get(mainMenu);
        }
    }
    private final static MenuItems menuItems = new MenuItems();


    // Tests begin here...

    @Test
    public void testNavigationMenuPanelVisible() {
        assertTrue(findNavigationPanelElement(webDriver).isDisplayed());
    }

    @Test
    public void testMainMenuItemsAreVisible() {
        for (String mainMenu: menuItems.getMainMenus()) {
            assertTrue("Main menu '" + mainMenu + "' is not visible", findMenuNavigationElement(webDriver, mainMenu).isDisplayed());
        }
    }

    @Test
    public void testSubMenuItemsAreVisible() {
        for (String mainMenu: menuItems.getMainMenus()) {
            clickOnMainMenuFoldUnfoldElement(webDriver, mainMenu);
            String[] subMenus = menuItems.getSubMenus(mainMenu);
            if (subMenus != null) {
                for (String subMenu: subMenus) {
                    assertTrue("Sub menu '" + subMenu + "' is not visible", findMenuNavigationElement(webDriver, subMenu).isDisplayed());
                }
            }
        }
    }



    // Utility methods
    private static WebElement findNavigationPanelElement(WebDriver webDriver) {
        return findElementInCurrentView(webDriver, NavigationPanel.GUIID_NAVIGATION_MENU_PANEL);
    }

    private static void clickOnMainMenuFoldUnfoldElement(WebDriver webDriver, String menuId) {
        List<WebElement> foldUnfoldElements = findMenuNavigationElement(webDriver, menuId).findElements(By.tagName("img"));
        if (foldUnfoldElements.size() > 0) {  // Check if any <img> tags were found
            foldUnfoldElements.get(0).click();  // If so - the first one is the one to use
        }
    }

    private static WebElement findMenuNavigationElement(WebDriver webDriver, String menuId) {
        return findElementInCurrentView(webDriver, menuId);
    }


    // Public Utility method

    /**
     * Navigate to a sub page, pointed out by either a Main Menu Id or a Sub Menu Id (guiId)
     * @param webDriver The Selenium Web Driver
     * @param menuId The menu to navigate to
     */
    public static void navigateTo(WebDriver webDriver, String menuId) {
        if (menuItems.isMainMenuItem(menuId)) {
            findMenuNavigationElement(webDriver, menuId).click();
        }
        else if (menuItems.isSubMenuItem(menuId)) {
            // First check, that submenu is visible
            if (!findMenuNavigationElement(webDriver, menuId).isDisplayed()) {
                clickOnMainMenuFoldUnfoldElement(webDriver, menuItems.isSubMenuUnder(menuId));  // Unfold the parent main menu
            }
            findMenuNavigationElement(webDriver, menuId).click();
        } else {  // The menu item is neither a Main Menu nor a Sub Menu
            assertTrue("That menu element does not exist", false);
        }
    }

}
