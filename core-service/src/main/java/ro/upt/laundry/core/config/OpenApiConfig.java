package ro.upt.laundry.core.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI coreServiceApi() {
        return new OpenAPI().info(new Info()
                .title("Laundry Booking - core-service")
                .version("1.0.0")
                .description("REST API: bookings, machines, faults, feedback, proposals"));
    }
}