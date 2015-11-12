package com.yahoo.satg.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Collections;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

/**
 * Provides the statistical performance test results for reporting
 * 
 * @author sglaser
 */
public class Report {
	
    private static final Logger log = Logger.getLogger(Report.class);
    private static final int DEFAULTSLA = 50;
	private static Map<String,Double> minHash = Collections.synchronizedMap(new HashMap<String,Double>());
	private static Map<String,Double> maxHash = Collections.synchronizedMap(new HashMap<String,Double>());
	private static Map<String,Double> aggHash = Collections.synchronizedMap(new HashMap<String,Double>());
	private static Map<String,Collection<Double>> timeHash = Collections.synchronizedMap(new HashMap<String,Collection<Double>>());
	private static Map<String,Integer> cntHash = Collections.synchronizedMap(new HashMap<String,Integer>());
	private static Map<String,String> tcHash = Collections.synchronizedMap(new HashMap<String,String>());
	private static TestUtil testUtil;
    private static String service;
    private static String startTime;
    private static String endTime;
    
	public static synchronized void save(Element tc, String service, String testName, double testTime)
    {
        save(tc.getAttribute("testCaseId"), service, testName, testTime);
    }

	public static synchronized void save(String testCase, String testClass, String testName, double testTime)
	{
        // Service (class) that contains api (method) under test
        Report.service = testClass;
            
        // Count the number of each test
        int count = 0;
        if (cntHash.containsKey(testName)) {
        	count = cntHash.get(testName);
        } else {
        	setStartTime();
        }
        cntHash.put(testName, ++count);

        // Compute aggregate elapsed test time
        if (aggHash.containsKey(testName)) {
        	double aggregateTime = aggHash.get(testName) + testTime;
        	aggHash.put(testName, aggregateTime);
        } else {
        	aggHash.put(testName, testTime);
        }
        
        // Save each elapsed test time
        if (timeHash.containsKey(testName)) {
        	timeHash.get(testName).add(testTime);
        } else {
        	Collection<Double> elapsedTime = new ArrayList<Double>();
        	elapsedTime.add(testTime);
        	timeHash.put(testName, elapsedTime);
        }       

        // Compute minimum elapsed test time
        if (minHash.containsKey(testName)) {
        	if (testTime < minHash.get(testName))
        		minHash.put(testName, testTime);
        } else {
        	minHash.put(testName, testTime);
        }
        
        // Compute maximum elapsed test time
        if (maxHash.containsKey(testName)) {
        	if (testTime > maxHash.get(testName))
        		maxHash.put(testName, testTime);
        } else {
        	maxHash.put(testName, testTime);
        }
        
        // Associate test name with unique test case id
    	Iterator<String> it = tcHash.keySet().iterator();
    	while (it.hasNext()) {
    		String key = it.next();
    		if (tcHash.get(key).equals(testCase) && !key.equals(testName)) {
    			log.error(" Test case id " + testCase + " is not unique");
                return;
    		}
    	}
    	tcHash.put(testName, testCase);
	}
	
	// Calcuate testing start time
	public static void setStartTime() {
		
		if (startTime == null) {
			DateFormat dateFormat = new SimpleDateFormat("MMM-dd-yy HH:mm:ss");
			startTime = dateFormat.format(new Date());
		}
	}
	
	// Calcuate testing end time
	public static void setEndTime() {
		
		if (endTime == null) {
			DateFormat dateFormat = new SimpleDateFormat("MMM-dd-yy HH:mm:ss");
			endTime = dateFormat.format(new Date());
		}
	}
	
	@Deprecated
	public synchronized static String report(boolean repeatTest) {
		if (repeatTest) {
			return report(Option.REPEATTEST, DEFAULTSLA);
		} else {
			return report(Option.NONE, DEFAULTSLA);
		}
	}
    
	public synchronized static String report() {
        return report(Option.NONE, DEFAULTSLA);
    }
	
	public synchronized static String report(int sla) {
        return report(Option.NONE, sla);
    }

    public synchronized static String report(Option option, int sla) {

        // Mark end of testing before doing anything else
        setEndTime();
        	
        // Get average elasped times
        Map<String,Double> avgHash = new HashMap<String,Double>(getAvg());
        Map<String,Double> PercHash = new HashMap<String,Double>(get90thPerc());
        
        // Get test execution parameters
        if (testUtil == null) testUtil = new TestUtil();
    	Element config = testUtil.getTestElementByName("PerfTest", "execute");
        config = testUtil.getTestElementByName(config.getParentNode().getNodeName(), config.getAttribute("config"));
        int users = Integer.parseInt(config.getAttribute("users"));
        int iterations = Integer.parseInt(config.getAttribute("iterations"));

        // Check if there is data to report
        if (avgHash.isEmpty()) {
        	log.warn("No data was saved to report");
        	return null;
        }

        Iterator<String> it = cntHash.keySet().iterator();
        
        // Perform specific data verification options
        //if (option.getId() == Option.NONE.getId() || option.getId() == Option.CHO.getId()) {
        if (option.getId() == Option.NONE.getId()) {
            while(it.hasNext()) {
                String testName = it.next();
                // NONE: Check if the counted tests equals the saved tests
                if (option.getId() == Option.NONE.getId() && users * iterations != cntHash.get(testName)) {
                    log.error("Inconsistent number of tests for " + testName + ": counted " + users * iterations + " saved " + cntHash.get(testName));
                    return null;
                }
                // CHO: Check if the counted tests equals is less than the saved tests
                //if (option.getId() == Option.CHO.getId() && users * iterations < cntHash.get(testName)) {
                //    log.error("Test " + testName + "Should have more executions than " + cntHash.get(testName));
                //    return null;
                //}
            }
        } 
        
        DateFormat dateFormat = new SimpleDateFormat("yyMMddHHmmss");
        String dateTag = dateFormat.format(new Date());
        File csvFile = null;
        
        // Save test results into a csv file
        try {
            String fileName = service + "Results_" + dateTag + ".csv";
            File dir = new File("performance");
            
            // Create performance directory if not exists
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    log.warn("Unable to create performance directory");
                	return null;
                }
            }

