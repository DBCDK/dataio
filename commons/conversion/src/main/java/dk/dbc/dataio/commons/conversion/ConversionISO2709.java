package dk.dbc.dataio.commons.conversion;

import dk.dbc.marc.Iso2709Packer;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class ConversionISO2709 extends Conversion {
    private final DocumentBuilder documentBuilder;

    public ConversionISO2709(ConversionParam param, DocumentBuilder documentBuilder) {
        super(param);
        this.documentBuilder = documentBuilder;
    }

    @Override
    public byte[] apply(byte[] bytes) {
        documentBuilder.reset();
        try {
            final Document document = documentBuilder.parse(new ByteArrayInputStream(bytes));
            final Charset encoding = param.getEncoding().orElse(StandardCharsets.UTF_8);
            return Iso2709Packer.create2709FromMarcXChangeRecord(document, encoding);
        } catch (SAXException | IOException e) {
            throw new ConversionException("Unable to parse XML", e);
        }
    }
}
