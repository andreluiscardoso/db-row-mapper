package com.andreluiscardoso.tuplemapper.annotations;

import com.andreluiscardoso.tuplemapper.converter.TupleConverter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field within a {@link TupleMapper}-annotated class to be mapped
 * from a tuple element.
 * <p>
 * Optional attributes:
 * <ul>
 *     <li>{@code name}: the alias of the tuple element to map. Defaults to the field name.</li>
 *     <li>{@code converter}: a class implementing {@link TupleConverter} to transform the value before assignment.
 *     Defaults to {@link TupleConverter.None} (no conversion).</li>
 * </ul>
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface TupleField {

    /**
     * Specifies the tuple element alias to map to this field.
     * If not provided, the field name is used.
     *
     * @return the tuple element alias
     */
    String name() default "";

    /**
     * Specifies a {@link TupleConverter} class to convert the tuple value before assignment.
     * Defaults to {@link TupleConverter.None}, meaning no conversion is applied.
     *
     * @return the converter class
     */
    Class<? extends TupleConverter<?, ?>> converter() default TupleConverter.None.class;
}
