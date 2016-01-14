/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.jobstore.service.util;

import dk.dbc.dataio.jobstore.types.MarcRecordInfo;
import dk.dbc.marc.binding.DataField;
import dk.dbc.marc.binding.Field;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.binding.SubField;

import java.util.Optional;

/**
 * This class parses MarcRecord objects into MarcRecordInfo instances.
 *
 * This class is thread safe.
 */
public class MarcRecordInfoBuilder {
    /**
     * Parses given MarcRecord object into its corresponding MarcRecordInfo instance
     * @param marcRecord MarcRecord object to be parsed
     * @return Optional containing MarcRecordInfo object or empty is given MarcRecord was null
     * @throws IllegalStateException on failure to create MarcRecordInfo object
     */
    public Optional<MarcRecordInfo> parse(MarcRecord marcRecord) throws IllegalStateException {
        if (marcRecord == null) {
            return Optional.empty();
        }

        final ParseResult parseResult = new ParseResult();
        for (Field field : marcRecord.getFields()) {
            if (parseResult.isComplete()) {
                break;
            }

            switch (field.getTag()) {
                case "001": parse001(parseResult, (DataField) field);
                    break;
                case "004": parse004(parseResult, (DataField) field);
                    break;
                case "014": parse014(parseResult, (DataField) field);
                    break;
                default: break; // Shuts up findbugs
            }
        }
        return Optional.of(parseResult.toMarcRecordInfo());
    }

    private void parse001(ParseResult parseResult, DataField datafield) {
        /* Felt 001 Postens ID-nummer
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
         */
        final Optional<SubField> subfield = datafield.getSubfields().stream().filter(s -> s.getCode() == 'a').findFirst();
        if (subfield.isPresent()) {
            parseResult.id = subfield.get().getData();
        }
    }

    private void parse004(ParseResult parseResult, DataField datafield) {
        /* Felt 004 Kode for poststatus og posttype
            Feltet kan ikke gentages og er obligatorisk i alle posttyper og på alle katalogiseringsniveauer.
            r       kode for poststatus
            a       kode for bibliografisk posttype
         */
        datafield.getSubfields().stream().forEach(s -> parse004Subfield(parseResult, s));
    }

    private void parse004Subfield(ParseResult parseResult, SubField subfield) {
        switch (subfield.getCode()) {
            case 'a': parseResult.type = getRecordType(subfield.getData());
                break;
            case 'r': parseResult.isDelete = "d".equals(subfield.getData());
                break;
            default: break;  // shuts up findbugs
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
            case "h": return MarcRecordInfo.RecordType.HEAD;
            case "s": return MarcRecordInfo.RecordType.SECTION;
            case "b": return MarcRecordInfo.RecordType.VOLUME;
            default: return MarcRecordInfo.RecordType.STANDALONE;
        }
    }

    private void parse014(ParseResult parseResult, DataField datafield) {
        /* Felt 014 ID-nummer på post på højere niveau
            Feltet kan ikke gentages og er obligatorisk ved katalogisering i flerpoststruktur.
            a       id-nummer på post på højere niveau
            x       typekode
         */
        final Optional<SubField> subfield = datafield.getSubfields().stream().filter(s -> s.getCode() == 'a').findFirst();
        if (subfield.isPresent()) {
            parseResult.parentRelation = subfield.get().getData();
        }
    }

    /* Private helper class used to gather up information during parsing */
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

        MarcRecordInfo toMarcRecordInfo() throws IllegalStateException {
            try {
                return new MarcRecordInfo(id, type, isDelete, parentRelation);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
