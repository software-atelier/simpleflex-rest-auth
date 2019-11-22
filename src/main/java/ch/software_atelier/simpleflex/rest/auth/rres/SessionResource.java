package ch.software_atelier.simpleflex.rest.auth.rres;

import ch.software_atelier.simpleflex.rest.auth.ExceptionHandler;
import ch.software_atelier.simpleflex.rest.auth.data.DataHandler;
import ch.software_atelier.simpleflex.rest.auth.data.DataHandlerException;
import ch.software_atelier.simpleflex.rest.auth.token.TokenHandler;
import ch.software_atelier.simpleflex.rest.auth.token.TokenHandlerException;
import ch.software_atelier.simpleflex.rest.auth.token.TokenParser;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Set;

import ch.software_atelier.simpleflex.rest.DefaultRestResource;
import ch.software_atelier.simpleflex.rest.RestRequest;
import ch.software_atelier.simpleflex.rest.RestResponse;
import ch.software_atelier.simpleflex.rest.swagger.BodyParameter;
import ch.software_atelier.simpleflex.rest.swagger.HeaderParameter;
import ch.software_atelier.simpleflex.rest.swagger.MethodDocumentation;
import ch.software_atelier.simpleflex.rest.swagger.ObjectSchemaBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SessionResource extends DefaultRestResource {
    private final DataHandler _dh;
    private final TokenHandler _th;
    private final TokenParser _tp;
    public SessionResource(DataHandler dh, TokenHandler th, TokenParser tp){
        _tp = tp;
        _dh = dh;
        _th = th;
    }

    /**
     * Updates the SessionToken. No Data required.
     * @param request
     * @return 
     */
    @Override
    public RestResponse onPUT(RestRequest request) {
        try{
            String token = _tp.getToken(request);
            if (token == null)
                return RestResponse.unauthorized_401();
            
            token = _th.renew(token);
            JSONObject obj = new JSONObject();
            obj.put("access_token", token);
            obj.put("lifetime", _th.getSessionLength());
            return RestResponse.json_200(obj);
        }catch(NoSuchElementException nsee){
            return RestResponse.unauthorized_401();
        }catch(TokenHandlerException | JSONException | NullPointerException nsee){
            return ExceptionHandler.handle(nsee, true);
        }
    }
    
    @Override
    public void docPUT(MethodDocumentation request){
        request.setTitle("Update Session");
        request.addTag("Authorisazion");
        request.setDescription("Renews an access token on a given session");
        request.addProduces("application/json");
        request.addParameter(new HeaderParameter("Authorization", "the access token, Baerer"));
                
        request.addResponse("200", "OK", 
            ObjectSchemaBuilder.create("The new session data")
                .addSimpleProperty("access_token", "string", "the new access token", true)
                .addSimpleProperty("lifetime", "number", "the session lifetime in seconds", true)
                .toJSON()
        );
        
    } 
    
    /**
     * The Login function. Creates a new Session.
     * {"user":"username", "pass":"password"}
     * @param request
     * @return 
     */
    @Override
    public RestResponse onPOST(RestRequest request) {
        try{
            JSONObject auth = request.getJSON();
            String user = auth.getString("user");
            String pass = auth.getString("pass");
            
            JSONObject result = new JSONObject();
            
            _dh.verifyUser(user, pass);
            
            // build realms
            JSONArray realmsArr = SessionResource.realmsToArray(_dh.getRealms(user));
            
            result.put("realms", realmsArr);
            String token = _th.createToken(user);
            result.put("access_token", token);
            result.put("lifetime", _th.getSessionLength());

            return RestResponse.json_201_created(result);
        }catch(DataHandlerException | TokenHandlerException | JSONException th){
            return ExceptionHandler.handle(th, true);
        }
    }
    
    @Override
    public void docPOST(MethodDocumentation request){
        request.addTag("Authorisazion");
        request.setTitle("Login");
        request.setDescription("Retrieves an access token for the given user credentials");
        request.addProduces("application/json");
        
        request.addParameter(new BodyParameter("body",
            ObjectSchemaBuilder.create("The User Credentials")
                .addSimpleProperty("user", "string", "the username", true)
                .addSimpleProperty("pass", "string", "the password", true)
                .toJSON()
        ));
        
        request.addResponse("200", "OK", 
            ObjectSchemaBuilder.create("The Login Response")
                .addSimpleProperty("access_token", "string", "the new access token", true)
                .addSimpleProperty("lifetime", "number", "the session lifetime in seconds", true)
                .toJSON()
        );
    }
    
    public static JSONArray realmsToArray(HashMap<String,String> realms) throws JSONException{
        JSONArray realmsArr = new JSONArray();
        Set<String> keys = realms.keySet();

        for (String key:keys){
            String value = realms.get(key);
            JSONObject r = new JSONObject();
            r.put("name", key);
            r.put("label", value);
            realmsArr.put(r);
        }
        return realmsArr;
    }
}
