package dk.dbc.dataio.sink.periodicjobs;

import java.util.Locale;
import java.util.ResourceBundle;

public class I18n {
    private static final ResourceBundle RESOURCE_BUNDLE = getResourceBundle();

    public static String get(String key) {
        return RESOURCE_BUNDLE.getString(key);
    }

    private static ResourceBundle getResourceBundle() {
        String language = System.getenv("LOCALE_LANGUAGE");
        if (language == null || language.trim().isEmpty()) {
            language = "da";
        }
        String country = System.getenv("LOCALE_COUNTRY");
        if (country == null || country.trim().isEmpty()) {
            country = "DK";
        }
        return ResourceBundle.getBundle("locales/PeriodicJobsSink", new Locale(language, country));
    }

    private I18n() {
    }
}
