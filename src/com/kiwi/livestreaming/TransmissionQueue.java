package com.kiwi.livestreaming;

import java.util.LinkedList;
import java.util.List;

public class TransmissionQueue {

	private LinkedList<Double> queue;
	
	private int maxNumberOfVideos;
	
	public TransmissionQueue(int maxNumberOfVideos, double unitVideoDuration, double bandwidthTimeslotWidth, double initDelay, double averageDelay, double unitVideoDelay){
		this.queue = new LinkedList<Double>();
		this.maxNumberOfVideos = maxNumberOfVideos;
		
		Statistics.unitVideoDuration = unitVideoDuration;
		Statistics.bandwidthTimeslotWidth = bandwidthTimeslotWidth;
		Statistics.initDelay = initDelay;
		Statistics.averageDelay = averageDelay;
		Statistics.unitVideoDelay = unitVideoDelay;
		
		

		
		
		
		//To-Do: provide value for Statistics.inputBandwidths
		
		Statistics.expectedArrivalTimes.add(Statistics.initDelay);
		
		
	}
	
	
	public int getMaxNumberOfVideos(){
		return this.maxNumberOfVideos;
	}
	
	
	public LinkedList<Double> getQueue(){
		return this.queue;
	}
	
	
	
	//delete transmitted videos, add new arriving videos. update arrival and delay times.
	public void updateQueueToCurrentTime(double startTime, double endTime){
		//Don't forget to check whether queue is empty of not
		
		//delete transmitted videos, and also add new arrivals
		
		double overallTransmissionSize = getThroughputOverTimeslot(startTime, endTime);
		
		double sizeOfVideoAtTheHeadOfQueue;
		
		if (queue.size()>0) sizeOfVideoAtTheHeadOfQueue = (double) queue.getFirst();
		else sizeOfVideoAtTheHeadOfQueue = -1;
		
		
		double arrivalTime = startTime;
		
		//check whether the video at the head of queue can finish transmission by current time.  		
		while((overallTransmissionSize - sizeOfVideoAtTheHeadOfQueue) >= 0 && sizeOfVideoAtTheHeadOfQueue != -1 ){
			

			
			
			overallTransmissionSize = overallTransmissionSize - sizeOfVideoAtTheHeadOfQueue;
			
			
			if(overallTransmissionSize>=0) {
				arrivalTime = updateArrivalAndDelayTime(arrivalTime, sizeOfVideoAtTheHeadOfQueue);
				queue.removeFirst();}
			
			
			if(queue.size()>0) sizeOfVideoAtTheHeadOfQueue = (double) queue.getFirst();
			else break;
		}
		
		
		//update the first element with the new result
		if(queue.size()>0) queue.set(0, sizeOfVideoAtTheHeadOfQueue - overallTransmissionSize);
		
		
		
		
		//add new arrivals
		double newArrivalVideo = retreiveDecisionAtTime(startTime);
		if(newArrivalVideo!=0) queue.add(newArrivalVideo);
		
				
	}
	
	
	//query against decisions already made.
	public double retreiveDecisionAtTime(double time){
		long index = Math.round(time/Statistics.unitVideoDuration);
		

		if(index < Statistics.fileSizeDecisions.size())
		return Statistics.fileSizeDecisions.get((int) index);
		else return 0;
	}
	
	
	
	
	//make decisions for the queried time
	public double decisionAtTime(double decisionTime, boolean isChannelAware){
		//To-Do: how to make decisions according to the bandwidth and delay information
		
		
		//To-Do: update decision LinkedList
		if(isChannelAware) return decisionAtTimeChannelAware(decisionTime);
		else return decisionAtTimeDelayOnly(decisionTime);
		
	
	}
	
	//delay only algorithm
	public double decisionAtTimeDelayOnly(double decisionTime){
		return 0;
	}
	
	
	//channel aware algorithm
	public double decisionAtTimeChannelAware(double decisionTime){
		//how to make decisions according to predicted bandwidth and delay information
		int slotOfDecisionTime = (int) Math.floor(decisionTime/Statistics.bandwidthTimeslotWidth);
		
		double predictedBandwidth = Statistics.inputBandwidths.get(slotOfDecisionTime+1);
		
		double accumulatedDelayTimes = getAccumulatedDelayTimes();
		
		double remainingOverallDelayTimes = maxNumberOfVideos * Statistics.averageDelay - accumulatedDelayTimes;
		
		
		
		double deadline = Statistics.expectedArrivalTimes.getLast() + Math.min(Statistics.unitVideoDelay, remainingOverallDelayTimes);
		
		double predictedThroughput = predictedBandwidth * (deadline - decisionTime);
		
		//To-Do: get the size of videos in the queue, and throughput between now and deadline, and make decisions for next video
		
		double existingFileSizes = 0;
		for(int i=0;i<queue.size();i++){
			existingFileSizes += queue.get(i);
		}
		
		double availableBitrate = (predictedThroughput - existingFileSizes)/Statistics.unitVideoDuration;
		
		
		double fileSizeDecision;
		int bitrateDecision;
		
		if(availableBitrate >= Statistics._2160P) {fileSizeDecision = Statistics._2160P; bitrateDecision = 2160;}
		else if(availableBitrate >= Statistics._1440P) {fileSizeDecision = Statistics._1440P; bitrateDecision = 1440;}
		else if(availableBitrate >= Statistics._1080P) {fileSizeDecision = Statistics._1080P; bitrateDecision = 1080;}
		else if(availableBitrate >= Statistics._720P) {fileSizeDecision = Statistics._720P; bitrateDecision = 720;}
		else if(availableBitrate >= Statistics._480P) {fileSizeDecision = Statistics._480P; bitrateDecision = 480;}
		else if(availableBitrate >= Statistics._360P) {fileSizeDecision = Statistics._360P; bitrateDecision = 360;}
		else if(availableBitrate >= Statistics._240P) {fileSizeDecision = Statistics._240P; bitrateDecision = 240;}
		else {fileSizeDecision = Statistics._144P; bitrateDecision = 144;}
		
		Statistics.fileSizeDecisions.add(fileSizeDecision*Statistics.unitVideoDuration);
		Statistics.bitrateDecisions.add(bitrateDecision);
		
		return fileSizeDecision;
		
	}
	
	
	
