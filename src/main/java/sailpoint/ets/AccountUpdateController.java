package sailpoint.ets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import retrofit2.Response;
import sailpoint.identitynow.api.IdentityNowService;
import sailpoint.identitynow.api.object.AccessEvent;
import sailpoint.identitynow.api.object.AccountActivity;
import sailpoint.identitynow.api.object.Identity;
import sailpoint.identitynow.api.object.QueryObject;
import sailpoint.identitynow.api.object.Schemas;
import sailpoint.identitynow.api.object.SearchQuery;
import sailpoint.identitynow.api.object.Snapshot;

@Component
@PropertySource("classpath:application.properties")
@ConfigurationProperties
@RestController
public class AccountUpdateController {

  static final Logger log = LogManager.getLogger(AccountUpdateController.class);

  private final AtomicLong counter = new AtomicLong();
  IdentityNowService idnService;
  Map<String, Object> attrs = new HashMap<>();

  @GetMapping("/accountupdated")
  public AccountUpdate accountupdate(@RequestBody String payload) {
    log.debug("Starting accountupdate");
    return new AccountUpdate(counter.incrementAndGet(), payload);
  }

  @PostMapping("/accountupdated")
  public AccountUpdate process(@RequestBody String payload, @RequestHeader("Host") String host) throws Exception {

    log.trace("Entering process");

    AccountUpdate update = new AccountUpdate(counter.incrementAndGet(), payload);

    log.debug("Host is : {}", host);
    String identityId = null;
    String sourceId = null;
    List<String> accountEntitlements = null;

    if (null != payload) {
      log.debug("payload: {}", payload);

      try {
        JsonObject jo = JsonParser.parseString(payload).getAsJsonObject();
        JsonObject start = jo.get("startInvocationInput").getAsJsonObject();
        JsonObject input = start.get("input").getAsJsonObject();
        identityId = input.get("identityId").getAsString();
        sourceId = input.get("sourceId").getAsString();
        log.debug("Identity  is {}", identityId);
      } catch (JsonParseException e) {
        e.printStackTrace();
      }
    } else {
      log.error("Payload should not be null!");
    }

    Properties appProps = getAppProps();
    log.debug(appProps.getProperty("tenant"));

    String tenant     = appProps.getProperty("tenant");
    String patid      = appProps.getProperty("patid");
    String patsecret  = appProps.getProperty("patsecret");
    String demotenant = appProps.getProperty("demotenant");
    String domain     = ".api.identitynow.com";
    if ("yes".equals(demotenant)) domain = ".api.identitynow-demo.com";
    String url = "https://" + tenant + domain;

    // Create IDN session
    try {
      createSession(url, patid, patsecret);
    } catch (MalformedURLException e1) {
      e1.printStackTrace();
    }

    // Fetch identity attributes
    log.debug("Fetching identity attributes");
    try {
      attrs = getAttributes(identityId);
    } catch (Exception e) {
      log.error("Error checking identity attributes: {}", e.getLocalizedMessage());
    }

    // Identify entitlements on account
    if (null != sourceId) {
      Schemas accountSchema = getAccountSchema(sourceId);

      List<String> entitlements = getEntitlementsFromSchema(accountSchema);
      log.debug("Got entitlements: {}", entitlements);

      accountEntitlements = update.getEntitlements(entitlements);

      int num = accountEntitlements.size();
      if (num != 0) {
        log.debug("Found {} entitlement(s)", accountEntitlements.size());
        log.debug(accountEntitlements);
        verifyEntitlements(identityId, sourceId, accountEntitlements);
      } else {
        log.debug("No entitlements detected.");
      }

    } else {
      log.error("SourceID can not be null fetching schemas");
    }

    log.trace("Leaving process");
    return new AccountUpdate(0, payload);
  }

