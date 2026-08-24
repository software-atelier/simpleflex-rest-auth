# Simpleflex REST Auth

`simpleflex-auth` is the authentication and authorisation extension for a
Simpleflex REST application. It supplies signed JWT sessions, login/session
resources, user and group administration, and a path-based ACL model.

The library builds on two sibling projects:

* **simpleflex-base** starts a `WebApp` and passes its configuration to
  `start(String, HashMap<String, Object>, SimpleFlexAccesser)`.
* **simpleflex-rest** supplies `RestApp`, `DefaultRestResource`,
  `RestRequest`, `RestResponse`, and `addResource(...)`.
* **simpleflex-auth** supplies `TokenHandler`, `TokenParser`, the
  `DataHandler` storage SPI, and ready-made REST resources.

There is deliberately no `AuthProvider` type and no annotation-based security
mechanism in this version. The integration point for an own database, LDAP, or
identity service is `DataHandler`; a protected endpoint explicitly uses a
`TokenParser`.

## Dependency

```xml
<dependency>
    <groupId>ch.software-atelier</groupId>
    <artifactId>simpleflex-auth</artifactId>
    <version>2.4.4</version>
</dependency>
```

`simpleflex-rest` is a transitive dependency of this artifact. Applications
that extend `RestApp` also need `simpleflex-base` available, as shown below.

## Quick start: login and a protected endpoint

The following complete application uses the supplied MongoDB implementation,
registers `POST /session` for login, and protects `GET /orders`.  The config
keys are application conventions; `App.java` in this project uses the same
`$mongoUri`, `$secret`, and `$sessionTimeout` names.

```java
package example;

import ch.software_atelier.simpleflex.SimpleFlexAccesser;
import ch.software_atelier.simpleflex.SimpleFlexBase;
import ch.software_atelier.simpleflex.rest.DefaultRestResource;
import ch.software_atelier.simpleflex.rest.RestApp;
import ch.software_atelier.simpleflex.rest.RestRequest;
import ch.software_atelier.simpleflex.rest.RestResponse;
import ch.software_atelier.simpleflex.rest.auth.data.DataHandler;
import ch.software_atelier.simpleflex.rest.auth.data.MongoDBDataHandler;
import ch.software_atelier.simpleflex.rest.auth.rres.SessionResource;
import ch.software_atelier.simpleflex.rest.auth.token.TokenHandler;
import ch.software_atelier.simpleflex.rest.auth.token.TokenHandlerException;
import ch.software_atelier.simpleflex.rest.auth.token.TokenParser;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;

public class ExampleApp extends RestApp {
    public static void main(String[] args) {
        HashMap<String, Object> config = new HashMap<String, Object>();
        config.put("$mongoUri", "mongodb://localhost:27017/example");
        // HS256 needs a sufficiently long random secret (at least 32 UTF-8 bytes).
        config.put("$secret", "replace-with-a-random-secret-of-at-least-32-bytes");
        config.put("$sessionTimeout", "900"); // seconds
        SimpleFlexBase.serveOnLocalhost(ExampleApp.class.getName(), config, 18001);
    }

    @Override
    public void start(String name, HashMap<String, Object> config,
                      SimpleFlexAccesser sfa) {
        super.start(name, config, sfa);

        DataHandler data = new MongoDBDataHandler(config.get("$mongoUri").toString());
        int sessionSeconds = Integer.parseInt(config.get("$sessionTimeout").toString());
        String secret = config.get("$secret").toString();
        TokenHandler tokens = new TokenHandler(secret, data, sessionSeconds);
        TokenParser parser = new TokenParser(secret);

        addResource("/session", new SessionResource(data, tokens, parser));
        addResource("/orders", new OrdersResource(parser));
    }

    static class OrdersResource extends DefaultRestResource {
        private final TokenParser tokens;

        OrdersResource(TokenParser tokens) {
            this.tokens = tokens;
        }

        @Override
        public RestResponse onGET(RestRequest request) {
            String token = tokens.getToken(request);
            if (token == null) {
                return RestResponse.unauthorized_401();
            }
            try {
                // `orders` is a realm carried in the JWT at login time.
                if (!tokens.isAuthorized(token, "orders")) {
                    return RestResponse.unauthorized_401();
                }
                return RestResponse.json_200(new JSONObject()
                        .put("owner", tokens.getUsername(token)));
            } catch (TokenHandlerException e) {
                return RestResponse.unauthorized_401();
            }
        }
    }
}
```