            csvFile = new File(dir,fileName);
            BufferedWriter out = new BufferedWriter(new FileWriter(csvFile, true));
            Iterator<String> iterator = avgHash.keySet().iterator();
            StringBuilder sb = new StringBuilder();

            // Create csv file header
            for (Header column : Header.values()) {
            	sb.append(column.getTitle()).append(",");
            }
            sb.replace(sb.length()-1, sb.length(), "\n");
        
            // Write saved test results to csv file
            while(iterator.hasNext()) {
                String testName = iterator.next();
                sb.append(tcHash.get(testName)).append(",");
                sb.append(service).append(",");
                sb.append(testName).append(",");
                sb.append(users).append(",");
                sb.append(iterations).append(",");
                sb.append(cntHash.get(testName)).append(",");
                sb.append(minHash.get(testName)).append(",");
                sb.append(maxHash.get(testName)).append(",");
                sb.append(avgHash.get(testName)).append(",");
                sb.append(PercHash.get(testName)).append(",");
                sb.append(sla).append(",");
                sb.append(startTime).append(",");
                sb.append(endTime);
                sb.append("\n");
            }
            out.write(sb.toString());
            out.close();
        } catch (IOException e) {
            log.error("IOException:");
            e.printStackTrace();
        }
        return csvFile.toString();
    }
	
	public static Map<String,Double> getMin()
	{
		return minHash;
	}

	public static Map<String,Double> getMax()
	{
		return maxHash;
	}
	
	public static Map<String,Double> getAvg()
	{
		Map<String,Double> avgHash = new HashMap<String,Double>();
		Iterator<String> iterator = aggHash.keySet().iterator();
		
		// Calculate average elapsed time
        while(iterator.hasNext()) {
        	String key = iterator.next();
        	Double value = aggHash.get(key) / cntHash.get(key);
        	avgHash.put(key, value);
        }
        
        return avgHash;
	}
	
	// Dump out all the elapsed times for each test
	public static void showTimes()
	{
		Iterator<String> iterator = timeHash.keySet().iterator();
		
        while(iterator.hasNext()) {
        	String key = iterator.next();
        	List<Double> et = new ArrayList<Double>(timeHash.get(key));
    		log.info("TEST = " + key);        		
        	for (Double t : et) {
        		log.info("VALUE = " + t);        		
        	}
        }
	}
	
	//Added by Venkat to compute the 90th percentile of PERF time.
	
	public static final double percentile90( ArrayList<Double> arr,double n) {
		Collections.sort(arr);
		int len = arr.size();
		
		if(len == 0){
		   return arr.get(len);
		}
		else if(len <= 9){
			return arr.get(len-1);
		}
		else{
		  //System.out.println("******Len Is.."+len);
		  return arr.get((int)(len*(n/100))-1);
		}
	}

public static Map<String,Double> get90thPerc()
	{
		Map<String,Double> PercHash = new HashMap<String,Double>();
		Iterator<String> iterator = timeHash.keySet().iterator();
		
		// get 90th percentile
        while(iterator.hasNext()) {
        	String key = iterator.next();
        	double val = percentile90((ArrayList<Double>)timeHash.get(key), 90);
         	PercHash.put(key, val);
        }
        
        return PercHash;
	}
	
	public enum Header {
        TESTID ("TESTID", "Test case id"),
        TESTCLASS ("TESTCLASS", "Name of test class"),
        TESTCASE ("TESTCASE", "Name of test case"),
        USERS ("USERS", "Number of users"),
        ITERATIONS ("ITERATIONS", "Number of iterations"),
        PASSED ("TESTS PASSED", "Number of passed tests"),
        MINIMUM ("MINIMUM(ms)", "Minumum elapsed time"),
        MAXIMUM ("MAXIMUM(ms)", "Maximum elapsed time"),
        AVERAGE ("AVERAGE(ms)", "Average elasped time"),
        PERCENTILE90 ("90thPERCENTILE(ms)", "90th Percentile time"),
        SLA ("SLA(ms)", "Service Level Agreement elapsed time"),
        STARTTIME ("START TIME", "Time test was started"),
        ENDTIME ("END TIME", "Time test was stopped");

        private final String title;
        private final String description;

        private Header(String title, String description) {
            this.title = title;
            this.description = description;
        }
        
        public String getDescription() {
        	return description;
        }
        
        public String getTitle() {
        	return title;
        }
    }
	
	public enum Option {
		NONE (0),  // Use default settings
		REPEATTEST (1),  // Allow duplicate tests
		CHO (2),  // Check continuous testing is performed
		NOCHECK (3);  // Do not check for duplicate tests
		
		private final int id;

	    private Option(int arg1) {
	        this.id = arg1;
	    }

	    public int getId() {
	        return id;
	    }
    }
}
