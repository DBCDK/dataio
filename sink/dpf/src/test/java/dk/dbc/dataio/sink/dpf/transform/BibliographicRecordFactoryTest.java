package dk.dbc.dataio.sink.dpf.transform;

import dk.dbc.dataio.sink.openupdate.bindings.BibliographicRecordExtraData;
import dk.dbc.dataio.sink.util.DocumentTransformer;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.reader.MarcReaderException;
import dk.dbc.oss.ns.catalogingupdate.BibliographicRecord;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.xmlunit.matchers.CompareMatcher;

import javax.xml.transform.TransformerException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class BibliographicRecordFactoryTest {
    private final BibliographicRecordFactory bibliographicRecordFactory = new BibliographicRecordFactory();

    private static BibliographicRecordExtraData unmarshallBibliographicRecordExtraData(Element element) {
        try {
            Unmarshaller unmarshaller = JAXBContext.newInstance(BibliographicRecordExtraData.class)
                    .createUnmarshaller();
            return unmarshaller.unmarshal(element, BibliographicRecordExtraData.class).getValue();
        } catch (JAXBException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    public void toBibliographicalRecord() throws MarcReaderException, BibliographicRecordFactoryException, TransformerException {
        final String marcXchange =
                "<?xml version='1.0' encoding='UTF-8'?>\n" +
                        "<record xmlns='info:lc/xmlns/marcxchange-v1' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:schemaLocation='info:lc/xmlns/marcxchange-v1 http://www.loc.gov/standards/iso25577/marcxchange-1-1.xsd'>" +
                        "<leader>00000     22000000 4500 </leader>" +
                        "<datafield ind1='0' ind2='0' tag='001'>" +
                        "<subfield code='a'>123456</subfield>" +
                        "<subfield code='b'>870970</subfield>" +
                        "</datafield>" +
                        "</record>";

        MarcRecord marcRecord = MarcRecordFactory.fromMarcXchange(marcXchange.getBytes(StandardCharsets.UTF_8));

        BibliographicRecord bibliographicRecord = bibliographicRecordFactory.toBibliographicRecord(marcRecord, "myQueueProvider");
        assertThat("Bibliographic record not null", bibliographicRecord, is(notNullValue()));
        assertThat("Bibliographic record recordPackaging", bibliographicRecord.getRecordPacking(), is("xml"));
        assertThat("Bibliographic record recordSchema", bibliographicRecord.getRecordSchema(), is("info:lc/xmlns/marcxchange-v1"));

        BibliographicRecordExtraData bibliographicRecordExtraData = unmarshallBibliographicRecordExtraData(
                (Element) bibliographicRecord.getExtraRecordData().getContent().get(0));
        assertThat("Bibliographic record extra data queue provider", bibliographicRecordExtraData.getProviderName(),
                is("myQueueProvider"));
        assertThat("Bibliographic record extra data queue priority", bibliographicRecordExtraData.getPriority(),
                is(1000));

        Element element = (Element) bibliographicRecord.getRecordData().getContent().get(0);
        byte[] bibliographicalContent = new DocumentTransformer().documentToByteArray(element.getOwnerDocument());
        String bibliographicalContentXml = new String(bibliographicalContent, StandardCharsets.UTF_8);
        assertThat(bibliographicalContentXml, CompareMatcher.isIdenticalTo(marcXchange));
    }

    /**
     * Shares one factory between several threads converting records at the same time, the way the
     * consumer threads of the sink share the factory held by its ServiceBroker.
     * <p>
     * A factory that lets threads share its XML parser or JAXB marshaller fails here with
     * {@code FWK005 parse may not be called while parsing} or a NullPointerException from inside the
     * parser or marshaller. The race is timing dependent, so a pass does not prove the absence of
     * sharing, but enough threads and conversions make a collision very likely when sharing exists.
     * </p>
     */
    @Test
    void toBibliographicRecordFromConcurrentThreads() throws Exception {
        final int threads = 8;
        final int conversionsPerThread = 200;
        final byte[] marcXchange = ("<?xml version='1.0' encoding='UTF-8'?>" +
                "<record xmlns='info:lc/xmlns/marcxchange-v1'>" +
                "<leader>00000     22000000 4500 </leader>" +
                "<datafield ind1='0' ind2='0' tag='001'>" +
                "<subfield code='a'>123456</subfield>" +
                "<subfield code='b'>870970</subfield>" +
                "</datafield>" +
                "</record>").getBytes(StandardCharsets.UTF_8);

        // Holds every worker back until all of them are submitted. Without it the first threads could
        // finish their conversions while later threads are still being created, and the calls would
        // overlap far less than they do when consumer threads pick up the items of one chunk together.
        CountDownLatch start = new CountDownLatch(1);
        Callable<Void> conversions = () -> {
            start.await();
            // Asserting inside the worker makes a garbled result fail the test, not only an exception.
            for (int i = 0; i < conversionsPerThread; i++) {
                BibliographicRecord bibliographicRecord = bibliographicRecordFactory.toBibliographicRecord(
                        marcXchange, "myQueueProvider");
                Element recordElement = (Element) bibliographicRecord.getRecordData().getContent().get(0);
                assertThat("record element", recordElement.getLocalName(), is("record"));
                Element extraDataElement = (Element) bibliographicRecord.getExtraRecordData().getContent().get(0);
                assertThat("extra data element", extraDataElement.getLocalName(), is("updateRecordExtraData"));
                assertThat("queue provider", extraDataElement.getTextContent(), containsString("myQueueProvider"));
            }
            return null;
        };

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            List<Future<Void>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(executor.submit(conversions));
            }
            // Releases all waiting workers at once.
            start.countDown();
            // Rethrows the first exception or assertion error raised in a worker, wrapped in an
            // ExecutionException, so a failure on any thread fails the test.
            for (Future<Void> future : futures) {
                future.get();
            }
        } finally {
            // Interrupts workers still waiting on the latch if submitting failed before countDown.
            executor.shutdownNow();
        }
    }
}
