package ch.software_atelier.simpleflex.rest.auth.data;

import java.util.HashMap;
import java.util.List;
import org.json.JSONObject;

public interface DataHandler {
    
    void putUser(String user, String pass, boolean isAdmin)
            throws DataHandlerException;
    void verifyUser(String user, String pass)
            throws DataHandlerException;
    void putUserSettings(String user, HashMap<String,String> settings)
            throws DataHandlerException;
    void putUserACL(String user, String ACL)
            throws DataHandlerException;
    String getUserACLasYAML(String user)
            throws DataHandlerException;
    JSONObject getUserACLasJSON(String user)
            throws DataHandlerException;
    
    HashMap<String,String> getUserSettings(String user)
            throws DataHandlerException;
    
    void setUserGroups(String name, List<String> groups)
            throws DataHandlerException;
    List<String> getUserGroups(String name)
            throws DataHandlerException;
    void putGroup(String name)
            throws DataHandlerException;
    JSONObject getGroupACLasJSON(String name)
            throws DataHandlerException;
    String getGroupACLasYAML(String name)
            throws DataHandlerException;
    void putGroupACL(String name, String acl)
            throws DataHandlerException;
    void deleteGroup(String name)
            throws DataHandlerException;
    List<String> getGroups()
            throws DataHandlerException;

    
    void putRealms(String user, HashMap<String,String> settings)
            throws DataHandlerException;
    HashMap<String,String> getRealms(String user)
            throws DataHandlerException;
    
    boolean isAdmin(String user)
            throws DataHandlerException;
    
    void putAdmin(String user, boolean isAdmin)
            throws DataHandlerException;
            
    List<String> getUsers();
    
    void deleteUser(String username)
            throws DataHandlerException;
    
}
