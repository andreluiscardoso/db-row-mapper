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

public class TupleMapperProcessor {
    
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

    public static <T> List<T> mapList(List<Tuple> tuples, Class<T> targetClass) {
        if (tuples == null) {
            throw new IllegalArgumentException("Tuples list must not be null");
        }

        return tuples.stream().map(tuple -> map(tuple, targetClass)).collect(Collectors.toList());
    }
}