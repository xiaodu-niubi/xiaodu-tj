package com.xiaodu.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.bootstrap.encrypt.KeyProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.rsa.crypto.KeyStoreKeyFactory;

import java.security.KeyPair;

@Configuration
public class AuthConfig {

    @Bean
    @ConfigurationProperties(prefix = "encrypt")
    public KeyProperties keyProperties(){
        return new KeyProperties();
    }

    @Bean
    public KeyPair keyPair(KeyProperties keyProperties){
        KeyStoreKeyFactory keyStoreKeyFactory =
                new KeyStoreKeyFactory(
                        keyProperties.getKeyStore().getLocation(),
                        keyProperties.getKeyStore().getPassword().toCharArray());
        return keyStoreKeyFactory.getKeyPair(
                keyProperties.getKeyStore().getAlias(),
                keyProperties.getKeyStore().getSecret().toCharArray());
    }

    // @Bean
    // public TomcatContextCustomizer cookieTomcatContextCustomizer(){
    //     return context -> context.setCookieProcessor(new LegacyCookieProcessor());
    // }
}
