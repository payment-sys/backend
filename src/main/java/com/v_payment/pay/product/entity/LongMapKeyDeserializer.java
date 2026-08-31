package com.v_payment.pay.product.entity;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.KeyDeserializer;

public class LongMapKeyDeserializer extends KeyDeserializer {

    @Override
    public Object deserializeKey(String key, DeserializationContext ctxt) throws JacksonException {
        return Long.valueOf(key);
    }
}
