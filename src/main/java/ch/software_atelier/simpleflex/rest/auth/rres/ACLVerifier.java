package ch.software_atelier.simpleflex.rest.auth.rres;

import ch.software_atelier.simpleflex.rest.auth.ExceptionHandler;
import ch.software_atelier.simpleflex.rest.auth.data.DataHandler;
import ch.software_atelier.simpleflex.rest.auth.token.TokenHandler;
import ch.software_atelier.simpleflex.rest.auth.token.TokenHandlerException;
import ch.software_atelier.simpleflex.rest.auth.token.TokenParser;
import ch.software_atelier.simpleflex.rest.DefaultRestResource;
import ch.software_atelier.simpleflex.rest.RestRequest;
import ch.software_atelier.simpleflex.rest.RestResponse;
import ch.software_atelier.simpleflex.rest.swagger.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class ACLVerifier extends DefaultRestResource {
    private final DataHandler _dh;
    private final TokenHandler _th;
    private final TokenParser _tp;
    
    public ACLVerifier(DataHandler dh, TokenHandler th, TokenParser tp){
        _tp = tp;
        _dh = dh;
        _th = th;
    }
    
    
    @Override
    public RestResponse onPOST(RestRequest request) {
        try{
            String token = _tp.getToken(request);
            JSONArray acls = request.getJSONArray();
            JSONObject result = new JSONObject();
            for (int i=0;i<acls.length();i++){
                String path = acls.getString(i);
                result.put(path, _tp.verifyACL(path, token));
            }
            return RestResponse.json_200(result);
            
        }catch(TokenHandlerException th){
            return ExceptionHandler.handle(th, true);
        }
    }
    
    @Override
    public void docPOST(MethodDocumentation request){
        request.setTitle("Verify Access");
        request.addTag("Authorisazion");
        request.setDescription("Verifiex the access to given paths");
        request.addProduces("application/json");
        request.addParameter(new HeaderParameter("Authorization", "the access token, Baerer"));
        
        request.addParameter(new BodyParameter("body",
            ArraySchemaBuilder.create("the user information")
                .setBasic( "string", "a path to check")
                .toJSON()
        ));
        request.addResponse("200", "OK", ObjectSchemaBuilder.create("the result")
                .addSimpleProperty("a path", "boolean", "the path as key and a boolean as value, indicating the access", true)
                .toJSON()
        );
    }
}
