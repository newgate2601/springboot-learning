package com.example.learning.token_config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.HashSet;

public class HashSetDeserializer extends JsonDeserializer<HashSet<String>> {

    @Override
    public HashSet<String> deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);

        // Expecting the structure ["java.util.HashSet", [...] ].
        if (node.isArray() && node.size() == 2 && node.get(1).isArray()) {
            HashSet<String> set = new HashSet<>();
            for (JsonNode element : node.get(1)) {
                set.add(element.asText()); // Convert array elements to strings
            }
            return set;
        }
        throw new IOException("Unexpected structure for HashSet field");
    }
}
