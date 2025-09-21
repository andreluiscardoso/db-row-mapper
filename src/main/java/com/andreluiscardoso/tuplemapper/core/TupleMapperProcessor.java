package com.andreluiscardoso.tuplemapper.core;

import com.andreluiscardoso.tuplemapper.annotations.TupleField;
import com.andreluiscardoso.tuplemapper.annotations.TupleMapper;
import com.andreluiscardoso.tuplemapper.converter.TupleConverter;
import jakarta.persistence.Tuple;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Processor responsible for mapping a {@link Tuple} into
 * an instance of a class annotated with {@link TupleMapper}.
 * <p>
 * For each field annotated with {@link TupleField}, the corresponding tuple element
 * will be assigned. If a {@link TupleConverter} is specified, it will be applied
 * before assignment.
 * </p>
 * <p>
 * This processor uses an internal cache to store metadata about classes and fields,
 * including constructors and converters, to improve performance in repeated mappings.
 * </p>
 */
public class TupleMapperProcessor {

    private static final Map<Class<?>, MapperMetadata<?>> CACHE = new ConcurrentHashMap<>();

    /**
     * Maps a {@link Tuple} into an instance of the given {@code targetClass}.
     * <p>
     * The target class must be annotated with {@link TupleMapper}. For each field
     * annotated with {@link TupleField}, the value from the tuple will be assigned.
     * If a {@link TupleConverter} is provided, it will be applied before assignment.
     * <p>
     * Exceptions thrown during conversion or assignment are wrapped in a {@link RuntimeException}
     * with a message including the field name and class.
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

        MapperMetadata<T> metadata = getMetadata(targetClass);
        T instance = metadata.newInstance();

        for (FieldMapping mapping : metadata.fieldMappings) {
            mapping.apply(instance, tuple);
        }

        return instance;
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

    @SuppressWarnings("unchecked")
    private static <T> MapperMetadata<T> getMetadata(Class<T> targetClass) {
        return (MapperMetadata<T>) CACHE.computeIfAbsent(targetClass, MapperMetadata::new);
    }

    /**
     * Holds metadata for a target class, including constructor and field mappings.
     */
    private static class MapperMetadata<T> {
        private final Constructor<T> constructor;
        private final List<FieldMapping> fieldMappings;

        MapperMetadata(Class<T> targetClass) {
            try {
                this.constructor = targetClass.getDeclaredConstructor();
                this.constructor.setAccessible(true);

                this.fieldMappings = Arrays.stream(targetClass.getDeclaredFields())
                        .filter(f -> f.isAnnotationPresent(TupleField.class))
                        .map(FieldMapping::new)
                        .collect(Collectors.toList());

            } catch (Exception e) {
                throw new RuntimeException("Error building metadata for " + targetClass.getName(), e);
            }
        }

        T newInstance() {
            try {
                return constructor.newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Error instantiating " + constructor.getDeclaringClass().getName(), e);
            }
        }
    }

    /**
     * Represents a single field mapping, including the alias and optional converter.
     */
    private static class FieldMapping {
        private final Field field;
        private final String alias;
        private final TupleConverter<Object, Object> converter;

        FieldMapping(Field field) {
            try {
                this.field = field;
                this.field.setAccessible(true);

                TupleField tupleField = field.getAnnotation(TupleField.class);
                this.alias = tupleField.name().isEmpty() ? field.getName() : tupleField.name();

                if (!tupleField.converter().equals(TupleConverter.None.class)) {
                    @SuppressWarnings("unchecked")
                    TupleConverter<Object, Object> conv =
                            (TupleConverter<Object, Object>) tupleField.converter().getDeclaredConstructor().newInstance();
                    this.converter = conv;
                } else {
                    this.converter = null;
                }
            } catch (Exception e) {
                throw new RuntimeException("Error building field mapping for " + field.getName(), e);
            }
        }

        /**
         * Applies the mapping for a single field: retrieves the value from the tuple,
         * applies the converter if present, and sets the value into the target instance.
         *
         * @param instance the target instance to populate
         * @param tuple    the tuple providing values
         * @throws RuntimeException if conversion or assignment fails, with detailed message
         */
        void apply(Object instance, Tuple tuple) {
            try {
                Object value = tuple.get(alias);
                if (value != null) {
                    if (converter != null) {
                        value = converter.convert(value);
                    }
                    field.set(instance, value);
                }
            } catch (Exception e) {
                throw new RuntimeException("Error mapping field '" + field.getName() + "' of class " + instance.getClass().getSimpleName() + ": " + e.getMessage(), e);
            }
        }
    }

}