package org.pado.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

/**
 * @Author xuda
 * @Date 2023/12/12 15:30
 */
@Configuration
public class ProxyConfig {
    @PostConstruct
    public void init(){
        System.setProperty("http.proxyHost", "127.0.0.1");
        System.setProperty("http.proxyPort", "7890");
        System.setProperty("https.proxyHost", "127.0.0.1");
        System.setProperty("https.proxyPort", "7890");
    }
}
