package dk.dbc.dataio.gui.server.jobrerun;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class JobRerunScheme implements Serializable {

    public static final String TICKLE_TOTAL = "sink::tickle-repo/total";

    public enum Action {COPY, RERUN_ALL, RERUN_FAILED}

    public enum Type {RR, TICKLE, ORIGINAL_FILE}

    private Set<Action> actions;
    private Type type;

    public JobRerunScheme() {
    }

    public Type getType() {
        return type;
    }

    public JobRerunScheme withType(Type type) {
        this.type = type;
        return this;
    }

    public Set<Action> getActions() {
        return actions;
    }

    public JobRerunScheme withActions(Set<Action> actions) {
        this.actions = new HashSet<>(actions);
        return this;
    }

    @Override
    public String toString() {
        return "JobRerunScheme{" +
                "actions=" + actions +
                ", type=" + type +
                '}';
    }
}
