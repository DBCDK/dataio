package dk.dbc.dataio.harvester.types;

import java.util.Objects;

public class MailPickup extends Pickup {
    private String recipients;
    private String subject;
    private String mimetype;
    private String body;
    private Integer recordLimit;
    public MailPickup() {
        super();
    }

    public String getRecipients() {
        return recipients;
    }

    public String getSubject() {
        return subject;
    }

    public String getMimetype() {
        return mimetype;
    }

    public String getBody() {
        return body;
    }

    public Integer getRecordLimit() {
        return recordLimit;
    }

    public MailPickup withRecipients(String recipients) {
        this.recipients = recipients;
        return this;
    }

    public MailPickup withSubject(String subject) {
        this.subject = subject;
        return this;
    }

    public MailPickup withMimetype(String mimetype) {
        this.mimetype = mimetype;
        return this;
    }

    public MailPickup withBody(String body) {
        this.body = body;
        return this;
    }

    public MailPickup withRecordLimit(Integer recordLimit) {
        this.recordLimit = recordLimit;
        return this;
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

        return Objects.equals(subject, that.subject)
                && Objects.equals(recipients, that.recipients)
                && Objects.equals(mimetype, that.mimetype)
                && Objects.equals(body, that.body)
                && Objects.equals(recordLimit, that.recordLimit);
    }

    @Override
    public int hashCode() {
        int result = recipients != null ? recipients.hashCode() : 0;
        result = 31 * result + (subject != null ? subject.hashCode() : 0);
        result = 31 * result + (mimetype != null ? mimetype.hashCode() : 0);
        result = 31 * result + (body != null ? body.hashCode() : 0);
        result = 31 * result + (recordLimit != null ? recordLimit.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "MailPickup{" +
                "recipients='" + recipients + '\'' +
                ", subject='" + subject + '\'' +
                ", mimetype='" + mimetype + '\'' +
                ", body='" + body + '\'' +
                ", recordLimit='" + recordLimit + '\'' +
                '}';
    }
}
