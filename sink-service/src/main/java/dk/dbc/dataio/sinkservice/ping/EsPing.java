package dk.dbc.dataio.sinkservice.ping;

import dk.dbc.dataio.commons.types.PingResponse;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

/**
 * Pings application server for resources necessary for sink ESbase connectivity
 */
public class EsPing {
    private static final Logger LOGGER = LoggerFactory.getLogger(EsPing.class);

    private EsPing() { }

    /**
     * Executes ES base ping
     *
     * @param context context for performing naming operations
     * @param sinkContent sink definition
     *
     * @return ping response
     *
     * @throws NullPointerException when given null-valued context or sinkContent argument
     */
    public static PingResponse execute(InitialContext context, SinkContent sinkContent) throws NullPointerException {
        InvariantUtil.checkNotNullOrThrow(context, "context");
        InvariantUtil.checkNotNullOrThrow(sinkContent, "sinkContent");
        PingResponse.Status status = PingResponse.Status.OK;
        final List<String> log = new ArrayList<>();
        try {
            doDataSourceLookup(context, sinkContent.getResource());
            log.add(String.format("Found DataSource resource with name '%s'", sinkContent.getResource()));
        } catch (NamingException e) {
            status = PingResponse.Status.FAILED;
            log.add(String.format("Unable to find DataSource resource with name '%s' : %s", sinkContent.getResource(), e.getMessage()));
        }
        return new PingResponse(status, log);
    }

    private static void doDataSourceLookup(InitialContext context, String jndiName) throws NamingException {
        final Object lookup = context.lookup(jndiName);
        if (!(lookup instanceof DataSource)) {
            throw new NamingException("Unexpected type of resource returned from lookup");
        }
    }


}
