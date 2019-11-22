package ch.software_atelier.simpleflex.rest.auth.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;
import java.util.ArrayList;
import java.util.Map;

public class YAML {

    public static Object toJSONOrArray(String s)throws JSONException {
        Yaml yaml= new Yaml();
        Object y = yaml.load(s);
        Object o = _convertToJson(y);
        return o;
    }
    
    public static JSONObject toJSON(String s)throws JSONException {
        Object o = toJSONOrArray(s);
        if (o instanceof JSONObject)
            return (JSONObject)o;
        else
            return null;
    }

    private static Object _convertToJson(Object o) throws JSONException {
        if (o instanceof Map) {
          Map<Object, Object> map = (Map<Object, Object>) o;

          JSONObject result = new JSONObject();

          for (Map.Entry<Object, Object> stringObjectEntry : map.entrySet()) {
            String key = stringObjectEntry.getKey().toString();

            result.put(key, _convertToJson(stringObjectEntry.getValue()));
          }

          return result;
        } else if (o instanceof ArrayList) {
          ArrayList arrayList = (ArrayList) o;
          JSONArray result = new JSONArray();

          for (Object arrayObject : arrayList) {
            result.put(_convertToJson(arrayObject));
          }

          return result;
        } else {
          return o;
        }
    }
}
