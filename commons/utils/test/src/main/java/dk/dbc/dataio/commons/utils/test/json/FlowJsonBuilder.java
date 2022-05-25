package dk.dbc.dataio.commons.utils.test.json;

public class FlowJsonBuilder extends JsonBuilder {
    private long id = 42L;
    private long version = 1L;
    private String content = new FlowContentJsonBuilder().build();

    public FlowJsonBuilder setContent(String content) {
        this.content = content;
        return this;
    }

    public FlowJsonBuilder setId(long id) {
        this.id = id;
        return this;
    }

    public FlowJsonBuilder setVersion(long version) {
        this.version = version;
        return this;
    }

    public String build() {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(START_OBJECT);
        stringBuilder.append(asLongMember("id", id));
        stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asLongMember("version", version));
        stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asObjectMember("content", content));
        stringBuilder.append(END_OBJECT);
        return stringBuilder.toString();
    }
}