	//currently already delayed time
	public double getAccumulatedDelayTimes(){
		
		int numberOfTranmisttedVideos = Statistics.delayTimes.size();
		
		double accumulatedDelayTimes = 0;
		for(int i=0;i<numberOfTranmisttedVideos;i++){
			accumulatedDelayTimes += Statistics.delayTimes.get(i);
		}
		
		return accumulatedDelayTimes;
	}
	
	
	
	
	public double getThroughputOverTimeslot(double startTime, double endTime){
		int startTimeslot = (int) Math.floor(startTime/Statistics.bandwidthTimeslotWidth);
		
		int endTimeslot = (int) Math.floor(endTime/Statistics.bandwidthTimeslotWidth);
		
		double throughput = 0;
		
		if(startTimeslot==endTimeslot) 
			throughput = (endTime - startTime) * Statistics.inputBandwidths.get(endTimeslot);
		else{
			throughput += ((startTimeslot+1) * Statistics.bandwidthTimeslotWidth - startTime)
					 * Statistics.inputBandwidths.get(startTimeslot);
			throughput += (endTime - endTimeslot * Statistics.bandwidthTimeslotWidth)
					 * Statistics.inputBandwidths.get(startTimeslot);
			
			//in case that there exist slots between first and end slots.			
			int numberOfSlotsBetween = endTimeslot - startTimeslot;
			for(int i =1;i<numberOfSlotsBetween;i++){
				throughput += Statistics.bandwidthTimeslotWidth * Statistics.inputBandwidths.get(startTimeslot+i);
			}
		}
		
		
		return throughput;
	}
	
	
	
	//when a video is transmitted, its arrival time and delay are updated accordingly. 
	public double updateArrivalAndDelayTime(double startTime, double sizeOfVideoAtTheHeadOfQueue){
		int startTimeslot = (int) Math.floor(startTime/Statistics.bandwidthTimeslotWidth);
		
		//remaining file size if transmitted in the startTimeslot
		double remainingSize = sizeOfVideoAtTheHeadOfQueue - ((startTimeslot+1) * 1.0 * Statistics.bandwidthTimeslotWidth - startTime)
			 * Statistics.inputBandwidths.get(startTimeslot);
		
		int endTimeslot = startTimeslot;
		
		
		//until the endingTimeslot is found
		while(remainingSize >= 0){
			endTimeslot = endTimeslot + 1;
			remainingSize = remainingSize - Statistics.bandwidthTimeslotWidth * Statistics.inputBandwidths.get(endTimeslot);
		}
		//add back the subtracted throughput in the while loop
		remainingSize = remainingSize + Statistics.bandwidthTimeslotWidth * Statistics.inputBandwidths.get(endTimeslot);
		

		
		
		//Different calculations for endTime for two different cases
		double endTime;
		if(endTimeslot == startTimeslot) 
			endTime = startTime + remainingSize/Statistics.inputBandwidths.get(endTimeslot);
		else 
			endTime = endTimeslot * Statistics.bandwidthTimeslotWidth + remainingSize/Statistics.inputBandwidths.get(endTimeslot);
				
		//add the actual arrival time of current video to the end of the arrivalTimes LinkedList
		Statistics.arrivalTimes.add(endTime);
		
		
		
		
		//Compare actual arrival time with expected arrival time to get the delay time
		double delayTime;
		if(endTime <= Statistics.expectedArrivalTimes.getLast())
			delayTime = 0;
		else 
			delayTime = endTime - Statistics.expectedArrivalTimes.getLast();

		//add the delay time of current video to the end of the delayTimes LinkedList.
		Statistics.delayTimes.add(delayTime);
		
		//update expected deadline for the next arriving video (eventually, the size of this list may be 
		//larger than the maximum number of videos.
		if(Statistics.expectedArrivalTimes.size()<this.maxNumberOfVideos) Statistics.expectedArrivalTimes.add(Statistics.expectedArrivalTimes.getLast()+Statistics.unitVideoDuration+delayTime);

		
		
		return endTime;
	}
	
	
	
	
//	//if arrival before expected time, delay is 0; otherwise, delay is the difference of both value.
//	public double updateDelayTime(double arrivalTime){
//		double delayTime;
//		
//		//compare actual arrival time with expected arrival time to get the delay time
//		if(arrivalTime <= Statistics.expectedArrivalTimes.getLast())
//			delayTime = 0;
//		else 
//			delayTime = arrivalTime - Statistics.expectedArrivalTimes.getLast();
//
//		//add the delay time of current video to the end of the delayTimes LinkedList.
//		Statistics.delayTimes.add(delayTime);
//		
//		return delayTime;
//	}
	
	
	
}
