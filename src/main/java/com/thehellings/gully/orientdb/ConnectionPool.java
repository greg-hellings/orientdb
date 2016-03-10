package com.thehellings.gully.orientdb;

import com.orientechnologies.orient.core.db.OPartitionedDatabasePool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wraps an {@link OPartitionedDatabasePool} with some basic functionality.
 * <p>
 *     Because of the shared nature of the pools, you should use the getInstance method to instantiate instances that
 *     you want to use, returning such objects from the ConnectionPoolFactory.
 * </p>
 */
public class ConnectionPool {
	private OPartitionedDatabasePool pool;
	private static Map<String, ConnectionPool> instances = new HashMap<String, ConnectionPool>();
	private static Logger log = Logger.getLogger(ConnectionPool.class.getName());

	public interface ConnectionPoolFactory {
		ConnectionPool getNewInstance(String mode);
	}

	protected ConnectionPool(final OPartitionedDatabasePool pool) {
		this.pool = pool;
	}

	public static ConnectionPool getInstance(final String mode, ConnectionPoolFactory factory) {
		if (!ConnectionPool.instances.containsKey(mode)) {
			try {
				ConnectionPool.instances.put(mode, factory.getNewInstance(mode));
			} catch (Exception ex) {
				log.log(Level.SEVERE, "Unable to instantiate configuration for database.");
				ex.printStackTrace();
			}
		}
		return ConnectionPool.instances.get(mode);
	}

	public boolean isAutoCreate() {
		return this.pool.isAutoCreate();
	}

	public ConnectionPool setAutoCreate(final boolean autoCreate) {
		this.pool.setAutoCreate(autoCreate);
		return this;
	}

	public ODatabaseDocumentTx acquire() {
		return this.pool.acquire();
	}
}
