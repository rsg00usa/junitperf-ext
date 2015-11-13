package com.yahoo.satg.utils;

import junit.extensions.TestDecorator;
import junit.framework.Test;
import junit.framework.TestResult;

/**
 * Runs junit tests repeatedly based on test time.
 *
 * @author sglaser
 */
public class RepeatTest extends TestDecorator {
    private int count;
    private long sleep;
    private long testTime;

    // Execute by test time with no sleep in-between
    public RepeatTest(Test test, long time) {
        this(test, time, 0L);
    }

    // Execute by test time with sleep in-between 
    public RepeatTest(Test test, long time, long sleep) {
    	super(test);
    	if (time < 1)
            throw new IllegalArgumentException("Test time must be > 0");
    	testTime = time;
        this.sleep = sleep;
    }

    public void run(TestResult result) {	
    	
    	long start = System.currentTimeMillis();
    	
    	while ((System.currentTimeMillis() - start) < testTime) {
    		if (count != 0) {
    			try {
    				Thread.sleep(sleep);		
    			} catch (Exception ignored) { }
    		}

			if (result.shouldStop()) {
				break;
			}			

			super.run(result);
			count++;
    	}		
    }
    
    public int countTestCases() {
    	return super.countTestCases() * count;
    }
}
