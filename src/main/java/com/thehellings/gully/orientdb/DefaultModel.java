package com.thehellings.gully.orientdb;

import com.orientechnologies.orient.core.record.impl.ODocument;

/**
 * Convenience class that provides some shared ODocument-based functionality
 * <p>
 *     Provides basic functionality, such as saving, for models that are based on ODocument. Inheriting this class is by
 *     no means necessary and very often might be considered wise. However, for those times when you just need a basic
 *     model, this can provide some of the shared boilerplate code.
 * </p>
 */
public class DefaultModel {
	protected ODocument document;

	public DefaultModel(final ODocument document) {
		this.document = document;
	}

	/**
	 * Saves the underlying document to the database.
	 * <p>
	 *     Changes to the underlying object are not completed until this method is called. Otherwise, they are simply stored
	 *     in volatile memory. Calling this method is the responsibility of any calling code, as it will not be automatically
	 *     called.
	 * </p>
	 */
	public void save() {
		this.document.save();
	}

	/**
	 * Returns the underlying ODocument object
	 * <p>
	 *     Use this for when it is necessary to operate on the underlying object and not on a wrapper class.
	 * </p>
	 *
	 * @return The underlying, raw {@link ODocument} object
	 */
	public ODocument getDocument() {
		return this.document;
	}
}
