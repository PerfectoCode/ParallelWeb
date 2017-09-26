
import com.perfecto.reportium.client.ReportiumClient;
import com.perfecto.reportium.client.ReportiumClientFactory;
import com.perfecto.reportium.model.Job;
import com.perfecto.reportium.model.PerfectoExecutionContext;
import com.perfecto.reportium.model.Project;
import com.perfecto.reportium.test.TestContext;
import com.perfecto.reportium.test.result.TestResultFactory;

import org.openqa.selenium.By;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testng.annotations.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ParallelWeb {
    private RemoteWebDriver driver;
    private ReportiumClient reportiumClient;

    // Create Remote WebDriver based on testng.xml configuration
    @Parameters({ "platformName", "platformVersion", "browserName", "browserVersion", "screenResolution", "location" })
    @BeforeTest
    public void beforeTest(String platformName, String platformVersion, String browserName, String browserVersion, String screenResolution, @Optional String location) throws MalformedURLException {


        driver = Utils.getRemoteWebDriver(platformName, platformVersion, browserName, browserVersion, screenResolution, location);
        PerfectoExecutionContext perfectoExecutionContext = new PerfectoExecutionContext.PerfectoExecutionContextBuilder()
                .withProject(new Project("Sample Script", "1.0"))
                .withJob(new Job("Sample Job", 45))
                .withContextTags("Java")
                .withWebDriver(driver)
                .build();

        // Reporting client. For more details, see http://developers.perfectomobile.com/display/PD/Reporting
        reportiumClient = new ReportiumClientFactory().createPerfectoReportiumClient(perfectoExecutionContext);
    }

    // Test Method - Navigate to way2automation.com and fill the registration form
    @Test
    public void way2automation(){
        try{
            reportiumClient.testStart("Way2Automation Flow", new TestContext("Sample", "Native"));
            stepStart("Navigate to site");
            driver.get("http://way2automation.com/way2auto_jquery/index.php");
            delay();
            stepEnd();

            stepStart("Opening signin window");
            driver.findElementByXPath("(//*[@id=\"load_form\"]/div/div[1]/p/a)[2]").click(); //Can use clickText(visual analysis) instead once its implemented for fast web
            delay();
            stepEnd();

            stepStart("Signing in");
            login("liron","123456");
            delay();
            stepEnd();

            stepStart("Going to registration");
            driver.findElementByXPath("//h2[text()=\"Registration\"]").click();
            delay();
            driver.findElement(By.id("register_form")).isDisplayed();
            stepEnd();

            stepStart("Test Registration");
            driver.findElementByXPath("//*[@name=\"name\"]").sendKeys("first name");
            driver.findElementByXPath("//*[@id=\"register_form\"]/fieldset[1]/p[2]/input").sendKeys("last name");
            driver.findElementByXPath("//*[text()=\" Single\"]//*[@name=\"m_status\"]").click();
            driver.findElementByXPath("//*[@class=\"relative\"]//*[@name=\"hobby\"]").click();
            driver.findElementByXPath("//*[text()=\" Reading\"]//*[@name=\"hobby\"]").click();
            driver.findElementByXPath("//*[@name=\"phone\"]").sendKeys("1234567890"); //Phone Number
            driver.findElementByXPath("//*[@name=\"username\"]").sendKeys("username");
            driver.findElementByXPath("//*[@name=\"email\"]").sendKeys("Email@gmail.com");
            driver.findElementByXPath("//*[@name=\"password\"]").sendKeys("Password");
            driver.findElementByXPath("//*[@name=\"c_password\"]").sendKeys("Password");
            driver.findElementByXPath("//*[@value=\"submit\"]").click(); //Click Submit
            delay();
            stepEnd();

            reportiumClient.testStop(TestResultFactory.createSuccess());

        } catch (Exception e) {
            reportiumClient.testStop(TestResultFactory.createFailure(e.getMessage(), e));
            e.printStackTrace();
        }
    }

    public void delay() throws InterruptedException { TimeUnit.SECONDS.sleep(5);}

    public void clickText(String text, int threshold){
        Map<String, Object> params = new HashMap<>();
        params.put("content", text);
        params.put("threshold", threshold);
        params.put("scrolling", "scroll");
        driver.executeScript("mobile:text:select", params);
    }

    public void login(String username, String password) throws InterruptedException {

        driver.findElementByXPath("(//*[@name='username'])[2]").sendKeys(username);
        driver.findElementByXPath("(//*[@name='password'])[2]").sendKeys(password);
        driver.findElementByXPath("(//*[@name='password'])[2]").submit();
        delay();
        //driver.findElementByXPath("//*[@class=\"ajaxlogin\"]//*[@class=\"button\"]").click(); //Can use clickText(visual analysis) instead once its implemented for fast web
    }

    @AfterTest
    public void afterTest() throws IOException {
        try {
            System.out.println("Report URL: " + reportiumClient.getReportUrl());
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            System.out.println("Way2automation flow ended");
            driver.close();
            driver.quit();
        }
    }

    private void stepStart(String message) {
        System.out.println(message);
        reportiumClient.stepStart(message);
    }

    private void stepEnd() {
        reportiumClient.stepEnd();
    }
}
