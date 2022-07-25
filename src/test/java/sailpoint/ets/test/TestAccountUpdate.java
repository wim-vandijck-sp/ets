package sailpoint.ets.test;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.TestPropertySource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import sailpoint.ets.AccountUpdate;
import sailpoint.ets.GlobalProperties;
import sailpoint.identitynow.api.IdentityNowService;

@Configuration
@EnableConfigurationProperties(value = GlobalProperties.class)
@TestPropertySource("application.properties")
public class TestAccountUpdate {

    final static Logger log = LogManager.getLogger(TestAccountUpdate.class);

    private Long timeout = (long) 10000L;
    private IdentityNowService idnService;
    private JsonObject body = null;
    Gson gson = new GsonBuilder().serializeNulls().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();

    AccountUpdate update = null;
    @BeforeAll
    public void setUp() throws Exception{

        Properties appProps = new Properties();

        // String rootPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
        // String appConfigPath = rootPath + "application.properties";
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
        log.debug(appProps.getProperty("tenant"));
        //appProps.load(new FileInputStream(appConfigPath));

        String tenant    = appProps.getProperty("tenant");
        String patid     = appProps.getProperty("patid");
        String patsecret = appProps.getProperty("patsecret");
        String url       = "https://" + tenant + ".api.identitynow.com";

        log.debug("tenant: " + tenant);
        idnService = new IdentityNowService(url, patid, patsecret, null, timeout);
        idnService.createSession();

        File resource = new ClassPathResource("body.json").getFile();
        String text = new String(Files.readAllBytes(resource.toPath()));
        body = JsonParser.parseString(text).getAsJsonObject();

    }

    @BeforeEach
    void init() {
        update = new AccountUpdate(0, body.toString());
    }

}
