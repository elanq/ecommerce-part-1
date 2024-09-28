package com.fastcampus.ecommerce.config.middleware;

import com.fastcampus.ecommerce.common.errors.BadRequestException;
import com.fastcampus.ecommerce.common.errors.EmailAlreadyExistsException;
import com.fastcampus.ecommerce.common.errors.InvalidPasswordException;
import com.fastcampus.ecommerce.common.errors.ResourceNotFoundException;
import com.fastcampus.ecommerce.common.errors.RoleNotFoundException;
import com.fastcampus.ecommerce.common.errors.UserNotFoundException;
import com.fastcampus.ecommerce.common.errors.UsernameAlreadyExistsException;
import com.fastcampus.ecommerce.model.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
@Slf4j
public class GenericExceptionHandler {

  @ExceptionHandler({
      ResourceNotFoundException.class,
      UserNotFoundException.class,
      RoleNotFoundException.class
  })
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public @ResponseBody ErrorResponse handleResourceNotFoundException(HttpServletRequest req, ResourceNotFoundException exception) {
    return ErrorResponse.builder()
        .code(HttpStatus.NOT_FOUND.value())
        .message(exception.getMessage())
        .timestamp(LocalDateTime.now())
        .build();
  }


  @ExceptionHandler(BadRequestException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public @ResponseBody ErrorResponse handleBadRequestException(HttpServletRequest req, BadRequestException exception) {
    return ErrorResponse.builder()
        .code(HttpStatus.BAD_REQUEST.value())
        .message(exception.getMessage())
        .timestamp(LocalDateTime.now())
        .build();
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public @ResponseBody ErrorResponse handleGenericException(HttpServletRequest req, Exception exception) {
    log.error("Terjadi error. status code: " + HttpStatus.INTERNAL_SERVER_ERROR + "error message: " + exception.getMessage());
    return ErrorResponse.builder()
        .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
        .message(exception.getMessage())
        .timestamp(LocalDateTime.now())
        .build();
  }


  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public @ResponseBody ErrorResponse handleValidationException(MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult().getAllErrors().forEach(objectError -> {
      String fieldName = ((FieldError) objectError).getField();
      String errorMessage = objectError.getDefaultMessage();
      errors.put(fieldName, errorMessage);
    });
    return ErrorResponse.builder()
        .code(HttpStatus.BAD_REQUEST.value())
        .message(errors.toString())
        .timestamp(LocalDateTime.now())
        .build();
  }

  @ExceptionHandler(InvalidPasswordException.class)
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public @ResponseBody ErrorResponse handleUnauthorizedException(HttpServletRequest req, Exception exception) {
    return ErrorResponse.builder()
        .code(HttpStatus.UNAUTHORIZED.value())
        .message(exception.getMessage())
        .timestamp(LocalDateTime.now())
        .build();
  }

  @ExceptionHandler({
      UsernameAlreadyExistsException.class,
      EmailAlreadyExistsException.class
  })
  @ResponseStatus(HttpStatus.CONFLICT)
  public @ResponseBody ErrorResponse handleConflictException(HttpServletRequest req, Exception exception) {
    return ErrorResponse.builder()
        .code(HttpStatus.CONFLICT.value())
        .message(exception.getMessage())
        .timestamp(LocalDateTime.now())
        .build();
  }



}
