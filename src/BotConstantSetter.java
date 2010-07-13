import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Random;


public class BotConstantSetter {

	static long bestTime;
	static double bestBotRepulsion;
	static double bestHomeRepulsion;
	
	static double curBotRepulsion;
	static double curHomeRepulsion;
	static long curTime;
	
	static double prevBotRepulsion;
	static double prevHomeRepulsion;
	static long prevTime;
	
	static int stepsSinceBotChanged;
	static int stepsSinceHomeChanged;
	
	static int numStepsUnchangedBeforeReset = 5;
	static int maxRepulsionValue = 1000000000;
	
	
	static DecimalFormat df;
	
	static Random numGen;
	
	
	public static void main(String[] args) {
		
		df = new DecimalFormat("000000.#");
		df.setRoundingMode(RoundingMode.HALF_DOWN);
		
		numGen = new Random();
		
		//set initial best values
		bestTime = Long.MAX_VALUE;
		bestBotRepulsion = Double.MAX_VALUE;
		bestHomeRepulsion = Double.MAX_VALUE;
		
		//start with random values 5 times
		for(int i = 0; i < 5; i++) {
			newRandomStartValues();

			//see if any of them are better than what we have
			seeIfCurValuesAreBest();
			
			while(stepsSinceBotChanged < numStepsUnchangedBeforeReset && stepsSinceHomeChanged < numStepsUnchangedBeforeReset) {
//				boolean movingInRightDirection = curTime < prevTime;
				
				long timeDif = curTime - prevTime;
				
				//calculate how far we're going to move
				double botSlope = timeDif / (curBotRepulsion - prevBotRepulsion);
				//make sure we want to move in that direction - switch if we don't
//				if(! movingInRightDirection) deltaBot *= -1.0;
				
				prevBotRepulsion = curBotRepulsion;
				curBotRepulsion = Double.valueOf(df.format(curBotRepulsion + botSlope));
				
				//do it again with the home repulsion
				double homeSlope = timeDif / (curHomeRepulsion - prevHomeRepulsion);
//				if(! movingInRightDirection) deltaHome *= -1.0;
				
				prevHomeRepulsion = curHomeRepulsion;
				Double.valueOf(df.format(curHomeRepulsion = curHomeRepulsion + homeSlope));
				
				prevTime = curTime;
				curTime = getTime(curBotRepulsion, curHomeRepulsion);
				
				seeIfCurValuesAreBest();
				
				if(prevBotRepulsion == curBotRepulsion) stepsSinceBotChanged++;
				else stepsSinceBotChanged = 0;
				
				if(prevHomeRepulsion == curHomeRepulsion) stepsSinceHomeChanged++;
				else stepsSinceHomeChanged = 0;
				
				System.out.println("Current: (" + curBotRepulsion + ", " + curHomeRepulsion + ", " + curTime + ")\tPrev: (" + prevBotRepulsion + ", " + prevHomeRepulsion + ", " + prevTime + ")\tBest: (" + bestBotRepulsion + ", " + bestHomeRepulsion + ", " + bestTime + ")");				
				
				if(curBotRepulsion > prevBotRepulsion) System.out.print("bot increasing\t");
				else System.out.print("bot decreasing\t");
				
				if(curHomeRepulsion > prevHomeRepulsion) System.out.println("home increasing");
				else System.out.println("home decreasing");
				
				if(curTime > prevTime) System.out.println("time increasing");
				else System.out.println("time decreasing");
				
			}
		}
		
		System.out.println("Overall Best: (" + bestBotRepulsion + ", " + bestHomeRepulsion + ", " + bestTime + ")");
	}
	
	public static void newRandomStartValues() {
		System.out.println("RESETTING!!!");
		
		//start off with random values
		curBotRepulsion = Double.valueOf(df.format(numGen.nextDouble() * maxRepulsionValue));
		curHomeRepulsion = Double.valueOf(df.format(numGen.nextDouble() * maxRepulsionValue));
		curTime = getTime(curBotRepulsion, curHomeRepulsion);
		
		//set the previous values in a reasonable manner - put it about 100 away
		prevBotRepulsion = Double.valueOf(df.format(curBotRepulsion + 100));
		prevHomeRepulsion = Double.valueOf(df.format(curHomeRepulsion + 100));
		prevTime = getTime(prevBotRepulsion, prevHomeRepulsion);
		
		stepsSinceBotChanged = 0;
		stepsSinceHomeChanged = 0;
		
		System.out.println("Current: (" + curBotRepulsion + ", " + curHomeRepulsion + ", " + curTime + ")\tPrev: (" + prevBotRepulsion + ", " + prevHomeRepulsion + ", " + prevTime + ")");
	}
	
	public static void seeIfCurValuesAreBest() {
		if(curTime < bestTime) {
			bestTime = curTime;
			bestBotRepulsion = curBotRepulsion;
			bestHomeRepulsion = curHomeRepulsion;
		}
	}
	
	public static long getTime(double botRepulsion, double homeRepulsion) {
		long avgTime = 0;
		
		//take 3 samples
		for(int i = 1; i <= 3; i++ ) {
			World w = new World(botRepulsion, homeRepulsion);

			long startTime = System.currentTimeMillis();
			w.go();
			long stopTime = System.currentTimeMillis();
			w.dispose();
			
			long timeElapsed = stopTime - startTime;
			
			avgTime = avgTime + (timeElapsed - avgTime)/i;
		}
		
		return avgTime;
	}
	
	
}
