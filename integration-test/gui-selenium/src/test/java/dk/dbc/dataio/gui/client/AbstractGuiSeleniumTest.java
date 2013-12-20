package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.integrationtest.ITUtil;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.safari.SafariDriver;

public abstract class AbstractGuiSeleniumTest {
    private static final int IMPLICIT_WEBDRIVER_WAIT_IN_SECONDS = 2;

    protected static WebDriver webDriver;
    protected static String applicationUrl;
    protected static Connection flowStoreDbConnection;

    @BeforeClass
    public static void setUpContext() throws ClassNotFoundException, SQLException {
        applicationUrl = String.format("http://localhost:%s/gui/gui.html", System.getProperty("glassfish.port"));
        flowStoreDbConnection = ITUtil.newDbConnection("flow_store");
//        webDriver = new FirefoxDriver();
        webDriver = new SafariDriver();
        webDriver.manage().timeouts().implicitlyWait(IMPLICIT_WEBDRIVER_WAIT_IN_SECONDS, TimeUnit.SECONDS);
    }

    @AfterClass
    public static void tearDownContext() throws SQLException {
        webDriver.quit();
        flowStoreDbConnection.close();
    }

    @Before
    public void setUpTest() {
        webDriver.get(applicationUrl);
    }

    @After
    public void tearDownTest() throws SQLException {
        ITUtil.clearAllDbTables(flowStoreDbConnection);
    }
}
