import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

public class Utils {

    public static RemoteWebDriver getRemoteWebDriver(String platformName, String platformVersion, String browserName,
                                                     String browserVersion, String screenResolution, String location) throws MalformedURLException {
        // Set cloud host and credentials values from CI, else use local values
        String PERFECTO_HOST = System.getenv("host");
        String PERFECTO_TOKEN = System.getenv("token");

        //Old School Credentials:
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
                new URL("https://" + PERFECTO_HOST + "/nexperience/perfectomobile/wd/hub/fast"), capabilities);

        // Define RemoteWebDriver timeouts
        webdriver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
        webdriver.manage().timeouts().pageLoadTimeout(30, TimeUnit.SECONDS);
        webdriver.manage().window().maximize();

        // Maximize browser window on Desktop
        if (!screenResolution.isEmpty()) {
            webdriver.manage().window().maximize();
        }

        return webdriver;
    }
}
