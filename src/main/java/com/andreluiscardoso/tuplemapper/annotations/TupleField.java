package com.andreluiscardoso.tuplemapper.annotations;

import com.andreluiscardoso.tuplemapper.converter.TupleConverter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface TupleField {

    String name() default "";

    Class<? extends TupleConverter<?, ?>> converter() default TupleConverter.None.class;
}
