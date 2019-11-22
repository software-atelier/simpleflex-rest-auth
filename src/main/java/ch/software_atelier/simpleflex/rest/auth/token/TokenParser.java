package ch.software_atelier.simpleflex.rest.auth.token;

import ch.software_atelier.simpleflex.rest.auth.utils.StrHlp;
import ch.software_atelier.simpleflex.rest.auth.utils.JSONHelper;
import ch.software_atelier.simpleflex.rest.RestRequest;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.impl.DefaultClaims;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import org.json.JSONObject;

public class TokenParser {
    private final String _secret;
    public TokenParser(String secret){
        _secret = secret;
    }
    
    public String getToken(RestRequest req){
        try{
            
            if (req.getHeaderValue("Authorization")!=null){
                String header = req.getHeaderValue("Authorization");
                StringTokenizer st = new StringTokenizer(header);

                st.nextToken();
                return st.nextToken();
            }
            else{
                return req.getRequestArgument("token");
            }
            
        }catch(NullPointerException | NoSuchElementException e){
            return null;
        }
    }
    
    public boolean isAdmin(String token)throws TokenHandlerException{
            DefaultClaims claims = getClaims(token);
            return (Boolean)claims.get("admin");
    }
    
    public String getUsername(String token) throws TokenHandlerException{
        DefaultClaims claims = getClaims(token);
        return claims.get("username").toString();
    }
    
    public boolean allowes(String token, JSONObject fields) throws TokenHandlerException{
        DefaultClaims claims = getClaims(token);
        for (String key: fields.keySet()){
            if (claims.containsKey(key)){
                if (!fields.get(key).equals(claims.get(key))){
                    return false;
                }
            }
            else{
                return false;
            }
        }
        return true;
    }
    
    public boolean isAuthorized(String token, String realm)throws TokenHandlerException{
            return getRealms(token).contains(realm);
    }
    
    public boolean verifyACL(String path, String token)throws TokenHandlerException{
        try{
            List<String> pathItems = StrHlp.tokenize(path, "/");
            
            DefaultClaims claims = getClaims(token);
            Map mapACL = claims.get("acl", Map.class);
            
            
            JSONObject jACL = JSONHelper.mapToJSON(mapACL);

            boolean allowed = false;
            if (JSONHelper.getAtPath(jACL, "/_access") != null)
                allowed = (Boolean)JSONHelper.getAtPath(jACL, "/_access");
            StringBuffer jPath = new StringBuffer();
            for (String item : pathItems){
                
                Object o = JSONHelper.getAtPath(jACL, new StringBuffer().append(jPath).append("/").append(item).toString());
                Object oValue;
                if (o == null){
                    oValue = JSONHelper.getAtPath(jACL, jPath+"/_other/_access");
                    jPath.append("/_other");
                }
                else{
                    oValue = JSONHelper.getAtPath(jACL, jPath+"/"+item+"/_access");
                    jPath.append("/").append(item);
                }

                if (oValue != null){
                    allowed = (Boolean)oValue;
                }
            }
            return allowed;
        }catch(ClassCastException cce){
            return false;
        }
    }
    
    public ArrayList<String> getRealms(String token)throws TokenHandlerException{
        DefaultClaims claims = getClaims(token);
        ArrayList<String> realms = (ArrayList<String>)claims.get("realms");
        return realms;
    }
    
    public DefaultClaims getClaims(String token) throws TokenHandlerException{
        try{
            Jwt jwt = Jwts.parser().setSigningKey(_secret.getBytes("UTF-8")).parse(token);
            return (DefaultClaims)jwt.getBody();
        }catch(UnsupportedEncodingException uee){
            throw new TokenHandlerException(TokenHandlerException.INTERNAL);
        }catch(ExpiredJwtException ee){
            throw new TokenHandlerException(TokenHandlerException.EXPIRED);
        }catch(SignatureException se){
            throw new TokenHandlerException(TokenHandlerException.INVALIDE_SIGNATURE);
        }catch(IllegalArgumentException | UnsupportedJwtException | MalformedJwtException iae){
            throw new TokenHandlerException(TokenHandlerException.MALFORMED);
        }
        
    }
}
