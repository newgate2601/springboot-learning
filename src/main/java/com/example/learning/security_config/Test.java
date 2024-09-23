package com.example.learning.security_config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.MapDeserializer;

import java.io.IOException;
import java.util.Map;

public class Test {
    public abstract class UserMixin {
        @JsonProperty("attributes")
        @JsonDeserialize(using = MapDeserializer2.class)  // Sử dụng custom deserializer
        private Map<String, Object> attributes;
    }
}

