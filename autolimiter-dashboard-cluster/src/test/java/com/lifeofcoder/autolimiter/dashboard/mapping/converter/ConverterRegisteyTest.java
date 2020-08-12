package com.lifeofcoder.autolimiter.dashboard.mapping.converter;

import com.lifeofcoder.autolimiter.dashboard.mapping.converter.impl.Long2DateConverter;
import org.junit.Test;

import java.util.Date;

public class ConverterRegisteyTest {

    @Test
    public void testConvert() {
        ConverterRegistey.register(new Long2DateConverter());
        Converter converter = ConverterRegistey.getConverter(long.class, Date.class);
        Object value = converter.convert(1000L);
        System.out.println(value);
        System.out.println(long.class.getName());
    }
}