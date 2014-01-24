package dk.dbc.dataio.commons.utils.test.json;

public class NewJobJsonBuilder extends JsonBuilder {
    private long jobId = 42;
    private long chunkCount = 1;
    private String sink = new SinkJsonBuilder().build();

    public NewJobJsonBuilder setJobId(long jobId) {
        this.jobId = jobId;
        return this;
    }

    public NewJobJsonBuilder setChunkCount(long chunkCount) {
        this.chunkCount = chunkCount;
        return this;
    }

    public NewJobJsonBuilder setSink(String sink) {
        this.sink = sink;
        return this;
    }

    public String build() {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(START_OBJECT);
        stringBuilder.append(asLongMember("jobId", jobId)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asLongMember("chunkCount", chunkCount)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asObjectMember("sink", sink));
        stringBuilder.append(END_OBJECT);
        return stringBuilder.toString();
    }
}
