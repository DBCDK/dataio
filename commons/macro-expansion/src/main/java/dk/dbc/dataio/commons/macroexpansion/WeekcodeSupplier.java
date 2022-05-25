package dk.dbc.dataio.commons.macroexpansion;

import java.time.LocalDate;

@FunctionalInterface
public interface WeekcodeSupplier {
    String get(String catalogueCode, LocalDate localDate);
}
