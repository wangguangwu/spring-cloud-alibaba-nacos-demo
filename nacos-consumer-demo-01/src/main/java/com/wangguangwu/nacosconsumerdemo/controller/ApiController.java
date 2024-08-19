package com.wangguangwu.nacosconsumerdemo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;

/**
 * @author wangguangwu
 */
@RestController
@RequestMapping("api")
public class ApiController {

    private static final String SERVICE_NAME = "http://nacos-provider-serviceA";

    @Resource
    private RestTemplate restTemplate;

    @GetMapping("/callService")
    public String hello() {
        return restTemplate.getForObject(SERVICE_NAME + "/api/callService", String.class);
    }
}
