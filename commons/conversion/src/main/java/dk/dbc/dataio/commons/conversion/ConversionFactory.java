package dk.dbc.dataio.commons.conversion;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.Optional;

/**
 * This class is not thread-safe.
 */
public class ConversionFactory {
    private DocumentBuilderFactory documentBuilderFactory;

    public Conversion newConversion(ConversionParam param) {
        final Optional<String> packaging = param.getPackaging();
        if (!packaging.isPresent()) {
            return new ConversionNOOP();
        }
        final String trimmedPackaging = packaging.get().trim().toLowerCase();
        switch (trimmedPackaging) {
            case "iso": return newConversionISO2709(param);
            case "":    return new ConversionNOOP();
            default:    throw new ConversionException("Unknown conversion: " + trimmedPackaging);
        }
    }

    public ConversionISO2709 newConversionISO2709(ConversionParam param) {
        // Not thread-safe
        if (documentBuilderFactory == null) {
            documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);
        }
        try {
            return new ConversionISO2709(param, documentBuilderFactory.newDocumentBuilder());
        } catch (ParserConfigurationException e) {
            throw new ConversionException("Unable to create XML document builder", e);
        }
    }
}
