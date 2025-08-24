package com.andreluiscardoso.tuplemapper.converter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class TupleConverterTest {

    @Test
    void noneConverterShouldAlwaysThrow() {
        TupleConverter<Object, Object> converter = new TupleConverter.None();
        assertThatThrownBy(() -> converter.convert("test"))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("No converter defined");
    }

    static class UpperCaseConverter implements TupleConverter<String, String> {
        @Override
        public String convert(String source) {
            return source.toUpperCase();
        }
    }

    @Test
    void customConverterShouldWork() {
        UpperCaseConverter converter = new UpperCaseConverter();
        assertThat(converter.convert("abc")).isEqualTo("ABC");
    }
}
