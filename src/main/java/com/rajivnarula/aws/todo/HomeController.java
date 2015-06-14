package com.rajivnarula.aws.todo;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@RestController
public class HomeController {


    @RequestMapping(value = "/",method = RequestMethod.GET)
    public String index() {
        return "TODO List";
    }

}