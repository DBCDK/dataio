package dk.dbc.dataio.flowstore.entity;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

/**
 * Representation of FlowBinderSearchIndexEntry composite key (packaging, format, charset, destination, submitter)
 */
public final class FlowBinderSearchKey {
    private final String packaging;
    private final String format;
    private final String charset;
    private final String destination;
    private final Long submitter;

    /**
     * Class constructor
     *
     * @throws NullPointerException if given null-valued argument
     * @throws IllegalArgumentException if empty valued String argument
     */

    /**
     * Class constructor
     *
     * @param packaging packaging
     * @param format format
     * @param charset charset
     * @param destination destination
     * @param submitter submitter
     *
     * @throws NullPointerException if given null-valued argument
     * @throws IllegalArgumentException if empty valued String argument
     */
    public FlowBinderSearchKey(String packaging, String format, String charset, String destination, Long submitter) {
        this.packaging = InvariantUtil.checkNotNullNotEmptyOrThrow(packaging, "packaging");
        this.format = InvariantUtil.checkNotNullNotEmptyOrThrow(format, "format");
        this.charset = InvariantUtil.checkNotNullNotEmptyOrThrow(charset, "charset");
        this.destination = InvariantUtil.checkNotNullNotEmptyOrThrow(destination, "destination");
        this.submitter = InvariantUtil.checkNotNullOrThrow(submitter, "submitter");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final FlowBinderSearchKey that = (FlowBinderSearchKey) o;

        if (!charset.equals(that.charset)) {
            return false;
        }
        if (!destination.equals(that.destination)) {
            return false;
        }
        if (!format.equals(that.format)) {
            return false;
        }
        if (!packaging.equals(that.packaging)) {
            return false;
        }
        if (!submitter.equals(that.submitter)) {
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
        result = 31 * result + submitter.hashCode();
        return result;
    }
}
