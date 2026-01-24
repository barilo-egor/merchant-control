package tgb.cryptoexchange.merchantcontrol.exception;

/**
 * Пробрасывается в случае ошибки при преобразовании в java объект.
 */
public class DeserializeEventException extends RuntimeException {

    public DeserializeEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
