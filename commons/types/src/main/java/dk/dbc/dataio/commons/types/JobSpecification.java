package dk.dbc.dataio.commons.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.io.Serializable;

/**
 * Job specification DTO class.
 *
 * In all essence objects of this class are immutable, but due to GWT serialization
 * issues we cannot have final fields and need a default no-arg constructor.
 */
public class JobSpecification implements Serializable {
    private static final long serialVersionUID = 731600708416455339L;

    private /* final */ String packaging;
    private /* final */ String format;
    private /* final */ String charset;
    private /* final */ String destination;
    private /* final */ long submitterId;
    private /* final */ String mailForNotificationAboutVerification;
    private /* final */ String mailForNotificationAboutProcessing;
    private /* final */ String resultmailInitials;
    // Due to GWT serialization issues we cannot use java.net.URI or java.net.URL
    private /* final */ String dataFile;

    private JobSpecification() { }

    /**
     * Class constructor
     *
     * Attention: when changing the signature of this constructor
     * remember to also change the signature in the corresponding *JsonMixIn class.
     *
     * @param packaging job packaging (rammeformat)
     * @param format  job format (indholdsformat)
     * @param charset job character set
     * @param destination job destination
     * @param submitterId id of job submitter (> 0)
     * @param mailForNotificationAboutVerification mail address for notification about the verification step.
     * @param mailForNotificationAboutProcessing mail address for notification about the processing step.
     * @param resultmailInitials According to transfile spec: "Initialer til identifikation af resultatmail fra DanBib".
     * @param dataFile job data file
     *
     * @throws NullPointerException if given null-valued argument
     * @throws IllegalArgumentException if given empty valued String argument
     * or if value of submitterId is <= 0
     */
    @JsonCreator
    public JobSpecification(@JsonProperty("packaging") String packaging,
                            @JsonProperty("format") String format,
                            @JsonProperty("charset") String charset,
                            @JsonProperty("destination") String destination,
                            @JsonProperty("submitterId") long submitterId,
                            @JsonProperty("mailForNotificationAboutVerification") String mailForNotificationAboutVerification,
                            @JsonProperty("mailForNotificationAboutProcessing") String mailForNotificationAboutProcessing,
                            @JsonProperty("resultmailInitials") String resultmailInitials,
                            @JsonProperty("dataFile") String dataFile) throws NullPointerException, IllegalArgumentException {
        this.packaging = InvariantUtil.checkNotNullNotEmptyOrThrow(packaging, "packaging");
        this.format = InvariantUtil.checkNotNullNotEmptyOrThrow(format, "format");
        this.charset = InvariantUtil.checkNotNullNotEmptyOrThrow(charset, "charset");
        this.destination = InvariantUtil.checkNotNullNotEmptyOrThrow(destination, "destination");
        this.submitterId = InvariantUtil.checkLowerBoundOrThrow(submitterId, "submitterId", Constants.PERSISTENCE_ID_LOWER_BOUND);
        this.mailForNotificationAboutVerification = InvariantUtil.checkNotNullOrThrow(mailForNotificationAboutVerification, "mailForNotificationAboutVerification");
        this.mailForNotificationAboutProcessing = InvariantUtil.checkNotNullOrThrow(mailForNotificationAboutProcessing, "mailForNotificationAboutProcessing");
        this.resultmailInitials = InvariantUtil.checkNotNullOrThrow(resultmailInitials, "resultmailInitials");
        this.dataFile = InvariantUtil.checkNotNullNotEmptyOrThrow(dataFile, "dataFile");
    }

    public String getCharset() {
        return charset;
    }

    public String getDataFile() {
        return dataFile;
    }

    public String getDestination() {
        return destination;
    }

    public String getFormat() {
        return format;
    }

    public String getPackaging() {
        return packaging;
    }

    public long getSubmitterId() {
        return submitterId;
    }

    public String getMailForNotificationAboutVerification() {
        return mailForNotificationAboutVerification;
    }

    public String getMailForNotificationAboutProcessing() {
        return mailForNotificationAboutProcessing;
    }

    public String getResultmailInitials() {
        return resultmailInitials;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        JobSpecification that = (JobSpecification) o;

        if (submitterId != that.submitterId) {
            return false;
        }
        if (!charset.equals(that.charset)) {
            return false;
        }
        if (!dataFile.equals(that.dataFile)) {
            return false;
        }
        if (!destination.equals(that.destination)) {
            return false;
        }
        if (!format.equals(that.format)) {
            return false;
        }
        if (!mailForNotificationAboutProcessing.equals(that.mailForNotificationAboutProcessing)) {
            return false;
        }
        if (!mailForNotificationAboutVerification.equals(that.mailForNotificationAboutVerification)) {
            return false;
        }
        if (!packaging.equals(that.packaging)) {
            return false;
        }
        if (!resultmailInitials.equals(that.resultmailInitials)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = packaging.hashCode();
        result = 31 * result + format.hashCode();
        result = 31 * result + charset.hashCode();
        result = 31 * result + destination.hashCode();
        result = 31 * result + (int) (submitterId ^ (submitterId >>> 32));
        result = 31 * result + mailForNotificationAboutVerification.hashCode();
        result = 31 * result + mailForNotificationAboutProcessing.hashCode();
        result = 31 * result + resultmailInitials.hashCode();
        result = 31 * result + dataFile.hashCode();
        return result;
    }
}
