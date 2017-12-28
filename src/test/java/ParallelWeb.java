
import com.perfecto.reportium.client.ReportiumClient;
import com.perfecto.reportium.client.ReportiumClientFactory;
import com.perfecto.reportium.model.Job;
import com.perfecto.reportium.model.PerfectoExecutionContext;
import com.perfecto.reportium.model.Project;
import com.perfecto.reportium.test.TestContext;
import com.perfecto.reportium.test.result.TestResultFactory;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
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
    private String url = "http://way2automation.com/way2auto_jquery/index.php";

    // Create Remote WebDriver based on testng.xml configuration
    @Parameters({ "platformName", "platformVersion", "browserName", "browserVersion", "screenResolution", "location" })
    @BeforeTest
    public void beforeTest(String platformName, String platformVersion, String browserName, String browserVersion, String screenResolution, @Optional String location) throws MalformedURLException {

        try{
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
        catch(Exception ex){
            System.out.println("ERROR: " + ex.toString() + " trying os/browser" + platformName + platformVersion + "(" + browserName + browserVersion +")");
        }
    }

    // Test Method - Navigate to way2automation.com and fill the registration form
    @Test
    public void way2automation(){
        try{
            reportiumClient.testStart("Way2Automation Flow", new TestContext("Sample", "Native"));
            stepStart("Navigate to site");
            driver.get(url);
            delay();
            stepEnd();

            stepStart("Opening signin window");
            driver.findElementByXPath(ObjectRepository.signin).click(); //Can use clickText(visual analysis) instead once its implemented for fast web
            delay();
            stepEnd();

            stepStart("Signing in");
            login("Perfecto","123456");
            delay();
            stepEnd();

            stepStart("Going to registration");
            WebElement registration = driver.findElementByXPath(ObjectRepository.registration);
            driver.executeScript(String.format("window.scrollTo(0, %d)", registration.getLocation().y));
            registration.click();
            delay();
            if (driver.findElement(By.id(ObjectRepository.registrationForm)).isDisplayed()){
                reportiumClient.reportiumAssert("Registration Form Visible", true);
            }
            else {
                reportiumClient.reportiumAssert("Registration Form NOT Visible", false);
            }

            stepEnd();

            stepStart("Test Registration");
            driver.findElementByXPath(ObjectRepository.registerFirstName).sendKeys("first name");
            driver.findElementByXPath(ObjectRepository.registerLastName).sendKeys("last name");
            driver.findElementByXPath(ObjectRepository.registerSingle).click();
            driver.findElementByXPath(ObjectRepository.registerHobbyDancing).click();
            driver.findElementByXPath(ObjectRepository.registerHobbyReading).click();
            driver.findElementByXPath(ObjectRepository.registerPhone).sendKeys("1234567890"); //Phone Number
            driver.findElementByXPath(ObjectRepository.registerUsername).sendKeys("username");
            driver.findElementByXPath(ObjectRepository.registerEmail).sendKeys("Email@gmail.com");
            driver.findElementByXPath(ObjectRepository.registerPassword1).sendKeys("Password");
            driver.findElementByXPath(ObjectRepository.registerPassword2).sendKeys("Password");

            WebElement submit = driver.findElementByXPath(ObjectRepository.registerSubmit);
            driver.executeScript(String.format("window.scrollTo(0, %d)", submit.getLocation().y));
            submit.click(); //Click Submit
            delay();
            stepEnd();

            reportiumClient.testStop(TestResultFactory.createSuccess());

        } catch (Exception e) {
            reportiumClient.testStop(TestResultFactory.createFailure(e.getMessage(), e));
            e.printStackTrace();
        }
    }

    public void delay() throws InterruptedException { TimeUnit.SECONDS.sleep(7);}

    public void clickText(String text, int threshold){
        Map<String, Object> params = new HashMap<>();
        params.put("content", text);
        params.put("threshold", threshold);
        params.put("scrolling", "scroll");
        driver.executeScript("mobile:text:select", params);
    }

    public void login(String username, String password) throws InterruptedException {
        WebElement userInput = driver.findElementByXPath(ObjectRepository.loginUser);
        userInput.sendKeys(username);
        if (userInput.getAttribute("value").equalsIgnoreCase(username)){
            reportiumClient.reportiumAssert("Username inserted successfully", true);
        }
        else {
            reportiumClient.reportiumAssert("Failed to insert username", false);
        }

        WebElement passwordInput = driver.findElementByXPath(ObjectRepository.loginPassword);
        passwordInput.sendKeys(password);

        if (passwordInput.getAttribute("value").equals(password)){
            reportiumClient.reportiumAssert("Password inserted successfully", true);
        }
        else {
            reportiumClient.reportiumAssert("Failed to insert password", false);
        }

        passwordInput.submit();
        Thread.sleep(5000);

        try{
            driver.findElementByXPath(ObjectRepository.loginPopup);
            if (driver.findElementByXPath(ObjectRepository.loginPopup).isDisplayed())
                reportiumClient.reportiumAssert("Failed to Sign in", false);
        } catch (Exception ex){
            reportiumClient.reportiumAssert("Signed in succesfully", true);
        }

        delay();
        //driver.findElementByXPath("//*[@class=\"ajaxlogin\"]//*[@class=\"button\"]").click(); //Can use clickText(visual analysis) instead once its implemented for fast web
    }

    @AfterTest
    public void afterTest() throws IOException {
        try {
            System.out.println("Report URL: " + reportiumClient.getReportUrl());
            /*************************/
            String reportPdfUrl = (String)(driver.getCapabilities().getCapability("reportPdfUrl"));
            System.out.println(reportPdfUrl);
            /*************************/
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
