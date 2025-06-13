package searchengine.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Контроллер для отображения главной страницы
 */
@Controller
public class DefaultController {
    @RequestMapping("/")
    public String index() {
        return "index";
    }
}