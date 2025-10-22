package dk.dbc.dataio.flowstore.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.Constants;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowBinderContent;

import java.util.List;
import java.util.function.Function;

public class FlowBindersResolved2 extends FlowBinder {
    @JsonIgnore
    private final Function<Long, String> sinkNameResolver;
    @JsonIgnore
    private final Function<Long, SubmitterContentLight> submitterResolver;
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

    public FlowBindersResolved2(Long id, Long version, FlowBinderContent content, Function<Long, String> sinkNameResolver, Function<Long, SubmitterContentLight> submitterResolver, Function<Long, String> flowNameResolver) {
        super(id, version, content);
        this.sinkNameResolver = sinkNameResolver;
        this.submitterResolver = submitterResolver;
        this.flowNameResolver = flowNameResolver;
    }

    public static FlowBindersResolved2 from(dk.dbc.dataio.flowstore.entity.FlowBinder flowBinder, Function<Long, String> sinkNameResolver, Function<Long, SubmitterContentLight> submitterResolver, Function<Long, String> flowNameResolver) {
        try {
            FlowBinderContent content = jsContext.unmarshall(flowBinder.getContent(), FlowBinderContent.class);
            return new FlowBindersResolved2(flowBinder.getId(), flowBinder.getVersion(), content, sinkNameResolver, submitterResolver, flowNameResolver);
        } catch (JSONBException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public String getFlowName() {
        return flowNameResolver.apply(getContent().getFlowId());
    }

    public List<SubmitterContentLight> getSubmitters() {
        return getContent().getSubmitterIds().stream().map(submitterResolver).toList();
    }

    public String getSinkName() {
        return sinkNameResolver.apply(getContent().getSinkId());
    }
}
