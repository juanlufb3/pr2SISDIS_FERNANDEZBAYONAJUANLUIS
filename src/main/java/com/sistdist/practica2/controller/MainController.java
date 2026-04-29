package com.sistdist.practica2.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller //controlador web, devuelve vistas HTML
public class MainController {

    @GetMapping("/")
    public String home() {

        return "index";
        //devuelve la vista index.html
    }

    @GetMapping("/login")
    public String login(@RequestParam(value = "error",  required = false) String error,
                        @RequestParam(value = "logout", required = false) String logout,
                        Model model) {
        // @RequestParam captura parametros de la URL: /login?error=true
        // required = false significa que no peta si no viene el parametro
        if (error  != null) model.addAttribute("errorMsg",  "Usuario o contraseña incorrectos.");
        if (logout != null) model.addAttribute("logoutMsg", "Has cerrado sesión correctamente.");
        // addAttribute mete datos en el modelo que Thymeleaf puede leer
        // en el HTML con th:if="${errorMsg}"
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }
}