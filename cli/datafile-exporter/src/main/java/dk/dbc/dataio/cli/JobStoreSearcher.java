package dk.dbc.dataio.cli;

import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.querylanguage.BiClause;
import dk.dbc.dataio.querylanguage.JsonValue;
import dk.dbc.dataio.querylanguage.Ordering;
import dk.dbc.dataio.querylanguage.QueryBuilder;
import dk.dbc.httpclient.HttpClient;
import jakarta.ws.rs.client.Client;
import net.sourceforge.argparse4j.inf.Namespace;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class JobStoreSearcher {
    private static final int MAX_PREFETCH = 500;
    private final JobStoreServiceConnector jobStoreServiceConnector;

    public JobStoreSearcher(String jobStoreServiceEndpoint) {
        final Client client = HttpClient.newClient(new ClientConfig()
                .register(new JacksonFeature()));
        jobStoreServiceConnector = new JobStoreServiceConnector(client, jobStoreServiceEndpoint);
    }

    public Map<String, Datafile> findDatafiles(Namespace args) {
        final QueryBuilder queryBuilder = createQuery(args);
        System.out.println("Finding datafiles using query: " + queryBuilder.buildQuery());
        final TreeMap<String, Datafile> datafiles = new TreeMap<>();

        int offset = 0;
        queryBuilder.limit(MAX_PREFETCH).offset(offset);
        while (true) {
            try {
                final List<JobInfoSnapshot> jobInfoSnapshots =
                        jobStoreServiceConnector.listJobs(queryBuilder.buildQuery());
                extractDatafiles(jobInfoSnapshots, datafiles);

                if (jobInfoSnapshots.size() < MAX_PREFETCH) {
                    break;
                }
                queryBuilder.offset(offset += MAX_PREFETCH);
            } catch (JobStoreServiceConnectorException e) {
                throw new CliException("Unable to query job-store", e);
            }
        }

        return datafiles;
    }

    private QueryBuilder createQuery(Namespace args) {
        final JsonValue jobSpecificationValue = new JsonValue()
                .put("submitterId", Integer.valueOf(args.getString("agency")));
        final String packaging = args.getString("packaging");
        if (packaging != null && !packaging.trim().isEmpty()) {
            jobSpecificationValue.put("packaging", packaging);
        }
        final String format = args.getString("format");
        if (format != null && !format.trim().isEmpty()) {
            jobSpecificationValue.put("format", format);
        }
        final String encoding = args.getString("encoding");
        if (encoding != null && !encoding.trim().isEmpty()) {
            jobSpecificationValue.put("charset", encoding);
        }
        final String destination = args.getString("destination");
        if (destination != null && !destination.trim().isEmpty()) {
            jobSpecificationValue.put("destination", destination);
        }
        final QueryBuilder queryBuilder = new QueryBuilder(
                new BiClause()
                        .withIdentifier("job:specification")
                        .withOperator(BiClause.Operator.JSON_LEFT_CONTAINS)
                        .withValue(jobSpecificationValue))
                .orderBy(new Ordering()
                        .withIdentifier("job:id")
                        .withOrder(Ordering.Order.ASC));
        final String createdFrom = args.getString("created_from");
        if (createdFrom != null && !createdFrom.trim().isEmpty()) {
            queryBuilder.and(
                    new BiClause()
                            .withIdentifier("job:timeOfCreation")
                            .withOperator(BiClause.Operator.GREATER_THAN_OR_EQUAL_TO)
                            .withValue(createdFrom));
        }
        final String createdTo = args.getString("created_to");
        if (createdTo != null && !createdTo.trim().isEmpty()) {
            queryBuilder.and(
                    new BiClause()
                            .withIdentifier("job:timeOfCreation")
                            .withOperator(BiClause.Operator.LESS_THAN)
                            .withValue(createdTo));
        }
        return queryBuilder;
    }

    private void extractDatafiles(List<JobInfoSnapshot> jobInfoSnapshots, Map<String, Datafile> datafiles) {
        for (JobInfoSnapshot jobInfoSnapshot : jobInfoSnapshots) {
            final FileStoreUrn datafileUrn = FileStoreUrn.parse(jobInfoSnapshot.getSpecification().getDataFile());
            final String fileId = datafileUrn.getFileId();
            if (datafiles.containsKey(fileId)) {
                datafiles.get(fileId).withJob(jobInfoSnapshot.getJobId());
            } else {
                datafiles.put(fileId, new Datafile(fileId).withJob(jobInfoSnapshot.getJobId()));
            }
        }
    }
}
