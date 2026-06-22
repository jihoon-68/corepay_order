package org.example.corepayorderservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaAuditing
@EnableFeignClients
@SpringBootApplication(scanBasePackages = {
        "org.example.corepayorderservice",
        "org.example.corepaycommon"
})
@EnableJpaRepositories(basePackages = {
        "org.example.corepayorderservice",
        "org.example.corepaycommon"
})
@EntityScan(basePackages = {
        "org.example.corepayorderservice",
        "org.example.corepaycommon"
})
public class CorepayOrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CorepayOrderServiceApplication.class, args);
    }

}
