package sailpoint.ets;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
@SpringBootApplication
@EnableConfigurationProperties(GlobalProperties.class)
public class EtsApplication {

    static final Logger log = LogManager.getLogger(EtsApplication.class);

    public static void main(String[] args) {
        log.info("Starting main");
        log.warn("Warning!");
        log.error("ERROR");
        log.debug("Debugging");
        log.trace("Tracing");
        SpringApplication.run(EtsApplication.class, args);
    }

}