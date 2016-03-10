package com.thehellings.gully.orientdb.types;

import com.google.common.collect.Sets;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.thehellings.gully.orientdb.annotations.Field;
import com.thehellings.gully.orientdb.exceptions.UnsupportedFieldType;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A simple utility class that keeps together the various field definition results, along with handling proper default
 * values.
 * <p>
 *     This type is not intended for direct consumption outside of this library. It is used to help normalize the results
 *     of the annotation of a field or method with the {@link Field} annotation. It will query the annotation and set
 *     logical defaults where values are not set within the annotation.
 * </p>
 */
public class NameType {
	private String name;
	private OType type;
	private Class targetClass = null;
	private boolean unique;
	private boolean requiresTargetClass = false;
	private static Logger log = Logger.getLogger(NameType.class.getName());

	/**
	 * List of types that require linking against a target type
	 * <p>
	 *     If an annotation is provided that is one of these types, then it will need to have a provided linked class
	 *     type.
	 * </p>
	 */
	private static final Set<OType> linkTypes = Sets.immutableEnumSet(
			OType.LINK,
			OType.LINKBAG,
			OType.LINKLIST,
			OType.LINKMAP,
			OType.LINKSET,
			OType.EMBEDDED,
			OType.EMBEDDEDLIST,
			OType.EMBEDDEDMAP,
			OType.EMBEDDEDSET
	);

	/**
	 * Directly create a field. Useful for testing or manual interaction with the library.
	 *
	 * @param name The database-safe name for this field
	 * @param type The data type for this field
	 */
	private void init(final String name, final OType type, final boolean unique, final Class targetClass) {
		this.name = name;
		this.type = type;
		this.unique = unique;
		this.targetClass = (targetClass != Field.class ? targetClass : null);

		if (targetClass != Field.class) {
			this.requiresTargetClass = true;
		} else {
			this.requiresTargetClass = false;
		}
	}

	/**
	 * Reads values directly from the {@link Field} annotation without any consideration for querying default values.
	 *
	 * @param field The annotation containing the relevant data
	 */
	public NameType(final Field field) {
		init(field.name(), field.type(), field.unique(), field.target());
	}

	/**
	 * Creates a field with handling for default naming of the field if the default is accepted.
	 * <p>
	 *     By default the name will be the name of the field will be the same as the annotated method itself. However,
	 *     if the name of the method starts with "get", then the string "get" will be stripped off and the remainder
	 *     will be used with the first character reduced to lowercase. This default behavior is skipped if the full
	 *     name of the method is "get" with no additional string.
	 * </p>
	 *
	 * @param field The field providing the type and name information
	 * @param method The method from which to read the default name if one is required
	 */
	public NameType(final Field field, Method method) {
		this(field);
		// Override the value set by the other constructor, if necessary
		if (field.name() == Field.DEFAULT) {
			String methodName = method.getName();
			if (methodName.startsWith("get") && methodName.length() > 3) {
				this.name = methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
			}
 		} else {
			this.name = field.name();
		}
	}

	/**
	 * Creates a field with handling for default naming of the field if the default is accepted.
	 * <p>
	 *     Unlike with methods, this default is very straightforward - if not otherwise specified, the name for this
	 *     field is exactly the same as the POJO field that the annotation rests on
	 * </p>
	 *
	 * @param field The field providing the type and name information
	 * @param javaField The POJO field upon which the annotation rests to derive default information from
	 */
	public NameType(final Field field, final java.lang.reflect.Field javaField) {
		this(field);
		// Override the name set by the earlier constructor, if necessary
		if (field.name() == Field.DEFAULT) {
			this.name = javaField.getName();
		} else {
			this.name = field.name();
		}
	}

	/**
	 * The determined name provided by the annotation or scoured from the defaults
	 *
	 * @return The name of this type field
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Manually specify a name after the type has been created.
	 *
	 * @param name The new name of the field to use
	 */
	public void setName(final String name) {
		this.name = name;
	}

	/**
	 * The databse type to define upon the new field
	 *
	 * @return The {@link OType} defined as the field type for this field
	 */
	public OType getType() {
		return this.type;
	}

	/**
	 * Whether this field should contain only unique values.
	 * <p>
	 *     As set by the {@link Field} annotation or such, this represents a field within the classes that needs to be a
	 *     unique value throughout the type.
	 * </p>
	 *
	 * @return True if the field should be unique, false otherwise
	 */
	public boolean isUnique() {
		return this.unique;
	}

	/**
	 * The target link class
	 *
	 * @return The {@link Class} that is the intended target of link or embed
	 */
	public Class getTargetClass() {
		return this.targetClass;
	}

	/**
	 * Whether this class requires specifying a target link type
	 *
	 * @return True if a target link type is required, false otherwise
	 */
	public boolean isRequiresTargetClass() {
		return this.requiresTargetClass;
	}

	/**
	 * Factory method to create the NameType based on what information is available from the fields.
	 *
	 * @param field The annotation read from the POJO member
	 * @param element The POJO member upon which the annotation lives
	 * @return A properly configured {@link NameType} object containing the relevant definition information
	 * @throws UnsupportedFieldType Indicates the {@link Field} annotaiton lives on an unsupported type
	 */
	public static NameType getNameType(final Field field, final AnnotatedElement element) throws UnsupportedFieldType {
		if (element instanceof Method) {
			return new NameType(field, (Method) element);
		} else if (element instanceof java.lang.reflect.Field) {
			return new NameType(field, (java.lang.reflect.Field) element);
		}

		throw new UnsupportedFieldType("An unrecognized field type was found.");
	}
}
