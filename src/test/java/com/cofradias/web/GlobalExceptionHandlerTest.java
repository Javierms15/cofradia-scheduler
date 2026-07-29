package com.cofradias.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.validation.MapBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void traduceUnResponseStatusExceptionAUnProblemDetailConElMismoEstadoYMensaje() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes hacer eso");

        ProblemDetail problemDetail = handler.handleResponseStatusException(ex);

        assertThat(problemDetail.getStatus()).isEqualTo(403);
        assertThat(problemDetail.getDetail()).isEqualTo("No puedes hacer eso");
    }

    @Test
    void traduceUnErrorDeValidacionAUnProblemDetail400ConLosCamposFallidos() throws NoSuchMethodException {
        Method metodoFicticio = GlobalExceptionHandlerTest.class.getDeclaredMethod("traduceUnErrorDeValidacionAUnProblemDetail400ConLosCamposFallidos");
        MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "createRequest");
        bindingResult.addError(new FieldError("createRequest", "puntos", "Un recorrido necesita al menos 2 puntos"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                new org.springframework.core.MethodParameter(metodoFicticio, -1), bindingResult);

        ProblemDetail problemDetail = handler.handleValidation(ex);

        assertThat(problemDetail.getStatus()).isEqualTo(400);
        assertThat(problemDetail.getDetail()).isEqualTo("Validacion fallida");
        @SuppressWarnings("unchecked")
        Map<String, String> errores = (Map<String, String>) problemDetail.getProperties().get("errores");
        assertThat(errores).containsEntry("puntos", "Un recorrido necesita al menos 2 puntos");
    }

    @Test
    void unErrorDeValidacionSinMensajeUsaUnValorPorDefecto() throws NoSuchMethodException {
        Method metodoFicticio = GlobalExceptionHandlerTest.class.getDeclaredMethod("unErrorDeValidacionSinMensajeUsaUnValorPorDefecto");
        MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "createRequest");
        bindingResult.addError(new FieldError("createRequest", "email", null));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                new org.springframework.core.MethodParameter(metodoFicticio, -1), bindingResult);

        ProblemDetail problemDetail = handler.handleValidation(ex);

        @SuppressWarnings("unchecked")
        Map<String, String> errores = (Map<String, String>) problemDetail.getProperties().get("errores");
        assertThat(errores).containsEntry("email", "valor invalido");
    }

    @Test
    void siHayDosErroresParaElMismoCampoSeQuedaConElPrimero() throws NoSuchMethodException {
        Method metodoFicticio = GlobalExceptionHandlerTest.class.getDeclaredMethod("siHayDosErroresParaElMismoCampoSeQuedaConElPrimero");
        MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "createRequest");
        bindingResult.addError(new FieldError("createRequest", "email", "no es un email valido"));
        bindingResult.addError(new FieldError("createRequest", "email", "no puede estar vacio"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                new org.springframework.core.MethodParameter(metodoFicticio, -1), bindingResult);

        ProblemDetail problemDetail = handler.handleValidation(ex);

        @SuppressWarnings("unchecked")
        Map<String, String> errores = (Map<String, String>) problemDetail.getProperties().get("errores");
        assertThat(errores).containsEntry("email", "no es un email valido");
    }
}
