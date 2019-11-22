package ch.software_atelier.simpleflex.rest.auth.token;

public class TokenHandlerException extends Exception{
    public static final int INVALIDE_SIGNATURE = -1;
    public static final int EXPIRED = -2;
    public static final int MALFORMED = -3;
    public static final int UNAUTHORIZED = -4;
    public static final int INTERNAL = -100;
    
    private final int _code;
    
    public TokenHandlerException(int code){
        _code = code;
    }
    
    public int getCode(){
        return _code;
    }
}
