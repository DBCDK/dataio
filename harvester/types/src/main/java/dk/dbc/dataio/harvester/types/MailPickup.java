package dk.dbc.dataio.harvester.types;

import java.util.Objects;

public class MailPickup extends Pickup {
    private String recipients;
    private String subject;

    public MailPickup() {
        super();
    }

    public String getRecipients() {
        return recipients;
    }

    public String getSubject() {
        return subject;
    }

    public MailPickup withRecipients(String recipients) {
        this.recipients = recipients;
        return this;
    }

    public MailPickup withSubject(String subject) {
        this.subject = subject;
        return this;
    }

    @Override
    public Pickup withOverrideFilename(String overrideFilename) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Value not allowed for overrideFilename with type MailPickup");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MailPickup that = (MailPickup) o;

        return Objects.equals(subject, that.subject) && Objects.equals(recipients, that.recipients);
    }

    @Override
    public int hashCode() {
        int result = recipients != null ? recipients.hashCode() : 0;
        result = 31 * result + (subject != null ? subject.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "MailPickup{" +
                "recipients='" + recipients + '\'' +
                ", subject='" + subject + '\'' +
                '}';
    }

}
