package org.store.app.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generischer Wrapper, um primitive oder einfache Werte mit Typinformationen
 * im Redis-Cache sicher und konsistent zu speichern.
 * Dies verhindert Probleme bei der Serialisierung und Deserialisierung.
 *
 * @param <T> Typ des eingewickelten Werts
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValueWrapper<T> {

    private T value;


}
