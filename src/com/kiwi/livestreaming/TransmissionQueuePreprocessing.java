package com.kiwi.livestreaming;

import java.util.*;

//This class implements our approach that needs to predict future bandwidth
public class TransmissionQueuePreprocessing {

	private LinkedList<Double> queue;

	private int maxNumberOfVideos;

	private int slideWindow;

	HashMap<Integer, Double> efficiency;
	HashMap<Integer, Double> indexTable;
	int[] resolutionReduction;
	double[] fileSizeReduction;

	int resolutionChoiceIndex;

	int[] resolutions = {144, 240, 360, 480, 720, 1080};
	double[] fileSizes = {Statistics._144P, Statistics._240P, Statistics._360P, Statistics._480P, Statistics._720P, Statistics._1080P};

	int countHigherBitrate;
	double lastAddedTimeForHigherBitrate;
	int countLowerBitrate;
	double lastAddedTimeForLowerBitrate;

	public TransmissionQueuePreprocessing(int maxNumberOfVideos, double unitVideoDuration, double bandwidthTimeslotWidth, double initDelay, double averageDelay, double unitVideoDelay, int slideWindow) {
		this.queue = new LinkedList<Double>();
		this.maxNumberOfVideos = maxNumberOfVideos;
		this.slideWindow = slideWindow;

		Statistics.unitVideoDuration = unitVideoDuration;
		Statistics.bandwidthTimeslotWidth = bandwidthTimeslotWidth;
		Statistics.initDelay = initDelay;
		Statistics.averageDelay = averageDelay;
		Statistics.unitVideoDelay = unitVideoDelay;


		//To-Do: provide value for Statistics.inputBandwidths

		Statistics.expectedArrivalTimes.add(Statistics.initDelay);

		updatePreprocessingIndexTable();

		countHigherBitrate = 0;
		countLowerBitrate = 0;
		lastAddedTimeForHigherBitrate = -1;
		lastAddedTimeForLowerBitrate = -1;

		resolutionChoiceIndex = 3;//default: 480p is chosen for the resolutionMax alg. {144, 240, 360, 480, 720, 1080}

	}


	public int getMaxNumberOfVideos() {
		return this.maxNumberOfVideos;
	}


	public LinkedList<Double> getQueue() {
		return this.queue;
	}

	public double getQueueLength() {
		int queueLength = 0;
		for (int i = 0; i < queue.size(); i++) {
			queueLength += queue.get(i);
		}

		return queueLength;
	}


	//delete transmitted videos, add new arriving videos. update arrival and delay times.
	public void updateQueueToCurrentTime(double startTime, double endTime) {
		//Don't forget to check whether queue is empty of not

		//delete transmitted videos, and also add new arrivals

		double overallTransmissionSize = getThroughputOverTimeslot(startTime, endTime);

		double sizeOfVideoAtTheHeadOfQueue;

		if (queue.size() > 0) sizeOfVideoAtTheHeadOfQueue = (double) queue.getFirst();
		else sizeOfVideoAtTheHeadOfQueue = -1;


		double arrivalTime = startTime;

		//check whether the video at the head of queue can finish transmission by current time.  		
		while ((overallTransmissionSize - sizeOfVideoAtTheHeadOfQueue) >= 0 && sizeOfVideoAtTheHeadOfQueue != -1) {


			overallTransmissionSize = overallTransmissionSize - sizeOfVideoAtTheHeadOfQueue;


			if (overallTransmissionSize >= 0) {
				arrivalTime = updateArrivalAndDelayTime(arrivalTime, sizeOfVideoAtTheHeadOfQueue);
				queue.removeFirst();
			}


			if (queue.size() > 0) sizeOfVideoAtTheHeadOfQueue = (double) queue.getFirst();
			else break;
		}


		//update the first element with the new result
		if (queue.size() > 0) queue.set(0, sizeOfVideoAtTheHeadOfQueue - overallTransmissionSize);


		//add new arrivals
		double newArrivalVideo = retreiveDecisionAtTime(startTime);
		if (newArrivalVideo != 0) queue.add(newArrivalVideo);


	}


	//query against decisions already made.
	public double retreiveDecisionAtTime(double time) {
		long index = Math.round(time / Statistics.unitVideoDuration);


		if (index < Statistics.fileSizeDecisions.size())
			return Statistics.fileSizeDecisions.get((int) index);
		else return 0;
	}


