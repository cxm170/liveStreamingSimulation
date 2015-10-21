package com.kiwi.livestreaming;

import java.util.Random;

public class NetworkTraces {


	
	private Random random;
	
	private int[] arr;
	private int[] freq;
	
	public NetworkTraces(int[] arr, int[] freq){

		this.random = new Random();
		this.arr = arr;
		this.freq = freq;
	};
	
//	public int[] generateTraces(){
//		int[] generatedTraces = new int[timeslotNumber];
//		
//		for(int i=0;i<timeslotNumber;i++){
//			generatedTraces[i] = getRandomBandwidth();
//		}
//		
//		return generatedTraces;
//	}
	
//	private int getRandomBandwidth(){
//		
//	}
	
	
	

	 
	// Utility function to find ceiling of r in arr[l..h]
	private int findCeil(int arr[], int r, int l, int h)
	{
	    int mid;
	    while (l < h)
	    {
	         mid = l + ((h - l) >> 1);  // Same as mid = (l+h)/2
	        if (r > arr[mid]) l = mid + 1; else h = mid;
	    }
	    return (arr[l] >= r) ? l : -1;
	}
	 
	 // The main function that returns a random number from arr[] according to
	// distribution array defined by freq[]. n is size of arrays.
	private int myRand()
	{
		
		int n = arr.length;
	    // Create and fill prefix array
	    int[] prefix = new int[n]; 
	    int i;
	    prefix[0] = freq[0];
	    for (i = 1; i < n; ++i)
	        prefix[i] = prefix[i - 1] + freq[i];
	 
	    // prefix[n-1] is sum of all frequencies. Generate a random number
	    // with value from 1 to this sum
	    int r = random.nextInt(prefix[n - 1])+1;
	 
	    // Find index of ceiling of r in prefix arrat
	    int indexc = findCeil(prefix, r, 0, n - 1);
	    return arr[indexc];
	}
	
	
	public void generateBandwidth(int number){
		for(int i=0;i<number;i++){
			Statistics.inputBandwidths.add(1.0*myRand());


		}
		
	}
	
	
//	public static void main(String [] args){
//		 int arr[]  = {1, 2, 3, 4};
//		    int freq[] = {1, 2, 3, 4};
//		    int i, n = arr.length;
//		 
//		    int all = 10000;
//		    NetworkTraces networkTraces = new NetworkTraces(arr, freq);
//		 
//		    // Let us generate 10 random numbers accroding to
//		    // given distribution
//		    
//		    int result;
//		    int count[] = {0,0,0,0};
//		    for (i = 0; i < all; i++){
//		    	result = networkTraces.myRand();
////		      System.out.print(result +" ");
//		      switch(result){
//		      case 1: count[0]++; break;
//		      case 2: count[1]++; break;
//		      case 3: count[2]++; break;
//		      case 4: count[3]++; break;
//		      }
//		    }
//		    System.out.println("");
//		    
//		    for (i = 0; i < 4; i++)
//		    System.out.println(count[i]*1.0/all*100 + " " + freq[i]*1.0/10*100);
//		 
//		    
//	
//	}
	
	
	
	public static void main(String[] args){
		int arr[]  = {200, 400, 800, 1000, 1500, 2000, 3000, 5000}; //unit: kbits/second
		int freq[] = {13, 16, 12, 6, 4, 2, 2, 1};
	    
	    
	    int sum =0;
	    int number =0;
	    for(int i=0;i<arr.length;i++){
	    	sum += arr[i]*freq[i];
	    	number += freq[i];
	    }
	    
	    double average = sum*1.0/number;
	    
	    double variance = 0.0;
	    for(int i=0;i<arr.length;i++){
	    	variance += 1.0*(average-arr[i])*(average-arr[i])*freq[i];
	    	
	    }
	    
	    variance = variance/number;
	    
	    System.out.println("Sum: " + sum);
	    System.out.println("Number: " + number);
	    System.out.println("Average: " + average);
	    System.out.println("StdDev: " + Math.sqrt(variance));
	    
	    
	    int accumFre = 0;
	    for(int i=0;i<arr.length;i++){
	    	accumFre += freq[i];
	    	System.out.println(accumFre*1.0/number);
	    	
	    }
	}
	
}
