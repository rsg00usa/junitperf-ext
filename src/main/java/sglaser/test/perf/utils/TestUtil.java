package sglaser.test.perf.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.File;
import java.io.FileReader;

// JAXP classes for parsing
import javax.xml.parsers.DocumentBuilder; 
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;

// W3C DOM classes for traversing the document
import org.w3c.dom.Document;         
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException; // SAX classes used for error handling by JAXP

/**
 * This class contains general purpose methods that are shared across projects.
 * All methods in this class need to either directly our indirectly use the QA XML test data file
 * 
 * @author sglaser
 */
public class TestUtil {
	
	private static final Logger log = Logger.getLogger(TestUtil.class);
	public Document document;
	private String testPlanId;
	private String environment;
	private String testType;
	private Long masterAccountId;
	private Long accountId;
	private String userName;
	private String password;

	public TestUtil() {
		try {
			document = parseTestData();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * This method must be called before get methods of this class except getDocument().
	 * This method need parseTestData() to run first which is called from the constructor.
	 */
	public void setTestEnvData() throws Exception
	{
        String env = System.getProperty("env");
        testType = System.getProperty("testType");
		
		if (env == null || testType == null) {
			 log.error("Please add parameters 'env' and 'testType'");
			 throw new Exception("Please add parameters 'env' and 'testType'");
		}
		if (!env.equals("dev") && !env.equals("int") && !env.equals("qa") && !env.equals("stg") && !env.equals("trg"))
		{
			log.error("Please specify correct values for parameter 'env' as either 'dev', 'int',' qa' or 'stg'");
			throw new Exception("Please specify correct values for parameter 'env' as either 'dev', 'int', 'qa' or 'stg'");
		}
		if (!testType.equals("smoke") && !testType.equals("functional") && !testType.equals("funct") && !testType.equals("perf")
				&& !testType.equals("regression") && !testType.equals("reg")) {
			log.error("Please specify correct values for parameter 'testType' as either 'smoke', 'funct/functional' or 'perf'");
			throw new Exception("Please specify correct values for parameter 'testType' as either 'smoke', 'funct/functional,' " +
					"'perf' or 'regression/reg'");
		}

		String testTypeElement = "";
		if (testType.equals("smoke")) {
			testTypeElement = "SmokeTestConfig";            	
		} else if (testType.equals("functional") || testType.equals("funct")) {
			testTypeElement = "FunctionalTestConfig"; 
		} else if (testType.equals("perf")) {
			testTypeElement = "PerformanceTestConfig"; 
		} else if (testType.equals("regression") || testType.equals("reg")) {
			testTypeElement = "RegressionTestConfig";
		}

		String testEnvTag = "";
		if (env.equals("dev")) {
			testEnvTag = "env-dev";
		} else if (env.equals("int")) {
			testEnvTag = "env-int";
		} else if (env.equals("qa")) {
			testEnvTag = "env-qa";
		} else if (env.equals("stg")) {
			testEnvTag = "env-stg";
		} else if (env.equals("trg")) {
			testEnvTag = "env-trg";
		}
			
		// TODO: This part can hang if there is missing data in the xml file
		Element testEnvElement = this.getTestElementByName (testTypeElement, testEnvTag);
		testPlanId = testEnvElement.getAttribute("testPlanId");
		environment = testEnvElement.getAttribute("environment");
		Element userAccount = this.getTestElementByName("UserAccount", testEnvTag);
		masterAccountId = new Long(userAccount.getAttribute("masterAccountId"));
		accountId = new Long(userAccount.getAttribute("accountId"));
		userName = userAccount.getAttribute("userName");
		password = userAccount.getAttribute("password");
		
		log.info("########## TestTypeElement: " + testTypeElement);
		log.info("########## Environment: " + environment);
		log.info("########## TestPlanId:  " + testPlanId);
		log.info("########## MasterAccountId: " + masterAccountId);
		log.info("########## AccountId:  " + accountId);
		log.info("########## UserName:   " + userName);
	}
	
	public  String getTestPlanId()
	{
		return testPlanId;
	}
	
	public  String getEnvironment()
	{
		return environment;
	}
	
	public String getTestType()
	{
		return testType;
	}
	
	public  Document getDocument()
	{
		return document;
	}
	
	public  Long getMasterAccountId()
	{
		return this.masterAccountId;
	}
	
	public  Long getAccountId()
	{
		return this.accountId;
	}
	
	public  String getUserName()
	{
		return userName;
	}
	
	public  String getPassword()
	{
		return password;
	}
	
	public String getTestName(String testName) throws Exception
    {  
    	String pattern = "test";
    	int idx = testName.indexOf(pattern);
    	if (idx == -1) {
    		System.err.println("Error: test name pattern not found: " + testName);
    		return "";
    	}
    	log.info("Test Name = " + testName.substring(idx + pattern.length()));
    	return testName.substring (idx + pattern.length());
    }    
	
	public Document parseTestData() throws IOException, SAXException, ParserConfigurationException {
		
		String xmlFile = System.getProperty("xmlfile");
        //if (xmlFile == null || !(new File(xmlFile)).exists) throw FileNotFoundException("QA XML test data file not found");
		
	    // Get a JAXP parser factory object
	    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	
	    // Tell the factory what kind of parser we want
	    dbf.setValidating(false);
	
	    // Use the factory to get a JAXP parser object
	    DocumentBuilder parser = dbf.newDocumentBuilder();
	
	    // Finally, use the JAXP parser to parse the file.  This call returns
	    // A Document object.  Now that we have this object, the rest of this
	    // class uses the DOM API to work with it; JAXP is no longer required.
	    log.debug("########## QA data file: " + xmlFile.substring(xmlFile.lastIndexOf("/")+1));
	    return parser.parse(xmlFile);
	}
	
	public synchronized Element getTestData(Element element, String tagName)
	{
		NodeList testData = element.getChildNodes();
	    for(int i = 0; i < testData.getLength(); i++) {
	        if (testData.item(i).getNodeType() == Node.ELEMENT_NODE ) {
	            Element tag = (Element)testData.item(i);
	            if (tag.getTagName().equals(tagName)) {
	                return tag;
	            }
	        }
	    }
	    return null;
	}
	
    public synchronized Element getTestElementByName (String element, String tagName)
    {
	    NodeList testMethod = document.getElementsByTagName(element);
	    NodeList testData = testMethod.item(0).getChildNodes();
	    for(int i = 0; i < testData.getLength(); i++) {
	        if (testData.item(i).getNodeType() == Node.ELEMENT_NODE ) {
	            Element tag = (Element)testData.item(i);
	            if (tag.getTagName().equals(tagName)) {
	                return tag;
	            }
	        }
	    }
	    return null;
    }
    
    public void csvFileInsertPerfResult(String fileName) {

        File file = new File(fileName);

        if (!file.exists()) {
            log.error("File does not exist: " + file);
            return;
        }
        
        // Read file line by line and update TM
        try {
            BufferedReader in = new BufferedReader(new FileReader(file));
            String line = null;
            int count = 0;
            while ((line = in.readLine()) != null) {
            	
                //Skip csv file header
            	count++;
            	if (count == 1) continue;  
            	
                String[] stat = line.split(",");

                // Check if avg response time is less than SLA
                String result = "Pass";         
                if (Float.parseFloat(stat[8]) > Integer.parseInt(stat[9])) {
                    result = "Fail";
                }
                
                // Insert test case id, max, min, and avg elasped times into TM
                //tmInsertPerfResult(stat[0], stat[6], stat[7], stat[8], result);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}
