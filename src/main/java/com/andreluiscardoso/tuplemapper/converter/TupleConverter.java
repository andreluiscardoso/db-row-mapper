package com.andreluiscardoso.tuplemapper.converter;

import com.andreluiscardoso.tuplemapper.annotations.TupleField;

/**
 * Interface defining a contract for converting a source value of type {@code S}
 * into a target value of type {@code T}.
 *
 * @param <S> source type
 * @param <T> target type
 */
public interface TupleConverter<S, T> {

    /**
     * Converts the source value to the target type.
     *
     * @param source the value to convert
     * @return the converted value
     */
    T convert(S source);

    /**
     * Sentinel implementation representing "no converter".
     * <p>
     * Using this class in {@link TupleField#converter()} indicates
     * that no conversion is needed.
     * </p>
     */
    final class None implements TupleConverter<Object, Object> {
        @Override
        public Object convert(Object source) {
            throw new UnsupportedOperationException("No converter defined");
        }
    }
}
