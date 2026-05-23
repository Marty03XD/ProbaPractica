package ro.autobrand.proba;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AutobrandProbaApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutobrandProbaApplication.class, args);
    }
}
