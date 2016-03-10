package com.thehellings.gully.orientdb.types;

import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.index.OIndexUnique;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.thehellings.gully.orientdb.annotations.Entity;
import com.thehellings.gully.orientdb.annotations.Field;
import com.thehellings.gully.orientdb.exceptions.UnsupportedFieldType;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Converts any class type to a list of field names annotated for declaration within OrientDB
 * <p>
 *     This type is used internally by the library to convert a class object to an array of {@link NameType} objects
 *     which can be inserted into the database as appropriate within their class. The class is probably not too useful
 *     for any other purpose.
 * </p>
 */
public class Type {
	private Class clazz;
	private String name;
	private static Logger log = Logger.getLogger(Type.class.getName());

	public Type(Class clazz) {
		this.clazz = clazz;
		final Entity entity = (Entity) this.clazz.getDeclaredAnnotation(Entity.class);
		if (entity != null) {
			this.name = entity.name();
			// Exact euqality in case the programmer wants to denote an empty string for that field
			if (this.name == Entity.DEFAULT) {
				this.name = this.clazz.getSimpleName();
			}
		}
	}

	/**
	 * Returns a list of types drawn from parent classes and interfaces.
	 * <p>
	 *     Gets a list of all the direct superclass and implemented interface types that are intended to be created as
	 *     a database type. Does not handle recursive parentage, or cases where one or more intervening types are not
	 *     annotated as database types. Only directly extended and implemented types will be returned.
	 * </p>
	 * <p>
	 *     Since orientdb permits multiple inheritence, this type system also supports multiple inheritence, although
	 *     the limitations of Java force the implementation to permit only one direct superclass. However, since both
	 *     fields and methods may be annotated as database fields, a Java interface may be used to represent a multiple
	 *     inheritence scenario.
	 * </p>
	 *
	 * @return List of all the direct parent types to set in the database
	 */
	public List<Type> findParentTypes() {
		List<Type> parents = new LinkedList<>();
		// Check actual superclass
		Type parent = new Type(this.clazz.getSuperclass());
		if (parent.isDatabaseType()) {
			parents.add(parent);
		}
		// Check super-interfaces, etc
		Class[] classes = this.clazz.getInterfaces();
		for (Class cls : classes) {
			Type type = new Type(cls);
			if (type.isDatabaseType()) {
				parents.add(type);
			}
		}
		try {
			List<NameType> fields = this.collectFields();
			for (NameType field : fields)
				if (field.isRequiresTargetClass()) {
					Type type = new Type(field.getTargetClass());
					if (type.isDatabaseType())
						parents.add(type);
					else
						throw new UnsupportedFieldType("Cannot determine parents for " + field.getName());
				}
		} catch(UnsupportedFieldType ex) {
			log.log(Level.WARNING, "Unable to collect fields. Cannot determine all parent types.");
		}

		return parents;
	}

	/**
	 * Creates both this type and all of its parent types in the database as OrientDB classes.
	 * <p>
	 *     Types associated with direct parental linkage will be recurisvely created until the base of the hierarchy is
	 *     reached or untill there is a linkage gap in the POJO class structure (only direct parents are considered, not
	 *     grandparents or other distant ancestor types, unless every interceding generation is also annotated as an
	 *     {@link Entity}.
	 * </p>
	 * <p>
	 *     Only the class itself is created. Fields are not configured, etc. For that method, see {@link #configureFields(OClass)}
	 *     which can accept as input the OClass returned from this method.
	 * </p>
	 * <p>
	 *     This method is idempotent. It is safe to invoke this method repeatedly on the same {@link OSchema} without
	 *     harming the schema or its data. If the type already exists, a reference to that type is returned and the
	 *     creation is a no-op.
	 * </p>
	 *
	 * @param schema The OrientDB database schema within which to create this type
	 * @return The {@link OClass} object representing this type - newly minted or not
	 */
	public OClass createInSchema(final OSchema schema) {
		OClass oClass = schema.getClass(this.getName());
		if (oClass != null) {
			return oClass;
		}
		// First, ensure that all parent types exist in the schema
		List<Type> parentTypes = this.findParentTypes();
		List<OClass> oParentTypes = new ArrayList<>(parentTypes.size());
		for (Type type : parentTypes) {
			oParentTypes.add(type.createInSchema(schema));
		}
		// Do not attempt to re-add self if self already exists - this means that changes in inheritance structure will
		// not be honored
		// TODO: See if this can be improved to handle restructuring type graph
		oClass = schema.createClass(this.getName(), oParentTypes.toArray(new OClass[oParentTypes.size()]));
		return oClass;
	}

