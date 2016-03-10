 package com.thehellings.gully.orientdb;

import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.thehellings.gully.orientdb.exceptions.InvalidTypeException;
import com.thehellings.gully.orientdb.types.Type;

/**
 * Contains methods for automatically inserting annotated types and fields into the database.
 * <p>
 *     The user is responsible to write some sort of migration script, but it can be very straightforward to add all new
 *     types and fields through this existing mechanism. To create the class in the schema, just call {@link #getOType(Class, OSchema, boolean)}
 *     with the second parameter set to true and then invoke {@link #configureBaseFields(Class, OClass)}.
 * </p>
 * <p>
 *     Types will only be created with this. Currently, there is no support for restructuring types, removing types,
 *     rearranging type hierarchies, or such. Similarly fields can only be created through this mechanism, they will
 *     never be removed, deleted, nor have their types altered.
 * </p>
 * <p>
 *     Creation of indexes automatically is not supported, but that is planned for addition. However, indexes are not
 *     currently planned for automated removal.
 * </p>
 * <p>
 *     Such destructive moves and changes are better handled explicitly in a database migration type of script rather
 *     than in this library. This library provides only convenience methods for creating missing fields and types in
 *     the database not for performing large, complex, and difficult migrations. For that, look at the more generic
 *     database migration types that can be created.
 * </p>
 */
public final class OrientDBEntity {
	/**
	 * Fetches the OType associated with this class object in order to allow further manipulation or linkage.
	 * <p>
	 *     By default the method will only return the OClass already associated with the existing type inside of the
	 *     schema being passed. In order to have the type created when not already present, pass "true" as the second
	 *     parameter. It is safe to pass "true" even if the type is already known to exist, as this method will check
	 *     before creation
	 * </p>
	 * <p>
	 *     There are other possible side effects of invoking this method in create mode - if the type is not present
	 *     and at least oen of the base types that ie extends or implements is also not present, this method will create
	 *     those base types. It is encouraged that the developer ensure base types are already created, but - like this
	 *     method itself - if that type is already present, the no operations will be performed on them. However, it will
	 *     not configure the fields on those base types, so it is still good practice to configure the fields yourself.
	 * </p>
	 *
	 * @param schema The OrientDB schema to create the class in
	 * @param createIfNotFound True to create the object if not found before returning, false to return null on not found
	 * @return The {@link OClass} type associated with this class, null if not found and create not specified
	 * @throws InvalidTypeException When the class is not properly annotated for creation
	 */
	public static OClass getOType(Class cls, final OSchema schema, final boolean createIfNotFound) throws InvalidTypeException {
		Type type = new Type(cls);
		if (!type.isDatabaseType()) {
			throw new InvalidTypeException(String.format("The type %s must be annotated properly in order to be auto-created.", cls.getName()));
		}
		OClass oClass = schema.getClass(type.getName());
		if (oClass == null && createIfNotFound) {
			oClass = type.createInSchema(schema);
		}
		return oClass;
	}

	/**
	 * Declares all the annotated fields in the POJO class as fields of the OrientDB type.
	 * <p>
	 *     This particular invocation will fetch the type from the database schema prior to configuring it. However, it
	 *     will not create the type if it does not already exist. For that, invoke the {@link #getOType(Class, OSchema, boolean)}
	 *     method directly.
	 * </p>
	 *
	 * @param schema The schema where the type resides
	 * @throws InvalidTypeException Indicating that the type has not yet been created in the Schema
	 */
	public static void configureBaseFields(Class cls, final OSchema schema) throws InvalidTypeException {
		OClass oClass = OrientDBEntity.getOType(cls, schema, false);
		if (oClass != null) {
			OrientDBEntity.configureBaseFields(cls, oClass);
		} else {
			throw new InvalidTypeException("Type not found in database. Be sure to create type before configuration.");
		}
	}

	/**
	 * Configures all the declared fields within this POJO type into the OrientDB type procided.
	 * <p>
	 *     This could be good when templating out multiple database types that are not directly related to one another,
	 *     but which use the same POJO definition.
	 * </p>
	 * <p>
	 *     Field creation is idempotent, where a field that already exists will not be created or modified if this method
	 *     is called again. This includes, the type of the field will not change if it differs from the type specified
	 *     by the annotation. For potentially destructive or content-altering, the conversion is left to the end user
	 *     to specify in a method appropriate to their type.
	 * </p>
	 *
	 * @param oClass The OrientDB class upon which to define the fields associated with this class
	 */
	public static void configureBaseFields(Class cls, final OClass oClass) {
		Type type = new Type(cls);
		type.configureFields(oClass);
	}
}
