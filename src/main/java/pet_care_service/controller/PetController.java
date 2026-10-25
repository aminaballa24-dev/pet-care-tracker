package pet_care_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;

@RestController
public class PetController {

    @GetMapping("/")
    public String home() throws Exception {
        String version = System.getenv().getOrDefault("APP_VERSION", "v1");
        String hostname = InetAddress.getLocalHost().getHostName();

        return "{\"app\":\"pet-care-service\",\"version\":\""
                + version
                + "\",\"hostname\":\""
                + hostname
                + "\"}";
    }

    @GetMapping("/health")
    public String health() {
        return "ok";
    }
}