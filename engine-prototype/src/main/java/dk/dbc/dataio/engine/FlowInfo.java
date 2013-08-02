package dk.dbc.dataio.engine;

public class FlowInfo {
    private final String data;

    public FlowInfo(String data) {
        if (data == null) {
            throw new NullPointerException("data arg can not be null");
        }
        if (data.isEmpty()) {
            throw new IllegalArgumentException("data arg can not be empty");
        }
        this.data = data;
    }

    public String getData() {
        return data;
    }
}
