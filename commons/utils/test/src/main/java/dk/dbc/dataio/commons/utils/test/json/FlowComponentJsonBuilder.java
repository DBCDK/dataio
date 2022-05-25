package dk.dbc.dataio.commons.utils.test.json;

public class FlowComponentJsonBuilder extends JsonBuilder {
    private Long id = 42L;
    private Long version = 1L;
    private String content = new FlowComponentContentJsonBuilder().build();
    private String next = null;

    public FlowComponentJsonBuilder setId(Long id) {
        this.id = id;
        return this;
    }

    public FlowComponentJsonBuilder setVersion(Long version) {
        this.version = version;
        return this;
    }

    public FlowComponentJsonBuilder setContent(String content) {
        this.content = content;
        return this;
    }

    public FlowComponentJsonBuilder setNext(String next) {
        this.next = next;
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
        stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asObjectMember("next", next));
        stringBuilder.append(END_OBJECT);
        return stringBuilder.toString();
    }
}
