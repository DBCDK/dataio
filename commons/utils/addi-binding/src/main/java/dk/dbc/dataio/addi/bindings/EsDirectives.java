package dk.dbc.dataio.addi.bindings;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import static org.apache.commons.lang3.StringEscapeUtils.escapeXml10;

public class EsDirectives {
    public String submitter;
    public String format;
    public String language;
    public String contentFrom;
    @JacksonXmlProperty(localName = "DBCTrackingId", namespace = EsReferenceData.ES_NAMESPACE, isAttribute = true)
    public String trackingId;

    public EsDirectives withSubmitter(String submitter) {
        this.submitter = submitter;
        return this;
    }

    public EsDirectives withFormat(String format) {
        this.format = format;
        return this;
    }

    public EsDirectives withLanguage(String language) {
        this.language = language;
        return this;
    }

    public EsDirectives withContentFrom(String contentFrom) {
        this.contentFrom = contentFrom;
        return this;
    }

    public EsDirectives withTrackingId(String trackingId) {
        this.trackingId = trackingId;
        return this;
    }

    public String toXmlString(String namespacePrefix) {
        final StringBuilder content = new StringBuilder();
        content.append("<");
        if (namespacePrefix != null) {
            content.append(namespacePrefix).append(':');
        }
        content.append("info");
        if (submitter != null) {
            content.append(" submitter=\"").append(xmlEscape(submitter)).append("\"");
        }
        if (format != null) {
            content.append(" format=\"").append(xmlEscape(format)).append("\"");
        }
        if (language != null) {
            content.append(" language=\"").append(xmlEscape(language)).append("\"");
        }
        if (contentFrom != null) {
            content.append(" contentFrom=\"").append(xmlEscape(contentFrom)).append("\"");
        }
        if (trackingId != null) {
            content.append(" DBCTrackingId=\"").append(xmlEscape(trackingId)).append("\"");
        }
        content.append("/>");
        return content.toString();
    }

    private String xmlEscape(String s) {
        /* Note that Unicode characters greater than 0x7f are not escaped.
           If we at some point need this functionality, we can achieve it via the following:
           StringEscapeUtils.ESCAPE_XML10.with( NumericEntityEscaper.between(0x7f, Integer.MAX_VALUE) );
        */
        return escapeXml10(s);
    }
}
