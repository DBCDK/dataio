package dk.dbc.dataio.gatekeeper.wal;

import dk.dbc.dataio.gatekeeper.operation.Opcode;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class OpcodeConverter implements AttributeConverter<Opcode, String> {
    @Override
    public String convertToDatabaseColumn(Opcode opcode) {
        return opcode.name();
    }

    @Override
    public Opcode convertToEntityAttribute(String opcodeName) {
        return Opcode.valueOf(opcodeName);
    }
}
