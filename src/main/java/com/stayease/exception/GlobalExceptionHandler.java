package com.stayease.exception;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.NoHandlerFoundException;

@ControllerAdvice
public class GlobalExceptionHandler
{
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(NoHandlerFoundException exception, Model model)
    {
        model.addAttribute("error", "The page you are looking for does not exist.");

        return "error/404";
    }

    @ExceptionHandler(RuntimeException.class)
    public String handleRuntimeException(RuntimeException exception, Model model)
    {
        model.addAttribute("error", exception.getMessage() != null ? exception.getMessage() : "Something went wrong.");

        return "error/error";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleUnexpectedException(Exception exception, Model model)
    {
        model.addAttribute("error", "An unexpected error occurred. Please try again later.");

        return "error/500";
    }
}