  private void verifyEntitlements(String identityId, String sourceId, List<String> accountEntitlements)
      throws Exception {

    log.trace("Entering verifyEntitlements");

    Boolean ncd = false;
    List<String> badgroups = new ArrayList<>();

    // Check if entitlements are part of the last snapshot.
    // 1. Fetch last snapshot
    Snapshot snapshot = idnService.getHistoricalIdentitiesService().getLatestSnapshot(identityId).execute().body();
    log.debug("Snapshot: {}", snapshot);
    String snapshotDate = snapshot.getSnapshot();

    // 2. Fetch snapshot of most recent date, listing access items
    List<AccessEvent.AccessItem> accesses = idnService.getHistoricalIdentitiesService()
        .getSnapshot(identityId, snapshotDate, "entitlement").execute().body();
    log.debug("Found {} accesses.", accesses.size());

    // 3. Check if each entitlement is part of the snapshot...
    List<String> snapshotGroups = new ArrayList<>();
    for (AccessEvent.AccessItem access : accesses) {
      log.debug(access.getValue());

      String accessSourceId = access.getSourceId();
      if (sourceId.equals(accessSourceId)) {
        String group = access.getValue();
        snapshotGroups.add(group);
      }
    }

    for (String ent : accountEntitlements) {
      if (!snapshotGroups.contains(ent)) {
        log.debug("Cannot find entitilement in snapshot, checking access requests.");
        ncd = checkAccountActivities(identityId, ent);
        if (Boolean.TRUE.equals(ncd))
          badgroups.add(ent);
      }
    }

    // 4. Check for each group if there was a request or assignment
    if (Boolean.TRUE.equals(ncd))
      raiseAlert(identityId, sourceId, badgroups);

    log.trace("Leaving verifyEntitlements");
  }

  private Boolean checkAccountActivities(String identityId, String ent) throws Exception {
    log.trace("Entering checkAccountActivities");
    Boolean ncd = true;

    List<AccountActivity> accountActivities = idnService.getAccountActivitiesService().getAccountActivities(identityId)
        .execute().body();
    for (AccountActivity accountActivity : accountActivities) {
      Boolean requested = checkAccountActivity(accountActivity, ent);
      if (Boolean.TRUE.equals(requested)) {
        ncd = false;
        break;
      }
    }
    log.trace("Leaving checkAccountActivities: {}", ncd);
    return ncd;
  }

  private Boolean checkAccountActivity(AccountActivity activity, String ent) {
    log.trace("Entering checkAccountActivity: {}", ent);
    Boolean requested = false;

    List<AccountActivity.Item> requestItems = activity.getItems();
    for (AccountActivity.Item item : requestItems) {
      log.trace("Item: {}", item);
      if (ent.equals(item.getValue()) && "FINISHED".equals(item.getApprovalStatus())) {
        log.debug("Found request for Entitlement:");
        log.debug(item.getId());
        log.debug("Operation: {}", item.getOperation());
        log.debug("Attribute: {}", item.getAttribute());
        log.debug("Value: {}", item.getValue());
        log.debug("Account: {}", item.getNativeIdentity());
        requested = true;
      }
    }
    log.trace("Leaving checkAccountActivity: {}", requested);
    return requested;
  }

  private void raiseAlert(String identityId, String sourceId, List<String> badgroups) {

    log.trace("Entering raiseAlert");

    log.warn("WE HAVE A NATIVE CHANGE DETECTION");
    log.warn("ACTIONS TO BE TAKEN HAPPEN HERE");
    log.warn("Identity :      {}", identityId);
    log.warn("Source:         {}", sourceId);
    log.warn("entitlement(s): {}", badgroups);

    log.trace("Leaving raiseAlert");
  }

