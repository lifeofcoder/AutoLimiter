package com.lifeofcoder.autolimiter.dashboard.mapping;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 *
 *
 * @author xbc
 * @date 2020/7/21
 */
public class FieldMapping {
    private Field field;
    private Method setter;

    public FieldMapping(Field field, Method setter) {
        this.field = field;
        this.setter = setter;
    }

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
    }

    public Method getSetter() {
        return setter;
    }

    public void setSetter(Method setter) {
        this.setter = setter;
    }
}
