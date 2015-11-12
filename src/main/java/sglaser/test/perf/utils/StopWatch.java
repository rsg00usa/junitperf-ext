package sglaser.test.perf.utils;

/**
 * This class is used to time specific lines of code
 *
 * @author sglaser
 *
 */
public class StopWatch {

	private long startTime = 0;
	private long stopTime = 0;

	public void start() {
	    this.startTime = System.currentTimeMillis();
	}
		
	public void stop() {
	    this.stopTime = System.currentTimeMillis();
	}
		
	//elaspsed time in milliseconds
	public long getElapsedTime() {
        if (stopTime < startTime) {
            System.out.println("ERROR: stop time is less than start time");
            return 0;
        }
            
		System.out.println("Elapsed Time: " + this.stopTime + " - " + this.startTime + " =  " + (this.stopTime - this.startTime) + "ms");
	    return this.stopTime - this.startTime;
	}
}