  private Properties getAppProps() {

    log.trace("Entering getAppProps");
    Properties appProps = new Properties();

    String fileName = "application.properties";
    ClassLoader classLoader = getClass().getClassLoader();
    InputStream inputStream = classLoader.getResourceAsStream(fileName);

    // the stream holding the file content
    if (inputStream == null) {
      throw new IllegalArgumentException("file not found! " + fileName);
    } else {
      try (InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
          BufferedReader reader = new BufferedReader(streamReader)) {
        appProps.load(reader);

      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    log.trace("Leaving getAppProps");
    return appProps;
  }

  private List<String> getEntitlementsFromSchema(Schemas accountSchema) {

    log.trace("Entering getEntitlementsFromSchema");

    List<String> entitlements = new ArrayList<>();
    if (null != accountSchema) {
      JsonArray attributes = accountSchema.getAttributes();
      for (JsonElement attr : attributes) {
        JsonObject attribute = attr.getAsJsonObject();
        if (attribute.get("isGroup").getAsBoolean()) {
          String entName = attribute.get("name").toString().replace("\"", "");
          log.debug("Found attribute: {}", entName);
          entitlements.add(entName);
        }
      }
    }

    log.trace("Leaving getEntitlementsFromSchema");
    return entitlements;
  }

  /**
   * Fetches account schema from source sourceId
   * 
   * @param sourceId
   * @return Schemas
   */
  private Schemas getAccountSchema(String sourceId) {

    log.trace("Entering getAccountSchema");
    Schemas accountSchema = null;

    List<Schemas> schemas = getSchemas(sourceId);

    for (Schemas schema : schemas) {
      if (schema.getNativeObjectType().equals("User"))
        accountSchema = schema;

    }
    log.trace("Leaving getAccountSchema");
    return accountSchema;
  }

  /**
   * Fetches schemas from source sourcId
   * 
   * @param sourceId
   * @return List of schemas
   */
  private List<Schemas> getSchemas(String sourceId) {

    log.trace("Entering getSchemas");

    List<Schemas> schemas = null;
    Response<List<Schemas>> schemasResponse;
    try {
      schemasResponse = idnService.getSchemaService().getSchemas(sourceId).execute();
      if (schemasResponse.isSuccessful()) {
        schemas = schemasResponse.body();
      } else {
        log.debug("Response was not successful for schemas search.");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    log.trace("Leaving getSchemas");
    return schemas;
  }

  /**
   * Creates the IDN session based on the URL
   * Needs to retrieve the PAT from the Secrets manager
   * 
   * @param url
   * @throws Exception
   * @throws ParseException
   */
  private void createSession(String url, String patid, String patsecret) throws Exception {

    log.trace("Entering createSession");
    log.debug("URL: {}", url);
    log.debug("patid: {}", patid);
    log.debug("patsecret: {}", patsecret);

    log.info("Getting idnService");
    idnService = new IdentityNowService(url, patid, patsecret, null, 60000L);
    log.info("Got idnService");

    log.info("Checking credentials...");
    try {
      idnService.createSession();
      log.info("Session created.");
    } catch (Exception e) {
      log.error(
          "Error Logging into IdentityNow.  Check your credentials and try again. [{}]", e.getLocalizedMessage());
      e.printStackTrace();
      System.exit(0);
    }
    log.trace("Leaving createSession");
  }

  /**
   * Gets identity attributes
   * 
   * @param idnService2
   * @param requesteeId
   * @return boolean : allowed or not
   * @throws IOException
   * @throws Exception
   */
  private Map<String, Object> getAttributes(String requesteeId)
      throws Exception {
    log.trace("Entering getAttributes: {}", requesteeId);

    Map<String, Object> attributes = new HashMap<>();
    String searchQueryString = "id:" + requesteeId;
    log.debug("search query string: {}", searchQueryString);
    SearchQuery searchQuery = new SearchQuery(searchQueryString);
    log.debug("using searchQuery: {}", searchQuery);
    QueryObject query = new QueryObject(searchQuery);

    log.debug("Executing search");
    Response<List<Identity>> response = idnService.getSearchService().getIdentities(false, 0, 10, query).execute();
    log.debug("Got response: {}", response);

    if (response.isSuccessful()) {
      log.debug("body: {}", response.body());
      log.debug("response: {}", response);
      List<Identity> ids = response.body();
      log.debug("Found {} identities", ids.size());

      int count = 0;
      for (Identity identity : ids) {
        log.debug("identity: {}", identity);
        count++;
        log.debug("{} : id           : {}", count, identity.getId());
        log.debug("{} : name         : {}", count, identity.getSource().getName());
        log.debug("{} : displayName  : {}", count, identity.getDisplayName());
        log.debug("{} : attributes   : {}", count, identity.getAttributes());
        log.debug("===============");
        attributes = identity.getAttributes();

      }
    } else {
      log.debug("Response was not successful for identity search.");
    }
    log.trace("Leaving getAttributes: {}", attributes);
    return attributes;
  }

}