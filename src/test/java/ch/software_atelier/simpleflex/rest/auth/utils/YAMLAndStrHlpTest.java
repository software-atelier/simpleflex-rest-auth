package ch.software_atelier.simpleflex.rest.auth.utils;

import java.util.Arrays;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class YAMLAndStrHlpTest {
    @Test
    void parsesNestedYamlMappingsAndSequences() throws Exception {
        Object result = YAML.toJSONOrArray("service:\n  active: true\n  ports:\n    - 80\n    - 443\n");

        JSONObject root = assertInstanceOf(JSONObject.class, result);
        JSONObject service = root.getJSONObject("service");
        assertEquals(Boolean.TRUE, service.getBoolean("active"));
        assertEquals(443, service.getJSONArray("ports").getInt(1));
    }

    @Test
    void convertsTopLevelYamlSequenceAndRejectsItAsObject() throws Exception {
        Object result = YAML.toJSONOrArray("- red\n- blue\n");

        JSONArray array = assertInstanceOf(JSONArray.class, result);
        assertEquals("blue", array.getString(1));
        assertEquals(null, YAML.toJSON("- red\n- blue\n"));
    }

    @Test
    void parsesNumericPrefixAndTokenizesIgnoringRepeatedSeparators() {
        assertEquals(-123L, StrHlp.parseLong("-123,45 CHF"));
        assertEquals(0L, StrHlp.parseLong("CHF"));
        assertEquals(Arrays.asList("a", "b", "c"), StrHlp.tokenize("a//b///c", "/"));
    }
}
