package zones;
import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import simulation.Bot;
import simulation.BotInfo;
import simulation.Message;
import simulation.Shout;
import simulation.Survivor;
import simulation.SurvivorPath;
import simulation.World;
import util.shapes.Circle2D;


public class BaseZone extends Zone {

	private static final long serialVersionUID = 1L;
	private final static Color BaseZoneColor = new Color(0, 100, 0);
	private List<Message> messageBuffer;
	private List<SurvivorPath> survivorPaths;

	public BaseZone(int[] xPoints, int[] yPoints, int numPoints, int _zoneID) {
		this(xPoints, yPoints, numPoints, _zoneID, BaseZoneColor);
	}

	protected BaseZone(int[] xPoints, int[] yPoints, int numPoints, int _zoneID, Color _zoneColor) {
		super(xPoints, yPoints, numPoints, _zoneID, _zoneColor);
		messageBuffer = new ArrayList<Message>();
		survivorPaths = new ArrayList<SurvivorPath>();
	}


	//	public BaseZone(Area a, int _zoneid) {
	//		super(a, _zoneid, BaseZone.BaseZoneColor);
	//	}

	@Override
	public boolean isObstacle() {
		return false;
	}


	private boolean recieveMessages = true;

	public synchronized void recieveMessage(Message message) throws InterruptedException {
		//bad way to do this, but it'll be OK
		while(!recieveMessages) {
			wait(10);
		}
		messageBuffer.add(message);
	}

	public void readMessages() {
		//stop recieving of messages
		recieveMessages = false;
		//get a copy of the array
		ArrayList<Message> messageBufferCopy = new ArrayList<Message>(messageBuffer);
		//clear the message buffer
		messageBuffer.clear();
		//start recieving messages again
		recieveMessages = true;
		
		//make a scanner to make going through the messages a bit easier
		Scanner s;
		//go through the messages and update the stored info about the other bots
		for(Message mes : messageBufferCopy) {

			s = new Scanner(mes.getText());

			if(! s.hasNext()) continue;

			//only interested in survivor path messages
			if(s.next().equals("fv")) {
				//extract all of he information about the path
				double vicStatus = s.nextDouble();
				double vicX = s.nextDouble();
				double vicY = s.nextDouble();
				double pathLength = s.nextDouble();
				double pathRating = s.nextDouble();
				double avgRating = s.nextDouble();

				List<BotInfo> pathBots = new ArrayList<BotInfo>();

				while(s.hasNextInt()) {
					pathBots.add(new BotInfo(s.nextInt(), s.nextDouble(), s.nextDouble(), s.nextInt()));
				}

				//make a new survivorPath and add it to our list
				//First, find the actual survivor that this message refers to
				Survivor vic = World.allSurvivors.get(World.allSurvivors.indexOf(new Survivor(vicX, vicY, vicStatus)));

				//now, make the path and add it to our list
				survivorPaths.add(new SurvivorPath(vic, pathLength, pathRating, avgRating, pathBots, this.getCenterLocation()));				
			} else continue;

		}
	}

	public void clearMessageBuffer() {
		messageBuffer.clear();
	}


	public List<SurvivorPath> getSurvivorPaths() {
		//first, check to make sure we don't have any waiting in the message buffer
		readMessages();
		//now, return the list of paths
		return survivorPaths;
	}


	public List<SurvivorPath> getBestSurvivorPaths() {
		//first, get all the survivor paths
		List<SurvivorPath> allPaths = getSurvivorPaths();

		List<SurvivorPath> bestPaths = new ArrayList<SurvivorPath>();

		//now, pick out the best ones
		for(int allIndex = 0; allIndex < allPaths.size(); allIndex++) {

			SurvivorPath curPath = allPaths.get(allIndex);

			boolean foundPathToSameVic = false;

			for(int bestIndex = 0; bestIndex < bestPaths.size(); bestIndex++) {
				SurvivorPath curFromBest = bestPaths.get(bestIndex);

				if(curPath.getSur().equals(curFromBest.getSur())) {
					foundPathToSameVic = true;

					if(curPath.getAvgRating() < curFromBest.getAvgRating()) {
						bestPaths.remove(bestIndex);
						bestPaths.add(curPath);
					}
					break;
				}
			}

			if(! foundPathToSameVic) {
				bestPaths.add(curPath);
			}
		}

		return bestPaths;
	}

	@Override
	public double getAudibleRange(Point2D originator) {
		return Bot.DEFAULT_AUDITORY_RADIUS;
	}

	@Override
	public double getBroadcastRange(Point2D originator) {
		return Bot.DEFAULT_BROADCAST_RADIUS;
	}

	@Override
	public double getFoundRange(Point2D originator) {
		return Bot.DEFAULT_FOUND_RANGE;
	}

	@Override
	public double getVisiblityRange(Point2D originator) {
		return Bot.DEFALUT_VISIBILITY_RADIUS;
	}

	@Override
	public Shout getShout(Survivor shouter) {
		return new Shout(new Circle2D(shouter.getCenterLocation(), Shout.DEFAULT_SHOUT_RADIUS), shouter);
	}
}
