package sailpoint.ets;

import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class AccountUpdate {
  private final long id;
  private final String content;
  private List<Change> changes = null;
  private AccountUpdate.Identity identity = null;
  private AccountUpdate.Source source = null;
  private AccountUpdate.Account account = null;

  private static final String OLDVALUE = "oldValue";
  private static final String NEWVALUE = "newValue";

  static final Logger log = LogManager.getLogger(AccountUpdate.class);

  public AccountUpdate(long id, String content) {
    this.id = id;
    this.content = content;
    JsonObject body = JsonParser.parseString(content).getAsJsonObject();
    JsonObject identityEl = body.get("identity").getAsJsonObject();
    JsonObject sourceEl = body.get("source").getAsJsonObject();
    JsonObject accountEl = body.get("account").getAsJsonObject();
    JsonArray changesEl = body.get("changes").getAsJsonArray();

    identity = new AccountUpdate.Identity(identityEl);
    source   = new AccountUpdate.Source(sourceEl);
    account  = new AccountUpdate.Account(accountEl);
    changes  = new ArrayList<>();
    for (JsonElement changeEl : changesEl) {
      JsonObject changeOb = changeEl.getAsJsonObject();
      Change change = new Change(changeOb);
      changes.add(change);
    }
  }

  public long getId() {
    return id;
  }

  public String getContent() {
    return content;
  }

  public AccountUpdate.Identity getIdentity() {
    return identity;
  }

  public AccountUpdate.Source getSource() {
    return source;
  }

  public AccountUpdate.Account getAccount() {
    return account;
  }

  public List<Change> getChanges() {
    return changes;
  }

  public class Identity {
    private String id;
    private String name;
    private String type;

    private Identity(JsonObject identityEl) {
      id = identityEl.get("id").getAsString();
      name = identityEl.get("name").getAsString();
      type = identityEl.get("type").getAsString(); 
    }

    public String getId() {
      return id;
    }
    public String getName() {
      return name;
    }
    public String getType() {
      return type;
    }

  }

  public class Source {

    private String id;
    private String name;
    private String type;

    private Source(JsonObject sourceEl) {

      id = sourceEl.get("id").getAsString();
      name = sourceEl.get("name").getAsString();
      type = sourceEl.get("type").getAsString(); 
    }

    public String getId() {
      return id;
    }
    public String getName() {
      return name;
    }
    public String getType() {
      return type;
    }
  }

  public class Account {

    private String id;
    private String name;
    private String type;
    private String uuid;
    private String nativeIdentity;

    private Account(JsonObject accountEl) {

      id = accountEl.get("id").getAsString();
      name = accountEl.get("name").getAsString();
      type = accountEl.get("type").getAsString();
      uuid = accountEl.get("uuid").getAsString();
      nativeIdentity = accountEl.get("nativeIdentity").getAsString();
      
    }

    public String getId() {
      return id;
    }
    public String getName() {
      return name;
    }
    public String getType() {
      return type;
    }

    public String getUuid() {
      return uuid;
    }

    public String getNativeIdentity() {
      return nativeIdentity;
    }
  }

  public class Change {
    private String attribute;
    private Object oldValue;
    private Object newValue;
     
    private Change(JsonObject changeEl) {

      attribute = changeEl.get("attribute").getAsString();
      if (changeEl.get(OLDVALUE) instanceof JsonArray) {
        oldValue = new Gson().fromJson(changeEl.get(OLDVALUE), List.class);
      } else {
        oldValue = new Gson().fromJson(changeEl.get(OLDVALUE), String.class);
      }
      if (changeEl.get(NEWVALUE) instanceof JsonArray) {
        newValue = new Gson().fromJson(changeEl.get(NEWVALUE), List.class);
      } else {
        newValue = new Gson().fromJson(changeEl.get(NEWVALUE), String.class);
      }
    }

    public String getAttribute() {
      return attribute;
    }

    public Object getOldValue() {
      return oldValue;
    }

    public Object getNewValue() {
      return newValue;
    }
  }
}
