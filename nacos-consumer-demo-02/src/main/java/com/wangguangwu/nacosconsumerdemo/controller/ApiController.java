package com.wangguangwu.nacosconsumerdemo.controller;

import com.wangguangwu.nacosconsumerdemo.constant.HttpProtocolConstants;
import com.wangguangwu.nacosconsumerdemo.constant.ServiceConstants;
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

    @Resource
    private RestTemplate restTemplate;

    @GetMapping("/callServiceA")
    public String callServiceA() {
        return restTemplate.getForObject(HttpProtocolConstants.HTTP_PROTOCOL + ServiceConstants.SERVICE_A_NAME + "/api/callService", String.class);
    }

    @GetMapping("/callServiceB")
    public String callServiceB() {
        return restTemplate.getForObject(HttpProtocolConstants.HTTP_PROTOCOL + ServiceConstants.SERVICE_B_NAME + "/api/callService", String.class);
    }
}
