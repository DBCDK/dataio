package dk.dbc.dataio.jobstore.service.mail;

import dk.dbc.dataio.jobstore.service.entity.NotificationEntity;
import dk.dbc.dataio.jobstore.service.util.MailDestination;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


public class MailTest {

    @Test
    public void oneAddress() throws AddressException {
        NotificationEntity notification = new NotificationEntity();
        notification.setDestination("office@company.dk");
        MailDestination mailDestination = new MailDestination(null, notification, null);
        InternetAddress[] addresses = mailDestination.getToAddresses();
        assertThat("addresses", addresses, is(new InternetAddress[]{new InternetAddress("office@company.dk")}));
    }
    @Test
    public void moreThanOneAddress() throws AddressException {
        NotificationEntity notification = new NotificationEntity();
        notification.setDestination("office@company.dk; dataio@dbc.dk");
        MailDestination mailDestination = new MailDestination(null, notification, null);
        InternetAddress[] addresses = mailDestination.getToAddresses();
        assertThat("addresses", addresses, is(new InternetAddress[]{new InternetAddress("office@company.dk"), new InternetAddress("dataio@dbc.dk")}));
    }

    @Test
    public void illegalAddresses() throws AddressException {
        NotificationEntity notification = new NotificationEntity();
        notification.setDestination("office@company.dk; dbc.dk");
        MailDestination mailDestination = new MailDestination(null, notification, null);
        InternetAddress[] addresses = mailDestination.getToAddresses();
        assertThat("addresses", addresses, is(new InternetAddress[]{new InternetAddress("office@company.dk")}));
    }
}
