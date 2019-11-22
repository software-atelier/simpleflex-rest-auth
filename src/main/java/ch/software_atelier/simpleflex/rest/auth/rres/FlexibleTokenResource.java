package ch.software_atelier.simpleflex.rest.auth.rres;

import ch.software_atelier.simpleflex.rest.auth.ExceptionHandler;
import ch.software_atelier.simpleflex.rest.auth.data.DataHandler;
import ch.software_atelier.simpleflex.rest.auth.token.TokenHandler;
import ch.software_atelier.simpleflex.rest.auth.token.TokenHandlerException;
import ch.software_atelier.simpleflex.rest.auth.token.TokenParser;
import java.util.HashMap;

import ch.software_atelier.simpleflex.rest.DefaultRestResource;
import ch.software_atelier.simpleflex.rest.RestRequest;
import ch.software_atelier.simpleflex.rest.RestResponse;
import ch.software_atelier.simpleflex.rest.swagger.BodyParameter;
import ch.software_atelier.simpleflex.rest.swagger.HeaderParameter;
import ch.software_atelier.simpleflex.rest.swagger.MethodDocumentation;
import ch.software_atelier.simpleflex.rest.swagger.ObjectSchemaBuilder;
import org.json.JSONException;
import org.json.JSONObject;

public class FlexibleTokenResource extends DefaultRestResource {
    private final DataHandler _dh;
    private final TokenHandler _th;
    private final TokenParser _tp;
    public FlexibleTokenResource(DataHandler dh, TokenHandler th, TokenParser tp){
        _tp = tp;
        _dh = dh;
        _th = th;
    }

    @Override
    public RestResponse onPOST(RestRequest request) {
        try{
            
            String token = _tp.getToken(request);
            if (token == null)
                return RestResponse.unauthorized_401();
            
            if (_tp.getUsername(token) == null){
                return RestResponse.unauthorized_401();
            }
            
            JSONObject data = request.getJSON();
            if (!data.has("lifetime") || !data.has("claims")){
                return RestResponse.internalServerError_500("Bad request");
            }
            
            int lifetime = data.getInt("lifetime");
            JSONObject jclaims = data.getJSONObject("claims");
            
            HashMap<String,Object> claims = new HashMap<>();
            for (String key:jclaims.keySet()){
                claims.put(key, jclaims.get(key));
            }
            
            String flexible_token = _th.createToken(claims, lifetime);

            JSONObject result = new JSONObject();            
            result.put("access_token", flexible_token);
            result.put("lifetime", lifetime);

            return RestResponse.json_201_created(result);
        }catch(TokenHandlerException | JSONException th){
            return ExceptionHandler.handle(th, true);
        }
    }
    
    @Override
    public void docPOST(MethodDocumentation request){
        request.addTag("Authorisazion");
        request.setTitle("Create Flexible Token");
        request.setDescription("Creates a flexible token that can be used for some actions");
        request.addProduces("application/json");
        request.addParameter(new HeaderParameter("Authorization", "the access token, Baerer"));
        
        request.addParameter(new BodyParameter("body",
            ObjectSchemaBuilder.create("The User Credentials")
                .addSimpleProperty("lifetime", "number", "the lifetime in seconds", true)
                .addObjectProperty("claims", ObjectSchemaBuilder.create("the claims")
                        .addSimpleProperty("key", "string", "key value paires", true).toJSON()
                , true)
                .toJSON()
        ));
        
        request.addResponse("200", "OK", 
            ObjectSchemaBuilder.create("the flexible token")
                .addSimpleProperty("access_token", "string", "the new access token", true)
                .addSimpleProperty("lifetime", "number", "the session lifetime in seconds", true)
                .toJSON()
        );
    }
}
