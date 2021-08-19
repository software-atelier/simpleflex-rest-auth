package ch.software_atelier.simpleflex.rest.auth.rres.test;

import ch.software_atelier.simpleflex.rest.DefaultRestResource;
import ch.software_atelier.simpleflex.rest.RestRequest;
import ch.software_atelier.simpleflex.rest.RestResponse;

public class AuthenticationPageResource extends DefaultRestResource {
    private static final String CONTENT = "<html>\n" +
            "  <head>\n" +
            "    <title>SimpleflexAuth</title>\n" +
            "  </head>\n" +
            "  <body>\n" +
            "  <form action=\"/login\" method=\"post\" enctype=\"multipart/form-data\">\n" +
            "    <p>User</p>\n" +
            "    <p><input type=\"text\" name=\"user\" value=\"\">\n" +
            "    <p>Pass</p>\n" +
            "    <p><input type=\"password\" name=\"pass\">\n" +
            "    <p><button type=\"submit\">Login</button>\n" +
            "  </form>\n" +
            "  </body>\n" +
            "</html>";

    @Override
    public RestResponse onGET(RestRequest request) {
        return new RestResponse("login.html", "text/html", CONTENT.getBytes());
    }
}
