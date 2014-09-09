
package dk.dbc.dataio.gui.client;

import org.junit.Assert;

import java.util.Properties;

class ConstantsProperties {
    private static final Properties prop = new Properties();

    public ConstantsProperties(String propertiesFile) {
        try {
            prop.load(ConstantsProperties.class.getClassLoader().getResourceAsStream("dk/dbc/dataio/gui/client/pages/" + propertiesFile));
        } catch (Exception ex) {
            Assert.assertTrue("Translation Properties file could not be found", false);
        }
    }

    public String translate(String key) {
        return prop.getProperty(key);
    }

}
