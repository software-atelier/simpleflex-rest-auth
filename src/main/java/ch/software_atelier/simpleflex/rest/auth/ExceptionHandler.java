package ch.software_atelier.simpleflex.rest.auth;

import ch.software_atelier.simpleflex.rest.auth.data.DataHandlerException;
import ch.software_atelier.simpleflex.rest.auth.token.TokenHandlerException;
import ch.software_atelier.simpleflex.rest.RestResponse;
import org.json.JSONException;

public class ExceptionHandler {
    public static RestResponse handle(Throwable th, boolean handleNullpointer){
        if (th instanceof TokenHandlerException){
            return tokenHandler((TokenHandlerException)th);
        }
        else if (th instanceof DataHandlerException){
            return dataHandler((DataHandlerException)th);
        }
        else if (th instanceof JSONException ){
            return RestResponse.badRequest_400("Invalid payload");
        }
        else if (th instanceof NullPointerException && handleNullpointer){
            return RestResponse.badRequest_400("Invalid payload");
        }
        else{
            return RestResponse.internalServerError_500(th.getMessage());
        }
    }
    
    private static RestResponse tokenHandler(TokenHandlerException the){
        switch(the.getCode()){
            case TokenHandlerException.INTERNAL:
                return RestResponse.internalServerError_500(
                        "Something went wrong. Probably an issue with the database."
                );
            case TokenHandlerException.INVALIDE_SIGNATURE:
            case TokenHandlerException.MALFORMED:
            case TokenHandlerException.UNAUTHORIZED:
            case TokenHandlerException.EXPIRED:
                return RestResponse.unauthorized_401();
        }
        return RestResponse.internalServerError_500(
                "Something went wrong. Probably an issue with the database."
        );
    }
    
    private static RestResponse dataHandler(DataHandlerException dhe){
        switch(dhe.getErrCode()){
            case DataHandlerException.FAILED:
                return RestResponse.notFound_404();
            case DataHandlerException.LOGIN_FAILED:
                return RestResponse.unauthorized_401();
            case DataHandlerException.INTERNAL_ERROR:
                return RestResponse.internalServerError_500(
                        "Something went wrong. Probably an issue with the database."
                );
        }
        
        return RestResponse.internalServerError_500(
                        "Something terribly went wrong. Probably an issue with the database."
                );
    }
}
