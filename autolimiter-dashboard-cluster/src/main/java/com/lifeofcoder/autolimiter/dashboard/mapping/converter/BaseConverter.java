package com.lifeofcoder.autolimiter.dashboard.mapping.converter;

/**
 *
 *
 * @author xbc
 * @date 2020/7/23
 */
public abstract class BaseConverter<S, T> implements Converter {
    private Class<S> srcType;
    private Class<T> targetType;

    public BaseConverter(Class<S> srcType, Class<T> targetType) {
        this.srcType = srcType;
        this.targetType = targetType;
    }

    @Override
    public Object convert(Object src) {
        return doConvert((S) src);
    }

    public abstract T doConvert(S srcObject);

    public Class<S> getSrcType() {
        return srcType;
    }

    public void setSrcType(Class<S> srcType) {
        this.srcType = srcType;
    }

    public Class<T> getTargetType() {
        return targetType;
    }

    public void setTargetType(Class<T> targetType) {
        this.targetType = targetType;
    }
}
