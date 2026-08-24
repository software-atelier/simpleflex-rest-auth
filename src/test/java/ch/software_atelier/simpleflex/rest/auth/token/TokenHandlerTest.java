package ch.software_atelier.simpleflex.rest.auth.token;

import java.util.ArrayList;
import java.util.HashMap;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenHandlerTest {
    private static final String SECRET = "test-secret-with-at-least-thirty-two-characters";

    private String tokenWithClaims() throws TokenHandlerException {
        HashMap<String, Object> claims = new HashMap<String, Object>();
        claims.put("username", "alice");
        claims.put("admin", Boolean.TRUE);
        ArrayList<String> realms = new ArrayList<String>();
        realms.add("members");
        claims.put("realms", realms);
        HashMap<String, Object> acl = new HashMap<String, Object>();
        acl.put("_access", Boolean.FALSE);
        HashMap<String, Object> projects = new HashMap<String, Object>();
        HashMap<String, Object> other = new HashMap<String, Object>();
        other.put("_access", Boolean.TRUE);
        projects.put("_other", other);
        acl.put("projects", projects);
        claims.put("acl", acl);
        return new TokenHandler(SECRET, null, 600).createToken(claims, 600);
    }

    @Test
    void readsClaimsAndAuthorizationFromSignedToken() throws Exception {
        String token = tokenWithClaims();
        TokenParser parser = new TokenParser(SECRET);

        assertEquals("alice", parser.getUsername(token));
        assertTrue(parser.isAdmin(token));
        assertTrue(parser.isAuthorized(token, "members"));
        assertFalse(parser.isAuthorized(token, "guests"));
        assertTrue(parser.allowes(token, new JSONObject().put("username", "alice")));
        assertFalse(parser.allowes(token, new JSONObject().put("username", "bob")));
    }

    @Test
    void appliesAclDefaultAndOtherFallback() throws Exception {
        TokenParser parser = new TokenParser(SECRET);
        String token = tokenWithClaims();

        assertTrue(parser.verifyACL("/projects/any-project", token));
        assertFalse(parser.verifyACL("/unlisted", token));
    }

    @Test
    void reportsExpiredAndMalformedTokens() throws Exception {
        TokenHandler handler = new TokenHandler(SECRET, null, 600);
        String expired = handler.createToken(new HashMap<String, Object>(), -60);
        TokenParser parser = new TokenParser(SECRET);

        TokenHandlerException expiration = assertThrows(TokenHandlerException.class,
                () -> parser.getClaims(expired));
        assertEquals(TokenHandlerException.EXPIRED, expiration.getCode());

        TokenHandlerException malformed = assertThrows(TokenHandlerException.class,
                () -> parser.getClaims("not-a-token"));
        assertEquals(TokenHandlerException.MALFORMED, malformed.getCode());
    }
}
