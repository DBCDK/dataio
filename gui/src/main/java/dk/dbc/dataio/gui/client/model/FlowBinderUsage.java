package dk.dbc.dataio.gui.client.model;


import java.util.Objects;

public class FlowBinderUsage extends GenericBackendModel {
    private String name;
    private String lastUsed;

    private long flowBinderId;

    public String getName() {
        return name;
    }

    public FlowBinderUsage withName(String name) {
        this.name = name;
        return this;
    }

    public String getLastUsed() {
        return lastUsed;
    }

    public FlowBinderUsage withLastUsed(String lastUsed) {
        this.lastUsed = lastUsed;
        return this;
    }

    public long getFlowBinderId() {
        return flowBinderId;
    }

    public FlowBinderUsage withFlowBinderId(long flowBinderId) {
        this.flowBinderId = flowBinderId;
        return this;
    }

    public FlowBinderUsage() {}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FlowBinderUsage that = (FlowBinderUsage) o;
        return Objects.equals(name, that.name) && Objects.equals(lastUsed, that.lastUsed);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, lastUsed);
    }

    @Override
    public String toString() {
        return "FlowBinderUsage{" +
                "name='" + name + '\'' +
                ", lastUsed='" + lastUsed + '\'' +
                '}';
    }
}
