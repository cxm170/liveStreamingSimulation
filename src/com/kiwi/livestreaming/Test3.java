   package com.kiwi.livestreaming;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.LinkedList;

   public class Test3 {
       public static void main(String[] args){

		   double v = 100000000;

//           double initDelayRatio = 0.03;

           double averageDelay;

           int slideWindow = 30;

//           for(double k=0;k<=10;k=k+0.25)
			for(double k = 0; k<=2; k=k+0.1)
   //		for(double k=0;k<=1.2;k=k+0.05)
           {
               averageDelay = k;

           int simulationTimes =99;

           int maxNumberOfVideos = 600; //times 10 seconds is the overall duration

           double utilization = 0;

           double delay = 0;

           double resolution = 0;

               double variance = 0;
               double overDelayRatio = 0;

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



           double initDelay = 30;


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
			   Statistics.resolutionChanges.clear();
			   Statistics.fileSizeChanges.clear();



           NetworkTraces networkTraces = new NetworkTraces(arr, freq);

           networkTraces.generateBandwidth((int) (2*(maxNumberOfVideos*(unitVideoDuration+averageDelay)+initDelay)/bandwidthTimeslotWidth));

			   TransmissionQueuePreprocessing transmissionQueue = new TransmissionQueuePreprocessing(maxNumberOfVideos, unitVideoDuration, bandwidthTimeslotWidth,
                   initDelay, averageDelay, unitVideoDelay, slideWindow);


           //for loop to simulate the process of live streaming
           //make decisions only from starting time to the ending time of maxNumberOfVideos*unitVideoDuration
           //queue update until the queue is empty
           int indexOfDecisionMakingTimes = 0;
           double currentTime = 0;

           //the first time to make decisions
           transmissionQueue.decisionAtTime(currentTime, false);

           LinkedList<Double> queue = transmissionQueue.getQueue();

   		int flagRound = 1;

           while(queue.size()!=0 || indexOfDecisionMakingTimes < maxNumberOfVideos){
               currentTime += Statistics.unitVideoDuration;
               transmissionQueue.updateQueueToCurrentTime(currentTime-Statistics.unitVideoDuration, currentTime);



               indexOfDecisionMakingTimes++;

               if(indexOfDecisionMakingTimes < maxNumberOfVideos){
                   transmissionQueue.decisionAtTime(currentTime, false);
               }

//   			System.out.print(flagRound+": ");
//   			System.out.print("queue size: "+queue.size()+", ");
//   			if(indexOfDecisionMakingTimes < maxNumberOfVideos) System.out.print("decision: "+ Statistics.bitrateDecisions.getLast()+", ");
//   			if(queue.size()>0)System.out.print("elements: "+queue+", ");
//   			System.out.print("bandiwidth: "+Statistics.inputBandwidths.get(flagRound));
//   			System.out.println("");

//			   transmissionQueue.preprocessVidesoInQueue(currentTime, v);


//			     if(queue.size()>0)System.out.println("elements: "+queue+", ");



   			flagRound++;
           }

//   		System.out.println(Statistics.fileSizeDecisions);
//               System.out.println(Statistics.fileSizeDecisions.size());
//   		System.out.println(Statistics.delayTimes.size());
//
//   		System.out.println(Statistics.arrivalTimes.size());
//   		System.out.println(Statistics.expectedArrivalTimes.size());

//			   System.out.println(Statistics.resolutionChanges.size());
//			   System.out.println(Statistics.fileSizeChanges.size());

//			   System.out.println(Statistics.resolutionChanges);
//			   System.out.println(Statistics.fileSizeChanges);


           //Upper bound of throughput that can be achieved during live streaming
           double endingTime = Statistics.arrivalTimes.getLast();
           int endTimeslot = (int) Math.floor(endingTime/Statistics.bandwidthTimeslotWidth);
           double upperBoundOfThroughput = 0;
           for(int i =0; i < endTimeslot; i++){
               upperBoundOfThroughput += Statistics.inputBandwidths.get(i)* Statistics.bandwidthTimeslotWidth;
           }
//   		System.out.println("Upper bound of throughput: " + upperBoundOfThroughput);


           //achieved throughput of live streaming using the algorithm
           double achievedOverallThroughput = 0;
           for(int i =0; i < maxNumberOfVideos; i++){
               achievedOverallThroughput += Statistics.fileSizeDecisions.get(i);
           }



			for(int i=0; i < Statistics.fileSizeChanges.size(); i++){
				achievedOverallThroughput -= Statistics.fileSizeChanges.get(i);
			}
//			   System.out.println("Achieved average throughput: " + achievedOverallThroughput/maxNumberOfVideos);



           //show the utilization ratio
//   		System.out.println("Ratio of utilization: " + achievedOverallThroughput/upperBoundOfThroughput);


           //the actual average delay time achieved
           double overallDelayTimes = 0;
               int overdelayCount = 0;
           for(int i=0;i<Statistics.delayTimes.size();i++){
               overallDelayTimes += Statistics.delayTimes.get(i);
               if(Statistics.delayTimes.get(i)>unitVideoDelay) overdelayCount++;
           }

//   		System.out.println("Average Delay Time (constraint and actual): " + averageDelay + " " + overallDelayTimes/Statistics.delayTimes.size());

               double averageDelayTimeAchieved = overallDelayTimes/Statistics.delayTimes.size();
               double delayVariance = 0;
               for(int i=0;i<Statistics.delayTimes.size();i++){
                  delayVariance += (Statistics.delayTimes.get(i)-averageDelayTimeAchieved) * (Statistics.delayTimes.get(i)-averageDelayTimeAchieved);
               }
               delayVariance = Math.sqrt(delayVariance / Statistics.delayTimes.size());
//               System.out.println("delay variance: " + delayVariance);
//               System.out.println("Over delay times: " + overdelayCount + " ratio: "+overdelayCount*1.0/Statistics.delayTimes.size());

           //the actual average resolution achieved
           double resolutionSum = 0;
           for(int i=0;i<Statistics.bitrateDecisions.size();i++){
               resolutionSum += Statistics.bitrateDecisions.get(i);
           }

			for(int i=0; i<Statistics.resolutionChanges.size();i++){
				resolutionSum -= Statistics.resolutionChanges.get(i);
			}

//			   System.out.println("Average achieved resolution:" + resolutionSum/Statistics.bitrateDecisions.size());

           utilization += achievedOverallThroughput/upperBoundOfThroughput;
           delay += overallDelayTimes/Statistics.delayTimes.size();
           resolution += resolutionSum/Statistics.bitrateDecisions.size();

           averageAchievedThroughput += achievedOverallThroughput/Statistics.fileSizeDecisions.size();

            variance += delayVariance;
               overDelayRatio += overdelayCount*1.0/Statistics.delayTimes.size();

           }

//           System.out.println("Average utilization ratio: " + utilization/(simulationTimes+1));
//           System.out.println("Average delay: " + delay/(simulationTimes+1));
//           System.out.println("Average resolution: " + resolution/(simulationTimes+1));
//           System.out.println("Average throughput: " + averageAchievedThroughput/(simulationTimes+1) + " expected resolution: " + getResolution(averageAchievedThroughput/(simulationTimes+1)));
//            System.out.println("Average variance: " + variance/(simulationTimes+1));
//               System.out.println("Average over delay ratio: " + overDelayRatio/(simulationTimes+1));




           String title ="***simulationTimes: " + (simulationTimes+1) + "\r\n" + "***InitDelay: " + Statistics.initDelay + " seconds\r\n"
                   +"***averageDelay: " + Statistics.averageDelay + " seconds\r\n"
                   + "***unitVideoDelay: " + Statistics.unitVideoDelay + " seconds\r\n"
                   + "***maxNumberOfVideos: " + maxNumberOfVideos + "\r\n";

   //		try {
   //			Files.write(Paths.get("output.txt"), title.getBytes(), StandardOpenOption.APPEND);
   //		} catch (IOException e) {
   //			// TODO Auto-generated catch block
   //			e.printStackTrace();
   //		}

           String output = "Average utilization ratio: " + round(utilization/(simulationTimes+1),4) + "\r\n" +
                           "Average delay: " + round(delay/(simulationTimes+1),3) + "\r\n" +
                           "Average resolution: " + round(resolution/(simulationTimes+1),2) + "\r\n" +
                           "Average throughput: " + round(averageAchievedThroughput/(simulationTimes+1),2) +  "\r\n" +
                           "Expected resolution: " + getResolution(averageAchievedThroughput/(simulationTimes+1)) +"\r\n" +
                   "Average variance: " + round(variance/(simulationTimes+1),3)+  "\r\n"+
                   "Average over delay ratio: " + round(overDelayRatio/(simulationTimes+1),3)+
                   "\r\n\r\n";

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
			availableBitrate = availableBitrate/Statistics.unitVideoDuration;

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
