package com.seanlei.demo.websocket.webapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Created by Sean Lei on 6/20/16.
 */
@Controller
public class DemoController {
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String index() {
        return "home";
    }
}