	//make decisions for the queried time
	public double decisionAtTime(double decisionTime, boolean isChannelAware) {
		//To-Do: how to make decisions according to the bandwidth and delay information


		//To-Do: update decision LinkedList
		if (isChannelAware) return decisionAtTimeChannelAware(decisionTime);
		else return decisionAtTimeChannelAwareMaximizeResolution(decisionTime);


	}

	//delay only algorithm
	public double decisionAtTimeChannelAwareMaximizeResolution(double decisionTime) {


		//no need to track the historical predicted bandwidths
		//just look at the change of queue length is actually enough
		double predictedBandwidth = predictedBandwidth(decisionTime);



		double accumulatedDelayTimes = getAccumulatedDelayTimes();

		double remainingOverallDelayTimes = maxNumberOfVideos * Statistics.averageDelay - accumulatedDelayTimes;

//			System.out.print(decisionTime+": ");
//			System.out.println(Statistics.arrivalTimes);
//			System.out.println(Statistics.expectedArrivalTimes);

		double deadline = Statistics.expectedArrivalTimes.getLast() + Math.min(Statistics.unitVideoDelay, remainingOverallDelayTimes);

		double predictedThroughput = predictedBandwidth * (deadline - decisionTime);

		//To-Do: get the size of videos in the queue, and throughput between now and deadline, and make decisions for next video

		double existingFileSizes = 0;
		for (int i = 0; i < queue.size(); i++) {
			existingFileSizes += queue.get(i);
		}

		double availableBitrate = (predictedThroughput - existingFileSizes) / Statistics.unitVideoDuration;

//		System.out.print(availableBitrate + " ");

		if(resolutionChoiceIndex>0)
		if(availableBitrate<fileSizes[resolutionChoiceIndex]*0.9) {
			if (lastAddedTimeForLowerBitrate == -1 || (decisionTime - lastAddedTimeForLowerBitrate)<=10) {

				countLowerBitrate++;

			} else{
				countLowerBitrate = 1;

			}
			lastAddedTimeForLowerBitrate = decisionTime;
		}

		if(resolutionChoiceIndex<5)
		if(availableBitrate>fileSizes[resolutionChoiceIndex+1]*0.9){
			if (lastAddedTimeForHigherBitrate == -1 || (decisionTime - lastAddedTimeForHigherBitrate)<=10){

				countHigherBitrate++;

			}
			else{
				countHigherBitrate = 1;

			}
			lastAddedTimeForHigherBitrate = decisionTime;
		}

//		System.out.println("countHigherBitrate: "+countHigherBitrate + " countLowerBitrate: "+countLowerBitrate);

		if(countLowerBitrate>=5*(resolutionChoiceIndex+1)){
			countLowerBitrate = 0;
			lastAddedTimeForLowerBitrate = -1;
			if(resolutionChoiceIndex > 0) resolutionChoiceIndex--;
		}

		if(countHigherBitrate>=5*(resolutionChoiceIndex-1)){
			countHigherBitrate = 0;
			lastAddedTimeForHigherBitrate = -1;
			if(resolutionChoiceIndex < 5) resolutionChoiceIndex++;
		}

//		System.out.println("resolution choice index: "+resolutionChoiceIndex);



		Statistics.fileSizeDecisions.add(fileSizes[resolutionChoiceIndex] * Statistics.unitVideoDuration);
		Statistics.bitrateDecisions.add(resolutions[resolutionChoiceIndex]);



		return 0;
	}


