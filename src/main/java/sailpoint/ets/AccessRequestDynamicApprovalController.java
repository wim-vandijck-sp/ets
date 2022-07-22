package sailpoint.ets;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonParseException;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
@RestController
public class AccessRequestDynamicApprovalController {

  static final Logger log = LogManager.getLogger(AccessRequestDynamicApprovalController.class);

  private static final String TEMPLATE = "Hello, %s!";
  private final AtomicLong counter = new AtomicLong();

  @GetMapping("/dynamicapproval")
  public AccessRequestDynamicApproval dynamicapproval(@RequestParam(value = "name", defaultValue = "World") String name) {
    log.info("Starting dynamicapproval");
    log.warn("Warning!");
    log.error("ERROR");
    log.debug("Debugging");
    log.trace("Tracing");
    return new AccessRequestDynamicApproval(counter.incrementAndGet(), String.format(TEMPLATE, name));
  }

  @PostMapping("/dynamicapproval")
  public AccessRequestDynamicApproval process(@RequestBody String payload) {

    log.debug("Entering process");
    
    if (null != payload) {
      log.debug("Payload: {}", payload);
      try {
        JsonObject jo = JsonParser.parseString(payload).getAsJsonObject();
        log.debug("Json object is {}", jo);
      } catch (JsonParseException e) {
        e.printStackTrace();
      }
    } else {
      log.error("Payload should not be null!");
    }
    
    log.debug("Leaving process");
    return new AccessRequestDynamicApproval(0, payload);
  }
}
