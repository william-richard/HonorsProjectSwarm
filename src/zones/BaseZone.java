package zones;
import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import simulation.Bot;
import simulation.BotInfo;
import simulation.Shout;
import simulation.Victim;
import simulation.VictimPath;
import simulation.World;


public class BaseZone extends Zone {
	
	private static final long serialVersionUID = 1L;
	private final static Color BaseZoneColor = new Color(0, 100, 0);
	private String messageBuffer;
	private List<VictimPath> victimPaths;
	
	public BaseZone(int[] xPoints, int[] yPoints, int numPoints, int _zoneID) {
		this(xPoints, yPoints, numPoints, _zoneID, BaseZoneColor);
	}
	
	protected BaseZone(int[] xPoints, int[] yPoints, int numPoints, int _zoneID, Color _zoneColor) {
		super(xPoints, yPoints, numPoints, _zoneID, _zoneColor);
		messageBuffer = "";
		victimPaths = new ArrayList<VictimPath>();
	}
	
	
//	public BaseZone(Area a, int _zoneid) {
//		super(a, _zoneid, BaseZone.BaseZoneColor);
//	}

	@Override
	public Shape getAudibleRange(Point2D originator) {
		//in this case, return a circle
		//know the center of the circle, and the radius - need to find the corner
		double broadcastRangeCornerX = originator.getX() - Bot.DEFAULT_AUDITORY_RADIUS;
		double broadcastRangeCornerY = originator.getY() - Bot.DEFAULT_AUDITORY_RADIUS;

		//now, make the broadcast range shape
		return new Ellipse2D.Double(broadcastRangeCornerX, broadcastRangeCornerY, Bot.DEFAULT_AUDITORY_RADIUS*2, Bot.DEFAULT_AUDITORY_RADIUS*2);
	}

	@Override
	public Shape getBroadcastRange(Point2D originator) {
		//in this case, return a circle
		//know the center of the circle, and the radius - need to find the corner
		double broadcastRangeCornerX = originator.getX() - Bot.DEFAULT_BROADCAST_RADIUS;
		double broadcastRangeCornerY = originator.getY() - Bot.DEFAULT_BROADCAST_RADIUS;

		//now, make the broadcast range shape
		return new Ellipse2D.Double(broadcastRangeCornerX, broadcastRangeCornerY, Bot.DEFAULT_BROADCAST_RADIUS*2, Bot.DEFAULT_BROADCAST_RADIUS*2);
	}

	@Override
	public Shout getShout(Victim shouter) {
		//for now, the shout is a circle of the default radius

		//calculate it's corner
		double cornerX = shouter.getCenterX() - Shout.DEFAULT_SHOUT_RADIUS;
		double cornerY = shouter.getCenterY() - Shout.DEFAULT_SHOUT_RADIUS;

		//return the circular shout
		return new Shout(new Ellipse2D.Double(cornerX, cornerY, Shout.DEFAULT_SHOUT_RADIUS*2, Shout.DEFAULT_SHOUT_RADIUS*2), shouter);	
	}

	@Override
	public Shape getVisibilityRange(Point2D originator) {
		//in this case, return a circle
		//know the center of the circle, and the radius - need to find the corner
		double broadcastRangeCornerX = originator.getX() - Bot.DEFALUT_VISIBILITY_RADIUS;
		double broadcastRangeCornerY = originator.getY() - Bot.DEFALUT_VISIBILITY_RADIUS;

		//now, make the broadcast range shape
		return new Ellipse2D.Double(broadcastRangeCornerX, broadcastRangeCornerY, Bot.DEFALUT_VISIBILITY_RADIUS*2, Bot.DEFALUT_VISIBILITY_RADIUS*2);
	}

	@Override
	public Shape getFoundRange(Point2D originator) {
		//in this case, return a circle
		//know the center of the circle, and the radius - need to find the corner
		double foundRangeCornerX = originator.getX() - Bot.DEFAULT_FOUND_RANGE;
		double foundRangeCornerY = originator.getY() - Bot.DEFAULT_FOUND_RANGE;

		//now, make the broadcast range shape
		return new Ellipse2D.Double(foundRangeCornerX, foundRangeCornerY, Bot.DEFAULT_FOUND_RANGE*2, Bot.DEFAULT_FOUND_RANGE*2);
	}

	@Override
	public double getBotMaxVelocity() {
		return Bot.DEFAULT_MAX_VELOCITY;
	}

	private boolean recieveMessages = true;

	public void recieveMessage(String message) throws InterruptedException {
		//bad way to do this, but it'll be OK
		while(!recieveMessages) {
			wait(10);
		}
		messageBuffer = messageBuffer + message;
	}

	public void readMessages() {
		//make sure that we don't recieve any messages while we're coping the buffer
		recieveMessages = false;

		String newMessages = messageBuffer;
		messageBuffer = "";

		recieveMessages = true;

		//go through all the messages
		//messages should be split up by '\n'
		String[] messageArray = newMessages.split("\n");

		//make a scanner to make going through the messages a bit easier
		Scanner s;
		//go through the messages and update the stored info about the other bots
		for(String mes : messageArray) {

			s = new Scanner(mes);

			if(! s.hasNext()) continue;

			//only interested in victim path messages
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
				
				//make a new VictimPath and add it to our list
				//First, find the actual victim that this message refers to
				Victim vic = World.allVictims.get(World.allVictims.indexOf(new Victim(vicX, vicY, vicStatus)));
				
				//now, make the path and add it to our list
				victimPaths.add(new VictimPath(vic, pathLength, pathRating, avgRating, pathBots, this.getCenterLocation()));				
			} else continue;

		}
	}
	
	
	public List<VictimPath> getVictimPaths() {
		//first, check to make sure we don't have any waiting in the message buffer
		readMessages();
		//now, return the list of paths
		return victimPaths;
	}
	
	
	public List<VictimPath> getBestVictimPaths() {
		//first, get all the victim paths
		List<VictimPath> allPaths = getVictimPaths();
		
		List<VictimPath> bestPaths = new ArrayList<VictimPath>();
		
		//now, pick out the best ones
		for(int allIndex = 0; allIndex < allPaths.size(); allIndex++) {
			
			VictimPath curPath = allPaths.get(allIndex);
			
			boolean foundPathToSameVic = false;
			
			for(int bestIndex = 0; bestIndex < bestPaths.size(); bestIndex++) {
				VictimPath curFromBest = bestPaths.get(bestIndex);
				
				if(curPath.getVic().equals(curFromBest.getVic())) {
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
}
