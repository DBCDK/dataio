package dk.dbc.dataio.commons.utils.test.model;

import dk.dbc.dataio.commons.types.JobSpecification;


public class JobSpecificationBuilder {
    private String packaging = "-packaging-";
    private String format = "-format-";
    private String charset = "-charset-";
    private String destination = "-destination-";
    private long submitterId = 222;
    private String mailForNotificationAboutVerification = "-mailForNotificationAboutVerification-";
    private String mailForNotificationAboutProcessing = "-mailForNotificationAboutProcessing-";
    private String resultmailInitials = "-resultmailInitials-";
    private String dataFile = "-dataFile-";
    private JobSpecification.Type type = JobSpecification.Type.TEST;
    private JobSpecification.Ancestry ancestry = null;

    public JobSpecificationBuilder setPackaging(String packaging) {
        this.packaging = packaging;
        return this;
    }

    public JobSpecificationBuilder setFormat(String format) {
        this.format = format;
        return this;
    }

    public JobSpecificationBuilder setCharset(String charset) {
        this.charset = charset;
        return this;
    }

    public JobSpecificationBuilder setDestination(String destination) {
        this.destination = destination;
        return this;
    }

    public JobSpecificationBuilder setSubmitterId(long submitterId) {
        this.submitterId = submitterId;
        return this;
    }

    public JobSpecificationBuilder setMailForNotificationAboutVerification(String mailForNotificationAboutVerification) {
        this.mailForNotificationAboutVerification = mailForNotificationAboutVerification;
        return this;
    }

    public JobSpecificationBuilder setMailForNotificationAboutProcessing(String mailForNotificationAboutProcessing) {
        this.mailForNotificationAboutProcessing = mailForNotificationAboutProcessing;
        return this;
    }

    public JobSpecificationBuilder setResultmailInitials(String resultmailInitials) {
        this.resultmailInitials = resultmailInitials;
        return this;
    }

    public JobSpecificationBuilder setDataFile(String dataFile) {
        this.dataFile = dataFile;
        return this;
    }

    public JobSpecificationBuilder setType(JobSpecification.Type type) {
        this.type = type;
        return this;
    }

    public JobSpecificationBuilder setAncestry(JobSpecification.Ancestry ancestry) {
        this.ancestry = ancestry;
        return this;
    }

    public JobSpecification build() {
        return new JobSpecification(packaging, format, charset, destination, submitterId,
                mailForNotificationAboutVerification, mailForNotificationAboutProcessing, resultmailInitials,
                dataFile, type, ancestry);
    }

    public static class AncestryBuilder {
        private String transfile = "transfile";
        private String datafile = "datafile";
        private String batchId = "";

        public AncestryBuilder setTransfile(String transfile) {
            this.transfile = transfile;
            return this;
        }

        public AncestryBuilder setDatafile(String datafile) {
            this.datafile = datafile;
            return this;
        }

        public AncestryBuilder setBatchId(String batchId) {
            this.batchId = batchId;
            return this;
        }

        public JobSpecification.Ancestry build() {
            return new JobSpecification.Ancestry(transfile, datafile, batchId);
        }
    }
}
