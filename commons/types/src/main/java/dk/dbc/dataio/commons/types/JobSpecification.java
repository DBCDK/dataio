package dk.dbc.dataio.commons.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.io.Serializable;

/**
 * Job specification DTO class.
 */
public class JobSpecification implements Serializable {
    private static final long serialVersionUID = 731600708416455339L;
    private static final Ancestry NO_ANCESTRY = null;
    public static final String EMPTY_MAIL_FOR_NOTIFICATION_ABOUT_VERIFICATION = "";
    public static final String EMPTY_MAIL_FOR_NOTIFICATION_ABOUT_PROCESSING = "";
    public static final String EMPTY_RESULT_MAIL_INITIALS = "";

    public enum Type { TRANSIENT, PERSISTENT, TEST, ACCTEST }

    private final String packaging;
    private final String format;
    private final String charset;
    private final String destination;
    private final long submitterId;
    private final String mailForNotificationAboutVerification;
    private final String mailForNotificationAboutProcessing;
    private final String resultmailInitials;
    // Due to GWT serialization issues we cannot use java.net.URI or java.net.URL
    private final String dataFile;
    private final Type type;
    private final Ancestry ancestry;

    /**
     * Class constructor
     *
     * @param packaging job packaging (rammeformat)
     * @param format  job format (indholdsformat)
     * @param charset job character set
     * @param destination job destination
     * @param submitterId submitter number(larger than or equal to {@value dk.dbc.dataio.commons.types.Constants#PERSISTENCE_ID_LOWER_BOUND})
     * @param mailForNotificationAboutVerification mail address for notification about the verification step.
     * @param mailForNotificationAboutProcessing mail address for notification about the processing step.
     * @param resultmailInitials According to transfile spec: "Initialer til identifikation af resultatmail fra DanBib".
     * @param dataFile job data file
     * @param type job type
     * @param ancestry job ancestry
     *
     * @throws NullPointerException if given null-valued argument
     * @throws IllegalArgumentException if given empty valued String argument
     * or if value of submitterId is less than or equals 0
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
                            @JsonProperty("dataFile") String dataFile,
                            @JsonProperty("type") Type type,
                            @JsonProperty("ancestry") Ancestry ancestry) throws NullPointerException, IllegalArgumentException {

        this.packaging = InvariantUtil.checkNotNullNotEmptyOrThrow(packaging, "packaging");
        this.format = InvariantUtil.checkNotNullNotEmptyOrThrow(format, "format");
        this.charset = InvariantUtil.checkNotNullNotEmptyOrThrow(charset, "charset");
        this.destination = InvariantUtil.checkNotNullNotEmptyOrThrow(destination, "destination");
        this.submitterId = InvariantUtil.checkLowerBoundOrThrow(submitterId, "submitterId", Constants.PERSISTENCE_ID_LOWER_BOUND);
        this.mailForNotificationAboutVerification = InvariantUtil.checkNotNullOrThrow(mailForNotificationAboutVerification, "mailForNotificationAboutVerification");
        this.mailForNotificationAboutProcessing = InvariantUtil.checkNotNullOrThrow(mailForNotificationAboutProcessing, "mailForNotificationAboutProcessing");
        this.resultmailInitials = InvariantUtil.checkNotNullOrThrow(resultmailInitials, "resultmailInitials");
        this.dataFile = InvariantUtil.checkNotNullNotEmptyOrThrow(dataFile, "dataFile");
        this.type = InvariantUtil.checkNotNullOrThrow(type, "type");
        this.ancestry = ancestry;
    }

    public JobSpecification(String packaging,
                            String format,
                            String charset,
                            String destination,
                            long submitterId,
                            String mailForNotificationAboutVerification,
                            String mailForNotificationAboutProcessing,
                            String resultmailInitials,
                            String dataFile,
                            Type type) throws NullPointerException, IllegalArgumentException {
        this(packaging, format, charset, destination, submitterId,
                mailForNotificationAboutVerification, mailForNotificationAboutProcessing, resultmailInitials,
                dataFile, type, NO_ANCESTRY);
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

    // Submitter id represents the unique submitter number and not the id generated by the system when a new submitter is created
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

    public Type getType() {
        return type;
    }

    public Ancestry getAncestry() {
        return ancestry;
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
        if (!packaging.equals(that.packaging)) {
            return false;
        }
        if (!format.equals(that.format)) {
            return false;
        }
        if (!charset.equals(that.charset)) {
            return false;
        }
        if (!destination.equals(that.destination)) {
            return false;
        }
        if (!mailForNotificationAboutVerification.equals(that.mailForNotificationAboutVerification)) {
            return false;
        }
        if (!mailForNotificationAboutProcessing.equals(that.mailForNotificationAboutProcessing)) {
            return false;
        }
        if (!resultmailInitials.equals(that.resultmailInitials)) {
            return false;
        }
        if (!dataFile.equals(that.dataFile)) {
            return false;
        }
        if (type != that.type) {
            return false;
        }
        return !(ancestry != null ? !ancestry.equals(that.ancestry) : that.ancestry != null);

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
        result = 31 * result + type.hashCode();
        result = 31 * result + (ancestry != null ? ancestry.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "JobSpecification{" +
                "packaging='" + packaging + '\'' +
                ", format='" + format + '\'' +
                ", charset='" + charset + '\'' +
                ", destination='" + destination + '\'' +
                ", submitterId=" + submitterId +
                ", mailForNotificationAboutVerification='" + mailForNotificationAboutVerification + '\'' +
                ", mailForNotificationAboutProcessing='" + mailForNotificationAboutProcessing + '\'' +
                ", resultmailInitials='" + resultmailInitials + '\'' +
                ", dataFile='" + dataFile + '\'' +
                ", type='" + type + '\'' +
                ", ancestry='" + ancestry + '\'' +
                '}';
    }

    public static class Ancestry implements Serializable {
        private final String transfile;
        private final String datafile;
        private final String batchId;


        @JsonCreator
        public Ancestry(
                @JsonProperty("transfile") String transfile,
                @JsonProperty("datafile") String datafile,
                @JsonProperty("batchId") String batchId) {
            this.transfile = transfile;
            this.datafile = datafile;
            this.batchId = batchId;
        }

        public String getTransfile() {
            return transfile;
        }

        public String getDatafile() {
            return datafile;
        }

        public String getBatchId() {
            return batchId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Ancestry ancestry = (Ancestry) o;

            if (transfile != null ? !transfile.equals(ancestry.transfile) : ancestry.transfile != null) {
                return false;
            }
            if (datafile != null ? !datafile.equals(ancestry.datafile) : ancestry.datafile != null) {
                return false;
            }
            return !(batchId != null ? !batchId.equals(ancestry.batchId) : ancestry.batchId != null);

        }

        @Override
        public int hashCode() {
            int result = transfile != null ? transfile.hashCode() : 0;
            result = 31 * result + (datafile != null ? datafile.hashCode() : 0);
            result = 31 * result + (batchId != null ? batchId.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "Ancestry{" +
                    "transfile='" + transfile + '\'' +
                    ", datafile='" + datafile + '\'' +
                    ", batchId='" + batchId + '\'' +
                    '}';
        }
    }
}
