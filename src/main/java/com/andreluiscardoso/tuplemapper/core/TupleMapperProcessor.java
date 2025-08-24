package com.andreluiscardoso.tuplemapper.core;

import com.andreluiscardoso.tuplemapper.annotations.TupleField;
import com.andreluiscardoso.tuplemapper.annotations.TupleMapper;
import com.andreluiscardoso.tuplemapper.converter.TupleConverter;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Processor responsible for mapping a {@link Tuple} into
 * an instance of a class annotated with {@link TupleMapper}.
 * <p>
 * For each field annotated with {@link TupleField}, the corresponding tuple element
 * will be assigned. If a {@link TupleConverter} is specified, it will be applied
 * before assignment.
 * </p>
 */
public class TupleMapperProcessor {

    /**
     * Maps a {@link Tuple} into an instance of the given {@code targetClass}.
     * <p>
     * The target class must be annotated with {@link TupleMapper}. For each field
     * annotated with {@link TupleField}, the value from the tuple will be assigned.
     * If a {@link TupleConverter} is provided, it will be applied before assignment.
     * </p>
     *
     * @param tuple       the tuple to map; must not be {@code null}
     * @param targetClass the class to instantiate and populate; must be annotated with {@link TupleMapper}
     * @param <T>         the type of the target class
     * @return an instance of {@code targetClass} populated with tuple values
     * @throws IllegalArgumentException if {@code tuple} is null or {@code targetClass} is not annotated with {@link TupleMapper}
     * @throws RuntimeException         if any reflection or conversion error occurs
     */
    public static <T> T map(Tuple tuple, Class<T> targetClass) {
        if (tuple == null) {
            throw new IllegalArgumentException("Tuple parameter must not be null");
        }

        if (!targetClass.isAnnotationPresent(TupleMapper.class)) {
            throw new IllegalArgumentException("Class " + targetClass.getName() + " is not annotated with @TupleMapper");
        }

        try {
            Constructor<T> constructor = targetClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            T instance = constructor.newInstance();

            Map<String, Object> tupleValues = new HashMap<>();
            for (TupleElement<?> element : tuple.getElements()) {
                tupleValues.put(element.getAlias(), tuple.get(element));
            }

            for (Field field : targetClass.getDeclaredFields()) {
                if (!field.isAnnotationPresent(TupleField.class)) continue;

                TupleField tupleField = field.getAnnotation(TupleField.class);
                String fieldName = tupleField.name().isEmpty() ? field.getName() : tupleField.name();
                Object value = tupleValues.get(fieldName);

                if (value == null) continue;

                Class<? extends TupleConverter<?, ?>> converterClass = tupleField.converter();

                // Apply converter only if specified
                if (!converterClass.equals(TupleConverter.None.class)) {
                    @SuppressWarnings("unchecked")
                    TupleConverter<Object, Object> converter = (TupleConverter<Object, Object>) converterClass.getDeclaredConstructor().newInstance();
                    value = converter.convert(value);
                }

                field.setAccessible(true);
                field.set(instance, value);
            }

            return instance;

        } catch (Exception e) {
            throw new RuntimeException("Error mapping Tuple to " + targetClass.getName(), e);
        }
    }

    /**
     * Maps a list of {@link Tuple} instances into a list of {@code targetClass} instances.
     * <p>
     * Each {@link Tuple} is processed using {@link #map(Tuple, Class)}, applying
     * any converters and field mappings defined by {@link TupleField} annotations.
     * </p>
     *
     * @param tuples      the list of tuples to map; must not be {@code null}
     * @param targetClass the class to instantiate and populate; must be annotated with {@link TupleMapper}
     * @param <T>         the type of the target class
     * @return a list of {@code targetClass} instances populated with tuple values; never {@code null}
     * @throws IllegalArgumentException if {@code tuples} is {@code null} or {@code targetClass} is not annotated with {@link TupleMapper}
     * @throws RuntimeException         if any reflection or conversion error occurs for any tuple
     */
    public static <T> List<T> mapList(List<Tuple> tuples, Class<T> targetClass) {
        if (tuples == null) {
            throw new IllegalArgumentException("Tuples list must not be null");
        }

        return tuples.stream().map(tuple -> map(tuple, targetClass)).collect(Collectors.toList());
    }
}