	/**
	 * Insert the probed fields onto the given {@link OClass} object.
	 * <p>
	 *     Typically this method would be called immediately after invoking the {@link #createInSchema(OSchema)}} method
	 *     to define the class itself. This method cannot and will not create the class itself or modify where the class
	 *     lives nor its hierarchy.
	 * </p>
	 * <p>
	 *     Like the createInSchema method, this method is idempotent. It is safe to call it repeatedly on existing data
	 *     as subsequent calls will be no-ops. However, if you add a new field to the POJO definition, then invoking this
	 *     method will declare that field on the corresponding database class.
	 * </p>
	 *
	 * @param oClass The database class that should have fields added to it.
	 */
	public void configureFields(OClass oClass) {
		try {
			List<NameType> fields = this.collectFields();
			for (NameType field : fields) {
				if (oClass.getProperty(field.getName()) == null) {
					// TODO: Detect and utilize references to other types - currently, only primitive fields are supported
					oClass.createProperty(field.getName(), field.getType());
				}
				// Check special index for uniqueness guarantee
				if (field.isUnique() && !this.hasIndex(field.getName(), oClass)) {
					oClass.createIndex(field.getName(), OClass.INDEX_TYPE.UNIQUE, field.getName());
				}
				// TODO: Support indexing on fields
			}
		} catch(UnsupportedFieldType exception) {
			log.log(Level.WARNING, "Warning - cannot parse improper annotation. Field skipped.", exception);
		}
	}

	/**
	 * Indicates whether this type is annotated as a valid Database Object or not
	 * <p>
	 *     Indicates whether this class type represents a type that is supposed to be inserteed into the database or
	 *     not.
	 * </p>
	 *
	 * @return True if type should be inserted to the database, false otherwise
	 */
	public boolean isDatabaseType() {
		return this.clazz.isAnnotationPresent(Entity.class);
	}

	/**
	 * The name of the database entity to be created from this type.
	 * <p>
	 *     Unless a value is specified explicitly in the {@link Entity} annotation, then the type will be equal to the
	 *     value returned from the {@link Class#getSimpleName()} method.
	 * </p>
	 * @return the name of the database entity
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Retrieves a list of all directly annotated databse fields on this type
	 * <p>
	 *     Names and types will be paried together in a {@link NameType} object that defines the type of the field to
	 *     create.
	 * </p>
	 *
	 * @return The list of all fields annotated directly onto this type
	 * @throws UnsupportedFieldType when a type other than a {@link java.lang.reflect.Field} or {@link Method} is annotated
	 * as such
	 */
	public List<NameType> collectFields() throws UnsupportedFieldType {
		final List<NameType> fieldNames = new LinkedList<>();
		this.collectMethodFields(fieldNames);
		this.collectFieldFields(fieldNames);
		return fieldNames;
	}

	/**
	 * Gathers the names of all fields annotated on delcared methods
	 * <p>
	 *     All the names of fields directly on a declared method are collected, along with their types, and added to the
	 *     presented collection of type pairs.
	 * </p>
	 *
	 * @param fieldNames A mutable collection of fields which are annotated directly onto this type
	 * @return this
	 */
	protected Type collectMethodFields(final Collection<NameType> fieldNames) throws UnsupportedFieldType {
		Method[] methods = this.clazz.getDeclaredMethods();
		for (Method method : methods) {
			this.addFieldName(fieldNames, method);
		}
		return this;
	}

	/**
	 * Gathers the names of all fields annotated on declared fields
	 * <p>
	 *     All the names of database fields that are annotated onto field entries in the Java object are collected and
	 *     added to the provided collection.
	 * </p>
	 *
	 * @param fieldNames A mutable collection of fields which are annotated directly onto this type
	 * @return this
	 */
	protected Type collectFieldFields(final Collection<NameType> fieldNames) throws UnsupportedFieldType {
		java.lang.reflect.Field[] fields = this.clazz.getDeclaredFields();
		for(java.lang.reflect.Field field : fields) {
			this.addFieldName(fieldNames, field);
		}
		return this;
	}

	/**
	 * Adds the field indicated by the AnnotatedElement directly to the provided collection of fields.
	 * <p>
	 *     This will operate on any element which is able to support annotations. If the element is annotated with the
	 *     {@link Field} annotation, then that annotation will be read for its type and added to the list of fields.
	 * </p>
	 * <p>
	 *     If no name is specified in the field, an appropriate name will be constructed based on the type of the
	 *     element being passed.
	 * </p>
	 *
	 * @param fields
	 * @param element
	 */
	protected void addFieldName(Collection<NameType> fields, AnnotatedElement element) throws UnsupportedFieldType {
		if (element.isAnnotationPresent(Field.class)) {
			fields.add(NameType.getNameType(element.getAnnotation(Field.class), element));
		}
	}

	/**
	 * Checks all indexes of an {@link OClass} to see if any have the same name as specified.
	 *
	 * @param indexName Name of index to search for
	 * @param oClass OClass object to search against
	 * @return True if an index with the same name exists, false otherwise
	 */
	protected boolean hasIndex(String indexName, OClass oClass) {
		Set<OIndex<?> > indexes = oClass.getIndexes();
		for (OIndex<?> index : indexes) {
			if (index.getName().equals(indexName)) {
				return true;
			}
		}
		return false;
	}
}
