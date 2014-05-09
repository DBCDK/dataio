package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.integrationtest.ITUtil;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

public abstract class AbstractGuiSeleniumTest {
    private final static XLogger XLOGGER = XLoggerFactory.getXLogger(AbstractGuiSeleniumTest.class);

    private static final int IMPLICIT_WEBDRIVER_WAIT_IN_SECONDS = 2;

    protected static WebDriver webDriver;
    protected static String applicationUrl;
    protected static Connection flowStoreDbConnection;

    @BeforeClass
    public static void setUpContext() throws ClassNotFoundException, SQLException {
       XLOGGER.error("BeforeClass: Begin");
       try {
        applicationUrl = String.format("http://localhost:%s/gui/gui.html", System.getProperty("glassfish.port"));
        flowStoreDbConnection = ITUtil.newDbConnection("flow_store");
        webDriver = new FirefoxDriver();
        webDriver.manage().timeouts().implicitlyWait(IMPLICIT_WEBDRIVER_WAIT_IN_SECONDS, TimeUnit.SECONDS);
        webDriver.manage().window().setSize(new Dimension(1024, 768));
       } finally {
        XLOGGER.error("BeforeClass: end");
       }
    }

    @AfterClass
    public static void tearDownContext() throws SQLException {
       XLOGGER.error("AfterClass: Begin");
       try {
        webDriver.quit();
        flowStoreDbConnection.close();
       } finally {
        XLOGGER.error("AfterClass: end");
       }
    }

    @Before
    public void setUpTest() {
       XLOGGER.error("Before: Begin");
       try {
        webDriver.get(applicationUrl);
       } finally {
        XLOGGER.error("Before: end");
       }
    }

    @After
    public void tearDownTest() throws SQLException {
       XLOGGER.error("After: Begin");
       try {
        ITUtil.clearAllDbTables(flowStoreDbConnection);
       } finally {
        XLOGGER.error("After: end");
       }
    }
}