Before a user can log in, create it through an administrator-protected
`UserResource`, or seed it through the chosen `DataHandler`. A user has a
username, password, boolean admin flag, and a map of realms. For example,
`orders -> Orders API` makes `tokens.isAuthorized(token, "orders")` succeed
for a newly issued token.

Login with JSON:

```http
POST /session
Content-Type: application/json

{"user":"alice","pass":"correct horse battery staple"}
```

The response is `201 Created` and contains `access_token`, `lifetime`, and a
`realms` array. Send the returned token on subsequent requests:

```http
GET /orders
Authorization: Bearer <access_token>
```

`TokenParser.getToken(request)` accepts a two-part `Authorization` header,
the `auth` cookie, or the request argument `token` (in that order). The scheme
word is not validated, so use the conventional `Bearer` spelling yourself.

## JWT tokens

`TokenHandler` creates HS256-signed JWTs with issuer `SimpeleflexAuth`, `iat`,
and `exp`. The user-oriented overload reads these claims from `DataHandler`:

| Claim | Source |
| --- | --- |
| `username` | requested user |
| `admin` | `DataHandler.isAdmin(user)` |
| `realms` | keys of `DataHandler.getRealms(user)` |
| `acl` | merged group ACLs and the user's ACL |

Use the public API directly when a non-session token is required:

```java
HashMap<String, Object> claims = new HashMap<String, Object>();
claims.put("username", "service-account");
claims.put("purpose", "one-time-export");

String token = tokenHandler.createToken(claims, 60);
Map parsedClaims = tokenParser.getClaims(token);
String user = tokenParser.getUsername(token); // requires a username claim
```

`createToken(String user)` uses the configured session length;
`createToken(String user, int lifetimeInSeconds)` overrides it. `renew(token)`
validates the old token and issues a new one with the configured lifetime,
preserving `admin`, `realms`, `username`, and `acl`. It does not reload those
claims from storage, so changed permissions take effect only when a new token
is created after login.

`TokenParser.getClaims`, `getUsername`, `isAdmin`, `isAuthorized`, `allowes`,
and `verifyACL` throw `TokenHandlerException` for expired, malformed, invalid
signature, or unauthorised tokens. Map that exception to `401`; the supplied
`ExceptionHandler.handle(...)` does so.

### Flexible tokens

`FlexibleTokenResource` exposes a token minting endpoint when you register it:

```java
addResource("/token", new FlexibleTokenResource(data, tokens, parser));
```

It requires a valid token with a `username` claim, then accepts:

```json
{"lifetime":60,"claims":{"purpose":"download","file":"report.csv"}}
```

and returns `201` with `access_token` and `lifetime`. This resource does not
apply an admin or ACL check of its own; register it only behind an appropriate
application-level policy.

## Login, renewal, and browser login

`SessionResource` provides the API session flow:

| Route and method | Behaviour |
| --- | --- |
| `POST /session` | Verifies JSON `user`/`pass`, then returns a new token, lifetime, and realm labels. |
| `PUT /session` | Validates the supplied token and returns a renewed token and lifetime. |

There is no server-side token store, revocation endpoint, or `DELETE /session`
logout implementation. For header-based clients, logout means discard the
token. For browser cookies, expire/clear the `auth` cookie in an application
resource.

For HTML form login, register `WebsiteLoginResource`:

```java
addResource("/login", new WebsiteLoginResource(
        data, tokens, "/login-success", "/login-failed", 3600));
```

It reads multipart form fields named `user` and `pass`, verifies them, redirects
to the configured URL, and sets `Set-Cookie: auth=<JWT>` on success. A resource
then obtains the cookie automatically through `TokenParser.getToken(request)`.
This implementation sets only the cookie name/value; deploy it over HTTPS and
add the cookie attributes required by your application at the proxy or in a
custom login resource.

## Protecting resources and authorisation

Protection is code-based: extend `DefaultRestResource`, obtain the token, and
call the check matching your policy. `DefaultRestResource` makes unimplemented
HTTP methods return 405.

```java
@Override
public RestResponse onGET(RestRequest request) {
    String token = parser.getToken(request);
    if (token == null) return RestResponse.unauthorized_401();
    try {
        if (!parser.isAdmin(token)) return RestResponse.unauthorized_401();
        return RestResponse.json_200(new JSONObject().put("ok", true));
    } catch (TokenHandlerException e) {
        return RestResponse.unauthorized_401();
    }
}
```

Available checks are:

