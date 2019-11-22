package ch.software_atelier.simpleflex.rest.auth.data;

public class DataHandlerException extends Exception{
    public static final short FAILED = 0;
    public static final short LOGIN_FAILED = -1;
    public static final short GROUP_IN_USE = -2;
    public static final short INTERNAL_ERROR = -100;
    private final short _code;

    public DataHandlerException(short code) {
        this._code = code;
    }
    
    public short getErrCode(){
        return _code;
    }
    
    
}
