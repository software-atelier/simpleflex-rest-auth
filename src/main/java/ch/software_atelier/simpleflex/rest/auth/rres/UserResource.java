package ch.software_atelier.simpleflex.rest.auth.rres;

import ch.software_atelier.simpleflex.rest.auth.utils.JSONHelper;
import ch.software_atelier.simpleflex.rest.auth.ExceptionHandler;
import ch.software_atelier.simpleflex.rest.auth.data.DataHandler;
import ch.software_atelier.simpleflex.rest.auth.data.DataHandlerException;
import ch.software_atelier.simpleflex.rest.auth.token.TokenHandler;
import ch.software_atelier.simpleflex.rest.auth.token.TokenHandlerException;
import ch.software_atelier.simpleflex.rest.auth.token.TokenParser;
import java.util.HashMap;
import java.util.List;

import ch.software_atelier.simpleflex.rest.DefaultRestResource;
import ch.software_atelier.simpleflex.rest.RestRequest;
import ch.software_atelier.simpleflex.rest.RestResponse;
import ch.software_atelier.simpleflex.rest.swagger.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 *
 * @author tk
 */
public class UserResource extends DefaultRestResource {
    private final DataHandler _dh;
    private final TokenHandler _th;
    private final TokenParser _tp;
    public UserResource(DataHandler dh, TokenHandler th, TokenParser tp){
        _tp = tp;
        _dh = dh;
        _th = th;
    }

    /**
     * Returns a List of all Users. Admin rights are required
     */
    @Override
    public RestResponse onGET(RestRequest request) {
        try{
            String token = _tp.getToken(request);
            if (token == null){
                return RestResponse.unauthorized_401();
            }
            if (_tp.isAdmin(token)){
                String key = request.getRequestArgument("key");
                String value = request.getRequestArgument("value");
                if (key!=null && value!=null){
                    List<String> users = _dh.getUsersBySetting(key, value);
                    JSONArray arr = JSONHelper.stringList2JSONArr(users);
                    return RestResponse.json_200(arr);
                }
                else{
                    List<String> users = _dh.getUsers();
                    JSONArray arr = JSONHelper.stringList2JSONArr(users);
                    return RestResponse.json_200(arr);
                }
            }
            return RestResponse.unauthorized_401();
        }catch(TokenHandlerException the){
            return RestResponse.unauthorized_401();
        }catch(JSONException | NullPointerException th){
            return ExceptionHandler.handle(th, false);
        }
    }
    
    @Override
    public void docGET(MethodDocumentation request){
        request.setTitle("List Users");
        request.addTag("Authorisazion");
        request.setDescription("Returns a list of allusers, if the authenticated user has admin previliges.");
        request.addProduces("application/json");
        request.addParameter(new HeaderParameter("Authorization", "the access token, Baerer"));

        request.addResponse("200", "OK", 
            ArraySchemaBuilder.create("all users on this instance")
                .setBasic("string", "a user")
                .toJSON()
                
        );
    }
    
    
    /**
     * Creates a new User. Admin rights are required.
     * {"user":"string", "pass":"string", "admin":"boolean", "realms":{"string":"string"}}
     */
    @Override
    public RestResponse onPOST(RestRequest request) {
        try{
            String token = _tp.getToken(request);
            if (!_tp.isAdmin(token)){
                return RestResponse.unauthorized_401();
            }
            
            JSONObject obj = request.getJSON();
            String user = obj.getString("user");
            String pass = obj.getString("pass");
            HashMap<String,String> realms = JSONHelper.jsonToHashmap(
                    obj.getJSONObject("realms")
            );
            
            boolean isAdmin = obj.getBoolean("admin");
            _dh.putUser(user, pass, isAdmin);
            _dh.putRealms(user, realms);
            
            return RestResponse.json_201_created(obj);
            
        }catch(DataHandlerException | TokenHandlerException | JSONException | NullPointerException th){
            return ExceptionHandler.handle(th,true);
        }
    }
    
    @Override
    public void docPOST(MethodDocumentation request){
        request.setTitle("Create User");
        request.addTag("Authorisazion");
        request.setDescription("Creates a new user, if the requesting user has admin previliges");
        request.addProduces("application/json");
        request.addParameter(new HeaderParameter("Authorization", "the access token, Baerer"));
        
        request.addParameter(new BodyParameter("body",
            ObjectSchemaBuilder.create("the user information")
                .addSimpleProperty("user", "string", "the username", true)
                .addSimpleProperty("pass", "string", "the password", true)
                .addSimpleProperty("admin", "boolean", "wether the user is admin or not", true)
                .addObjectProperty("realms", 
                    ObjectSchemaBuilder.create("The realms. key: realmname, value: realmdescription").toJSON(), true)
                .toJSON()
        ));
        request.addResponse("201", "Created",new JSONObject());
    }
    
    
}