	//channel aware algorithm
	public double decisionAtTimeChannelAware(double decisionTime) {
		//how to make decisions according to predicted bandwidth and delay information
		int slotOfDecisionTime = (int) Math.floor(decisionTime / Statistics.bandwidthTimeslotWidth);

		//Know-future approach
//		double predictedBandwidth = Statistics.inputBandwidths.get(slotOfDecisionTime+1);

		//our predictive approach
		double predictedBandwidth = predictedBandwidth(decisionTime);
//		System.out.println("predicted bandwidth: " + predictedBandwidth);

		double fileSizeDecision;
		int bitrateDecision;


		if(predictedBandwidth !=0)
//		if(false)
		{


			double accumulatedDelayTimes = getAccumulatedDelayTimes();

			double remainingOverallDelayTimes = maxNumberOfVideos * Statistics.averageDelay - accumulatedDelayTimes;

//			System.out.print(decisionTime+": ");
//			System.out.println(Statistics.arrivalTimes);
//			System.out.println(Statistics.expectedArrivalTimes);

			double deadline = Statistics.expectedArrivalTimes.getLast() + Math.min(Statistics.unitVideoDelay, remainingOverallDelayTimes);

			double predictedThroughput = predictedBandwidth * (deadline - decisionTime);

			//To-Do: get the size of videos in the queue, and throughput between now and deadline, and make decisions for next video

			double existingFileSizes = 0;
			for (int i = 0; i < queue.size(); i++) {
				existingFileSizes += queue.get(i);
			}

			double availableBitrate = (predictedThroughput - existingFileSizes) / Statistics.unitVideoDuration;

//			System.out.println("deadline: "+deadline+"available bitrate: "+availableBitrate);


//		if (availableBitrate >= Statistics._2160P) {
//			fileSizeDecision = Statistics._2160P;
//			bitrateDecision = 2160;
//		} else if (availableBitrate >= Statistics._1440P) {
//			fileSizeDecision = Statistics._1440P;
//			bitrateDecision = 1440;
//		} else


			//reduced available bitrate leads to higher achieved resolution and lower delays
			//but lower throughput utilization ratio
//			availableBitrate = availableBitrate/1.4;

			//the following decision method should be changed to make more consistent decisions
			//than before. choosing higher resolution requires higher available bitrate,
			//choosing lower resolution requires lower available bitrate.

			if (availableBitrate >= Statistics._1080P * 1.6) {
				fileSizeDecision = Statistics._1080P;
				bitrateDecision = 1080;
			} else if (availableBitrate >= Statistics._720P * 1.6) {
				fileSizeDecision = Statistics._720P;
				bitrateDecision = 720;
			} else if (availableBitrate >= Statistics._480P*0.7) {
				fileSizeDecision = Statistics._480P;
				bitrateDecision = 480;
			} else if (availableBitrate >= Statistics._360P*0.3) {
				fileSizeDecision = Statistics._360P;
				bitrateDecision = 360;
			} else if (availableBitrate >= Statistics._240P * 0.1) {
				fileSizeDecision = Statistics._240P;
				bitrateDecision = 240;
			} else {
				fileSizeDecision = Statistics._144P;
				bitrateDecision = 144;
			}

			Statistics.fileSizeDecisions.add(fileSizeDecision * Statistics.unitVideoDuration);
			Statistics.bitrateDecisions.add(bitrateDecision);

//			System.out.println("bitrate decision: "+ bitrateDecision);
		}
		else
		{
			fileSizeDecision = Statistics._360P;
			Statistics.fileSizeDecisions.add(Statistics._360P * Statistics.unitVideoDuration);
			Statistics.bitrateDecisions.add(360);
		}


		return fileSizeDecision*Statistics.unitVideoDuration;

	}


	//currently already delayed time
	public double getAccumulatedDelayTimes() {

		int numberOfTranmisttedVideos = Statistics.delayTimes.size();

		double accumulatedDelayTimes = 0;
		for (int i = 0; i < numberOfTranmisttedVideos; i++) {
			accumulatedDelayTimes += Statistics.delayTimes.get(i);
		}

		return accumulatedDelayTimes;
	}


	public double getThroughputOverTimeslot(double startTime, double endTime) {
		int startTimeslot = (int) Math.floor(startTime / Statistics.bandwidthTimeslotWidth);

		int endTimeslot = (int) Math.floor(endTime / Statistics.bandwidthTimeslotWidth);

		double throughput = 0;

		if (startTimeslot == endTimeslot)
			throughput = (endTime - startTime) * Statistics.inputBandwidths.get(endTimeslot);
		else {
			throughput += ((startTimeslot + 1) * Statistics.bandwidthTimeslotWidth - startTime)
					* Statistics.inputBandwidths.get(startTimeslot);
			throughput += (endTime - endTimeslot * Statistics.bandwidthTimeslotWidth)
					* Statistics.inputBandwidths.get(startTimeslot);

			//in case that there exist slots between first and end slots.			
			int numberOfSlotsBetween = endTimeslot - startTimeslot;
			for (int i = 1; i < numberOfSlotsBetween; i++) {
				throughput += Statistics.bandwidthTimeslotWidth * Statistics.inputBandwidths.get(startTimeslot + i);
			}
		}


		return throughput;
	}


