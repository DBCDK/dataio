package dk.dbc.dataio.commons;

import dk.dbc.dataio.jobstore.types.MarcRecordInfo;
import dk.dbc.marc.binding.ControlField;
import dk.dbc.marc.binding.DataField;
import dk.dbc.marc.binding.Field;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.binding.SubField;

import java.util.Optional;

/**
 * This class parses MarcRecord objects into MarcRecordInfo instances.
 * <p>
 * This class is thread safe.
 */
public class MarcRecordInfoBuilder {
    /**
     * Parses given MarcRecord object into its corresponding MarcRecordInfo instance
     *
     * @param marcRecord MarcRecord object to be parsed
     * @return Optional containing MarcRecordInfo object or empty is given MarcRecord was null
     * @throws IllegalStateException on failure to create MarcRecordInfo object
     */
    public Optional<MarcRecordInfo> parse(MarcRecord marcRecord) throws IllegalStateException {
        if (marcRecord == null) {
            return Optional.empty();
        }

        ParseResult parseResult = new ParseResult();
        for (Field<?> field : marcRecord.getFields()) {
            if (parseResult.isComplete()) {
                break;
            }

            switch (field.getTag()) {
                case "001":
                    parse001(parseResult, field);
                    break;
                case "004":
                    parse004(parseResult, (DataField) field);
                    break;
                case "014":
                    parse014(parseResult, (DataField) field);
                    break;
                default:
                    break; // Shuts up findbugs
            }
        }
        return Optional.of(parseResult.toMarcRecordInfo());
    }

    private void parse001(ParseResult parseResult, Field<?> field) {
        /* Handles 001 field either as data field with subfields:

           (documentation - in danish - from http://www.kat-format.dk/danMARC2/Danmarc2.5.htm#pgfId=1532869)
           Felt 001 Postens ID-nummer
            Feltet kan ikke gentages, og delfelterne *a og *f er obligatoriske i alle posttyper og på
            alle katalogiseringsniveauer.
            a       postens id-nummer (hos dataproducenten)
            b       dataproducentens biblioteksnummer
            c       ajourføringstidspunkt (ååååmmddttmmss)
            d       første oprettelsesdato (ååååmmdd)
            e       tegnsæt
            f       format
            g       filnavn
            o       oprindeligt format (maskinkonverterede poster)
            t       nummersystem for id-nummer

           or as control field (https://www.loc.gov/marc/bibliographic/bd001.html)
         */
        if (field instanceof DataField) {
            DataField datafield = (DataField) field;
            Optional<SubField> subfield = datafield.getSubfields().stream().filter(s -> s.getCode() == 'a').findFirst();
            subfield.ifPresent(subField -> parseResult.id = subField.getData());
        } else {
            parseResult.id = ((ControlField) field).getData();
        }
    }

    private void parse004(ParseResult parseResult, DataField datafield) {
        /* Felt 004 Kode for poststatus og posttype
            Feltet kan ikke gentages og er obligatorisk i alle posttyper og på alle katalogiseringsniveauer.
            r       kode for poststatus
            a       kode for bibliografisk posttype
         */
        datafield.getSubfields().forEach(s -> parse004Subfield(parseResult, s));
    }

    private void parse004Subfield(ParseResult parseResult, SubField subfield) {
        switch (subfield.getCode()) {
            case 'a':
                parseResult.type = getRecordType(subfield.getData());
                break;
            case 'r':
                parseResult.isDelete = "d".equals(subfield.getData());
                break;
            default:
                break;  // shuts up findbugs
        }
    }

    private MarcRecordInfo.RecordType getRecordType(String data) {
        /* e Enkeltstående post
           h Hovedpost
           s Sektionspost
           b Bindpost
           f Underordnet analysepost (reserveret til fremtidigt brug)
           i Enkeltstående analysepost (I-analyse)
         */
        switch (data) {
            case "h":
                return MarcRecordInfo.RecordType.HEAD;
            case "s":
                return MarcRecordInfo.RecordType.SECTION;
            case "b":
                return MarcRecordInfo.RecordType.VOLUME;
            default:
                return MarcRecordInfo.RecordType.STANDALONE;
        }
    }

    private void parse014(ParseResult parseResult, DataField datafield) {
        /* Felt 014 ID-nummer på post på højere niveau
            Feltet kan ikke gentages og er obligatorisk ved katalogisering i flerpoststruktur.
            a       id-nummer på post på højere niveau
            x       typekode
         */
        Optional<SubField> subfield = datafield.getSubfields().stream().filter(s -> s.getCode() == 'a').findFirst();
        subfield.ifPresent(subField -> parseResult.parentRelation = subField.getData());
    }

    /* Private helper class used to gather information during parsing */
    private static class ParseResult {
        String id = null;
        String parentRelation = null;
        boolean isDelete = false;
        MarcRecordInfo.RecordType type;

        boolean isComplete() {
            if (id != null && type != null) {
                switch (type) {
                    case STANDALONE:
                    case HEAD:
                        return true;                    // no need to look at the 014 field in this case
                    default:
                        return parentRelation != null;  // test if the 014 field has been seen yet
                }
            }
            return false;
        }

        MarcRecordInfo toMarcRecordInfo() {
            return new MarcRecordInfo(id, type, isDelete, parentRelation);
        }
    }
}
