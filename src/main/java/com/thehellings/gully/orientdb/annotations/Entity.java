package com.thehellings.gully.orientdb.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Denotes a type whose members annotated with {@link Field} should be saved to the database.
 * <p>
 *     Any type that is annotated with this represents a class that ought to be declared in the OrientDB database. If
 *     this type has a DIRECT supertype that it extends or implements that is also annotated as an Entity, then this
 *     type will be created as a subtype of those.
 * </p>
 * <p>
 *     An entity can only be created with its direct fields or methods annotated. Annotating fields declared in a parent
 *     type will not be picked up in this type, as only {@link Class#getDeclaredFields()} and {@link Class#getDeclaredMethods()}
 *     will be checked, and not any methods or fields that come from inherited types.
 * </p>
 * <p>
 *     It is important, if no "name" value is specified, that the value of the class name as returned by
 *     {@link Class#getSimpleName()} be a valid name within OrientDB and also that it be unique within the declared
 *     namespace.
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Entity {
	String name() default DEFAULT;

	String DEFAULT = "";
}