	//when a video is transmitted, its arrival time and delay are updated accordingly. 
	public double updateArrivalAndDelayTime(double startTime, double sizeOfVideoAtTheHeadOfQueue) {
		int startTimeslot = (int) Math.floor(startTime / Statistics.bandwidthTimeslotWidth);

		//remaining file size if transmitted in the startTimeslot
		double remainingSize = sizeOfVideoAtTheHeadOfQueue - ((startTimeslot + 1) * 1.0 * Statistics.bandwidthTimeslotWidth - startTime)
				* Statistics.inputBandwidths.get(startTimeslot);

		int endTimeslot = startTimeslot;


		//until the endingTimeslot is found
		while (remainingSize >= 0) {
			endTimeslot = endTimeslot + 1;
			remainingSize = remainingSize - Statistics.bandwidthTimeslotWidth * Statistics.inputBandwidths.get(endTimeslot);
		}
		//add back the subtracted throughput in the while loop
		remainingSize = remainingSize + Statistics.bandwidthTimeslotWidth * Statistics.inputBandwidths.get(endTimeslot);


		//Different calculations for endTime for two different cases
		double endTime;
		if (endTimeslot == startTimeslot)
			endTime = startTime + remainingSize / Statistics.inputBandwidths.get(endTimeslot);
		else
			endTime = endTimeslot * Statistics.bandwidthTimeslotWidth + remainingSize / Statistics.inputBandwidths.get(endTimeslot);

		//add the actual arrival time of current video to the end of the arrivalTimes LinkedList
		Statistics.arrivalTimes.add(endTime);


		//Compare actual arrival time with expected arrival time to get the delay time
		double delayTime;
		if (endTime <= Statistics.expectedArrivalTimes.getLast())
			delayTime = 0;
		else
			delayTime = endTime - Statistics.expectedArrivalTimes.getLast();

		//add the delay time of current video to the end of the delayTimes LinkedList.
		Statistics.delayTimes.add(delayTime);

		//update expected deadline for the next arriving video (eventually, the size of this list may be 
		//larger than the maximum number of videos.
		if (Statistics.expectedArrivalTimes.size() < this.maxNumberOfVideos)
			Statistics.expectedArrivalTimes.add(Statistics.expectedArrivalTimes.getLast() + Statistics.unitVideoDuration + delayTime);


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


	public double predictedBandwidth(double currentTime) {
		//assumes that the system can track the achieved bandwidth at discrete time
		//e.g., every unit time, check the length of reduced data size
		int currentTimeslot = (int) Math.floor(currentTime / Statistics.bandwidthTimeslotWidth);

		if(currentTimeslot==0) return 0;

		int number = Math.min(currentTimeslot, slideWindow);

		double[] historicalBandwidths = new double[number];

		double sum = 0;
		for (int i = 1; i <= number; i++) {
			historicalBandwidths[i-1] = Statistics.inputBandwidths.get(currentTimeslot - i);
			sum += historicalBandwidths[i-1];
		}


		return sum / (number*Statistics.bandwidthTimeslotWidth);
	}


	public void updatePreprocessingIndexTable() {

		//preprocessing level = original resolution/output resolution
		//size reduction = original data size - output data size
		//there is a mapping between resolution and size


		//preprocessing efficiency sort order:
		//file size reduction/resolution reduction
		//how much file size can be reduced per unit resolution reduction


		//preprocessing level No., resolution reduction, file size reduction, efficiency
		//0, 0, 0
		//1080p
		//1, 1080-720=360,
		//2, 1080-480=600,
		//3, 1080-360=720,
		//4, 1080-240=840,
		//5, 1080-144=936,

		//720p
		//6, 720-480=240,
		//7, 720-360=360,
		//8,

		this.efficiency = new HashMap<>();

		efficiency.put(0, 0.0);

		this.resolutionReduction = new int[16];

		resolutionReduction[0] = 0;

		this.fileSizeReduction = new double[16];

		fileSizeReduction[0] = 0;

		this.indexTable = new HashMap<>();

		indexTable.put(0, 0.0);




		int initIndex = 0;
		for (int i = resolutions.length - 1; i >= 0; i--) {
			for (int j = i - 1; j >= 0; j--) {
				initIndex++;
				indexTable.put(initIndex, fileSizes[i]);

				resolutionReduction[initIndex] = resolutions[i] - resolutions[j];
				fileSizeReduction[initIndex] = (fileSizes[i] - fileSizes[j])*Statistics.unitVideoDuration;
				efficiency.put(initIndex, fileSizeReduction[initIndex] / resolutionReduction[initIndex]);
//				System.out.println(resolutionReduction[initIndex]+ " " + fileSizeReduction[initIndex]);
			}
		}
//		System.out.println(resolutionReduction.length + " " + fileSizeReduction.length);




	}


	public void preprocessVidesoInQueue(double currentTime, double v) {
		//L framework method: monitor queue length and bandwidth

		//iterate through all options for preprocessing, sort the answers by results
		//(current queue length - next timeslot data transmission) * fileSizeReduction - V * resolutionReduction
		//next timeslot data transmission is decided base on current queue length and fileSizeReduction
		double currentQueueLength = getQueueLength();

		double predictedBandwidth = predictedBandwidth(currentTime);

		HashMap<Integer, Double> results = new HashMap<>();

		for (int i = 0; i < fileSizeReduction.length; i++) {
			double currentFileSizeReduction = fileSizeReduction[i];
			int currentResolutionReduction = resolutionReduction[i];

			double nextTimeslotDataTransmission;
			if ((predictedBandwidth * Statistics.unitVideoDuration) < (currentQueueLength - currentFileSizeReduction))
				nextTimeslotDataTransmission = predictedBandwidth * Statistics.unitVideoDuration;
			else
				nextTimeslotDataTransmission = currentQueueLength - currentFileSizeReduction;

			double currentResult = (currentQueueLength - nextTimeslotDataTransmission) * currentFileSizeReduction - v * currentResolutionReduction;

			results.put(i, currentResult);
		}

		LinkedHashMap<Integer, Double> sortedResults = HashMapSorting.sortHashMapByValuesD(results);

//		System.out.println("sorted results: "+ sortedResults);

//		Map.Entry<Integer, Double> firstEntry = sortedResults.entrySet().iterator().next();
//		int bestResultsKey = firstEntry.getKey();

		//If the best way is not to preprocess, then return directly.
//		if(sortedResults.entrySet().iterator().next().getKey() == 0) return;


		//If preprocessing is considered better than non-preprocessing, do the following qualification process:
		//video selection criteria: according to suggested proprocessing policy, look for qualified unit videos in the queue

		for (Map.Entry<Integer, Double> entry : sortedResults.entrySet()) {
//			System.out.println(entry);
			//if the suggested is non-processing, then return directly
			if (entry.getKey() == 0) {
				Statistics.fileSizeChanges.add(0.0);
				Statistics.resolutionChanges.add(0);
//				System.out.println("suggested is nonpreprocessing.");
				return;
			}

			double targetFileSize = indexTable.get(entry.getKey()) * Statistics.unitVideoDuration;


			//when qualified videos are found, verify if there is sufficient time to process it before it is its turn to transmit
			//current stats: 1080p transcoding needs 6 times its original duraiton

			double accumulatedDataSize =0;
			for (int i = 0; i < queue.size(); i++) {
				accumulatedDataSize += queue.get(i);
//				System.out.println("queue:" + queue);
				if (queue.get(i) == targetFileSize)
					if(isPreprocessingThisVideoOK(indexTable.get(entry.getKey()), (accumulatedDataSize-queue.get(i)+fileSizeReduction[entry.getKey()])/predictedBandwidth))
					{
						Statistics.fileSizeChanges.add(fileSizeReduction[entry.getKey()]);
						Statistics.resolutionChanges.add(resolutionReduction[entry.getKey()]);

						queue.set(i, targetFileSize-fileSizeReduction[entry.getKey()]);
//						System.out.println("after:"+ queue.get(i));
//						System.out.println("change occurs: " + Statistics.fileSizeChanges.getLast() + " "+ Statistics.resolutionChanges.getLast());
						return;
					}
			}


		}

//		Statistics.fileSizeChanges.add(0.0);
//		Statistics.resolutionChanges.add(0);
//		System.out.println("no changes.");


		//if qualification passes, do the actual transcoding:
		//video transcoding according to suggested preprocessing policy


	}


	public boolean isPreprocessingThisVideoOK(double targetBitrate, double estimatedTransmissionTime) {
		double processingTime;

//		System.out.println("Target bitrate:" + targetBitrate);
		switch ((int)targetBitrate) {
			case (int)Statistics._144P:
				processingTime = 1 * Statistics.unitVideoDuration;
				break;
			case (int)Statistics._240P:
				processingTime = 2 * Statistics.unitVideoDuration;
				break;
			case (int)Statistics._360P:
				processingTime = 3 * Statistics.unitVideoDuration;
				break;
			case (int)Statistics._480P:
				processingTime = 4 * Statistics.unitVideoDuration;
				break;
			case (int)Statistics._720P:
				processingTime = 5  * Statistics.unitVideoDuration;
				break;
			case (int)Statistics._1080P:
				processingTime = 6  * Statistics.unitVideoDuration;
				break;
			default:
				processingTime = -1;
				System.out.println("Proprocessing time abnormal");
				break;
		}

//		System.out.println("processing time:" + processingTime + "needed time:" + estimatedTransmissionTime);


		if(processingTime >= estimatedTransmissionTime) return false;
		else return true;


	}

}

