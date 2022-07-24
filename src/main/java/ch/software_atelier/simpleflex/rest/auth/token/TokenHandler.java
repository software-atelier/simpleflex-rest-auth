package ch.software_atelier.simpleflex.rest.auth.token;

import ch.software_atelier.simpleflex.rest.auth.utils.JSONHelper;
import ch.software_atelier.simpleflex.rest.auth.data.DataHandler;
import ch.software_atelier.simpleflex.rest.auth.data.DataHandlerException;
import com.google.gson.Gson;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;

/**
 * @author tk
 */
public class TokenHandler {
    private final String _secret;
    private final DataHandler _dataHandler;
    private final int _maxSessionLength;
    private final TokenParser _parser;

    public TokenHandler(String secret, DataHandler dh, int maxSessionLength) {
        _secret = secret;
        _dataHandler = dh;
        _maxSessionLength = maxSessionLength;
        _parser = new TokenParser(secret);
    }

    public String renew(String token) throws TokenHandlerException {
        Map claims = _parser.getClaims(token);
        try {
            Object realms = claims.get("realms");
            Object admin = claims.get("admin");
            Object user = claims.get("username");
            Object acl = claims.get("acl");
            String newToken = Jwts.builder()
                    .setIssuer("SimpeleflexAuth")
                    .setIssuedAt(Date.from(Instant.now()))
                    .setExpiration(Date.from(Instant.now().plus(_maxSessionLength, ChronoUnit.SECONDS)))
                    .claim("admin", admin)
                    .claim("realms", realms)
                    .claim("username", user)
                    .claim("acl", acl)
                    .signWith(SignatureAlgorithm.HS256, _secret.getBytes("UTF-8"))
                    .compact();
            return newToken;
        } catch (UnsupportedEncodingException uee) {
            throw new TokenHandlerException(TokenHandlerException.INTERNAL);
        }
    }

    public String createToken(String user) throws TokenHandlerException {
        return createToken(user, _maxSessionLength);
    }

    public String createToken(String user, int lifetimeInSeconds) throws TokenHandlerException {
        HashMap<String, Object> claims = new HashMap<>();
        System.out.println("Vreating token");
        try {
            HashMap<String, String> realmsMap = _dataHandler.getRealms(user);
            ArrayList<String> realms = new ArrayList<>();
            Set<String> keys = realmsMap.keySet();
            for (String key : keys) {
                realms.add(key);
            }
            claims.put("admin", _dataHandler.isAdmin(user));
            claims.put("realms", realms);
            claims.put("username", user);

            JSONObject aclJSON = new JSONObject();
            for (String group : _dataHandler.getUserGroups(user)) {
                JSONHelper.deepMerge(_dataHandler.getGroupACLasJSON(group), aclJSON);
            }
            JSONHelper.deepMerge(_dataHandler.getUserACLasJSON(user), aclJSON);
            Gson gson = new Gson();
            Map acl = gson.fromJson(aclJSON.toString(), Map.class);
            claims.put("acl", acl);

        } catch (DataHandlerException dhe) {
            throw new TokenHandlerException(TokenHandlerException.UNAUTHORIZED);
        }

        return createToken(claims, lifetimeInSeconds);
    }

    public String createToken(HashMap<String, Object> claims, int lifetimeInSeconds) throws TokenHandlerException {
        try {

            JwtBuilder builder = Jwts.builder();
            builder = builder.setIssuer("SimpeleflexAuth")
                    .setIssuedAt(Date.from(Instant.now()))
                    .setExpiration(Date.from(Instant.now().plus(lifetimeInSeconds, ChronoUnit.SECONDS)));

            if (claims != null) {
                for (String key : claims.keySet()) {
                    builder = builder.claim(key, claims.get(key));
                }
            }
            String token = builder.signWith(SignatureAlgorithm.HS256, _secret.getBytes("UTF-8"))
                    .compact();
            return token;
        } catch (UnsupportedEncodingException uee) {
            throw new TokenHandlerException(TokenHandlerException.INTERNAL);
        }
    }

    public int getSessionLength() {
        return _maxSessionLength;
    }
}
