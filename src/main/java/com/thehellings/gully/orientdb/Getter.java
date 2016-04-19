package com.thehellings.gully.orientdb;

import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLQuery;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.thehellings.gully.orientdb.exceptions.WrapperInstantiationException;
import com.thehellings.gully.orientdb.types.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;

public class Getter <T> {
    private Class<T> clazz;
    private Constructor<T> constructor;
    private static Logger log = Logger.getLogger(Getter.class.getName());
    private String query;

    public Getter(Class<T> clazz, String fieldName) {
        this.clazz = clazz;
        try {
            this.constructor = this.clazz.getConstructor(clazz);
        } catch(NoSuchMethodException nse) {
            log.warning("Constructor not found. Cannot cast object.");
        }
        this.query = "SELECT FROM " + (new Type(this.clazz)).getName() + " WHERE " + fieldName + " ? ";
    }

    public T get(Object value) throws WrapperInstantiationException {
        ODocument document = this.getRaw(value);
        if (document != null)
            try {
                return this.constructor.newInstance(document);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException exception) {
                throw new WrapperInstantiationException("Failed to instantiate warpper class", exception);
            }
        return null;
    }

    public ODocument getRaw(Object value) {
        OSQLQuery query = new OSQLSynchQuery<ODocument>();
        query.setText(this.query);
        return (ODocument) query.runFirst(value);
    }
}
