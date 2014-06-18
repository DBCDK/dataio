package dk.dbc.dataio.commons.utils.test.json;

public class ChunkCounterJsonBuilder extends JsonBuilder {
    private long total = 0;
    private String itemResultCounter = new ItemResultCounterJsonBuilder().build();

    public ChunkCounterJsonBuilder setTotal(long total) {
        this.total = total;
        return this;
    }

    public ChunkCounterJsonBuilder setItemResultCounter(String itemResultCounter) {
        this.itemResultCounter = itemResultCounter;
        return this;
    }

    public String build() {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(START_OBJECT);

        stringBuilder.append(asLongMember("total", total)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asObjectMember("itemResultCounter", itemResultCounter));
        stringBuilder.append(END_OBJECT);
        return stringBuilder.toString();
    }
}