package dk.dbc.dataio.commons.utils.test.json;

public class ItemResultCounterJsonBuilder extends JsonBuilder {

    private long success = 0;
    private long failure = 0;
    private long ignore = 0;

    public ItemResultCounterJsonBuilder setSuccess(long success) {
        this.success = success;
        return this;
    }

    public ItemResultCounterJsonBuilder setFailure(long failure) {
        this.failure = failure;
        return this;
    }

    public ItemResultCounterJsonBuilder setIgnore(long ignore) {
        this.ignore = ignore;
        return this;
    }

    public String build() {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(START_OBJECT);

        stringBuilder.append(asLongMember("success", success)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asLongMember("failure", failure)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asLongMember("ignore", ignore));
        stringBuilder.append(END_OBJECT);
        return stringBuilder.toString();
    }
}