* `isAdmin(token)` for the boolean administrator claim.
* `isAuthorized(token, realm)` for a realm name.
* `allowes(token, JSONObject fields)` for exact equality of supplied claim
  fields (the method name is intentionally `allowes` in the API).
* `verifyACL(path, token)` for ACL path access.

### Roles, groups, and ACLs

Roles are represented by the `admin` claim and realms. Groups carry YAML ACLs;
at token creation, ACLs of all valid user groups are deep-merged and the user
ACL is merged afterwards, so the user ACL wins on conflicting scalar values.

ACL YAML is a tree. `_access` sets the current node's default and `_other`
sets a fallback for unmatched children:

```yaml
_access: false
projects:
  _other:
    _access: true
  payroll:
    _access: false
```

With this ACL, `verifyACL("/projects/alpha", token)` is true and
`verifyACL("/projects/payroll", token)` is false. Register `ACLVerifier` to
batch-check paths through `POST`:

```java
addResource("/acl", new ACLVerifier(data, tokens, parser));
```

Its request body is a JSON array such as `["/projects/alpha", "/payroll"]`
and the response maps each input path to a boolean.

The management resources are separate and must be registered explicitly:

```java
addResource("/user", new UserResource(data, tokens, parser));
addResource("/user/{name}", new SpecificUserResource(data, tokens, parser));
addResource("/user/{name}/settings", new UserSettingsResource(data, tokens, parser));
addResource("/user/{name}/acl", new UserACLResource(data, tokens, parser));
addResource("/user/{name}/groups", new UserGroupsResource(data, tokens, parser));
addResource("/group", new GroupResource(data, tokens, parser));
addResource("/group/{name}", new SpecificGroupResource(data, tokens, parser));
addResource("/group/{name}/acl", new GroupACLResource(data, tokens, parser));
```

These resources use the following policies: user/group listings and creation,
group deletion, and ACL/group assignments require `admin`; a user may read
their own details and settings, and change their own password only with
`old_pass`. An administrator may manage other users. A group cannot be deleted
while it is assigned to a user.

## Storage options

### MongoDB

`MongoDBDataHandler(String uri)` connects to the database selected by the
MongoDB connection URI and creates/uses the `_auth` collection. It stores
users, groups, realms, settings, group membership, and YAML ACLs (also parsed
as JSON). It hashes passwords using MD5. This is retained for compatibility;
for a new deployment, use a custom `DataHandler` with a modern password hash.

### Own provider: implement `DataHandler`

`DataHandler` is the complete provider SPI. There is no smaller credential
provider interface: the same provider supplies credentials, users, groups,
realms, settings, and ACLs to the built-in resources. An LDAP, JDBC, or remote
identity implementation must implement every method below; throw
`DataHandlerException.LOGIN_FAILED` for rejected credentials,
`DataHandlerException.FAILED` when an entity is absent, and
`DataHandlerException.INTERNAL_ERROR` for infrastructure failures.

This compilable adapter demonstrates the exact interface surface. Delegate
each operation to your DB/LDAP service; the example intentionally leaves
unsupported administration operations explicit instead of silently succeeding.

