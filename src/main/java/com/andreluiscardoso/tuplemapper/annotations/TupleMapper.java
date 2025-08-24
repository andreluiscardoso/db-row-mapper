package com.andreluiscardoso.tuplemapper.annotations;


import com.andreluiscardoso.tuplemapper.core.TupleMapperProcessor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as eligible for mapping from a {@link jakarta.persistence.Tuple}.
 * <p>
 * Classes annotated with {@code @TupleMapper} can be processed by
 * {@link TupleMapperProcessor} to automatically map tuple elements to their fields.
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TupleMapper {
}
