/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.harvester.periodicjobs;

import dk.dbc.dataio.bfs.api.BinaryFileFsImpl;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.solr.JsonUpdateRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.cloud.MiniSolrCloudCluster;
import org.apache.solr.common.cloud.ClusterState;
import org.apache.solr.common.cloud.DocCollection;
import org.apache.solr.common.cloud.Replica;
import org.apache.solr.common.cloud.Slice;
import org.apache.solr.common.cloud.SolrZkClient;
import org.apache.solr.common.cloud.ZkConfigManager;
import org.apache.solr.common.cloud.ZkStateReader;
import org.apache.solr.common.params.CollectionParams;
import org.apache.solr.common.params.CoreAdminParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.zookeeper.KeeperException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class RecordSearcherIT {
    @ClassRule
    public static TemporaryFolder solrHome = new TemporaryFolder();

    private static final String COLLECTION = "record-searcher-test";
    private static MiniSolrCloudCluster miniSolrCloudCluster;
    private static CloudSolrClient cloudSolrClient;

    @BeforeClass
    public static void startCluster() throws Exception {
        miniSolrCloudCluster = new MiniSolrCloudCluster(1, null,
                FileSystems.getDefault().getPath(solrHome.getRoot().getAbsolutePath()),
                MiniSolrCloudCluster.DEFAULT_CLOUD_SOLR_XML, null, null);
        pingCluster();
        createClient();
        populateCollection();
        closeClient();
    }

    @AfterClass
    public static void stopCluster() throws Exception {
        if (miniSolrCloudCluster != null) {
            miniSolrCloudCluster.shutdown();
        }
    }

    @Rule
    public TemporaryFolder mountPoint = new TemporaryFolder();

    @Test
    public void search() throws HarvesterException, IOException {
        final Path filePath = Paths.get(mountPoint.getRoot().toPath().toString(), "test.out");
        final BinaryFileFsImpl out = new BinaryFileFsImpl(filePath);

        try (RecordSearcher recordSearcher = new RecordSearcher(getZkAddress())) {
            final long numberOfRecords = recordSearcher.search(COLLECTION, "*:*", out);
            assertThat("number of records", numberOfRecords, is(10L));
        }

        final List<String> fileContent = Files.readAllLines(filePath);
        assertThat("file content", fileContent, is(Arrays.asList(
                "055357342X", "080508049X", "380014300", "441385532", "553293354",
                "553573403", "553579908", "805080481", "812521390", "812550706")));
    }

    private static void createClient() {
        cloudSolrClient = new CloudSolrClient.Builder().withZkHost(getZkAddress()).build();
        cloudSolrClient.connect();
    }

    private static void populateCollection() throws IOException, SolrServerException {
        final File confDir = new File("src/test/resources/conf");
        createCollection(cloudSolrClient, COLLECTION, 2, 1, confDir);
        try (InputStream inputStream = new FileInputStream("src/test/resources/books.json")) {
            final JsonUpdateRequest request = new JsonUpdateRequest(inputStream);
            request.process(cloudSolrClient, COLLECTION);
        }
        cloudSolrClient.commit(COLLECTION);
    }

    private static void createCollection(CloudSolrClient cloudSolrClient, String collection,
                                         int numShards, int replicationFactor, File confDir) {
        try {
            if (confDir != null) {
                assertThat("Specified Solr config directory '" + confDir.getAbsolutePath() + "' not found!",
                        confDir.isDirectory(), is(true));

                // upload the configs
                final SolrZkClient zkClient = cloudSolrClient.getZkStateReader().getZkClient();
                final ZkConfigManager zkConfigManager = new ZkConfigManager(zkClient);
                zkConfigManager.uploadConfigDir(confDir.toPath(), collection);
            }

            final int liveNodes = cloudSolrClient.getZkStateReader().getClusterState().getLiveNodes().size();
            final int maxShardsPerNode = (int) Math.ceil(((double) numShards * replicationFactor) / liveNodes);

            final ModifiableSolrParams params = new ModifiableSolrParams();
            params.set(CoreAdminParams.ACTION, CollectionParams.CollectionAction.CREATE.name());
            params.set("name", collection);
            params.set("numShards", numShards);
            params.set("replicationFactor", replicationFactor);
            params.set("maxShardsPerNode", maxShardsPerNode);
            params.set("collection.configName", collection);
            final QueryRequest request = new QueryRequest(params);
            request.setPath("/admin/collections");
            cloudSolrClient.request(request);

            verifyReplicas(cloudSolrClient, collection, numShards, replicationFactor);
        } catch (InterruptedException | IOException | KeeperException | SolrServerException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void verifyReplicas(CloudSolrClient cloudSolrClient, String collection,
                                       int numShards, int replicationFactor)
            throws KeeperException, InterruptedException {
        final ZkStateReader zkStateReader = cloudSolrClient.getZkStateReader();

        long waitMs = 0L;
        long maxWaitMs = 20 * 1000L;
        boolean allReplicasUp = false;
        waitLoop: while (waitMs < maxWaitMs && !allReplicasUp) {
            Thread.sleep(500L);
            waitMs += 500L;

            zkStateReader.forceUpdateCollection(collection);
            final ClusterState clusterState = zkStateReader.getClusterState();
            final DocCollection docCollection = clusterState.getCollectionOrNull(collection);
            if (docCollection != null) {
                final Collection<Slice> activeSlices = docCollection.getActiveSlices();
                if (activeSlices.size() < numShards) {
                    continue waitLoop;
                }
                for (Slice slice : activeSlices) {
                    final Collection<Replica> replicas = slice.getReplicas();
                    if (replicas.size() != replicationFactor) {
                        continue waitLoop;
                    }
                    for (Replica replica : replicas) {
                        if (replica.getState() != Replica.State.ACTIVE) {
                            continue waitLoop;
                        }
                    }
                }
                allReplicasUp = true;
            }
        }

        if (!allReplicasUp) {
            fail("Didn't see all replicas for " + collection + " come up within " + maxWaitMs + " ms");
        }
    }

    private static void pingCluster() throws IOException {
        try (CloudSolrClient cloudSolrClient = new CloudSolrClient.Builder()
                .withZkHost(getZkAddress())
                .build()) {
            cloudSolrClient.connect();

            assertThat("live nodes", cloudSolrClient.getZkStateReader().getClusterState().getLiveNodes().isEmpty(),
                    is(not(true)));

            System.out.println("cluster state: " + cloudSolrClient.getZkStateReader().getClusterState());
        }
    }

    private static String getZkAddress() {
        if (miniSolrCloudCluster != null) {
            return miniSolrCloudCluster.getZkServer().getZkAddress();
        }
        return null;
    }

    private static void closeClient() throws IOException {
        if (cloudSolrClient != null) {
            cloudSolrClient.close();
        }
    }
}
