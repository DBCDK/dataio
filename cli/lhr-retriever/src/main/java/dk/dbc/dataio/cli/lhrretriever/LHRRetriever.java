package dk.dbc.dataio.cli.lhrretriever;

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.cli.lhrretriever.arguments.ArgParseException;
import dk.dbc.dataio.cli.lhrretriever.arguments.Arguments;
import dk.dbc.dataio.cli.lhrretriever.config.ConfigJson;
import dk.dbc.dataio.cli.lhrretriever.config.ConfigParseException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.commons.types.Pid;
import dk.dbc.dataio.commons.utils.lang.JaxpUtil;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.dataio.javascript.Script;
import dk.dbc.dataio.javascript.StringSourceSchemeHandler;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.reader.MarcReaderException;
import dk.dbc.marc.reader.MarcXchangeV1Reader;
import dk.dbc.marc.writer.MarcXchangeV1Writer;
import dk.dbc.rawrepo.dto.RecordDTO;
import dk.dbc.rawrepo.dto.RecordIdDTO;
import dk.dbc.rawrepo.queue.ConfigurationException;
import dk.dbc.rawrepo.queue.QueueException;
import dk.dbc.rawrepo.record.RecordServiceConnector;
import dk.dbc.rawrepo.record.RecordServiceConnectorFactory;
import dk.dbc.vipcore.exception.VipCoreException;
import dk.dbc.vipcore.libraryrules.VipCoreLibraryRulesConnector;
import dk.dbc.vipcore.libraryrules.VipCoreLibraryRulesConnectorFactory;
import dk.dbc.vipcore.marshallers.LibraryRule;
import dk.dbc.vipcore.marshallers.LibraryRules;
import jakarta.ws.rs.client.Client;
import org.apache.commons.codec.binary.Base64;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.postgresql.ds.PGSimpleDataSource;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.sql.DataSource;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class LHRRetriever implements Closeable {
    private final Client client;
    private final DataSource dataSource;
    private final RawRepoConnector rawRepoConnector;
    private final RecordServiceConnector rawRepoRecordServiceConnector;
    private final Ocn2PidServiceConnector ocn2PidServiceConnector;
    private final FlowStoreServiceConnector flowStoreServiceConnector;
    private final VipCoreLibraryRulesConnector vipCoreLibraryRulesConnector;
    private final Map<Long, AddiMetaData.LibraryRules> libraryRulesCache;

    public LHRRetriever(Arguments arguments) throws SQLException,
            ConfigurationException, QueueException, ConfigParseException {
        ConfigJson config = ConfigJson.parseConfig(arguments.configPath);
        dataSource = setupDataSource(config);
        client = HttpClient.newClient(new ClientConfig().register(new JacksonFeature()));
        rawRepoConnector = setupRRConnector(dataSource);
        rawRepoRecordServiceConnector = RecordServiceConnectorFactory.create(rawRepoConnector.getRecordServiceUrl());
        ocn2PidServiceConnector = new Ocn2PidServiceConnector(
                client, config.getOcn2pidServiceTarget());
        flowStoreServiceConnector = new FlowStoreServiceConnector(client,
                config.getFlowStoreEndpoint());
        vipCoreLibraryRulesConnector = VipCoreLibraryRulesConnectorFactory.create(
                config.getVipCoreEndpoint());
        libraryRulesCache = new HashMap<>();
    }

    public static void main(String[] args) {
        LHRRetriever lhrRetriever = null;
        try {
            Arguments arguments = Arguments.parseArgs(args);
            lhrRetriever = new LHRRetriever(arguments);
            byte[] records = lhrRetriever.processRecordsWithLHR();
            lhrRetriever.writeLHRToFile(arguments.outputPath, records);
        } catch (ArgParseException | SQLException | ConfigParseException |
                 LHRRetrieverException | QueueException | ConfigurationException e) {
            System.err.println("unexpected error: " + e);
            System.exit(1);
        } finally {
            if(lhrRetriever != null) lhrRetriever.close();
        }
    }

    public void writeLHRToFile(String outputPath, byte[] lhrIso2709Records)
            throws LHRRetrieverException {
        final File outputFile = new File(outputPath);
        if (outputFile.exists()) {
            throw new LHRRetrieverException(String.format(
                    "%s already exists", outputFile.getAbsolutePath()));
        }
        try (FileOutputStream os = new FileOutputStream(outputFile)) {
            os.write(lhrIso2709Records);
        } catch (IOException e) {
            throw new LHRRetrieverException(String.format(
                    "error writing records to file: %s", e.toString()), e);
        }
    }

    private byte[] processRecordsWithLHR(List<Script> scripts)
            throws LHRRetrieverException {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            for (Pid pid : fetchPids()) {
                final String ocn = ocn2PidServiceConnector
                        .getOcnByPid(pid.toString());
                final AddiMetaData.LibraryRules libraryRules =
                        getLibraryRules(pid.getAgencyId());
                final AddiMetaData metaData = new AddiMetaData()
                        .withOcn(ocn)
                        .withPid(pid.toString())
                        .withLibraryRules(libraryRules);
                final RecordIdDTO recordId = new RecordIdDTO(
                        pid.getBibliographicRecordId(), pid.getAgencyId());
                final String addi = processJavascript(scripts, recordId,
                        metaData);
                final byte[] record = addiToIso2709(addi);
                os.write(record);
            }
            return os.toByteArray();
        } catch (IOException | VipCoreException e) {
            throw new LHRRetrieverException(String.format(
                    "error getting lhr marked pids: %s", e.toString()), e);
        }
    }

    private List<Pid> fetchPids() throws IOException {
        final InputStream is =
                ocn2PidServiceConnector.getEntitiesWithLHRStream();
        final List<Pid> pids = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is))) {
            String pidStr;
            while ((pidStr = reader.readLine()) != null) {
                pids.add(Pid.of(pidStr));
            }
        }
        return pids;
    }

    private AddiMetaData.LibraryRules getLibraryRules(long agencyId)
            throws VipCoreException {
        if (libraryRulesCache.containsKey(agencyId)) {
            return libraryRulesCache.get(agencyId);
        }
        final LibraryRules libraryRules = vipCoreLibraryRulesConnector.getLibraryRulesByAgencyId(Long.toString(agencyId));
        final List<LibraryRule> libraryRuleList = libraryRules.getLibraryRule();
        final AddiMetaData.LibraryRules metadataRules =
                new AddiMetaData.LibraryRules();
        libraryRuleList.stream().filter(entry -> entry.getBool() != null).
                forEach(
                        entry -> metadataRules.withLibraryRule(
                                entry.getName(), entry.getBool()));
        metadataRules.withAgencyType(libraryRules.getAgencyType());

        libraryRulesCache.put(agencyId, metadataRules);
        return metadataRules;
    }

    // convert an AddiMetaData to a string formatted as a javascript object
    private String makeSupplementaryDataString(AddiMetaData metaData)
            throws LHRRetrieverException {
        if (Stream.of(metaData.pid(), metaData.ocn(), metaData.trackingId())
                .filter(s -> s == null || s.isEmpty()).count() > 0) {
            throw new LHRRetrieverException(String.format(
                    "invalid metadata: %s", metaData.toString()));
        }
        try {
            JSONBContext jsonbContext = new JSONBContext();
            return jsonbContext.marshall(metaData);
        } catch (JSONBException e) {
            throw new LHRRetrieverException(
                    "error marshalling addimetadata to json string", e);
        }
    }

    private byte[] addiToIso2709(String addi)
            throws LHRRetrieverException {
        try {
            AddiReader addiReader = new AddiReader(new ByteArrayInputStream(
                    addi.getBytes(StandardCharsets.UTF_8)));
            AddiRecord addiRecord = addiReader.getNextRecord();
            if (addiRecord == null)
                throw new LHRRetrieverException("addi record is null");
            Document document = JaxpUtil.toDocument(
                    addiRecord.getContentData());
            return Base64.decodeBase64(document.getDocumentElement()
                    .getTextContent());
        } catch (IOException | SAXException e) {
            throw new LHRRetrieverException(String.format(
                    "error reading addi: %s", e.toString()), e);
        }
    }

    // metaData should contain pid, ocn, and library rules
    private String processJavascript(List<Script> scripts, RecordIdDTO recordId,
                                     AddiMetaData metaData) throws LHRRetrieverException {
        try {
            RecordServiceConnector.Params params = new RecordServiceConnector.Params()
                    .withUseParentAgency(false)
                    .withExcludeAutRecords(true)
                    .withAllowDeleted(true)
                    .withExpand(true);
            final Map<String, RecordDTO> recordCollection = rawRepoRecordServiceConnector
                    .getRecordDataCollection(recordId, params);
            if (!recordCollection.containsKey(recordId.getBibliographicRecordId())) {
                throw new LHRRetrieverException(String.format(
                        "error retrieving record, id:%s agency:%s",
                        recordId.getBibliographicRecordId(),
                        recordId.getAgencyId()));
            }

            final RecordDTO record = recordCollection.get(
                    recordId.getBibliographicRecordId());
            String trackingId = record.getTrackingId();
            if (trackingId == null || trackingId.isEmpty()) {
                trackingId = String.format("lhr-%s:%s",
                        recordId.getBibliographicRecordId(),
                        recordId.getAgencyId());
            }
            final AddiMetaData supplementaryData = new AddiMetaData()
                    .withPid(metaData.pid()).withOcn(metaData.ocn())
                    .withLibraryRules(metaData.libraryRules())
                    .withTrackingId(trackingId);
            String supplementaryDataString = makeSupplementaryDataString(
                    supplementaryData);
            // parentheses in the string are significant here
            Object supplementaryDataObject = scripts.get(0).eval(
                    String.format("(%s)", supplementaryDataString));

            String marcXCollection = recordsToMarcXchangeCollection(
                    recordCollection.values());
            for (Script script : scripts) {
                marcXCollection = (String) script.invoke(new Object[]{
                        marcXCollection, supplementaryDataObject});
            }
            return marcXCollection;
        } catch (Throwable e) {
            throw new LHRRetrieverException(String.format(
                    "error processing javascript: %s", e.toString()), e);
        }
    }

    private String recordsToMarcXchangeCollection(Collection<RecordDTO> records)
            throws LHRRetrieverException {
        Charset charset = StandardCharsets.UTF_8;
        List<MarcRecord> marcRecords = new ArrayList<>();
        try {
            for (RecordDTO record : records) {
                final MarcXchangeV1Reader marcReader = new MarcXchangeV1Reader(
                        new BufferedInputStream(new ByteArrayInputStream(
                                record.getContent())), charset);
                MarcRecord marcRecord = marcReader.read();
                if (marcRecord != null)
                    marcRecords.add(marcRecord);
                else
                    throw new LHRRetrieverException("no marcxchange data found");
            }
        } catch (MarcReaderException e) {
            throw new LHRRetrieverException(String.format(
                    "error reading marc record collection: %s", e.toString()), e);
        }
        byte[] collection = new MarcXchangeV1Writer().writeCollection(
                marcRecords, charset);
        return new String(collection, charset);
    }

    // taken from FlowCache
    private static Script createScript(FlowComponentContent componentContent) throws Throwable {
        final List<JavaScript> javaScriptsBase64 = componentContent.getJavascripts();
        final List<StringSourceSchemeHandler.Script> javaScripts = new ArrayList<>(javaScriptsBase64.size());
        for (JavaScript javascriptBase64 : javaScriptsBase64) {
            javaScripts.add(new StringSourceSchemeHandler.Script(javascriptBase64.getModuleName(),
                    StringUtil.base64decode(javascriptBase64.getJavascript())));
        }
        String requireCacheJson = null;
        if (componentContent.getRequireCache() != null) {
            requireCacheJson = StringUtil.base64decode(componentContent.getRequireCache());
        }
        return new Script(componentContent.getName(), componentContent.getInvocationMethod(),
                javaScripts, requireCacheJson);
    }

    /**
     * Sets up raw repo data source
     *
     * @param config parsed values from config file
     * @return raw repo data source
     */
    private DataSource setupDataSource(ConfigJson config) {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setDatabaseName(config.getDbName());
        dataSource.setServerName(config.getDbHost());
        dataSource.setPortNumber(config.getDbPort());
        dataSource.setUser(config.getDbUser());
        dataSource.setPassword(config.getDbPassword());
        return dataSource;
    }

    /**
     * Sets up raw repo connector
     *
     * @param dataSource raw repo data source
     * @return raw repo connector
     */
    private RawRepoConnector setupRRConnector(DataSource dataSource) {
        return new RawRepoConnector(dataSource);
    }

    @Override
    public void close() {
        client.close();
    }
}
