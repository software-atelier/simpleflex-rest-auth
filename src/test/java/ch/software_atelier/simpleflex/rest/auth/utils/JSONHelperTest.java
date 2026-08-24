package ch.software_atelier.simpleflex.rest.auth.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JSONHelperTest {
    @Test
    void deepMergePreservesNestedValuesAndLetsFirstObjectWin() {
        JSONObject base = new JSONObject("{\"role\":\"user\",\"permissions\":{\"read\":true}}");
        JSONObject target = new JSONObject("{\"role\":\"guest\",\"permissions\":{\"write\":true}}");

        JSONHelper.deepMerge(base, target);

        assertEquals("user", target.getString("role"));
        assertTrue(target.getJSONObject("permissions").getBoolean("read"));
        assertTrue(target.getJSONObject("permissions").getBoolean("write"));
    }

    @Test
    void resolvesObjectAndArrayPathsAndReturnsNullForMissingPath() {
        JSONObject document = new JSONObject("{\"items\":[{\"id\":\"first\"},{\"id\":\"last\"}]}");

        assertEquals("first", JSONHelper.getAtPath(document, "/items/0/id"));
        assertEquals("last", JSONHelper.getAtPath(document, "/items/-/id"));
        assertEquals(null, JSONHelper.getAtPath(document, "/items/2/id"));
    }

    @Test
    void convertsNestedMapsAndListsBothDirections() throws Exception {
        Map<String, Object> source = new HashMap<String, Object>();
        source.put("names", Arrays.asList("alice", "bob"));
        Map<String, Object> nested = new HashMap<String, Object>();
        nested.put("enabled", Boolean.TRUE);
        source.put("settings", nested);

        JSONObject json = JSONHelper.mapToJSON(source);
        Map<String, Object> converted = JSONHelper.jsonToMap(json);

        assertEquals(Arrays.asList("alice", "bob"), converted.get("names"));
        assertEquals(Boolean.TRUE, ((Map<?, ?>) converted.get("settings")).get("enabled"));
        assertEquals(Arrays.asList("alice", "bob"), JSONHelper.toStringList(new JSONArray("[\"alice\",\"bob\"]")));
    }
}
