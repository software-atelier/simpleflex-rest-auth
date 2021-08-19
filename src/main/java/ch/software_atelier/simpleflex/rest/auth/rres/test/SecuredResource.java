package ch.software_atelier.simpleflex.rest.auth.rres.test;

import ch.software_atelier.simpleflex.rest.DefaultRestResource;
import ch.software_atelier.simpleflex.rest.RestRequest;
import ch.software_atelier.simpleflex.rest.RestResponse;
import ch.software_atelier.simpleflex.rest.auth.data.DataHandler;
import ch.software_atelier.simpleflex.rest.auth.token.TokenHandler;
import ch.software_atelier.simpleflex.rest.auth.token.TokenHandlerException;
import ch.software_atelier.simpleflex.rest.auth.token.TokenParser;

public class SecuredResource extends DefaultRestResource {
    private static final String CONTENT_SECURE = "<html>\n" +
            "  <head>\n" +
            "    <title>SimpleflexAuth</title>\n" +
            "  </head>\n" +
            "  <body>\n" +
            "    <p>You can see secure Content!</p>\n" +
            "  </body>\n" +
            "</html>";

    private static final String CONTENT_FAILED = "<html>\n" +
            "  <head>\n" +
            "    <title>SimpleflexAuth</title>\n" +
            "  </head>\n" +
            "  <body>\n" +
            "    <p>You can <b>NOT</b> see secure Content!</p>\n" +
            "  </body>\n" +
            "</html>";

    private final TokenParser tokenParser;

    public SecuredResource(TokenParser tp) {
        tokenParser = tp;

    }

    @Override
    public RestResponse onGET(RestRequest request) {

        System.out.println(request.getheaders());
        String token = tokenParser.getToken(request);
        try {
            if (tokenParser.isAuthorized(token, "test")) {
                return new RestResponse("secured.html", "text/html", CONTENT_SECURE.getBytes());
            } else {
                return new RestResponse("secured.html", "text/html", CONTENT_FAILED.getBytes());
            }
        } catch (TokenHandlerException e) {
            return new RestResponse("secured.txt", "text/plain", e.getMessage().getBytes());
        }

    }

}
