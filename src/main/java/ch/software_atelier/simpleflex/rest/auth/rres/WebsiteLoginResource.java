package ch.software_atelier.simpleflex.rest.auth.rres;

import ch.software_atelier.simpleflex.docs.impl.RedirectorDoc;
import ch.software_atelier.simpleflex.rest.DefaultRestResource;
import ch.software_atelier.simpleflex.rest.RestRequest;
import ch.software_atelier.simpleflex.rest.RestResponse;
import ch.software_atelier.simpleflex.rest.auth.data.DataHandler;
import ch.software_atelier.simpleflex.rest.auth.data.DataHandlerException;
import ch.software_atelier.simpleflex.rest.auth.token.TokenHandler;
import ch.software_atelier.simpleflex.rest.auth.token.TokenHandlerException;
import ch.software_atelier.simpleflex.rest.swagger.MethodDocumentation;

public class WebsiteLoginResource extends DefaultRestResource {

    private final DataHandler dataHandler;
    private final TokenHandler tokenHandler;
    private final String successUrl;
    private final String failedUrl;
    private int sessionTimeoutInSeconds;

    public WebsiteLoginResource(DataHandler dh, TokenHandler th, String successUrl, String failedUrl, int sessionTimeoutInSeconds) {
        dataHandler = dh;
        tokenHandler = th;
        this.successUrl = successUrl;
        this.failedUrl = failedUrl;
        this.sessionTimeoutInSeconds = sessionTimeoutInSeconds;
    }

    @Override
    public RestResponse onPOST(RestRequest request) {
        String username = new String(request.getData("user"));
        username = username.substring(0, username.length() - 2);
        String password = new String(request.getData("pass"));
        password = password.substring(0, password.length() - 2);
        try {
            dataHandler.verifyUser(username, password);
            RestResponse response = buildRedirect(successUrl);
            String token = tokenHandler.createToken(username, sessionTimeoutInSeconds);
            bakeCookie(response, token);
            return response;
        } catch (DataHandlerException | TokenHandlerException e) {
            return buildRedirect(failedUrl);
        }
    }

    @Override
    public void docPOST(MethodDocumentation request) {
        super.docPOST(request);
    }

    private RestResponse buildRedirect(String url) {
        RedirectorDoc doc = new RedirectorDoc(url);
        RestResponse response = new RestResponse(doc.name(), doc.mime(), doc.byteData());
        return response;
    }

    private void bakeCookie(RestResponse response, String token) {
        response.addHeader("Set-Cookie:", "auth=" + token);
    }

}
