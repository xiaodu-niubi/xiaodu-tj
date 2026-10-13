package com.xiaodu.auth.controller;

import cn.hutool.core.codec.Base64;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.KeyPair;
import java.security.PublicKey;

@Hidden
@RestController
@RequestMapping("jwks")
public class JwkController {

    private final KeyPair keyPair;

    @Autowired
    public JwkController(KeyPair keyPair) {
        this.keyPair = keyPair;
    }

    @GetMapping
    public String getJwk(){
        PublicKey publicKey = keyPair.getPublic();
        if (publicKey == null) {
            throw new IllegalStateException("JWK公钥未正确加载，请检查密钥库配置(alias/password/jks文件)");
        }
        return Base64.encode(publicKey.getEncoded());
    }
}
