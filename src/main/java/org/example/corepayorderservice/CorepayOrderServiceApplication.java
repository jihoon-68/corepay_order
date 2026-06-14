package org.example.corepayorderservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@EnableFeignClients
@SpringBootApplication(scanBasePackages = {
        "org.example.corepayorderservice",
        "org.example.corepaycommon"
})
public class CorepayOrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CorepayOrderServiceApplication.class, args);
    }

}
