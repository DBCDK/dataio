package dk.dbc.dataio.sink.periodicjobs.mail;

import dk.dbc.dataio.sink.periodicjobs.SinkConfig;
import jakarta.mail.Session;
import java.util.Map;
import java.util.Properties;

public class MailSession {
    public static Session make() {
        Properties mailProperties = new Properties();
        mailProperties.putAll(
                        Map.of(
                                "mail.smtp.auth", false,
                                "mail.smtp.starttls.enable", "true",
                                "mail.smtp.host", SinkConfig.MAIL_HOST.asString(),
                                "mail.smtp.port", "25",
                                "user", SinkConfig.MAIL_USER.asString()));
        return Session.getInstance(mailProperties);
    }
}
