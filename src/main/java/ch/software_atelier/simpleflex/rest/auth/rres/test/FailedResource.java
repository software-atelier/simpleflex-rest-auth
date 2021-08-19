package ch.software_atelier.simpleflex.rest.auth.rres.test;

import ch.software_atelier.simpleflex.rest.DefaultRestResource;
import ch.software_atelier.simpleflex.rest.RestRequest;
import ch.software_atelier.simpleflex.rest.RestResponse;

public class FailedResource extends DefaultRestResource {
    private static final String CONTENT = "<html>\n" +
            "  <head>\n" +
            "    <title>SimpleflexAuth</title>\n" +
            "  </head>\n" +
            "  <body>\n" +
            "    <p>Failed</p>\n" +
            "  </body>\n" +
            "</html>";

    @Override
    public RestResponse onGET(RestRequest request) {
        return new RestResponse("failed.html", "text/html", CONTENT.getBytes());
    }
}
