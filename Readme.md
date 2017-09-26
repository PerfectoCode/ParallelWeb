# Parallel Web

This code sample demonstrates how to run a test suite on several web machines parallely.
It allows the test to run on several operation systems, OS versions, different browsers and browser versions.

:exclamation:This project uses Perfecto Turbo Web. For more information regarding Turbo Web Solution please visit [here](http://developers.perfectomobile.com/display/PD/Automating+Web-apps+with+Perfecto)

### Getting Stated:
- Inject your Perfecto Lab credentials using environment variables or within the Utils.java file:
```Java
/* Utils.java */
...
String PERFECTO_HOST = System.getenv("host");
String PERFECTO_TOKEN = System.getenv("token");
...
```

### Web Capabilities:

- To ensure your tests run on Perfecto Web machines on the cloud use the capabilities as in testng.xml i.e: <br/>
```Xml
 <test name="Windows 10 Chrome latest">
         <parameter name="platformName" value="Windows" />
         <parameter name="platformVersion" value="10" />
         <parameter name="browserName" value="Chrome" />
         <parameter name="browserVersion" value="latest" />
         <parameter name="screenResolution" value="" />
         <classes>
             <class name="ParallelWeb" />
         </classes>
     </test>
```

- More capabilities are available, read more [here](http://developers.perfectomobile.com/display/PD/Supported+Platforms).

### Perfecto Turbo Web Automation:

Perfecto's Desktop Web environment introduces an accelerated interface to Web Browser automation with its new Turbo web interface. Using this new environment will allow you to connect quicker to the browser "device" you select for automating and testing your web application.

- To enable Turbo Web Automation in this code sample follow the instructions in the link above in order to generate authentication token.
Place the authentication in one of the Turbo Web test's files:
```Python
token = os.environ['token']
host = os.environ['host']
...
self.driver = webdriver.Remote('https://' + self.host + '/nexperience/perfectomobile/wd/hub/fast', capabilities)
```

### Perfecto DigitalZoom reporting:

Perfecto Reporting is a multiple execution digital report, that enables quick navigation within your latest build execution. Get visibility of your test execution status and quickly identify potential problems with an aggregated report.
Hone-in and quickly explore your test results all within customized views, that include logical steps and synced artifacts. Distinguish between test methods within a long execution. Add personalized logical steps and tags according to your team and organization.

*Click [here](http://developers.perfectomobile.com/display/PD/Reporting) to read more about DigitalZoom Reporting.*
