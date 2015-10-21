   package com.kiwi.livestreaming;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.LinkedList;

public class Test {
	public static void main(String[] args){
		double initDelayRatio = 0.3;
		
		double averageDelay;
		
		for(double k=0;k<=10;k=k+0.25)
		
//		for(double k=0;k<=1.2;k=k+0.05)
		{
			averageDelay = k;
		
		int simulationTimes = 2000;
		
		int maxNumberOfVideos = 100; //times 10 seconds is the overall duration
		
		double utilization = 0;
		
		double delay = 0;
		
		double resolution = 0;
		
		double averageAchievedThroughput = 0;
		
		int j;
		
		//Probability distribution pattern 1:
		int arr[]  = {200, 400, 800, 1000, 1500, 2000, 3000, 5000}; //unit: kbits/second
	    int freq[] = {13, 16, 12, 6, 4, 2, 2, 1};
	    
		//instantiate the class TransmissionQueue
		//need to specify the following parameters:
		// maxNumberOfVideos
		// unitVideoDuration
		// bandwidthTimeslotWidth
		// initDelay
		// averageDelay
		// unitVideoDelay

		double unitVideoDuration = 10; //seconds
		double bandwidthTimeslotWidth = 1;
		
		
		
		double initDelay = maxNumberOfVideos * unitVideoDuration * initDelayRatio;

		
		double individualToAverageRatio = 4;
		double unitVideoDelay = averageDelay * individualToAverageRatio;
		
		for(j=0;j<=simulationTimes;j++){
		//provide input bandwidths. Statistics.inputBandwidths should be filled up with random value

		System.out.println("Simulation progress: " + j*1.0/simulationTimes*100 + "%");	
			
			
		//initialization
		Statistics.inputBandwidths.clear();
		Statistics.arrivalTimes.clear();
		Statistics.delayTimes.clear();
		Statistics.expectedArrivalTimes.clear();
		Statistics.fileSizeDecisions.clear();
		Statistics.bitrateDecisions.clear(); 
		 

	    	    
	    NetworkTraces networkTraces = new NetworkTraces(arr, freq);
	
		networkTraces.generateBandwidth((int) (2*(maxNumberOfVideos*(unitVideoDuration+averageDelay)+initDelay)/bandwidthTimeslotWidth));
		
		TransmissionQueue transmissionQueue = new TransmissionQueue(maxNumberOfVideos, unitVideoDuration, bandwidthTimeslotWidth,
				initDelay, averageDelay, unitVideoDelay);
		
				
		//for loop to simulate the process of live streaming
		//make decisions only from starting time to the ending time of maxNumberOfVideos*unitVideoDuration
		//queue update until the queue is empty
		int indexOfDecisionMakingTimes = 0;
		double currentTime = 0;
		
		//the first time to make decisions
		transmissionQueue.decisionAtTime(currentTime, true);
		
		LinkedList<Double> queue = transmissionQueue.getQueue();
		
//		int flagRound = 1;
		
		while(queue.size()!=0 || indexOfDecisionMakingTimes < maxNumberOfVideos){
			currentTime += Statistics.unitVideoDuration;
			transmissionQueue.updateQueueToCurrentTime(currentTime-Statistics.unitVideoDuration, currentTime);
			
			indexOfDecisionMakingTimes++;
			
			if(indexOfDecisionMakingTimes < maxNumberOfVideos){
				transmissionQueue.decisionAtTime(currentTime, true);
			}
									
//			System.out.print(flagRound+": ");
//			System.out.print("queue size: "+queue.size()+", ");
//			if(indexOfDecisionMakingTimes < maxNumberOfVideos) System.out.print("decision: "+ Statistics.fileSizeDecisions.getLast()+", ");
//			if(queue.size()>0)System.out.print("elements: "+queue+", ");
//			System.out.print("bandiwidth: "+Statistics.inputBandwidths.get(flagRound));
//			System.out.println("");
			
			
//			flagRound++;
		}
		
//		System.out.println(Statistics.fileSizeDecisions.size());
//		System.out.println(Statistics.delayTimes.size());
//		
//		System.out.println(Statistics.arrivalTimes.size());
//		System.out.println(Statistics.expectedArrivalTimes.size());
		

		//Upper bound of throughput that can be achieved during live streaming
		double endingTime = Statistics.arrivalTimes.getLast();
		int endTimeslot = (int) Math.floor(endingTime/Statistics.bandwidthTimeslotWidth);
		double upperBoundOfThroughput = 0;
		for(int i =0; i < endTimeslot; i++){
			upperBoundOfThroughput += Statistics.inputBandwidths.get(i)* Statistics.bandwidthTimeslotWidth;
		}
//		System.out.println("Upper bound of throughput: " + upperBoundOfThroughput);
		
		
		//achieved throughput of live streaming using the algorithm						
		double achievedOverallThroughput = 0;
		for(int i =0; i < maxNumberOfVideos; i++){
			achievedOverallThroughput += Statistics.fileSizeDecisions.get(i);
		}
//		System.out.println("Achieved throughput: " + achievedOverallThroughput);
		
		
		//show the utilization ratio
//		System.out.println("Ratio of utilization: " + achievedOverallThroughput/upperBoundOfThroughput);
		
		
		//the actual average delay time achieved
		double overallDelayTimes = 0;
		for(int i=0;i<Statistics.delayTimes.size();i++){
			overallDelayTimes += Statistics.delayTimes.get(i);
		}
//		System.out.println("Average Delay Time (constraint and actual): " + averageDelay + " " + overallDelayTimes/Statistics.delayTimes.size());

		//the actual average resolution achieved
		double resolutionSum = 0;
		for(int i=0;i<Statistics.delayTimes.size();i++){
			resolutionSum += Statistics.bitrateDecisions.get(i);
		}
		
		
		
		utilization += achievedOverallThroughput/upperBoundOfThroughput;
		delay += overallDelayTimes/Statistics.delayTimes.size();
		resolution += resolutionSum/Statistics.bitrateDecisions.size();
		
		averageAchievedThroughput += achievedOverallThroughput/Statistics.unitVideoDuration/Statistics.fileSizeDecisions.size();

		
		
		}
		
		System.out.println("Average utilization ratio: " + utilization/j);
		System.out.println("Average delay: " + delay/j);
		System.out.println("Average resolution: " + resolution/j);
		System.out.println("Average throughput: " + averageAchievedThroughput/j + " expected resolution: " + getResolution(averageAchievedThroughput/j));
		
		
		
		
		
		String title = "InitDelay: " + Statistics.initDelay + " seconds\r\n"
				+"averageDelay: " + Statistics.averageDelay + " seconds\r\n"
				+ "unitVideoDelay: " + Statistics.unitVideoDelay + " seconds\r\n"
				+ "maxNumberOfVideos: " + maxNumberOfVideos + "\r\n";
		
//		try {
//			Files.write(Paths.get("output.txt"), title.getBytes(), StandardOpenOption.APPEND);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		String output = "Average utilization ratio: " + round(utilization/j,4) + "\r\n" +
						"Average delay: " + round(delay/j,3) + "\r\n" +
						"Average resolution: " + round(resolution/j,2) + "\r\n" +
						"Average throughput: " + round(averageAchievedThroughput/j,2) + " expected resolution: " + getResolution(averageAchievedThroughput/j) + "\r\n\r\n";
		
		try {
			Files.write(Paths.get("output.txt"), title.getBytes(), StandardOpenOption.APPEND);
			Files.write(Paths.get("output.txt"), output.getBytes(), StandardOpenOption.APPEND);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		}

		
	}
	
	
	public static double round(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();

	    BigDecimal bd = new BigDecimal(value);
	    bd = bd.setScale(places, RoundingMode.HALF_UP);
	    return bd.doubleValue();
	}
	
	
	public static double getResolution(double availableBitrate){
		double fileSizeDecision, bitrateDecision;
		
		if(availableBitrate >= Statistics._2160P) {fileSizeDecision = Statistics._2160P; bitrateDecision = 2160;}
		else if(availableBitrate >= Statistics._1440P) {fileSizeDecision = Statistics._1440P; bitrateDecision = 1440;}
		else if(availableBitrate >= Statistics._1080P) {fileSizeDecision = Statistics._1080P; bitrateDecision = 1080;}
		else if(availableBitrate >= Statistics._720P) {fileSizeDecision = Statistics._720P; bitrateDecision = 720;}
		else if(availableBitrate >= Statistics._480P) {fileSizeDecision = Statistics._480P; bitrateDecision = 480;}
		else if(availableBitrate >= Statistics._360P) {fileSizeDecision = Statistics._360P; bitrateDecision = 360;}
		else if(availableBitrate >= Statistics._240P) {fileSizeDecision = Statistics._240P; bitrateDecision = 240;}
		else {fileSizeDecision = Statistics._144P; bitrateDecision = 144;}
		
		return bitrateDecision;
	}
	
}
