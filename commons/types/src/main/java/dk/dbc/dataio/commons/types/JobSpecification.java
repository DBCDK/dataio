package dk.dbc.dataio.commons.types;

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

    // Temporary field until we get proper flow analysis in the job-store
    private /* final */ long flowId;

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
     * @param dataFile job data file
     *
     * @throws NullPointerException if given null-valued argument
     * @throws IllegalArgumentException if given empty valued String argument
     * or if value of submitterId is <= 0
     */
    public JobSpecification(String packaging, String format, String charset, String destination, long submitterId, String dataFile, long flowId)
            throws NullPointerException, IllegalArgumentException {
        this.packaging = InvariantUtil.checkNotNullNotEmptyOrThrow(packaging, "packaging");
        this.format = InvariantUtil.checkNotNullNotEmptyOrThrow(format, "format");
        this.charset = InvariantUtil.checkNotNullNotEmptyOrThrow(charset, "charset");
        this.destination = InvariantUtil.checkNotNullNotEmptyOrThrow(destination, "destination");
        this.submitterId = InvariantUtil.checkAboveThresholdOrThrow(submitterId, "submitterId", Submitter.ID_VERSION_LOWER_THRESHOLD);
        this.dataFile = InvariantUtil.checkNotNullNotEmptyOrThrow(dataFile, "dataFile");

        this.flowId = flowId;
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

    public long getFlowId() {
        return flowId;
    }
}
