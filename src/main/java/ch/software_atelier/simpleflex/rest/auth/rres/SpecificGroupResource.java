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
import ch.software_atelier.simpleflex.rest.swagger.HeaderParameter;
import ch.software_atelier.simpleflex.rest.swagger.MethodDocumentation;
import ch.software_atelier.simpleflex.rest.swagger.PathParameter;
import org.json.JSONException;
import org.json.JSONObject;

public class SpecificGroupResource extends DefaultRestResource {

    private final DataHandler _dh;
    private final TokenHandler _th;
    private final TokenParser _tp;
    public SpecificGroupResource(DataHandler dh, TokenHandler th, TokenParser tp){
        _tp = tp;
        _dh = dh;
        _th = th;
    }

    @Override
    public RestResponse onDELETE(RestRequest request){
        try{
            
            String token = _tp.getToken(request);
            if (token == null){
                return RestResponse.unauthorized_401();
            }
            if (_tp.isAdmin(token)){
                
                String name = request.getResourcePlaceholder("name");
                
                _dh.deleteGroup(name);
                return RestResponse.noContent_204();
            }
            return RestResponse.unauthorized_401();
        }catch(TokenHandlerException the){
            return RestResponse.unauthorized_401();
        }catch(JSONException | NullPointerException th){
            th.printStackTrace();
            return ExceptionHandler.handle(th, false);
        }catch(DataHandlerException th){
            if (th.getErrCode() == DataHandlerException.GROUP_IN_USE){
                return RestResponse.badRequest_400("This group is in use by at least one user");
            }
            th.printStackTrace();
            return ExceptionHandler.handle(th, false);
        }
    }
    
    @Override
    public void docDELETE(MethodDocumentation request){
        request.setTitle("Delete Group");
        request.addTag("Authorisazion");
        request.setDescription("Deletes a group if the requestiung user is an admin.");
        request.addProduces("application/json");
        request.addParameter(new HeaderParameter("Authorization", "the access token, Baerer"));
        request.addParameter(new PathParameter("name", "the group name"));
        request.addResponse("204", "No Content", 
            new JSONObject()
        );
        request.addResponse("401", "Unauthorized", 
            new JSONObject()
        );
    }

}
