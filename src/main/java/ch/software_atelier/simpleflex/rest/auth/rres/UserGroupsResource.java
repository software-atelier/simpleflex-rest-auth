package ch.software_atelier.simpleflex.rest.auth.rres;

import ch.software_atelier.simpleflex.rest.auth.utils.JSONHelper;
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
import org.json.JSONArray;
import org.json.JSONObject;

public class UserGroupsResource extends DefaultRestResource {
    private final TokenHandler _th;
    private final DataHandler _dh;
    private final TokenParser _tp;
    public UserGroupsResource(DataHandler dh, TokenHandler th, TokenParser tp){
        _dh = dh;
        _th = th;
        _tp = tp;
    }

    @Override
    public RestResponse onPUT(RestRequest request) {
        try{
            String token = _tp.getToken(request);
            String userByToken = _tp.getUsername(token);
            String userByPath = request.getResourcePlaceholder("name");
            
            if (_tp.isAdmin(token)){
                JSONArray groups = request.getJSONArray();

                _dh.setUserGroups(userByPath, JSONHelper.toStringList(groups));
                
                return RestResponse.json_200(new JSONObject().put("ok", true));
            }
            return RestResponse.unauthorized_401();
            
        }catch(DataHandlerException | TokenHandlerException th){
            return ExceptionHandler.handle(th, true);
        }
    }
    
    @Override
    public void docPUT(MethodDocumentation request){
        request.setTitle("Set User Groups");
        request.addTag("Authorisazion");
        request.setDescription("Sets the users groups if the requesting user has admin previliges.");
        request.addProduces("application/json");
        request.addParameter(new HeaderParameter("Authorization", "the access token, Baerer"));
        request.addParameter(new PathParameter("name", "the username"));
        request.addParameter(new BodyParameter("body",
            ArraySchemaBuilder.create("the groups").setBasic("string", "group names").toJSON()
        ));
        request.addResponse("200", "OK", 
            ObjectSchemaBuilder.create("the status object")
                .addSimpleProperty("ok", "boolean", "always true", true)
                .toJSON()
        );
    }

    @Override
    public RestResponse onGET(RestRequest request) {
        try{
            String token = _tp.getToken(request);
            String userByToken = _tp.getUsername(token);
            String userByPath = request.getResourcePlaceholder("name");
            
            if (_tp.isAdmin(token)){
                
                JSONArray result = JSONHelper.stringList2JSONArr(_dh.getUserGroups(userByPath));
                return RestResponse.json_200(result);
            }
            return RestResponse.unauthorized_401();
            
        }catch(DataHandlerException | TokenHandlerException th){
            return ExceptionHandler.handle(th, true);
        }    
    }
    
    @Override
    public void docGET(MethodDocumentation request){
        request.setTitle("Get User Groups");
        request.addTag("Authorisazion");
        request.setDescription("Returns the users groups if the requesting user has admin previliges.");
        request.addProduces("application/json");
        request.addParameter(new HeaderParameter("Authorization", "the access token, Baerer"));
        request.addParameter(new PathParameter("name", "the username"));
        request.addResponse("200", "OK", 
            ArraySchemaBuilder.create("the users groups").setBasic("string", "group names")
                .toJSON()
        );
    }
}
