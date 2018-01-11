import com.google.gson.*;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class Utils {

    private static final String PERFECTO_HOST = System.getenv("host");
    private static final String PERFECTO_TOKEN = System.getenv("token");
    private static final int PDF_DOWNLOAD_ATTEMPTS = 5;

    private static HttpClient httpClient = HttpClientBuilder.create().build();
    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static String REPORTIUM_SERVER = "https://" + PERFECTO_HOST + ".reporting.perfectomobile.com";

    public static RemoteWebDriver getRemoteWebDriver(String platformName, String platformVersion, String browserName,
                                                     String browserVersion, String screenResolution, String location) throws MalformedURLException {

        // Set cloud host and credentials values from CI, else use local values
        //String PERFECTO_HOST = System.getenv("host");
        //String PERFECTO_TOKEN = System.getenv("token");

        //Old School Credentials (Not viable for Fast web):
        //String PERFECTO_USER = System.getProperty("np.testUsername", "MY_USER");
        //String PERFECTO_PASSWORD = System.getProperty("np.testPassword", "MY_PASS");

        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability("securityToken", PERFECTO_TOKEN);

        //Old School Credentials Login:
        //capabilities.setCapability("user", PERFECTO_USER);
        //capabilities.setCapability("password", PERFECTO_PASSWORD);

        capabilities.setCapability("platformName", platformName);
        capabilities.setCapability("platformVersion", platformVersion);
        capabilities.setCapability("browserName", browserName);
        capabilities.setCapability("browserVersion", browserVersion);
        capabilities.setCapability("location", location);

        // Define test name
        capabilities.setCapability("scriptName", "ParallelWeb");

        if (!screenResolution.isEmpty()) {
            capabilities.setCapability("resolution", screenResolution);
            System.out.println("Creating Remote WebDriver on: " + platformName + " " + platformVersion + ", " + browserName + " " + browserVersion + ", " + screenResolution);
        }

        else {
            if (!platformName.isEmpty())
                System.out.println("Creating Remote WebDriver on: " + platformName + " " + platformVersion);
            else
                System.out.println("Creating Remote WebDriver on: " + browserName);
        }

        RemoteWebDriver webdriver = new RemoteWebDriver(
                new URL("https://" + PERFECTO_HOST + ".perfectomobile.com/nexperience/perfectomobile/wd/hub/fast"), capabilities);

        // Define RemoteWebDriver timeouts
        webdriver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
        //webdriver.manage().timeouts().pageLoadTimeout(15, TimeUnit.SECONDS);
        webdriver.manage().window().maximize();

        // Maximize browser window on Desktop
        if (!screenResolution.isEmpty()) {
            webdriver.manage().window().maximize();
        }

        return webdriver;
    }

    public static void downloadReport(String type, String id) throws Exception {

        Path downloadPath = Paths.get(Files.createTempDirectory("reporting_pdf_sample_").toString(), id + ".pdf"); //Check about directory creation - maybe need to create it once for the whole project, or check if it exists before creating

        switch(type){
            case "summary":
                downloadExecutionSummaryReport(downloadPath, id);
                System.out.println(downloadPath.toString());
                break;

            case "test":
                String testId = retrieveTestExecutions(id);
                CreatePdfTask task = startTestReportGeneration(testId);
                downloadTestReport(downloadPath, task, testId);
                break;

            default:
                break;
        }
    }

    private static void downloadExecutionSummaryReport(Path summaryPdfPath, String driverExecutionId) throws URISyntaxException, IOException {
        System.out.println("Downloading PDF for driver execution ID: " + driverExecutionId);
        URIBuilder uriBuilder = new URIBuilder(REPORTIUM_SERVER + "/export/api/v1/test-executions/pdf");
        uriBuilder.addParameter("externalId[0]", driverExecutionId);
        downloadPdfFileToFS(summaryPdfPath, uriBuilder.build());
    }

    private static CreatePdfTask startTestReportGeneration(String testId) throws URISyntaxException, IOException {
        System.out.println("Starting PDF generation for test ID: " + testId);
        URIBuilder taskUriBuilder = new URIBuilder(REPORTIUM_SERVER + "/export/api/v2/test-executions/pdf/task");
        taskUriBuilder.addParameter("testExecutionId", testId);
        HttpPost httpPost = new HttpPost(taskUriBuilder.build());
        addDefaultRequestHeaders(httpPost);

        CreatePdfTask task = null;
        for (int attempt = 1; attempt <= PDF_DOWNLOAD_ATTEMPTS; attempt++) {

            HttpResponse response = httpClient.execute(httpPost);
            try {
                int statusCode = response.getStatusLine().getStatusCode();
                if (HttpStatus.SC_OK == statusCode) {
                    task = gson.fromJson(EntityUtils.toString(response.getEntity()), CreatePdfTask.class);
                    break;
                } else if (HttpStatus.SC_NO_CONTENT == statusCode) {

                    // if the execution is being processed, the server will respond with empty response and status code 204
                    System.out.println("\nThe server responded with 204 (no content). " +
                            "The execution is still being processed. Attempting again in 5 sec (" + attempt + "/" + PDF_DOWNLOAD_ATTEMPTS + ")");
                    Thread.sleep(5000);
                } else {
                    String errorMsg = IOUtils.toString(response.getEntity().getContent(), Charset.defaultCharset());
                    System.err.println("Error downloading file. Status: " + response.getStatusLine() + ".\nInfo: " + errorMsg);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                EntityUtils.consumeQuietly(response.getEntity());
            }
        }
        return task;
    }

    private static void downloadTestReport(Path testPdfPath, CreatePdfTask task, String testId) throws URISyntaxException, IOException {
        System.out.println("Downloading PDF for test ID: " + testId);
        long startTime = System.currentTimeMillis();
        int maxWaitMin = 10;
        long maxGenerationTime = TimeUnit.MINUTES.toMillis(maxWaitMin);
        String taskId = task.getTaskId();

        CreatePdfTask updatedTask;
        do {
            updatedTask = getUpdatedTask(taskId);
            try {
                if (updatedTask.getStatus() != TaskStatus.COMPLETE) {
                    Thread.sleep(3000);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        while (updatedTask.getStatus() != TaskStatus.COMPLETE && startTime + maxGenerationTime > System.currentTimeMillis());

        if (updatedTask.getStatus() == TaskStatus.COMPLETE) {
            downloadPdfFileToFS(testPdfPath, new URI(updatedTask.getUrl()));
        } else {
            throw new RuntimeException("The task is still in " + updatedTask.getStatus() + " status after waiting " + maxWaitMin + "minutes");
        }
    }

    private static CreatePdfTask getUpdatedTask(String taskId) throws URISyntaxException, IOException {
        CreatePdfTask task;
        URIBuilder taskUriBuilder = new URIBuilder(REPORTIUM_SERVER + "/export/api/v2/test-executions/pdf/task/" + taskId);
        HttpGet httpGet = new HttpGet(taskUriBuilder.build());
        addDefaultRequestHeaders(httpGet);
        HttpResponse response = httpClient.execute(httpGet);
        int statusCode = response.getStatusLine().getStatusCode();
        if (HttpStatus.SC_OK == statusCode) {
            task = gson.fromJson(EntityUtils.toString(response.getEntity()), CreatePdfTask.class);
        } else {
            throw new RuntimeException("Error while getting AsyncTask: " + response.getStatusLine().toString());
        }
        return task;
    }

    private static void downloadPdfFileToFS(Path pdfPath, URI uri) throws IOException {
        boolean downloadComplete = false;
        HttpGet httpGet = new HttpGet(uri);
        addDefaultRequestHeaders(httpGet);
        for (int attempt = 1; attempt <= PDF_DOWNLOAD_ATTEMPTS && !downloadComplete; attempt++) {

            HttpResponse response = httpClient.execute(httpGet);
            FileOutputStream fileOutputStream = null;

            try {
                int statusCode = response.getStatusLine().getStatusCode();
                if (HttpStatus.SC_OK == statusCode) {
                    fileOutputStream = new FileOutputStream(pdfPath.toFile());
                    IOUtils.copy(response.getEntity().getContent(), fileOutputStream);
                    System.out.println("\nSaved downloaded file to: " + pdfPath.toString());
                    downloadComplete = true;
                } else if (HttpStatus.SC_NO_CONTENT == statusCode) {

                    // if the execution is being processed, the server will respond with empty response and status code 204
                    System.out.println("\nThe server responded with 204 (no content). " +
                            "The execution is still being processed. Attempting again in 5 sec (" + attempt + "/" + PDF_DOWNLOAD_ATTEMPTS + ")");
                    Thread.sleep(5000);
                } else {
                    String errorMsg = IOUtils.toString(response.getEntity().getContent(), Charset.defaultCharset());
                    System.err.println("Error downloading file. Status: " + response.getStatusLine() + ".\nInfo: " + errorMsg);
                    downloadComplete = true;
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                EntityUtils.consumeQuietly(response.getEntity());
                IOUtils.closeQuietly(fileOutputStream);
            }
        }
        if (!downloadComplete) {
            System.err.println("The execution is still being processed. No more download attempts");
        }
    }

    private static void addDefaultRequestHeaders(HttpRequestBase request) {
        request.addHeader("PERFECTO_AUTHORIZATION", PERFECTO_TOKEN);
    }

    private enum TaskStatus {
        IN_PROGRESS, COMPLETE
    }

    private static class CreatePdfTask {
        private String taskId;
        private TaskStatus status;
        private String url;

        public CreatePdfTask() { }

        public String getTaskId() {
            return taskId;
        }

        public void setTaskId(String taskId) {
            this.taskId = taskId;
        }

        public TaskStatus getStatus() {
            return status;
        }

        public void setStatus(TaskStatus status) {
            this.status = status;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    private static String retrieveTestExecutions(String executionId) throws URISyntaxException, IOException {
        URIBuilder uriBuilder = new URIBuilder(REPORTIUM_SERVER + "/export/api/v1/test-executions");
        uriBuilder.addParameter("externalId[0]", executionId);

        HttpGet getExecutions = new HttpGet(uriBuilder.build());
        addDefaultRequestHeaders(getExecutions);
        HttpClient httpClient = HttpClientBuilder.create().build();

        HttpResponse getExecutionsResponse = httpClient.execute(getExecutions);
        JsonObject executions;

        try (InputStreamReader inputStreamReader = new InputStreamReader(getExecutionsResponse.getEntity().getContent())) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String response = IOUtils.toString(inputStreamReader);
            try {
                executions = gson.fromJson(response, JsonObject.class);
                JsonElement resources = executions.getAsJsonArray("resources").get(0);
                executions = gson.fromJson(resources, JsonObject.class);
            } catch (JsonSyntaxException e) {
                throw new RuntimeException("Unable to parse response: " + response);
            }
        }
        String id = executions.get("id").toString();
        return id.substring(1, id.length() - 1);
    }
}
