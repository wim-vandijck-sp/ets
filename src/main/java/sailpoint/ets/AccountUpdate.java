package sailpoint.ets;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class AccountUpdate {
  private final long id;
  private final String content;

  static final Logger log = LogManager.getLogger(AccountUpdate.class);

  public AccountUpdate(long id, String content) {
    this.id = id;
    this.content = content;
  }

  public long getId() {
    return id;
  }

  public String getContent() {
    return content;
  }

  public List<String> getEntitlements(List<String> entitlements) {
    
    log.trace("Entering getEntitlements: {}", entitlements);
    List<String> identityEntitlements = new ArrayList<>();
    JsonObject  body = JsonParser.parseString(content).getAsJsonObject();
    JsonObject attributes = body.get("startInvocationInput")
      .getAsJsonObject().get("input")
      .getAsJsonObject().get("attributes").getAsJsonObject();
    
    for(Map.Entry<String, JsonElement> entry : attributes.entrySet()) {
      String key = entry.getKey();
      if (entitlements.contains(key)) {
        log.debug("Found entitlement!");
        Object value = entry.getValue();
        log.debug("value is {}", value.getClass().getName());
        if (value instanceof JsonArray) {
          for (JsonElement group : (JsonArray)value) {
            identityEntitlements.add(group.getAsString());
          }
        } else if (value instanceof JsonObject) {
          identityEntitlements.add(value.toString());
        }
      }
    }

    log.trace("Leaving getEntitlements");
    return identityEntitlements;
  }

}
