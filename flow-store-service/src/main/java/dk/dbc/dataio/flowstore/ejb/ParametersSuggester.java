package dk.dbc.dataio.flowstore.ejb;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.flowstore.model.ParameterSuggestion;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Stateless
@Path("/")
public class ParametersSuggester extends AbstractResourceBean{
    private static final Logger LOGGER = LoggerFactory.getLogger(ParametersSuggester.class);
    JSONBContext jsonbContext = new JSONBContext();

    @PersistenceContext
    EntityManager entityManager;

    @GET
    @Path(FlowStoreServiceConstants.PARAMETERS)
    @Produces({MediaType.APPLICATION_JSON})
    public Response getParameter(@PathParam(FlowStoreServiceConstants.PARM_VARIABLE) FlowStoreServiceConstants.ParameterSuggestionNames parm) throws JSONBException {
        LOGGER.info("Get all '{}' from flowBinders and HarvesterConfigs", parm);
        ParameterSuggestion parameterSuggestion = queryForParameter(parm);
        return Response.ok().entity(jsonbContext.marshall(parameterSuggestion)).build();

    }

    private ParameterSuggestion queryForParameter(FlowStoreServiceConstants.ParameterSuggestionNames parm) {
        List<String> parameters = new ArrayList<>();
        for (String table : List.of("FLOW_BINDERS", "HARVESTER_CONFIGS")) {
            Query query = entityManager.createNativeQuery("SELECT content ->> '" + parm.getValue() + "' FROM " + table);
            parameters.addAll(query.getResultList());
        }
        return new ParameterSuggestion()
                .withName(parm.getValue())
                .withValues(parameters.stream().filter(Objects::nonNull).filter(s -> !s.trim()
                        .isEmpty()).distinct()
                        .sorted(Comparator.comparing(String::toLowerCase))
                        .collect(Collectors.toList()));
    }
}
