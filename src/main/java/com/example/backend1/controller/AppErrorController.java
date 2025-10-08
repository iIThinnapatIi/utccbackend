package com.example.backend1.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AppErrorController implements ErrorController {

    @RequestMapping("/error")
    public Map<String, Object> handleError(HttpServletRequest req) {
        Object status = req.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object path = req.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        Object msg = req.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        return Map.of(
                "app", "backend1",
                "error", true,
                "status", status,
                "path", path,
                "message", msg == null ? "Not Found" : msg.toString()
        );
    }
}
