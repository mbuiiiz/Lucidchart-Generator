package com.group16.aetherxmlbridge.controller;

import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.ModelAndViewAssert;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppErrorControllerTest {

    private AppErrorController controller;

    @BeforeEach
    void setUp() {
        controller = new AppErrorController();
    }

    @Test
    void handleError_withStatusCodeAndMessage_addsThemToModel() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 404);
        request.setAttribute(RequestDispatcher.ERROR_MESSAGE, "Page not found");

        Model model = new ExtendedModelMap();

        String viewName = controller.handleError(request, model);

        assertEquals("error", viewName);
        assertEquals("404", model.getAttribute("statusCode"));
        assertEquals("Page not found", model.getAttribute("errorMessage"));
    }

    @Test
    void handleError_withoutStatusCode_usesUnknown() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_MESSAGE, "Something failed");

        Model model = new ExtendedModelMap();

        String viewName = controller.handleError(request, model);

        assertEquals("error", viewName);
        assertEquals("Unknown", model.getAttribute("statusCode"));
        assertEquals("Something failed", model.getAttribute("errorMessage"));
    }

    @Test
    void handleError_withoutErrorMessage_usesDefaultMessage() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 500);

        Model model = new ExtendedModelMap();

        String viewName = controller.handleError(request, model);

        assertEquals("error", viewName);
        assertEquals("500", model.getAttribute("statusCode"));
        assertEquals("An unexpected error occurred.", model.getAttribute("errorMessage"));
    }

    @Test
    void handleError_withoutStatusCodeOrMessage_usesDefaults() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        Model model = new ExtendedModelMap();

        String viewName = controller.handleError(request, model);

        assertEquals("error", viewName);
        assertEquals("Unknown", model.getAttribute("statusCode"));
        assertEquals("An unexpected error occurred.", model.getAttribute("errorMessage"));
    }
}