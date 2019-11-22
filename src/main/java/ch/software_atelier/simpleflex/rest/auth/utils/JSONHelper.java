package ch.software_atelier.simpleflex.rest.auth.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.*;

public class JSONHelper {
    
    public static HashMap jsonToHashmap(JSONObject obj){
        HashMap hm = new HashMap();
        try{
            Set<String> keys = obj.keySet();
            for (String key:keys){
                hm.put(key, obj.get(key));
            }
        }catch(Exception ex){
            ex.printStackTrace();
        }
        return hm;
    }

    public static JSONObject hashMapToJSON(HashMap hm){
        JSONObject jo = new JSONObject();
        
        try{
            Object[] keys = hm.keySet().toArray();
            for (int i=0;i<keys.length;i++){
                jo.put((String)keys[i], hm.get(keys[i]));
            }
        }catch(JSONException je){
            je.printStackTrace();
        }
        return jo;
        
    }

    public static JSONArray stringList2JSONArr(List<String> list)throws JSONException {
        JSONArray jArr = new JSONArray();
        for (int i=0;i<list.size();i++){
            jArr.put(list.get(i));
        }
        return jArr;
    }

    /**
     * Merges the first object to the second.
     * @param first
     * @param second
     */
    public static void deepMerge(JSONObject first, JSONObject second){
        
        Set<String> keys = first.keySet();
        
        for (String k:keys){
            if (!second.has(k)){
                second.put(k, first.get(k));
            }
            else if (first.get(k) instanceof JSONObject && second.get(k) instanceof JSONObject){
                deepMerge((JSONObject)first.get(k),(JSONObject)second.get(k));
            }
            else{
                second.put(k, first.get(k));
            }
        }
    }

    public static Object getAtPath(JSONObject o, String path){
        return getAtPath(o,"/",path);
    }

    public static Object getAtPath(JSONObject o, String separator, String path){
        StringTokenizer st = new StringTokenizer(path,separator);
        try{
            Object node = o;
            while (st.hasMoreTokens()){
                String key = st.nextToken();

                if (node instanceof JSONArray){
                    node = getFromArray((JSONArray)node,key);
                }
                else if (node instanceof JSONObject){
                    node = ((JSONObject)node).get(key);
                }
                else if (st.hasMoreElements()){
                    return null;
                }

                if (!st.hasMoreElements())
                    return node;

            }
        }catch(JSONException je){
            return null;
        }
        return null;
    }

    private static Object getFromArray(JSONArray arr, String pos){
            if (pos.equals("-")){
                return arr.get(arr.length()-1);
            }
            else{
                int p = Integer.parseInt(pos);
                return arr.get(p);
            }
    }

    public static List<String> toStringList(JSONArray arr){
        ArrayList<String> list = new ArrayList<>();
        for (int i=0;i<arr.length();i++){
            list.add(arr.getString(i));
        }
        return list;
    }

    public static Map<String, Object> jsonToMap(JSONObject object) throws JSONException {
        Map<String, Object> map = new HashMap<String, Object>();

        Iterator<String> keysItr = object.keys();
        while(keysItr.hasNext()) {
            String key = keysItr.next();
            Object value = object.get(key);

            if(value instanceof JSONArray) {
                value = jsonToList((JSONArray) value);
            }
            else if(value instanceof JSONObject) {
                value = jsonToMap((JSONObject) value);
            }
            map.put(key, value);
        }
        return map;
    }

    public static List<Object> jsonToList(JSONArray array) throws JSONException {
        List<Object> list = new ArrayList<Object>();
        for(int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if(value instanceof JSONArray) {
                value = jsonToList((JSONArray) value);
            }
            else if(value instanceof JSONObject) {
                value = jsonToMap((JSONObject) value);
            }
            list.add(value);
        }
        return list;
    }

    public static JSONObject mapToJSON(Map<String,Object> data){
        JSONObject result = new JSONObject();
        for(String key:data.keySet()){
            Object value = data.get(key);
            Object jValue; 
            if (value instanceof Map){
                jValue = mapToJSON((Map)value);
            }
            else if (value instanceof List){
                jValue = listToJSON((List)value);
            }
            else{
                jValue = value;
            }
            result.put(key, jValue);
            
        }
        return result;
    }
    
    public static JSONArray listToJSON(List list){
        JSONArray result = new JSONArray();
        for (Object o:list){
            if (o instanceof Map){
                result.put(mapToJSON((Map)o));
            }
            else if (o instanceof List){
                result.put(listToJSON((List)o));
            }
            else{
                result.put(o);
            }
        }
        return result;
    }
}
