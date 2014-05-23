package com.stormpath.sample.impl.converters;

import com.google.common.collect.ImmutableMap;
import com.stormpath.sample.api.converters.MapValueRetriever;
import com.stormpath.sample.common.Error;
import com.stormpath.sample.common.exceptions.client.ClientValidationException;
import com.stormpath.sdk.lang.Assert;
import org.apache.shiro.util.StringUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @since 1.0.1
 */
@Component
public class DefaultMapValueRetriever implements MapValueRetriever {

    private final Map<Class<?>, Converter<String, ?>> converterMap;

    public DefaultMapValueRetriever() {
        converterMap = new ImmutableMap.Builder<Class<?>, Converter<String, ?>>().build();
    }

    @Override
    public <T> T getRequiredValue(Map body, String propertyName, Class<T> targetClass) throws ClientValidationException {
        return getValue(body, propertyName, targetClass, false);
    }

    @Override
    public <T> T getOptionalValue(Map body, String propertyName, Class<T> targetClass) throws ClientValidationException {
        return getValue(body, propertyName, targetClass, false);
    }

    private <T> T getValue(Map body, String propertyName, Class<T> targetClass, boolean isRequired) throws ClientValidationException {
        if (!body.containsKey(propertyName)) {
            if (isRequired) {
                throw new ClientValidationException(Error.REQUIRED_VALUE, propertyName);
            }
            return null;
        }

        Object value = body.get(propertyName);

        if (value == null) {
            throw new ClientValidationException(Error.INVALID_VALUE, propertyName);
        }

        if (targetClass.isAssignableFrom(value.getClass())) {
            return (T) value;
        }

        if (!(value instanceof String)) {
            throw new ClientValidationException(Error.INVALID_VALUE, propertyName);
        }

        return tryStringToInstanceConversion(propertyName, (String) value, targetClass);
    }

    private <T> T tryStringToInstanceConversion(String propertyName, String propertyValue, Class<T> targetClass) {

        Converter<String, ?> converter = converterMap.get(targetClass);

        Assert.notNull(converter, "There is not converter for class: " + targetClass);

        try {
            String cleanValue = StringUtils.clean(propertyValue);
            return (T) converter.convert(cleanValue);
        } catch (RuntimeException re) {
            throw new ClientValidationException(Error.INVALID_VALUE, propertyName);
        }
    }
}
