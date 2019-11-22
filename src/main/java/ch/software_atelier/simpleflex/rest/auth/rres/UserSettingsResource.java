package ch.software_atelier.simpleflex.rest.auth.rres;

import ch.software_atelier.simpleflex.rest.auth.utils.JSONHelper;
import ch.software_atelier.simpleflex.rest.auth.ExceptionHandler;
import ch.software_atelier.simpleflex.rest.auth.data.DataHandler;
import ch.software_atelier.simpleflex.rest.auth.data.DataHandlerException;
import ch.software_atelier.simpleflex.rest.auth.token.TokenHandler;
import ch.software_atelier.simpleflex.rest.auth.token.TokenHandlerException;
import ch.software_atelier.simpleflex.rest.auth.token.TokenParser;
import java.util.HashMap;

import ch.software_atelier.simpleflex.rest.DefaultRestResource;
import ch.software_atelier.simpleflex.rest.RestRequest;
import ch.software_atelier.simpleflex.rest.RestResponse;
import ch.software_atelier.simpleflex.rest.swagger.*;
import org.json.JSONObject;


/**
 *
 * @author tk
 */
public class UserSettingsResource extends DefaultRestResource {
    private final TokenHandler _th;
    private final DataHandler _dh;
    private final TokenParser _tp;
    public UserSettingsResource(DataHandler dh, TokenHandler th, TokenParser tp){
        _dh = dh;
        _th = th;
        _tp = tp;
    }

    /**
     * Saves the given settings for the given user if he is
     * that user or if he is an admin.
     * {"key":"value",...} Key-Value pairs
     * @param request
     * @return 
     */
    @Override
    public RestResponse onPUT(RestRequest request) {
        try{
            String token = _tp.getToken(request);
            String userByToken = _tp.getUsername(token);
            String userByPath = request.getResourcePlaceholder("name");
            
            if (_tp.isAdmin(token) || (userByToken.equals(userByPath))){
                JSONObject settings = request.getJSON();
                HashMap<String,String> settingsMap
                        = JSONHelper.jsonToHashmap(settings);
                _dh.putUserSettings(userByPath, settingsMap);
                
                return RestResponse.json_200(settings);
            }
            return RestResponse.unauthorized_401();
            
        }catch(DataHandlerException | TokenHandlerException th){
            return ExceptionHandler.handle(th, true);
        }
    }
    
    @Override
    public void docPUT(MethodDocumentation request){
        request.setTitle("Put User Settings");
        request.addTag("Authorisazion");
        request.setDescription("Saves the users settings if they belong to him or if the requesting user has admin previliges.\r\n"
        +"Settings consist of key/value-pairs.");
        request.addProduces("application/json");
        request.addParameter(new HeaderParameter("Authorization", "the access token, Baerer"));
        request.addParameter(new PathParameter("name", "the username"));
        request.addParameter(new BodyParameter("body",
            ObjectSchemaBuilder.create("the settings object with key/value-pairs").toJSON()
        ));
        request.addResponse("200", "OK", 
            ObjectSchemaBuilder.create("the settings object with key/value-pairs")
                .toJSON()
        );
    }

    /**
     * Returns the settings of a given user if those settings belong to him or
     * if the user is an admin.
     * @param request
     * @return 
     */
    @Override
    public RestResponse onGET(RestRequest request) {
        try{
            String token = _tp.getToken(request);
            String userByToken = _tp.getUsername(token);
            String userByPath = request.getResourcePlaceholder("name");
            
            if (_tp.isAdmin(token) || (userByToken.equals(userByPath))){
                JSONObject result = JSONHelper.hashMapToJSON(
                        _dh.getUserSettings(userByPath)
                );
                return RestResponse.json_200(result);
            }
            return RestResponse.unauthorized_401();
            
        }catch(DataHandlerException | TokenHandlerException th){
            return ExceptionHandler.handle(th, true);
        }    
    }
    
    @Override
    public void docGET(MethodDocumentation request){
        request.setTitle("Get User Settings");
        request.addTag("Authorisazion");
        request.setDescription("Returns the users settings if they belong to him or if the requesting user has admin previliges.");
        request.addProduces("application/json");
        request.addParameter(new HeaderParameter("Authorization", "the access token, Baerer"));
        request.addParameter(new PathParameter("name", "the username"));
        request.addResponse("200", "OK", 
            ObjectSchemaBuilder.create("the users settings")
                .toJSON()
        );
    }
    
}