```java
import ch.software_atelier.simpleflex.rest.auth.data.DataHandler;
import ch.software_atelier.simpleflex.rest.auth.data.DataHandlerException;
import java.util.HashMap;
import java.util.List;
import org.json.JSONObject;

public final class LdapDataHandler implements DataHandler {
    // Implement these two methods with the LDAP bind/search for your directory.
    @Override public void verifyUser(String user, String pass) throws DataHandlerException {
        boolean accepted = ldapBindAndFindUser(user, pass);
        if (!accepted) throw new DataHandlerException(DataHandlerException.LOGIN_FAILED);
    }
    @Override public boolean isAdmin(String user) throws DataHandlerException {
        return ldapGroups(user).contains("simpleflex-admin");
    }
    @Override public HashMap<String, String> getRealms(String user) throws DataHandlerException {
        return realmLabelsFor(ldapGroups(user));
    }

    // ACL data can come from your DB; empty ACL/group data is valid.
    @Override public List<String> getUserGroups(String user) throws DataHandlerException { return ldapGroups(user); }
    @Override public JSONObject getUserACLasJSON(String user) throws DataHandlerException { return new JSONObject(); }
    @Override public String getUserACLasYAML(String user) throws DataHandlerException { return ""; }
    @Override public JSONObject getGroupACLasJSON(String name) throws DataHandlerException { return new JSONObject(); }
    @Override public String getGroupACLasYAML(String name) throws DataHandlerException { return ""; }

    // Implement these if the built-in administration resources are registered.
    @Override public void putUser(String u, String p, boolean a) throws DataHandlerException { unsupported(); }
    @Override public void putUserSettings(String u, HashMap<String, String> s) throws DataHandlerException { unsupported(); }
    @Override public void putUserACL(String u, String acl) throws DataHandlerException { unsupported(); }
    @Override public void setUserGroups(String n, List<String> g) throws DataHandlerException { unsupported(); }
    @Override public void putGroup(String n) throws DataHandlerException { unsupported(); }
    @Override public void putGroupACL(String n, String acl) throws DataHandlerException { unsupported(); }
    @Override public void deleteGroup(String n) throws DataHandlerException { unsupported(); }
    @Override public void putRealms(String u, HashMap<String, String> r) throws DataHandlerException { unsupported(); }
    @Override public void putAdmin(String u, boolean a) throws DataHandlerException { unsupported(); }
    @Override public List<String> getUsers() { return java.util.Collections.emptyList(); }
    @Override public List<String> getUsersBySetting(String k, String v) { return java.util.Collections.emptyList(); }
    @Override public void deleteUser(String u) throws DataHandlerException { unsupported(); }
    @Override public HashMap<String, String> getUserSettings(String u) throws DataHandlerException {
        return new HashMap<String, String>();
    }
    @Override public List<String> getGroups() throws DataHandlerException {
        return java.util.Collections.emptyList();
    }

    private void unsupported() throws DataHandlerException {
        throw new DataHandlerException(DataHandlerException.FAILED);
    }
    // Application-specific methods, implemented against your directory:
    private boolean ldapBindAndFindUser(String user, String pass) { /* LDAP bind */ return false; }
    private List<String> ldapGroups(String user) { /* LDAP group lookup */ return java.util.Collections.emptyList(); }
    private HashMap<String, String> realmLabelsFor(List<String> groups) { return new HashMap<String, String>(); }
}
```

Use it exactly where `MongoDBDataHandler` would be constructed; all resources
receive the same instance:

```java
DataHandler data = new LdapDataHandler();
TokenHandler tokens = new TokenHandler(secret, data, sessionSeconds);
TokenParser parser = new TokenParser(secret);
addResource("/session", new SessionResource(data, tokens, parser));
```

If you register `UserResource`, `GroupResource`, ACL, settings, or membership
resources, replace the corresponding `unsupported()` methods with real writes
and reads. For a login-only directory, register only `SessionResource` and
your own protected application resources; the methods used by issuing a token
are `verifyUser`, `getRealms`, `isAdmin`, `getUserGroups`,
`getGroupACLasJSON`, and `getUserACLasJSON`.

## Errors and HTTP responses

The built-in resources return `401 Unauthorized` for a missing, expired,
malformed, invalidly signed token, failed credential verification, or an
unsatisfied authorisation check. This library does not expose a 403 response in
these flows; use `401` consistently when using the supplied resources, or add
your own policy/response in a custom resource if your API distinguishes an
authenticated-but-forbidden request. Invalid JSON/payloads are generally 400;
missing users/groups map to 404 in `ExceptionHandler`; storage faults map to
500. Group deletion while still assigned returns 400.

## Security notes

* Keep `$secret` outside source control (environment, deployment secret store,
  or protected Simpleflex configuration). All services that validate a token
  must use the same secret; rotating it invalidates existing tokens.
* Use a cryptographically random secret with at least 32 UTF-8 bytes for HS256.
  Do not use the demonstration value from this README.
* Keep access-token lifetimes short and renew deliberately. There is no
  revocation list, and renewal preserves claims from the old token.
* Use TLS. JWTs are bearer credentials whether sent in `Authorization`, a
  query/request argument, or the `auth` cookie. Prefer the header for APIs and
  avoid logging tokens.
* `MongoDBDataHandler` uses MD5, which is not suitable for new password
  storage. Use a custom `DataHandler` backed by Argon2id, bcrypt, scrypt, or
  PBKDF2 and a properly configured identity store.
* Review who can register `FlexibleTokenResource` and management resources;
  their route registration is application-controlled.

## Testing

Run the test suite with Maven:

```bash
mvn test
```

`TokenHandlerTest` covers signed claims, realm checks, ACL defaults and
`_other` fallback, expired tokens, and malformed tokens. Add tests for every
custom `DataHandler`, especially credential failure, realm mapping, ACL merge
inputs, and the status codes of protected resources.

## License

Apache License 2.0. See [LICENSE](LICENSE).
