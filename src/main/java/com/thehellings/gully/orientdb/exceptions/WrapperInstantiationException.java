package com.thehellings.gully.orientdb.exceptions;

public class WrapperInstantiationException extends Exception {
    public WrapperInstantiationException(String msg, Throwable t) {
        super(msg + t.getMessage());
    }
}
