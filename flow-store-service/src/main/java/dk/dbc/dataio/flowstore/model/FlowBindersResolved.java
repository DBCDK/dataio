package dk.dbc.dataio.flowstore.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.Constants;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowBinderContent;

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
    private static final JSONBContext jsContext = new JSONBContext();

    /**
     * Class constructor
     *
     * @param id      flow binder id (larger than or equal to {@value Constants#PERSISTENCE_ID_LOWER_BOUND})
     * @param version flow binder version (larger than or equal to {@value Constants#PERSISTENCE_VERSION_LOWER_BOUND})
     * @param content flow binder content
     * @throws NullPointerException     if given null-valued content
     * @throws IllegalArgumentException if value of id or version is not larger than or equal to lower bound
     */

    public FlowBindersResolved(Long id, Long version, FlowBinderContent content, Function<Long, String> sinkNameResolver, Function<Long, String> submitterNameResolver, Function<Long, String> flowNameResolver) {
        super(id, version, content);
        this.sinkNameResolver = sinkNameResolver;
        this.submitterNameResolver = submitterNameResolver;
        this.flowNameResolver = flowNameResolver;
    }

    public static FlowBindersResolved from(dk.dbc.dataio.flowstore.entity.FlowBinder flowBinder, Function<Long, String> sinkNameResolver, Function<Long, String> submitterNameResolver, Function<Long, String> flowNameResolver) {
        try {
            FlowBinderContent content = jsContext.unmarshall(flowBinder.getContent(), FlowBinderContent.class);
            return new FlowBindersResolved(flowBinder.getId(), flowBinder.getVersion(), content, sinkNameResolver, submitterNameResolver, flowNameResolver);
        } catch (JSONBException e) {
            throw new IllegalArgumentException(e);
        }
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
