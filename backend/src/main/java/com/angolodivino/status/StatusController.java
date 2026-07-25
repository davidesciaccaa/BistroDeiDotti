package com.angolodivino.status;

import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class StatusController {

    @GetMapping("/status")
    public ApiStatusResponse status() {
        return new ApiStatusResponse("bistro-dei-dotti-api", "UP", Instant.now());
    }
}
