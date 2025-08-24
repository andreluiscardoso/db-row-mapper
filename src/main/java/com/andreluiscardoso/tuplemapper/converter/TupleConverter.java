package com.andreluiscardoso.tuplemapper.converter;

public interface TupleConverter<S, T> {

    T convert(S source);

    final class None implements TupleConverter<Object, Object> {
        @Override
        public Object convert(Object source) {
            throw new UnsupportedOperationException("No converter defined");
        }
    }
}
