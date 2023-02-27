package ch.software_atelier.simpleflex.rest.auth.data;

import ch.software_atelier.simpleflex.rest.auth.utils.YAML;
import com.google.gson.Gson;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import jakarta.xml.bind.DatatypeConverter;
import org.bson.Document;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import org.json.JSONObject;

public class MongoDBDataHandler implements DataHandler {

    private final MongoDatabase _db;
    private final MongoCollection<Document> _c;

    public MongoDBDataHandler(String uriStr) {

        MongoClientURI uri = new MongoClientURI(uriStr);
        MongoClient m = new MongoClient(uri);
        _db = m.getDatabase(uri.getDatabase());

        MongoCursor<String> c = _db.listCollectionNames().iterator();
        while (c.hasNext()) {
            String name = c.next();
            if (name.equals("_auth")) {
                _c = _db.getCollection(name);
                return;
            }
        }

        _db.createCollection("_auth");
        _c = _db.getCollection("_auth");
    }

    @Override
    public void putUser(String user, String pass, boolean isAdmin) throws DataHandlerException {

        Document userObject = new Document();
        userObject.put("user", user);
        userObject.put("pass", md5(pass));
        userObject.put("admin", isAdmin);
        userObject.put("type", "user");
        if (getUser(user) == null) {
            // insert
            _c.insertOne(userObject);
        } else {
            // update
            Document set = new Document("$set", userObject);
            Document qry = new Document("user", user);
            qry.put("type", "user");
            _c.updateOne(qry, set);
        }
    }

    @Override
    public void putAdmin(String user, boolean isAdmin)
            throws DataHandlerException {
        if (getUser(user) != null) {
            Document admin = new Document("admin", isAdmin);
            Document set = new Document("$set", admin);
            Document qry = new Document("user", user);
            _c.updateOne(qry, set);
        } else {
            throw new DataHandlerException(DataHandlerException.LOGIN_FAILED);
        }
    }

    @Override
    public void verifyUser(String user, String pass) throws DataHandlerException {
        Document userObject = getUser(user);
        if (userObject == null) {
            throw new DataHandlerException(DataHandlerException.LOGIN_FAILED);
        }
        String upass = userObject.get("pass").toString();
        if (!upass.equals(md5(pass))) {
            throw new DataHandlerException(DataHandlerException.LOGIN_FAILED);
        }
    }

    @Override
    public void putUserSettings(String user, HashMap<String, String> settings) throws DataHandlerException {
        putMap(user, "settings", settings);
    }

    @Override
    public void putUserACL(String user, String acl)
            throws DataHandlerException {
        Document userObject = getUser(user);
        if (userObject == null) {
            throw new DataHandlerException(DataHandlerException.FAILED);
        }
        String jsonACL = "{}";
        if (!acl.trim().isEmpty()) {
            jsonACL = YAML.toJSON(acl).toString();
        }
        Document aclDoc = new Document("yaml", acl);
        aclDoc.put("json", jsonACL);
        Document set = new Document("$set", new Document("acl", aclDoc));
        Document qry = new Document("user", user);
        qry.put("type", "user");
        _c.updateOne(qry, set);
    }

    @Override
    public String getUserACLasYAML(String user)
            throws DataHandlerException {

        Gson gson = new Gson();
        Document userObject = getUser(user);
        if (userObject == null) {
            throw new DataHandlerException(DataHandlerException.FAILED);
        }
        String yaml = "";
        if (userObject.containsKey("acl")) {
            yaml = userObject.get("acl", Document.class).getString("yaml");
        }
        return yaml;
    }

    @Override
    public JSONObject getUserACLasJSON(String user)
            throws DataHandlerException {

        Gson gson = new Gson();
        Document userObject = getUser(user);
        if (userObject == null) {
            throw new DataHandlerException(DataHandlerException.FAILED);
        }
        String json = "{}";
        if (userObject.containsKey("acl")) {
            json = userObject.get("acl", Document.class).getString("json");
        }
        return new JSONObject(json);
    }

    @Override
    public HashMap<String, String> getUserSettings(String user) throws DataHandlerException {
        Document userObject = getUser(user);
        if (userObject == null) {
            throw new DataHandlerException(DataHandlerException.FAILED);
        }
        HashMap<String, String> settings = new HashMap<>();
        if (userObject.containsKey("settings")) {
            Document settingsObject = (Document) userObject.get("settings");
            Set<String> keys = settingsObject.keySet();
            for (String key : keys) {
                settings.put(key, settingsObject.get(key).toString());
            }
        }
        return settings;
    }

    @Override
    public void putRealms(String user, HashMap<String, String> realms) throws DataHandlerException {
        putMap(user, "realms", realms);
    }

    @Override
    public HashMap<String, String> getRealms(String user) throws DataHandlerException {
        return getMap(user, "realms");
    }

    @Override
    public boolean isAdmin(String user) throws DataHandlerException {
        Document userObj = getUser(user);
        if (userObj == null) {
            throw new DataHandlerException(DataHandlerException.FAILED);
        }
        if (!userObj.containsKey("admin"))
            return false;
        return userObj.get("admin").toString().equals("true");
    }

    @Override
    public List<String> getUsers() {
        Document qry = new Document("type", "user");
        return getUsers(qry);
    }

    @Override
    public List<String> getUsersBySetting(String key, String value) {
        Document userQry = new Document("type", "user");
        Document settingsQry = new Document("settings." + key, value);
        Document andDoc = new Document("$and", Arrays.asList(userQry, settingsQry));
        return getUsers(andDoc);
    }

