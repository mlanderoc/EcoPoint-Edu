package com.ecopoint.controller;

import com.ecopoint.dto.ApiError;
import com.ecopoint.dto.FieldErrorDto;
import com.ecopoint.exceptions.AccessDeniedException;
import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.exceptions.PersistenceException;
import com.ecopoint.exceptions.QualityControlException;
import com.ecopoint.exceptions.ResourceNotFoundException;
import com.ecopoint.exceptions.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Turns every exception thrown while answering a request into the error body of
 * spec 003 (R-110) with its HTTP status (R-111). Internal errors never expose SQL
 * or stack traces: their detail goes to the server log with an error id (R-112).
 *
 * @author craulgomez
 * @version 1.0
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /** Message shown for every internal error (R-112). */
    static final String INTERNAL_MESSAGE = "Ocurrió un error interno. Contacte al administrador.";
    /** Error code of unexpected internal errors. */
    static final String ERR_INTERNAL = "ERR_INTERNAL";

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final String GENERAL_FIELD = "general";

    private final Clock clock;

    /**
     * Creates the handler.
     *
     * @param clock clock that dates the errors
     */
    public ApiExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    /**
     * Invalid format, null or range (400). Reports the JSON field when the
     * controller knows it.
     *
     * @param exception validation error
     * @param handler   controller method that was called, if any
     * @param request   current request
     * @return error response
     */
    @ExceptionHandler(InvalidDataException.class)
    public ResponseEntity<ApiError> invalidData(InvalidDataException exception, HandlerMethod handler,
                                                HttpServletRequest request) {
        String jsonField = jsonFieldOf(exception.getFieldName(), handler);
        if (jsonField == null) {
            return error(HttpStatus.BAD_REQUEST, exception.getErrorCode(), exception.getMessage(), request);
        }
        return error(HttpStatus.BAD_REQUEST, exception.getErrorCode(), exception.getMessage(), request,
                List.of(new FieldErrorDto(jsonField, exception.getProblemDescription())));
    }

    /**
     * Body that is not valid JSON, or a parameter of the wrong type or missing (400).
     *
     * @param exception parsing error
     * @param request   current request
     * @return error response
     */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, MissingServletRequestPartException.class})
    public ResponseEntity<ApiError> unreadableRequest(Exception exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, InvalidDataException.ERROR_CODE,
                "La petición tiene datos con un formato inválido o incompletos.", request);
    }

    /**
     * Uploaded file larger than the limit (400, RN-090).
     *
     * @param exception size error
     * @param request   current request
     * @return error response
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> fileTooLarge(MaxUploadSizeExceededException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, InvalidDataException.ERROR_CODE,
                "El archivo supera el tamaño máximo de 5 MB.", request);
    }

    /**
     * Wrong login or invalid session (401, CA-202).
     *
     * @param exception authentication error
     * @param request   current request
     * @return error response
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiError> unauthorized(UnauthorizedException exception, HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, exception.getErrorCode(), exception.getMessage(), request);
    }

    /**
     * The role of the user does not allow the action (403).
     *
     * @param exception permission error
     * @param request   current request
     * @return error response
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> accessDenied(AccessDeniedException exception, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, exception.getErrorCode(), exception.getMessage(), request);
    }

    /**
     * The requested record or path does not exist (404).
     *
     * @param exception not-found error
     * @param request   current request
     * @return error response
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> notFound(ResourceNotFoundException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, exception.getErrorCode(), exception.getMessage(), request);
    }

    /**
     * Path that does not exist in the API (404).
     *
     * @param exception missing route
     * @param request   current request
     * @return error response
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> unknownPath(NoResourceFoundException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, ResourceNotFoundException.ERROR_CODE,
                "La ruta solicitada no existe.", request);
    }

    /**
     * HTTP method not supported by the path (405).
     *
     * @param exception method error
     * @param request   current request
     * @return error response
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> methodNotAllowed(HttpRequestMethodNotSupportedException exception,
                                                     HttpServletRequest request) {
        return error(HttpStatus.METHOD_NOT_ALLOWED, InvalidDataException.ERROR_CODE,
                "La ruta no admite el método " + exception.getMethod() + ".", request);
    }

    /**
     * Quality control out of the 2 % tolerance (422). Declared before the general
     * business-rule handler so it keeps its own error code.
     *
     * @param exception quality error
     * @param request   current request
     * @return error response
     */
    @ExceptionHandler(QualityControlException.class)
    public ResponseEntity<ApiError> qualityControl(QualityControlException exception, HttpServletRequest request) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, exception.getErrorCode(), exception.getMessage(), request);
    }

    /**
     * Business rule violated (422).
     *
     * @param exception rule error
     * @param request   current request
     * @return error response
     */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiError> businessRule(BusinessRuleException exception, HttpServletRequest request) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, exception.getErrorCode(), exception.getMessage(), request);
    }

    /**
     * Database failure (500). The detail goes only to the log.
     *
     * @param exception persistence error
     * @param request   current request
     * @return error response
     */
    @ExceptionHandler(PersistenceException.class)
    public ResponseEntity<ApiError> persistence(PersistenceException exception, HttpServletRequest request) {
        return internalError(exception.getErrorCode(), exception, request);
    }

    /**
     * Any other unexpected error (500). The detail goes only to the log.
     *
     * @param exception unexpected error
     * @param request   current request
     * @return error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception exception, HttpServletRequest request) {
        return internalError(ERR_INTERNAL, exception, request);
    }

    private ResponseEntity<ApiError> internalError(String errorCode, Exception exception, HttpServletRequest request) {
        String errorId = UUID.randomUUID().toString();
        LOG.error("Error interno {} en {} {}", errorId, request.getMethod(), request.getRequestURI(), exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, errorCode, INTERNAL_MESSAGE, request);
    }

    private static String jsonFieldOf(String validatorField, HandlerMethod handler) {
        if (validatorField == null || GENERAL_FIELD.equals(validatorField) || handler == null
                || !(handler.getBean() instanceof FieldNameMapping mapping)) {
            return null;
        }
        return mapping.fieldNames().get(validatorField);
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String errorCode, String message,
                                           HttpServletRequest request) {
        return error(status, errorCode, message, request, List.of());
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String errorCode, String message,
                                           HttpServletRequest request, List<FieldErrorDto> fieldErrors) {
        ApiError body = new ApiError(LocalDateTime.now(clock), status.value(), errorCode, message,
                request.getRequestURI(), fieldErrors);
        return ResponseEntity.status(status).body(body);
    }
}
