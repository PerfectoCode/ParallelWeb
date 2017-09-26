public class ObjectRepository {

    public static String signin = "//*[contains(text(),'Signin')]";
    public static String loginUser = "(//*[@name='username'])[2]";
    public static String loginPassword = "(//*[@name='password'])[2]";
    public static String registration = "//h2[text()='Registration']";
    public static String registrationForm = "register_form";
    public static String registerFirstName = "//*[@name='name']";
    public static String registerLastName = "(//*[@class='fieldset']//input)[2]";
    public static String registerSingle = "//*[text()=' Single']//*[@name='m_status']";
    public static String registerHobbyDancing = "//*[@class='relative']//*[@name='hobby']";
    public static String registerHobbyReading = "//*[text()=' Reading']//*[@name='hobby']";
    public static String registerPhone = "//*[@name='phone']";
    public static String registerUsername = "//*[@name='username']";
    public static String registerEmail = "//*[@name='email']";
    public static String registerPassword1 = "//*[@name='password']";
    public static String registerPassword2 = "//*[@name='c_password']";
    public static String registerSubmit = "//*[@value='submit']";
}

