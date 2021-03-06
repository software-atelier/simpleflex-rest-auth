package ch.software_atelier.simpleflex.rest.auth.rres;

import ch.software_atelier.simpleflex.rest.auth.ExceptionHandler;
import ch.software_atelier.simpleflex.rest.auth.data.DataHandler;
import ch.software_atelier.simpleflex.rest.auth.data.DataHandlerException;
import ch.software_atelier.simpleflex.rest.auth.token.TokenHandler;
import ch.software_atelier.simpleflex.rest.auth.token.TokenHandlerException;
import ch.software_atelier.simpleflex.rest.auth.token.TokenParser;
import ch.software_atelier.simpleflex.rest.DefaultRestResource;
import ch.software_atelier.simpleflex.rest.RestRequest;
import ch.software_atelier.simpleflex.rest.RestResponse;
import ch.software_atelier.simpleflex.rest.swagger.*;
import com.google.gson.Gson;
import org.json.JSONObject;

public class UserACLResource extends DefaultRestResource {
    private final DataHandler _dh;
    private final TokenHandler _th;
    private final TokenParser _tp;
    
    public UserACLResource(DataHandler dh, TokenHandler th, TokenParser tp){
        _tp = tp;
        _dh = dh;
        _th = th;
    }

    /**
     * Saves the given settings for the given user if he is
     * that user or if he is an admin.
     * {"key":"value",...} Key-Value pairs
     */
    @Override
    public RestResponse onPUT(RestRequest request) {
        try{
            String token = _tp.getToken(request);
            String userByToken = _tp.getUsername(token);
            String userByPath = request.getResourcePlaceholder("name");
            
            if (_tp.isAdmin(token)){
                JSONObject req = request.getJSON();
                String yaml = req.getString("yaml");
                                
                _dh.putUserACL(userByPath, yaml);
                
                return RestResponse.json_200(new JSONObject().put("ok", true));
            }
            return RestResponse.unauthorized_401();
            
        }catch(DataHandlerException | TokenHandlerException th){
            return ExceptionHandler.handle(th, true);
        }
    }
    
    @Override
    public void docPUT(MethodDocumentation request){
        request.setTitle("Put User ACL");
        request.addTag("Authorisazion");
        request.setDescription("Saves the users ACL if the requesting user has admin previliges.\r\n");
        request.addProduces("application/json");
        request.addParameter(new HeaderParameter("Authorization", "the access token, Baerer"));
        request.addParameter(new PathParameter("name", "the username"));
        request.addParameter(new BodyParameter("body",
            ObjectSchemaBuilder.create("the Access control list as YAML")
                    .addSimpleProperty("yaml", "string", "the ACL as YAML", true).toJSON()
        ));
        request.addResponse("200", "OK", 
            ObjectSchemaBuilder.create("ok")
                    .addSimpleProperty("ok", "boolean","will always be true", true)
                .toJSON()
        );
    }

    /**
     * Returns the settings of a given user if those settings belong to him or
     * if the user is an admin.
     */
    @Override
    public RestResponse onGET(RestRequest request) {
        try{
            String token = _tp.getToken(request);
            String userByToken = _tp.getUsername(token);
            String userByPath = request.getResourcePlaceholder("name");
            
            if (_tp.isAdmin(token)){
                Gson gson = new Gson(); 
                JSONObject result = new JSONObject().put("yaml", _dh.getUserACLasYAML(userByPath));
                return RestResponse.json_200(result);
            }
            return RestResponse.unauthorized_401();
            
        }catch(DataHandlerException | TokenHandlerException th){
            return ExceptionHandler.handle(th, true);
        }    
    }
    
    @Override
    public void docGET(MethodDocumentation request){
        request.setTitle("Get Users ACL");
        request.addTag("Authorisazion");
        request.setDescription("Returns the users ACL if the requesting user has admin previliges.");
        request.addProduces("application/json");
        request.addParameter(new HeaderParameter("Authorization", "the access token, Baerer"));
        request.addParameter(new PathParameter("name", "the username"));
        request.addResponse("200", "OK", 
            ObjectSchemaBuilder.create("the users ACL as YAML")
                    .addSimpleProperty("yaml", "string", "the ACL as YAML", true)
                .toJSON()
        );
    }
    
}
