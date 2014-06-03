package dk.dbc.dataio.sinkservice.ping;

import dk.dbc.dataio.commons.types.PingResponse;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Pings application server for necessary resource
 */
public class ResourcePing {

    private ResourcePing() { }

    /**
     * Executes resource ping
     *
     * @param context context for performing naming operations
     * @param resourceName name of resource
     * @param resourceClass type of resource
     *
     * @return ping response
     *
     * @throws NullPointerException when given null-valued argument
     * @throws IllegalArgumentException when given empty-valued resourceName argument
     */
    public static <T> PingResponse execute(InitialContext context, String resourceName, Class<T> resourceClass)
            throws NullPointerException, IllegalArgumentException {
        InvariantUtil.checkNotNullOrThrow(context, "context");
        InvariantUtil.checkNotNullNotEmptyOrThrow(resourceName, "resourceName");
        InvariantUtil.checkNotNullOrThrow(resourceClass, "resourceClass");
        PingResponse.Status status = PingResponse.Status.OK;
        final List<String> log = new ArrayList<>();
        try {
            doResourceLookup(context, resourceName, resourceClass);
            log.add(String.format("Found %s resource with name '%s'", resourceClass.getName(), resourceName));
        } catch (NamingException e) {
            status = PingResponse.Status.FAILED;
            log.add(String.format("Unable to find %s resource with name '%s' : %s", resourceClass.getName(), resourceName, e.getMessage()));
        }
        return new PingResponse(status, log);
    }

    private static <T> void doResourceLookup(InitialContext context, String resourceName, Class<T> resourceClass) throws NamingException {
        final Object lookup = context.lookup(resourceName);
        if (!resourceClass.isInstance(lookup)) {
            throw new NamingException("Unexpected type of resource returned from lookup");
        }
    }


}
