package dk.dbc.dataio.addi.bindings;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "referencedata", namespace = EsReferenceData.ES_NAMESPACE)
public class EsReferenceData {
    public static final String ES_NAMESPACE = "http://oss.dbc.dk/ns/es";
    public static final String DATAIO_DIRECTIVES_NAMESPACE = "dk.dbc.dataio.processing";
    private static final String ES_REFERENCE_DATA_XML_TEMPLATE =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<es:referencedata xmlns:es=\"http://oss.dbc.dk/ns/es\">%s</es:referencedata>";

    @JacksonXmlProperty(localName = "info", namespace = ES_NAMESPACE)
    public EsDirectives esDirectives;

    @JacksonXmlProperty(localName = "sink-processing", namespace = DATAIO_DIRECTIVES_NAMESPACE)
    public SinkDirectives sinkDirectives;

    @JacksonXmlProperty(localName = "sink-update-template", namespace = DATAIO_DIRECTIVES_NAMESPACE)
    public UpdateSinkDirectives updateSinkDirectives;

    public EsReferenceData withEsDirectives(EsDirectives esDirectives) {
        this.esDirectives = esDirectives;
        return this;
    }

    public EsReferenceData withSinkDirectives(SinkDirectives sinkDirectives) {
        this.sinkDirectives = sinkDirectives;
        return this;
    }

    public EsReferenceData withUpdateSinkDirectives(UpdateSinkDirectives updateSinkDirectives) {
        this.updateSinkDirectives = updateSinkDirectives;
        return this;
    }

    /**
     * @return XML string without any dataIO specific directives
     */
    public String toXmlString() {
        if (esDirectives != null) {
            return String.format(ES_REFERENCE_DATA_XML_TEMPLATE, esDirectives.toXmlString("es"));
        }
        return String.format(ES_REFERENCE_DATA_XML_TEMPLATE, "");
    }
}
