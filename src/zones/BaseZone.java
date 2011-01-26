package zones;
import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import simulation.Bot;
import simulation.Message;
import simulation.Shout;
import simulation.Survivor;
import simulation.SurvivorPath;
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

			if(mes.getType().equals(Bot.CREATE_PATH_MESSAGE)) {
				//read the message and add the path it has to our list of paths
				//TODO really should have a static method (in the Message class?) that does this
				//TODO message types should be in the Message class along with the construct methods

				//read out the survivor
				Survivor pathSur = new Survivor(s.nextDouble(), s.nextDouble(), s.nextDouble());
				//read out each of the points
				List<Point2D> pathPoints = new ArrayList<Point2D>();
				while(s.hasNextDouble()) {
					Point2D nextPathPoint = new Point2D.Double(s.nextDouble(), s.nextDouble());
					pathPoints.add(nextPathPoint);
				}

				//make a path out of it
				SurvivorPath sp = new SurvivorPath(pathSur, pathPoints);

				//store that path if we haven't already
				survivorPaths.add(sp);
			}
		}
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

					if(curPath.getPathLength() < curFromBest.getPathLength()) {
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
	public double getAudibleRange() {
		return Bot.DEFAULT_AUDITORY_RADIUS;
	}

	@Override
	public double getBroadcastRange() {
		return Bot.DEFAULT_BROADCAST_RADIUS;
	}

	@Override
	public double getFoundRange() {
		return Bot.DEFAULT_FOUND_RANGE;
	}

	@Override
	public double getVisiblityRange() {
		return Bot.DEFAULT_VISIBILITY_RADIUS;
	}

	@Override
	public Shout getShout(Survivor shouter) {
		return new Shout(new Circle2D(shouter.getCenterLocation(), Shout.DEFAULT_SHOUT_RADIUS), shouter);
	}

	@Override
	public boolean causesRepulsion() {
		return false;
	}

	@Override
	public double repulsionMinDist() {
		return 0;
	}

	@Override
	public double repulsionMaxDist() {
		return 0;
	}

	@Override
	public double repulsionCurveShape() {
		return 0;
	}
	
}
