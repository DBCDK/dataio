
package dk.dbc.dataio.gui.client;

import java.io.IOException;
import java.util.Properties;
import org.junit.Assert;

class ConstantsProperties {
    private static final Properties prop = new Properties();

    public ConstantsProperties(String propertiesFile) {
        try {
            prop.load(SubmitterCreationSeleniumIT.class.getClassLoader().getResourceAsStream("dk/dbc/dataio/gui/client/i18n/" + propertiesFile));
        } catch (IOException ex) {
            Assert.assertTrue("Translation Properties file could not be found", false);
        }
    }

    public String translate(String key) {
        return prop.getProperty(key);
    }
    
}
