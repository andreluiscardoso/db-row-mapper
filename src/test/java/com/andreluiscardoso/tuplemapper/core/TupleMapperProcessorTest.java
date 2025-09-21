package com.andreluiscardoso.tuplemapper.core;

import com.andreluiscardoso.tuplemapper.annotations.TupleField;
import com.andreluiscardoso.tuplemapper.annotations.TupleMapper;
import com.andreluiscardoso.tuplemapper.converter.TupleConverter;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TupleMapperProcessorTest {

    @TupleMapper
    static class Partial {
        private String ignoredField;

        @TupleField
        private String includedField;

        public String getIgnoredField() {
            return ignoredField;
        }

        public String getIncludedField() {
            return includedField;
        }
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException if tuple is null")
    void shouldThrowIfTupleIsNull() {
        assertThatThrownBy(() -> TupleMapperProcessor.map(null, Partial.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tuple parameter must not be null");
    }

    @Test
    void shouldThrowIfClassIsNotAnnotated() {
        class InvalidClassMapper {
            @TupleField
            private String name;
        }

        Tuple tuple = Mockito.mock(Tuple.class);
        Mockito.when(tuple.getElements()).thenReturn(List.of());

        assertThatThrownBy(() -> TupleMapperProcessor.map(tuple, InvalidClassMapper.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is not annotated with @TupleMapper");
    }

    @Test
    void shouldIgnoreFieldsWithoutAnnotation() {
        Tuple tuple = Mockito.mock(Tuple.class);
        @SuppressWarnings("unchecked")
        TupleElement<String> element = Mockito.mock(TupleElement.class);
        Mockito.when(element.getAlias()).thenReturn("includedField");
        Mockito.when(tuple.getElements()).thenReturn(List.of(element));
        Mockito.when(tuple.get("includedField")).thenReturn("value");

        Partial obj = TupleMapperProcessor.map(tuple, Partial.class);

        assertThat(obj.getIncludedField()).isEqualTo("value");
        assertThat(obj.getIgnoredField()).isNull();
    }

    @TupleMapper
    static class MissingColumn {
        @TupleField(name = "missing")
        private String value;

        public String getValue() {
            return value;
        }
    }

    @Test
    void givenMissingTupleColumnThenValueIsNull() {
        Tuple tuple = Mockito.mock(Tuple.class);
        Mockito.when(tuple.getElements()).thenReturn(List.of());

        MissingColumn obj = TupleMapperProcessor.map(tuple, MissingColumn.class);

        assertThat(obj.getValue()).isNull();
    }

    @TupleMapper
    static class DateClass {
        @TupleField(converter = StringToLocalDateConverter.class)
        private LocalDate date;

        public LocalDate getDate() {
            return date;
        }
    }

    public static class StringToLocalDateConverter implements TupleConverter<String, LocalDate> {
        @Override
        public LocalDate convert(String source) {
            return LocalDate.parse(source);
        }
    }

    @Test
    void shouldApplyCustomConverter() {
        Tuple tuple = Mockito.mock(Tuple.class);
        @SuppressWarnings("unchecked")
        TupleElement<String> element = Mockito.mock(TupleElement.class);
        Mockito.when(element.getAlias()).thenReturn("date");
        Mockito.when(tuple.getElements()).thenReturn(List.of(element));
        Mockito.when(tuple.get("date")).thenReturn("1970-01-01");

        DateClass dto = TupleMapperProcessor.map(tuple, DateClass.class);

        assertThat(dto.getDate()).isEqualTo(LocalDate.of(1970, 1, 1));
    }

    @TupleMapper
    static class DirectAssignment {
        @TupleField
        private String name;

        public String getName() {
            return name;
        }
    }

    @Test
    void shouldAssignValueDirectlyIfNoConverterSpecified() {
        Tuple tuple = Mockito.mock(Tuple.class);
        @SuppressWarnings("unchecked")
        TupleElement<String> element = Mockito.mock(TupleElement.class);
        Mockito.when(element.getAlias()).thenReturn("name");
        Mockito.when(tuple.getElements()).thenReturn(List.of(element));
        Mockito.when(tuple.get("name")).thenReturn("Java");

        DirectAssignment obj = TupleMapperProcessor.map(tuple, DirectAssignment.class);

        assertThat(obj.getName()).isEqualTo("Java");
    }

    @TupleMapper
    static class ExceptionConverter {
        @TupleField(converter = ExplodingConverter.class)
        private String field;

        public String getField() {
            return field;
        }
    }

    public static class ExplodingConverter implements TupleConverter<String, String> {
        @Override
        public String convert(String source) {
            throw new RuntimeException("boom");
        }
    }

    @Test
    void shouldWrapExceptionFromConverter() {
        Tuple tuple = Mockito.mock(Tuple.class);
        @SuppressWarnings("unchecked")
        TupleElement<String> element = Mockito.mock(TupleElement.class);
        Mockito.when(element.getAlias()).thenReturn("field");
        Mockito.when(tuple.getElements()).thenReturn(List.of(element));
        Mockito.when(tuple.get("field")).thenReturn("fail");

        assertThatThrownBy(() -> TupleMapperProcessor.map(tuple, ExceptionConverter.class))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error mapping field 'field'")
                .hasMessageContaining("ExceptionConverter");
    }

    @Test
    @DisplayName("Should map a list of tuples correctly")
    void shouldMapListOfTuples() {
        Tuple tuple1 = Mockito.mock(Tuple.class);
        Tuple tuple2 = Mockito.mock(Tuple.class);

        @SuppressWarnings("unchecked")
        TupleElement<String> element1 = Mockito.mock(TupleElement.class);
        Mockito.when(element1.getAlias()).thenReturn("includedField");
        Mockito.when(tuple1.getElements()).thenReturn(List.of(element1));
        Mockito.when(tuple1.get("includedField")).thenReturn("value1");

        Mockito.when(tuple2.getElements()).thenReturn(List.of(element1));
        Mockito.when(tuple2.get("includedField")).thenReturn("value2");

        List<Tuple> tuples = List.of(tuple1, tuple2);

        List<Partial> partials = TupleMapperProcessor.mapList(tuples, Partial.class);

        assertThat(partials).hasSize(2);
        assertThat(partials.get(0).getIncludedField()).isEqualTo("value1");
        assertThat(partials.get(1).getIncludedField()).isEqualTo("value2");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException if list of tuples is null")
    void shouldThrowIfTupleListIsNull() {
        assertThatThrownBy(() -> TupleMapperProcessor.mapList(null, Partial.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tuples list must not be null");
    }

    @Test
    @DisplayName("Should return an empty list if input list is empty")
    void shouldReturnEmptyListForEmptyInput() {
        List<Partial> partials = TupleMapperProcessor.mapList(List.of(), Partial.class);
        assertThat(partials).isEmpty();
    }

}