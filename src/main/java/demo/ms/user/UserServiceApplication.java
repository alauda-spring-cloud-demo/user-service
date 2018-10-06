package demo.ms.user;

import demo.ms.common.config.ResourceServerConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@EntityScan({"demo.ms.user.entity"})
@EnableEurekaClient
@Import(ResourceServerConfig.class)
@SpringBootApplication
@Configuration
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
