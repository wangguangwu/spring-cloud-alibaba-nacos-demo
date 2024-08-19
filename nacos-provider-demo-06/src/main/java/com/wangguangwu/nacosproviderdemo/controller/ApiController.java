package com.wangguangwu.nacosproviderdemo.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * @author wangguangwu
 */
@RestController
@RequestMapping("api")
public class ApiController {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${server.port}")
    private String serverPort;

    @GetMapping("/callService")
    public String callService() {
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            // ignore
        }
        return "Hello, I'm " + applicationName + ". I'm from port:" + serverPort;
    }
}
