package com.learn.patientservice;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MainController {

    @Value("${spring.application.name: no name}")
    private String appName;

    @Value("${server.port: no port}")
    private String port ;

    @GetMapping("/doctors")
    public String doctors() {
        return "list of doctors";
    }

    @GetMapping("/location")
    public String location() {
        return appName + " " + port;
    }
}
