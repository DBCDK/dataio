package dk.dbc.dataio.commons.utils.test.json;

public class FlowBinderJsonBuilder extends JsonBuilder {

    private long id = 42L;
    private long version = 1L;
    private String content = new FlowBinderContentJsonBuilder().build();

    public FlowBinderJsonBuilder setContent(String content) {
        this.content = content;
        return this;
    }

    public FlowBinderJsonBuilder setId(long id) {
        this.id = id;
        return this;
    }

    public FlowBinderJsonBuilder setVersion(long version) {
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
