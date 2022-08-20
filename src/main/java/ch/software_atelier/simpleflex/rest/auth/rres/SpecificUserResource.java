package ch.software_atelier.simpleflex.rest.auth.rres;

import java.util.HashMap;
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
import org.json.JSONException;
import org.json.JSONObject;

public class SpecificUserResource extends DefaultRestResource {

    private final DataHandler _dh;
    private final TokenHandler _th;
    private final TokenParser _tp;
    public SpecificUserResource(DataHandler dh, TokenHandler th, TokenParser tp){
        _tp = tp;
        _dh = dh;
        _th = th;
    }
    
    /**
     * Returns the User Information and the realms if the user is an admin
     * of if he retrieves his own information.
     */
    @Override
    public RestResponse onGET(RestRequest request) {
        try{
            String token = _tp.getToken(request);
            String usernameByToken = _tp.getUsername(token);
            String usernameByPath = request.getResourcePlaceholder("name");
            if (usernameByToken.equals(usernameByPath) || _tp.isAdmin(token)){
                JSONObject obj = new JSONObject();
                obj.put("username", usernameByPath);
                obj.put("admin", _dh.isAdmin(usernameByPath));
                obj.put("realms", SessionResource.realmsToArray(
                        _dh.getRealms(usernameByPath))
                );
                return RestResponse.json_200(obj);
            }
            return RestResponse.unauthorized_401();
        }catch(DataHandlerException | TokenHandlerException | JSONException | NullPointerException th){
            return ExceptionHandler.handle(th, true);
        }
    }
    
    @Override
    public void docGET(MethodDocumentation request){
        request.setTitle("User Information");
        request.addTag("Authorisazion");
        request.setDescription("Returns the User Information and the realms if the user is an admin of if he retrieves his own information.");
        request.addProduces("application/json");
        request.addParameter(new HeaderParameter("Authorization", "the access token, Baerer"));
        request.addParameter(new PathParameter("name", "the username"));
        request.addResponse("200", "OK", 
            ObjectSchemaBuilder.create("The new session data")
                .addSimpleProperty("username", "string", "the username", true)
                .addSimpleProperty("admin", "boolean", "wether the user is admin or not", true)
                .addObjectProperty("realms", 
                    ArraySchemaBuilder.create("the realms this user has access to")
                    .setBasic("string", "a realm")
                    .toJSON()
                , true)
                .toJSON()
        );
    }
    
    @Override
    public RestResponse onDELETE(RestRequest request){
        try{
            
            String token = _tp.getToken(request);
            if (token == null){
                return RestResponse.unauthorized_401();
            }
            if (_tp.isAdmin(token)){
                
                String username = request.getResourcePlaceholder("name");
                if (_tp.getUsername(token).equals(username)){
                    return RestResponse.unauthorized_401();
                }
                
                _dh.deleteUser(username);
                return RestResponse.noContent_204();
            }
            return RestResponse.unauthorized_401();
        }catch(TokenHandlerException the){
            return RestResponse.unauthorized_401();
        }catch(JSONException | NullPointerException th){
            return ExceptionHandler.handle(th, false);
        }catch(DataHandlerException th){
            return ExceptionHandler.handle(th, false);
        }
    }
    
    @Override
    public void docDELETE(MethodDocumentation request){
        request.setTitle("Delete User");
        request.addTag("Authorisazion");
        request.setDescription("Deletes a user if the requestiung user is an admin and if he does not try to delete his own account.");
        request.addProduces("application/json");
        request.addParameter(new HeaderParameter("Authorization", "the access token, Baerer"));
        request.addParameter(new PathParameter("name", "the username"));
        request.addResponse("204", "No Content", 
            new JSONObject()
        );
        request.addResponse("401", "Unauthorized", 
            new JSONObject()
        );
    }
    
    /**
     * The User can change his password, if he knows the old one. An admin can change the password of any user.
     * TODO Code Cleanup and Refactoring
     */
    @Override
    public RestResponse onPUT(RestRequest request) {
        try{
            String token = _tp.getToken(request);
            
            String userByToken = _tp.getUsername(token);
            String user = request.getResourcePlaceholder("name");

            JSONObject obj = request.getJSON();
            
            boolean changePass = (obj.has("old_pass") || _tp.isAdmin(token)) && obj.has("pass");
            boolean changeRealms = obj.has("realms");
            boolean changeAdmin = obj.has("admin");
            // check wether the user can do this or not
            if (!isAdminOrSelf(userByToken, user)){
                return RestResponse.unauthorized_401();
            }
            
            if (isNoAdminButWantsToChangePermissions(userByToken,obj)){
                return RestResponse.unauthorized_401();   
            }
            
            if (changePass){
                String old_pass = null;
                if (obj.has("old_pass")||!obj.isNull("old_pass")){
                    old_pass = obj.getString("old_pass"); obj.isNull("old_pass");
                }

                String pass = obj.getString("pass");
                boolean isAdmin = _dh.isAdmin(user);
                try{
                    if (!_tp.isAdmin(userByToken)) {
                        if (old_pass == null){
                            return RestResponse.badRequest_400("old password is invalid");
                        }
                        _dh.verifyUser(user, old_pass);
                    }
                }catch(DataHandlerException e){
                    return RestResponse.badRequest_400("old password is invalid");
                }
                _dh.putUser(user, pass, isAdmin);
            }
            
            if (changeRealms){
                JSONObject realms = obj.getJSONObject("realms");
                HashMap<String,String> realmsH = new HashMap<>();
                for (String key:realms.keySet()){
                    realmsH.put(key,realms.optString(key,""));
                }
                _dh.putRealms(user, realmsH);
            }
            
            if (changeAdmin){
                boolean admin = obj.optBoolean("admin");
                _dh.putAdmin(user, admin);
            }
            
            JSONObject response = new JSONObject();
            response.put("msg", "ok");
            return RestResponse.json_200(response);
              
        }catch(DataHandlerException | JSONException | TokenHandlerException th){
            return ExceptionHandler.handle(th, true);
        }
    }

    private boolean isAdminOrSelf(String self, String user){
        try {
            return (user.equals(self) || _tp.isAdmin(self));
        } catch (TokenHandlerException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean isNoAdminButWantsToChangePermissions(String self, JSONObject obj){
        try {
            boolean changeRealms = obj.has("realms");
            boolean changeAdmin = obj.has("admin");
            return !_tp.isAdmin(self) & (changeRealms || changeAdmin);
        } catch (TokenHandlerException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void docPUT(MethodDocumentation request){
        request.setTitle("Change password");
        request.addTag("Authorisazion");
        request.setDescription("Change the users password, realms or admin-status. None-Admin user can only change the password if old_pass is known.");
        request.addProduces("application/json");
        request.addParameter(new HeaderParameter("Authorization", "the access token, Baerer"));
        request.addParameter(new PathParameter("name", "the username"));
        request.addParameter(new BodyParameter("body", 
            ObjectSchemaBuilder.create("The Change password data")
                .addSimpleProperty("user", "string", "the username", false)
                .addSimpleProperty("old_pass", "string", "the previews password", false)
                .addSimpleProperty("pass", "string", "the new password", false)
                .addObjectProperty("realms", 
                    ObjectSchemaBuilder.create("The realms. key: realmname, value: realmdescription").toJSON(), false)
                .addSimpleProperty("admin", "boolean", "change this users admin privileges", false)
                .toJSON()
        ));
        
        request.addResponse("200", "OK", 
            ObjectSchemaBuilder.create("update a user")
                .addSimpleProperty("msg", "string", "the password changed message", true)
                .toJSON()
        );
    }
    
}
