package sailpoint.ets;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccessRequestDynamicApprovalController {
  private static final String template = "Hello, %s!";
  private final AtomicLong counter = new AtomicLong();

  @GetMapping("/dynamicapproval")
  public AccessRequestDynamicApproval dynamicapproval(@RequestParam(value = "name", defaultValue = "World") String name) {
    return new AccessRequestDynamicApproval(counter.incrementAndGet(), String.format(template, name));
  }
}