    private List<String> getUsers(Document doc) {
        FindIterable<Document> c = _c.find(doc);

        ArrayList<String> userList = new ArrayList<>();
        for (Document o : c) {
            userList.add(o.get("user").toString());
        }
        return userList;
    }

    public List<String> getGroups()
            throws DataHandlerException {
        Document qry = new Document("type", "group");
        FindIterable<Document> c = _c.find(qry);

        ArrayList<String> userList = new ArrayList<>();
        for (Document o : c) {
            userList.add(o.get("name").toString());
        }
        return userList;
    }

    @Override
    public void deleteUser(String username)
            throws DataHandlerException {

        _c.deleteOne(new Document("user", username));
    }

    private void putMap(String user, String field, HashMap<String, String> fields)
            throws DataHandlerException {
        if (getUser(user) == null) {
            throw new DataHandlerException(DataHandlerException.FAILED);
        }

        Document settingsObj = new Document();
        Set<String> keys = fields.keySet();
        for (String key : keys) {
            settingsObj.put(key, fields.get(key));
        }

        _c.updateOne(new Document("user", user),
                new Document("$set", new Document(field, settingsObj))
        );
    }

    private HashMap<String, String> getMap(String user, String field)
            throws DataHandlerException {
        Document userObject = getUser(user);
        if (userObject == null) {
            throw new DataHandlerException(DataHandlerException.FAILED);
        }
        HashMap<String, String> fields = new HashMap<>();
        if (userObject.containsKey(field)) {
            Document settingsObject = (Document) userObject.get(field);
            Set<String> keys = settingsObject.keySet();
            for (String key : keys) {
                fields.put(key, settingsObject.get(key).toString());
            }
        }
        return fields;
    }


    private Document getUser(String user) {
        Document qry = new Document("user", user);
        qry.put("type", "user");
        return _c.find(qry).first();
    }

    private Document getGroup(String name) {
        Document qry = new Document("name", name);
        qry.put("type", "group");
        return _c.find(qry).first();
    }

    private static String md5(String pass) throws DataHandlerException {
        try {

            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(pass.getBytes());
            byte[] digest = md.digest();
            return DatatypeConverter
                    .printHexBinary(digest).toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            throw new DataHandlerException(DataHandlerException.INTERNAL_ERROR);
        }
    }

    @Override
    public void setUserGroups(String name, List<String> groups)
            throws DataHandlerException {
        Document user = getUser(name);
        if (user == null) {
            throw new DataHandlerException(DataHandlerException.FAILED);
        }
        Document set = new Document("groups", groups);

        Document query = new Document();
        query.put("type", "user");
        query.put("user", name);

        _c.updateOne(query, new Document("$set", set));
    }

    @Override
    public List<String> getUserGroups(String name)
            throws DataHandlerException {
        Document user = getUser(name);
        if (user == null) {
            throw new DataHandlerException(DataHandlerException.FAILED);
        }
        ArrayList<String> groups = new ArrayList<>();

        if (user.containsKey("groups")) {
            List<String> loadedGroups = user.get("groups", List.class);

            for (String group : loadedGroups) {
                if (getGroup(group) != null) {
                    groups.add(group);
                }
            }

        }


        return groups;
    }

    @Override
    public void putGroup(String name)
            throws DataHandlerException {
        Document groupObject = new Document();
        groupObject.put("name", name);
        groupObject.put("type", "group");
        if (getGroup(name) == null) {
            // insert
            _c.insertOne(groupObject);
        }
    }

    @Override
    public JSONObject getGroupACLasJSON(String name)
            throws DataHandlerException {
        Gson gson = new Gson();
        Document userObject = getGroup(name);
        if (userObject == null) {
            throw new DataHandlerException(DataHandlerException.FAILED);
        }
        String json = "{}";
        if (userObject.containsKey("acl")) {
            json = userObject.get("acl", Document.class).getString("json");
        }
        return new JSONObject(json);
    }

    @Override
    public String getGroupACLasYAML(String name)
            throws DataHandlerException {
        Gson gson = new Gson();
        Document userObject = getGroup(name);
        if (userObject == null) {
            throw new DataHandlerException(DataHandlerException.FAILED);
        }
        String yaml = "";
        if (userObject.containsKey("acl")) {
            yaml = userObject.get("acl", Document.class).getString("yaml");
        }
        return yaml;
    }

    @Override
    public void putGroupACL(String name, String acl)
            throws DataHandlerException {
        Document userObject = getGroup(name);
        if (userObject == null) {
            throw new DataHandlerException(DataHandlerException.FAILED);
        }
        String jsonACL = "{}";
        if (!acl.trim().isEmpty()) {
            jsonACL = YAML.toJSON(acl).toString();
        }
        Document aclDoc = new Document("yaml", acl);
        aclDoc.put("json", jsonACL);
        Document set = new Document("$set", new Document("acl", aclDoc));
        Document qry = new Document("name", name);
        qry.put("type", "group");
        _c.updateOne(qry, set);
    }

    @Override
    public void deleteGroup(String name)
            throws DataHandlerException {
        if (_c.count(new Document("groups", name)) > 0) {
            throw new DataHandlerException(DataHandlerException.GROUP_IN_USE);
        }
        Document qry = new Document();
        qry.put("name", name);
        qry.put("type", "group");
        _c.deleteOne(qry);
    }
}
