package com.thehellings.gully.orientdb.annotations;

import com.orientechnologies.orient.core.metadata.schema.OType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Denotes that a particular method or field is to be placed into the schema for the class type in OrientDB.
 * <p>
 *     If placed on a field, the name of the field in Java will match the name of the field in OrientDB. If placed on
 *     a method, the name of the field in OrientDB will match the name of the method with any prepended get/set removed.
 *     To override the default naming option, set the <code>name</code> field in the annotation to the exact value
 *     that you want.
 * </p>
 * <p>
 *     <code>
 *         @Field
 *         private String name;
 *         @Field
 *         private String email;
 *         @Field(name = "age")
 *         private int ageOfPerson;
 *         @Field(type = OType.STRING);
 *         private JSONObject extraData;
 *         @Field(name = "address")
 *         public String getHomeAddress() {...}
 *     </code>
 * </p>
 * <p>
 *     In order for an annotated field or method to be properly designated as a member of the fields for its type, the
 *     annotation and the method or field must be declared directly in the type that is annotated with @Entity. This
 *     annotation, then, is not useful in a class without the {@link Entity} annotation, as only those classes will be
 *     checked for their directly declared members. Members declared in parent types are ignored, regardless of their
 *     annotation status.
 * </p>
 * <p>
 *     However, OrientDB since version 2.1 does support multiple inheritance and has long supported inheritance in
 *     general, so it is easily possible to model the relationship by having both types annotated.
 * </p>
 * <p>
 *     Fields as named through this annotation will only ever be created, and not destroyed. There is no way for this
 *     library to automatically clean up dangling fields - only create them. It is up to the administrator of the system
 *     to handle deletion in another way.
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface Field {
	/**
	 * The name of the field in the database.
	 * <p>
	 *     This value needs to a valid database identifier for this class type. If you do not provide a value, then this
	 *     will default to the same as the field name provided by reflection.
	 * </p>
	 *
	 * @return The name of the database field
	 */
	String name() default DEFAULT;

	/**
	 * The datatype to store in the field
	 * <p>
	 *     The type to define for the field. Defaults to just a basic string type.
	 * </p>
	 *
	 * @return The type for the particular document field
	 */
	OType type() default OType.STRING;

	/**
	 * The target type to link against
	 * <p>
	 *     For instances of {@link OType} that have a target type - e.g. {@link OType#LINK} and its friends and
	 *     {@link OType#EMBEDDED} and its friends - this is the class type of the target. Leave this null if there is
	 *     no such target type.
	 * </p>
	 *
	 * @return The class representing the target type, null if none
	 */
	Class target() default Field.class; // Using Field.class because, apparently, null is not a valid default

	/**
	 * Whether this field should have a unique index created on it.
	 * <p>
	 *     Although OrientDB does not provide a direct method on its class type to require a field be unique, it does allow
	 *     creation of an index on a single column that sets the uniqueness property.
	 * </p>
	 *
	 * @return True if the field should have a unique index created for it, defaults to false
	 */
	boolean unique() default false;

	/*
	 * Taking advantage of the reference equivalence in Java, we can compare directly against this value to see if the
	 * user of our annotation has provided a value or not.
	 */
	String DEFAULT = "";
}
