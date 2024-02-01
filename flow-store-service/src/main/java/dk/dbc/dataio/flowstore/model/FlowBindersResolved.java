package dk.dbc.dataio.flowstore.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dk.dbc.dataio.commons.types.Constants;
import dk.dbc.dataio.commons.types.FlowBinder;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FlowBindersResolved extends FlowBinder {
    @JsonIgnore
    private final Function<Long, String> sinkNameResolver;
    @JsonIgnore
    private final Function<Long, String> submitterNameResolver;
    @JsonIgnore
    private final Function<Long, String> flowNameResolver;

    /**
     * Class constructor
     *
     * @param id      flow binder id (larger than or equal to {@value Constants#PERSISTENCE_ID_LOWER_BOUND})
     * @param version flow binder version (larger than or equal to {@value Constants#PERSISTENCE_VERSION_LOWER_BOUND})
     * @param content flow binder content
     * @throws NullPointerException     if given null-valued content
     * @throws IllegalArgumentException if value of id or version is not larger than or equal to lower bound
     */

    public FlowBindersResolved(FlowBinder flowBinder, Function<Long, String> sinkNameResolver, Function<Long, String> submitterNameResolver, Function<Long, String> flowNameResolver) {
        super(flowBinder.getId(), flowBinder.getVersion(), flowBinder.getContent());
        this.sinkNameResolver = sinkNameResolver;
        this.submitterNameResolver = submitterNameResolver;
        this.flowNameResolver = flowNameResolver;
    }

    public String getFlowName() {
        return flowNameResolver.apply(getContent().getFlowId());
    }

    public List<String> getSubmitters() {
        return getContent().getSubmitterIds().stream().map(id -> formatSubmitter(id, submitterNameResolver.apply(id))).collect(Collectors.toList());
    }

    public String getSinkName() {
        return sinkNameResolver.apply(getContent().getSinkId());
    }

    private String formatSubmitter(Long id ,String name) {
        return id + (name == null ? "" : " (" + name + ")");
    }
}
