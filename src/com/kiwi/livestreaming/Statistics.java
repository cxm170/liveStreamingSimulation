package com.kiwi.livestreaming;

import java.util.LinkedList;

public class Statistics {
	
	//hard coded settings for unit video duration
	public static double unitVideoDuration;
	
	//slot width in the given bandwidths
	public static double bandwidthTimeslotWidth;
	
	public static double initDelay;
	
	public static double averageDelay;
	
	public static double unitVideoDelay;



	//unit: kb/s
	public final static double _2160P = 11000;
	public final static double _1440P = 6000;
	public final static double _1080P = 4000;
	public final static double _720P = 2000;
	public final static double _480P = 1000;
	public final static double _360P = 600;
	public final static double _240P = 400;
	public final static double _144P = 200;
	
	
	
	public static LinkedList<Double> arrivalTimes = new LinkedList<>();
	public static LinkedList<Double> delayTimes = new LinkedList<>();
	
	public static LinkedList<Double> expectedArrivalTimes = new LinkedList<>();
	
	public static LinkedList<Double> fileSizeDecisions = new LinkedList<>();
	public static LinkedList<Integer> bitrateDecisions = new LinkedList<>();
	
	public static LinkedList<Double> inputBandwidths = new LinkedList<>();

	public static LinkedList<Integer> resolutionChanges = new LinkedList<>();
	public static LinkedList<Double> fileSizeChanges = new LinkedList<>();

	

}
