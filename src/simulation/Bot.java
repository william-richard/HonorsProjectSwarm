package simulation;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

import zones.BaseZone;
import zones.BoundingBox;
import zones.Building;
import zones.Fire;
import zones.SafeZone;
import zones.Zone;


public class Bot extends Rectangle implements Runnable {

	private static final long serialVersionUID = 1L;

	/***************************************************************************
	 * CONSTANTS
	 **************************************************************************/
	private final int DIMENSION = 6;
	private final double VISUAL_ID_VICTIM_PROB = .70;
	private final double HEAR_VICTIM_PROB = .75;
	private final double MOVE_RANDOMLY_PROB = .25;
	private final double ASSES_VICTIM_CORRECTLY_PROB = .9;
	private final double CORRECT_ZONE_ASSESMENT_PROB = .5; //the probability that the bot will asses the zones correctly

	public static final double DEFAULT_OUTDOOR_BROADCAST_RADIUS = 95;
	public static final double DEFAULT_INDOOR_BROADCAST_RADIUS = 32;
	public static final double DEFALUT_VISIBILITY_RADIUS = 10;
	public static final double DEFAULT_AUDITORY_RADIUS = 50;
	public static final double DEFAULT_AUDITORY_RADIS_THROUGH_WALL = 20;
	public static final double DEFAULT_FOUND_RANGE = DEFALUT_VISIBILITY_RADIUS;
	public static final double DEFAULT_MAX_VELOCITY = 8;

	public static final double RADIAL_SWEEP_INCREMENT_DEGREES = 2.0;
	public static final double RADIAL_SWEEP_INCREMENT_RADIANS = RADIAL_SWEEP_INCREMENT_DEGREES * Math.PI / 180.0;


	private final int ZONE_SAFE = 1;
	private final int ZONE_DANGEROUS = 2;

	private final double DANGER_MULTIPLIER = 2;

	private static double REPULSION_FACTOR_FROM_OTHER_BOTS = 800;
	private static double REPULSION_FACTOR_FROM_HOME_BASES = 2500;


	private boolean OVERALL_BOT_DEBUG = 	true;
	private boolean LISTEN_BOT_DEBUG = 		false;
	private boolean LOOK_BOT_DEBUG = 		false;
	private boolean MESSAGE_BOT_DEBUG = 	false;
	private boolean MOVE_BOT_DEBUG = 		false;
	private boolean FIND_VICTIM_DEBUG = 	false;


	/***************************************************************************
	 * VARIABLES
	 **************************************************************************/

	/** These variables the bot does not know about - we store them for our convience. */
	private Zone currentZone; //what zones we actually are in can change some behavior
	private List<Shout> heardShouts; //the shouts that have been heard recently
	private Bot previousBot;
	private final Random numGen = new Random();
	private Vector movementVector;
	private BoundingBox boundingBox;


	private List<BotInfo> otherBotInfo; //storage of what information we know about all of the other Bots
	private String messageBuffer; //keep a buffer of messages from other robots
	private boolean keepGoing; //allows us to start or stop the robots
	private int botID;
	private int zoneAssesment; //stores the bot's assesment of what sort of zones it is in
	private Zone baseZone; //the home base zones.
	private HashMap<Victim, java.lang.Double> knownVicitms; //keep a list of map we have been told about, so we don't duplicate efforts.  Values are the avg rating of the best path we've seen to them

	/***************************************************************************
	 * CONSTRUCTORS
	 **************************************************************************/
	public Bot(double centerX, double centerY, int _numBots, int _botID, Zone homeBase, BoundingBox _bounds) {
		super();

		//first, in order to store our location, we need to find our top left corner
		double cornerX = centerX - DIMENSION/2;
		double cornerY = centerY - DIMENSION/2;

		//set our location and size
		setFrame(cornerX, cornerY, DIMENSION, DIMENSION);

		//now, set up the list of other bot information
		otherBotInfo = new ArrayList<BotInfo>();
		//		for(int i = 0; i < _numBots; i++) {
		//			BotInfo newBotInfo = new BotInfo(i);
		//			otherBotInfo.add(newBotInfo);
		//		}

		//set up other variables with default values
		messageBuffer = "";

		heardShouts = new CopyOnWriteArrayList<Shout>();

		botID = _botID;

		baseZone = homeBase;

		knownVicitms = new HashMap<Victim, java.lang.Double>();

		boundingBox = _bounds;

		//find out what zones we start in, and try to determine how safe it is
		currentZone = World.findZone(getCenterLocation());
		assessZone();

		movementVector = new Vector(this.getCenterLocation(), this.getCenterLocation());
	}

	/***************************************************************************
	 * GETTERS
	 **************************************************************************/
	public Point2D getCenterLocation() {
		return new Point2D.Double(getCenterX(), getCenterY());
	}

	public Shape getBroadcastArea() {
		//see how far the current zones thinks we can broadcast
		return currentZone.getBroadcastRange(getCenterLocation());
	}

	public Shape getVisibibleArea() {
		//see how far the current zones thinks we can see
		return currentZone.getVisibilityRange(getCenterLocation());
	}

	public Shape getAuditbleArea() {
		//see how far the current zones thinks we can hear
		return currentZone.getAudibleRange(getCenterLocation());
	}

	public ListIterator<Shout> getShoutIterator() {
		return heardShouts.listIterator();
	}

	public double getObstacleBufferRange() {
		return DEFALUT_VISIBILITY_RADIUS / 8.0;
	}


	public int getID() {
		return botID;
	}

	public Vector getMovementVector() {
		return movementVector;
	}


	public void setCenterLocation(Point2D newCenterLoc) {
		//need find the new upper-left corner location
		Point newCornerLoc = new Point((int) (newCenterLoc.getX()-(DIMENSION/2.0)), (int) (newCenterLoc.getY()-(DIMENSION/2.0)));
		this.setLocation(newCornerLoc);
	}

	public static void setRepulsionConstants(double botRepulsion, double homeBaseRepulsion) {
		REPULSION_FACTOR_FROM_OTHER_BOTS = botRepulsion;
		REPULSION_FACTOR_FROM_HOME_BASES = homeBaseRepulsion;
	}	

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof Bot))
			return false;
		Bot other = (Bot) obj;
		if (botID != other.botID)
			return false;
		return true;
	}


	/***************************************************************************
	 * METHODS
	 **************************************************************************/
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

			String messageType = s.next();

			if(messageType.equals("loc")) {
				int botNum = s.nextInt();

				if(botNum == botID) {
					if(MESSAGE_BOT_DEBUG)
						print("got message from myself - skip it");
					continue;
				}

				double newX = s.nextDouble();
				double newY = s.nextDouble();

				BotInfo newBotInfo = new BotInfo(botNum, newX, newY);

				otherBotInfo.add(newBotInfo);
			} else if(messageType.equals("fv")) {
				if(FIND_VICTIM_DEBUG)
					print("Reading message '" + mes + "'");

				double vicStatus = s.nextDouble();
				double vicX = s.nextDouble();
				double vicY = s.nextDouble();
				double pathLengthSoFar = s.nextDouble();
				double pathRating = s.nextDouble();
				double avgRating = s.nextDouble();

				//see if we have seen this victim yet, and if this is a better path than the one we know about
				Victim vic = new Victim(vicX, vicY, vicStatus);
				java.lang.Double storedRating = knownVicitms.get(vic);

				//we've determined that this path is worth continuing, so finish reading the message
				List<BotInfo> pathBots = new ArrayList<BotInfo>();

				while(s.hasNextInt()) {
					pathBots.add(new BotInfo(s.nextInt(), s.nextDouble(), s.nextDouble(), s.nextInt()));
				}

				//we now have read all the information from the message, so add on our part and send it out
				//first, calculate our added distance and path rating
				BotInfo endOfPathBot = pathBots.get(pathBots.size() - 1);
				double newSegmentLength = endOfPathBot.getCenterLocation().distance(this.getCenterLocation());
				double newPathLength = pathLengthSoFar + newSegmentLength;				
				double ourDangerMultipler;
				if(zoneAssesment == ZONE_DANGEROUS || endOfPathBot.getZoneAssessment() == ZONE_DANGEROUS) ourDangerMultipler = DANGER_MULTIPLIER;
				else ourDangerMultipler = 1;
				double newPathRating = pathRating + (newSegmentLength * ourDangerMultipler);
				double newAvgRating = newPathRating;
				//				double newAvgRating = newPathRating / (newPathLength * DANGER_MULTIPLIER);

				//skip it if we know about this victim already and have already added data to a better path
				if(storedRating != null && newAvgRating > storedRating.doubleValue()) continue;

				if(storedRating != null) {
					print("Stored value is " + storedRating.doubleValue() + " newValue is " + newAvgRating);
				} else {
					print("Frist time seeing this");
				}

				//add this information into our map of Victims
				knownVicitms.put(vic, new java.lang.Double(newAvgRating));

				//make the message
				//start it off with the vic info
				String message = "fv " + vicStatus + " " + vicX + " " + vicY + " " + newPathLength + " " + newPathRating + " " + newAvgRating + " ";
				//add all the bots on the path so far
				for(BotInfo bi : pathBots) {
					message = message + bi.getBotID() + " " + bi.getCenterX() + " " + bi.getCenterY() + " " + bi.getZoneAssessment() + " ";
				}
				//add this bot
				message = message + this.getID() + " " + this.getCenterX() + " " + this.getCenterY() + " " + this.zoneAssesment + "\n";				

				//broadcast the message
				broadcastMessage(message);

			} else continue;

		}
	}

	private boolean listeningForShouts = true;

	public synchronized void hearShout(Shout s) throws InterruptedException {
		while(! listeningForShouts) {
			wait(500);
		}
		heardShouts.add(s);
	}


	private void move() {
		//store our current state, so we can undo it if necessary.
		this.previousBot = (Bot) this.clone();


		/*determine what actions we want to take
		 * there are levels to what we want to do
		 * 
		 * 1) See if we can detect a victim, first by sight and then by sound
		 * 		head towards them if we can
		 * 2) See if we are within broadcast range of any other robots by 
		 * 		seeing what messages have come in since we last checked.
		 * 	a) If there are robots nearby, try to maximize distance from them and from the base
		 * 	b) If there are not, try to find robots by heading back towards base.
		 */			

		boolean haveMoved = false; //once we have made a movement, this will be set to true

		//1) See if we can detect a victim, first by sight and then by sound head towards them if we can 
		if(!haveMoved) {
			List<Victim> visibleVics = lookForVictims();
			//if we find some, go towards one of them
			if(visibleVics.size() > 0) {
				//want to go towards the nearest victim
				Vector nearestVicVect = null;
				double nearestDistSquare = java.lang.Double.MAX_VALUE;


				//so, we need to figure out which one is the nearest one
				for(Victim v : visibleVics) {
					Vector vicVect = new Vector(this.getCenterLocation(), v.getCenterLocation());
					if(vicVect.getMagSquare() < nearestDistSquare) {
						nearestVicVect = vicVect;
						nearestDistSquare = vicVect.getMagSquare();
					}
				}

				//make a bee-line for that victim!
				actuallyMoveAlong(nearestVicVect);

				haveMoved = true;
			}
		}

		if(!haveMoved) {
			List<Shout> audibleShouts = listenForVictims();
			//if we can hear anything, go towards one of them
			if(audibleShouts.size() > 0) {
				//want to go towards the nearest shout
				Vector nearestShoutVect = null;
				double nearestDistSquare = java.lang.Double.MAX_VALUE;


				//so, we need to figure out which one is the nearest one
				for(Shout s : audibleShouts) {
					Vector shoutVect = new Vector(this.getCenterLocation(), s.getCenterLocation());
					if(shoutVect.getMagSquare() < nearestDistSquare) {
						nearestShoutVect = shoutVect;
						nearestDistSquare = s.getCenterLocation().distanceSq(this.getCenterLocation());
					}
				}

				//make a bee-line for that victim!
				actuallyMoveAlong(nearestShoutVect);

				haveMoved = true;
			}
		}

		/* 2) See if we are within broadcast range of any other robots by 
		 * 		seeing what messages have come in since we last checked.
		 * 	a) If there are robots nearby, try to maximize distance from them and from base zones
		 */

		if(MOVE_BOT_DEBUG) {
			print("I know about " + otherBotInfo.size() + " other bots");
		}

		if( (!haveMoved) && (otherBotInfo.size() > 0)) {

			Vector movementVector = new Vector(this.getCenterLocation(), this.getCenterLocation());


			//for now, just move away from nearest neighbor
			//			if(numGen.nextDouble() < MOVE_RANDOMLY_PROB) {
			//				moveRandomly();
			//			//			} else {
			//			Point2D nearestPoint = new Point2D.Double(java.lang.Double.MAX_VALUE, java.lang.Double.MAX_VALUE);
			//			double nearestPointDistSq = java.lang.Double.MAX_VALUE;

			for(int i = 0; i < otherBotInfo.size(); i++) {
				BotInfo bi = otherBotInfo.get(i);

				//don't consider ourselves
				if(bi.getBotID() == botID) continue;

				//make a "force" vector from the other bot
				Vector curBotVect = new Vector(this.getCenterLocation(), bi.getCenterLocation());
				//scale it based on how far away we are from the bot and the repulsion factor
				//also, multiply by -1 so the vector points away from the thing we want to get away from
				curBotVect = curBotVect.rescaleRatio(-1.0 * REPULSION_FACTOR_FROM_OTHER_BOTS / curBotVect.getMagSquare());

				//now add it to our movement vector
				movementVector = movementVector.add(curBotVect);
			}

			//now, also try to maximize distance from base zones
			for(Zone z : World.allZones) {
				if(z instanceof BaseZone) {
					//find the point on the edge of the zone that is closest to us
					Point2D nearestBasePoint = Utilities.getNearestPoint(z, getCenterLocation());

					//try to move away from that point
					Vector curZoneVect = new Vector(this.getCenterLocation(), nearestBasePoint);
					//scale it based on how far away we are from the base, and the repulsion factor 
					//also, multiply by -1 so the vector points away from the thing we want to get away from
					curZoneVect = curZoneVect.rescale(-1.0 * REPULSION_FACTOR_FROM_HOME_BASES / curZoneVect.getMagSquare());

					//add it to our movement vector
					movementVector = movementVector.add(curZoneVect);
				}
			}


			//			if(MOVE_BOT_DEBUG)
			//				print("Trying to move away from " + nearestBotIndex + " who is sqrt(" + nearestPointDistSq + ") away");

			//			//want to move away from the nearest bot
			//			actuallyMoveAway(nearestPoint);

			//move along the vector we made
			actuallyMoveAlong(movementVector);

			haveMoved = true;
			//			}
		}

		if(!haveMoved) {
			//move toward the base, hopefully finding other robots and/or getting messages about paths to follow
			if(MOVE_BOT_DEBUG) {
				print("No bots within broadcast distance - move back towards base\nKnow location of " + otherBotInfo.size() + " other bots");
			}

			Vector baseZoneVect = new Vector(this.getCenterLocation(), baseZone.getCenterLocation());

			actuallyMoveAlong(baseZoneVect);

			haveMoved = true;
		}

		//we've now moved - broadcast location to nearby bots
		//construct the message we want to send them.
		String outgoingMessage = "loc " + botID + " " + this.getCenterX() + " " + this.getCenterY() + "\n";

		//broadcast it
		broadcastMessage(outgoingMessage);

	}

	private void moveRandomly() {
		int xChange, yChange;
		//50-50 chance to go left or right
		if(numGen.nextDouble() < .5) {
			xChange = DIMENSION;
		} else {
			xChange = -1 * DIMENSION;
		}

		//50-50 chance to go up or down
		if(numGen.nextDouble() < .5) {
			yChange = DIMENSION;
		} else {
			yChange = -1 * DIMENSION;
		}

		this.translate(xChange, yChange);
	}


	private void actuallyMoveAlong(Vector v) {
		if(MOVE_BOT_DEBUG)
			print("Current location : " + this.getCenterLocation());

		//make sure the vector starts in the right place
		if(! v.getP1().equals(this.getCenterLocation())) {
			//move the vector to fix this
			print("HAD TO ADJUST MOVE VECTOR");
			v = v.moveTo(this.getCenterLocation());
		}

		if(MOVE_BOT_DEBUG) 
			print("Moving along vector '" + v + "'");

		//make sure the vector isn't too long i.e. assert our max velocity
		//this basically allows us to move to the end of the vector as 1 step
		v = verifyMovementVector(v);


		//don't hit the walls of the bounding box 
		if(Utilities.edgeIntersects(this.boundingBox, currentZone.getVisibilityRange(getCenterLocation()))) {
			//this means we can "see" the edge of the bounding box
			//try to move such that we don't hit it
			v = boundingBox.getPathThatStaysInside(v);
		}

		//don't hit any obsacles
		v = avoidObstacles(v);

		//again, make sure our movement vector is legal
		v = verifyMovementVector(v);

		if(MOVE_BOT_DEBUG)
			print("rescaled vector is " + v);

		//now that everything is all set with the vector, we can move to the other end of it
		this.setCenterLocation(v.getP2());

		movementVector = v;

	}

	//makes sure that the passed movement vector is OK i.e. it isn't too long
	private Vector verifyMovementVector(Vector v) {
		if(v.getMagSquare() > currentZone.getBotMaxVelocitySquared()) {
			v = v.rescale(currentZone.getBotMaxVelocity());
		}
		return v;
	}

	private Vector avoidObstacles(Vector intendedPath) {

		//first, look for any obstacles nearby
		List<? extends Shape> visibleZones = Utilities.findAreaIntersectionsInList(this.getVisibibleArea(), World.allZones);

		List<Zone> visibleObstacles = new ArrayList<Zone>();
		for(Shape s : visibleZones) {
			if(s instanceof Zone && ((Zone) s).isObstacle()) {
				visibleObstacles.add((Zone)s);
			}
		}

		//don't do anything if we're not going to run into anything.
		if(visibleObstacles.size() == 0) return intendedPath;
		
		//Try to avoid any obstacles we might run into
		//first, make an Area that is all the obstacles we can see
		Area obstacleArea = new Area();
		for(Zone curObstacle : visibleObstacles) {
			if(curObstacle instanceof Building) {
				obstacleArea.add(new Area(((Building) curObstacle).getFloorplan()));
			} else {
				obstacleArea.add(new Area(curObstacle));
			}
		}

		//start by finding it's edges that we can see
		List<Line2D> visibleObstacleEdges = getVisibleObstacleEdges(obstacleArea);

		boolean hasBeenSet = false;
		//now, check all those edges, and find the one we're heading into
		for(Line2D curEdge : visibleObstacleEdges) {
			if(intendedPath.intersectsLine(curEdge)) {
				//choose to head towards one of the endpoints of the edge
				//whichever one is closer to our P2
				if(intendedPath.getP2().distanceSq(curEdge.getP1()) < intendedPath.getP2().distanceSq(curEdge.getP2())) {
					intendedPath.setLine(intendedPath.getP1(), curEdge.getP1());
				} else {
					intendedPath.setLine(intendedPath.getP1(), curEdge.getP2());
				}

				//also, add a bit of buffer around the obstacle so that we don't get too near to it
				//to get the buffer, we're going to add a small vector going out from the current edge
				Vector bufferVect = (new Vector(curEdge)).getPerpendicularVector(intendedPath.getP2(), this.getObstacleBufferRange());
				//flip it around if there is an intersection with the shape
				if(bufferVect.getClosestIntersectionToStart(obstacleArea) != null) {
						bufferVect = bufferVect.rotate(Math.PI);
				}

				intendedPath = intendedPath.add(bufferVect);

				hasBeenSet = true;
				break;
			}
		}
//		if(hasBeenSet) print("I have tried to avoid the obstacle");
//		else print("I did not avoid the obstacle - something is wrong");


		return intendedPath;
	}

	private List<Line2D> getVisibleObstacleEdges(Area obstacleArea) {
		//essentially, we're going to do a radial sweep around our view range
		//and see where the obstacle starts and stops

		Area viewRangeArea = new Area(this.getVisibibleArea());
		Area obstacleInViewRange = (Area) obstacleArea.clone();
		obstacleInViewRange.intersect(viewRangeArea);

//		print("I see only one continuous obstacle : " + obstacleInViewRange.isSingular());

		List<Line2D> visibleObstacleSegments = new ArrayList<Line2D>();

		//start at 0 radians, and go around the full circle
		//when you hit part of the obstacle you can see, store that point
		//use just the part of the obstacle in our view range to make the calculation easier
		//keep going until you can't see it any more
		//add those 2 points in an array into the list edgePoints

		Line2D curSeg = new Line2D.Double();
		boolean currentlyAddingSegment = false;
		Point2D curPoint = null;
		Point2D prevPoint = null;

		for(double curRad = 0.0; curRad < 2*Math.PI; curRad += RADIAL_SWEEP_INCREMENT_RADIANS) {
			//			curPoint = getVisiblePointOnShape(curRad, obstacle);
			curPoint = getClosestPointOnShapeInDirection(curRad, obstacleInViewRange);


			//			print("At " + curRad + " radians I see an obstacle at " + Utilities.pointToString(curPoint));


			if( (! currentlyAddingSegment) && curPoint != null) {
				curSeg = new Line2D.Double();
				curSeg.setLine(curPoint, curSeg.getP2());
				currentlyAddingSegment = true;
			} else if(currentlyAddingSegment && curPoint == null) {
				curSeg.setLine(curSeg.getP1(), prevPoint);
				visibleObstacleSegments.add(curSeg);
				currentlyAddingSegment = false;
			}

			if(curPoint != null) {
				prevPoint = (Point2D) curPoint.clone();
			}
		}

		//if we come out of this, and we're still adding a segment, then we have a segment that goes over 0 radians
		//so, we should only have one segment rather than 2
		//		if(currentlyAddingSegment) {
		//			//the segment at 0 radians should be the first one stored i.e. index 0
		//			Line2D firstSeg = visibleObstacleSegments.remove(0);
		//
		//			//put the start point of the current segment, as that is the real start of this segment
		//			firstSeg.setLine(curSeg.getP1(), firstSeg.getP2());
		//
		//			//re-add the segment
		//			visibleObstacleSegments.add(firstSeg);
		//		}

		//make sure we close any segemnts if we are still adding them
		if(currentlyAddingSegment) {
			curSeg.setLine(curSeg.getP1(), prevPoint);
			visibleObstacleSegments.add(curSeg);
		}

//		for(Line2D l :visibleObstacleSegments) {
//			print("Saw a side : " + l.getX1() + ", " + l.getY1() + " --> " + l.getX2() + ", " + l.getY2());
//		}
//
//		List<Line2D> obstacleSides = Utilities.getSides(obstacleArea);
//		for(Line2D l : obstacleSides) {
//			print("Obstacles have sides " + l.getX1() + ", " + l.getY1() + " --> " + l.getX2() + ", " + l.getY2());
//		}

		//return the list of segments
		return visibleObstacleSegments;
	}

	private Point2D getClosestPointOnShapeInDirection(double direction, Shape s) {
		//make a vector at 0 radians
		Vector testRay = new Vector(this.getCenterX(), this.getCenterY(), this.getCenterX()+1.0, this.getCenterY());
		//rotate the vector to the correct angle
		testRay = testRay.rotate(direction);
		//make it really long, so that it is sure to intersect the shape if it is in that direction
		//we're going to make it the length of the Bounding Box's diagonal, so that it is pretty much guaranteed to go out of the bounding box
		testRay = testRay.rescale(boundingBox.getDiagonalLength());
		//now, get the intersection point closest to P1 along the vector
		return testRay.getClosestIntersectionToStart(s);
	}

	@Deprecated
	private Point2D getVisiblePointOnShape(double viewAngle, Shape lookingAt) {
		//first, make a vector along the view angle
		//to do this, make a vector going along 0 degrees and then rotate it to the view angle
		Vector viewVect = new Vector(this.getCenterX(), this.getCenterY(), this.getCenterX() + 1, this.getCenterY());
		//rotate it
		viewVect = viewVect.rotate(viewAngle);
		//now, need to scale it such that it goes just to the edge of the visible range
		//start by scaling it really far, then finding the really long vector's intersection with the view radius
		//and then rescaling it to end at the intersection point
		viewVect = viewVect.rescale(boundingBox.getDiagonalLength());
		Point2D intersectionWithVisibleRange = viewVect.getClosestIntersectionToStart(this.getVisibibleArea());
		//it is possible no such intersection exists, in which case return null
		if(intersectionWithVisibleRange == null)  return null;

		viewVect = new Vector(viewVect.getP1(), intersectionWithVisibleRange);

		//now, we have a vector that goes from us to the edge of our visible range at the angle viewAngle
		//see if there is an intersection point along that vector with the shape we're looking at
		return viewVect.getClosestIntersectionToStart(lookingAt);
	}

	@SuppressWarnings("unchecked")
	private void broadcastMessage(String mes) {
		//first, get our broadcast range
		Shape broadcastRange = getBroadcastArea();

		//find any nearby bots
		List<Bot> nearbyBots = (List<Bot>) Utilities.findAreaIntersectionsInList(broadcastRange, World.allBots);

		//send out the message to all the nearby bots
		for(Bot b : nearbyBots) {
			if(b.getID() == this.getID()) {
				continue;
			}
			try {
				b.recieveMessage(mes);
				//				if(MESSAGE_BOT_DEBUG)
				//					print("Sucessfully sent message to " + b.getID());
			} catch (InterruptedException e) {
				//				if(MESSAGE_BOT_DEBUG)
				//					print("Failed to send message to " + b.getID());
			}
		}

		//also, send it to any BaseZones
		List<Zone> nearbyZones = (List<Zone>) Utilities.findAreaIntersectionsInList(broadcastRange, World.allZones);

		for(Zone z : nearbyZones) {
			//skip non-BaseZones
			if(! (z instanceof BaseZone)) continue;

			//send messages to those basezones
			BaseZone bz = (BaseZone) z;

			try {
				bz.recieveMessage(mes);
			} catch (InterruptedException e) {
			}

		}
	}

	@SuppressWarnings("unchecked")
	private List<Victim> lookForVictims() {
		//first, get our visibility radius
		Shape visibilityRange = getVisibibleArea();

		//see if the location of any of our victims intersects this range
		List<Victim> visibleVictims = (List<Victim>) Utilities.findAreaIntersectionsInList((Shape)visibilityRange, World.allVictims);

		if(LOOK_BOT_DEBUG)
			print("In perfect world, would have just seen " + visibleVictims.size() + " victims");

		//ignore any vicitms we already know about
		//use a list iterator for the reasons described below
		ListIterator<Victim> vicIteratior = visibleVictims.listIterator();
		while(vicIteratior.hasNext()) {
			Victim curVic = vicIteratior.next();
			if(knownVicitms.containsKey(curVic)) {
				vicIteratior.remove();
			}
		}

		//visibileVictims is now a list of all the victims the robot could see and dosen't already know about 
		//if it was perfect but it's not, and there is some probability that it will miss some of the victims
		//so, go through the list and remove some or all of the victims with that probability.
		//need to use an iterator, because it won't let us remove as we go otherwise
		vicIteratior = visibleVictims.listIterator();
		while(vicIteratior.hasNext()) {
			vicIteratior.next();
			if(! (numGen.nextDouble() <= VISUAL_ID_VICTIM_PROB)) {
				vicIteratior.remove(); //removes from the iterator AND the list
			}
		}

		if(LOOK_BOT_DEBUG)
			print("Actually able to see " + visibleVictims.size() + " victims");

		//we have our list victims that the Bot saw - return it
		return visibleVictims;
	}

	@SuppressWarnings("unchecked")
	private List<Shout> listenForVictims() {
		listeningForShouts = false;

		//first, get our auditory radius
		Shape auditoryRange = getAuditbleArea();

		//see if any of the shouts we know about intersect this range
		List<Shout> audibleShouts = (List<Shout>) Utilities.findAreaIntersectionsInList((Shape) auditoryRange, heardShouts);

		if(LISTEN_BOT_DEBUG)
			print("In perfect world, would have just heard " + audibleShouts.size() + " vicitms");

		//ignore any shouts that sound like they're coming from vicitms we already know about
		//use a list iterator for the reasons described below
		ListIterator<Shout> shoutIterator = audibleShouts.listIterator();
		while(shoutIterator.hasNext()) {
			Shout curShout = shoutIterator.next();
			if(knownVicitms.containsKey(curShout.getShouter())) {
				shoutIterator.remove();
			}
		}

		//audible shouts is now all the shouts we could hear if the robot could hear perfectly
		//but it can't - we're using a probability to model this fact
		//so, go through the list and remove the ones that probability says we can't identify
		shoutIterator = audibleShouts.listIterator();
		while(shoutIterator.hasNext()) {
			shoutIterator.next();
			if(! (numGen.nextDouble() <= HEAR_VICTIM_PROB)) {
				shoutIterator.remove();  //remove from iterator AND list
			}
		}

		listeningForShouts = true;

		if(LOOK_BOT_DEBUG)
			print("Actually just heard " + audibleShouts.size() + " victims");

		//we have our list of shouts - return it
		return audibleShouts;	
	}

	private void assessZone() {
		//with some probability, the bot will asses the zones correctly
		if(numGen.nextDouble() < CORRECT_ZONE_ASSESMENT_PROB) {
			if(currentZone instanceof SafeZone) {
				zoneAssesment = ZONE_SAFE;
			} else {
				zoneAssesment = ZONE_DANGEROUS;
			}
		} else {
			//if we don't get it right, assign the incorrect value
			if(currentZone instanceof SafeZone) {
				zoneAssesment = ZONE_DANGEROUS;
			} else {
				zoneAssesment = ZONE_SAFE;
			}
		}
	}

	private double assesVictim(Victim v) {
		//with some probability we'll get it wrong
		if(numGen.nextDouble() < ASSES_VICTIM_CORRECTLY_PROB) {
			return v.getDamage();
		} else {
			return numGen.nextInt(101)/100.0;
		}
	}


	private void findAndAssesVictim() {
		//first, see if there are any victims that we can see
		List<Victim> visibleVictims = lookForVictims();

		//see if any of them are within the FOUND_RANGE
		List<Victim> foundVictims = new ArrayList<Victim>();

		for(Victim v : visibleVictims) {
			if(v.getCenterLocation().distance(this.getCenterLocation()) < DEFAULT_FOUND_RANGE) {
				foundVictims.add(v);
			}
		}

		//we now know what victims we have found
		//evaluate each of them in turn
		for(Victim v : foundVictims) {
			double vicDamage = assesVictim(v);

			//send out a message letting everyone know where the victim is, what condition they are in, and how safe the zones is
			double vicDistance = v.getCenterLocation().distance(this.getCenterLocation());
			double currentSegmentRating;
			if(zoneAssesment == ZONE_SAFE) currentSegmentRating = vicDistance;
			else currentSegmentRating = vicDistance * DANGER_MULTIPLIER;

			double avgPathRating = currentSegmentRating/(vicDistance*DANGER_MULTIPLIER);

			String message = "fv " + vicDamage + " " + v.getCenterX() + " " + v.getCenterY() + " " + vicDistance + " " + currentSegmentRating  + " " +  currentSegmentRating 
			+ " " + this.getID() + " " + this.getCenterX() + " " + this.getCenterY()  + " " + zoneAssesment + "\n";

			broadcastMessage(message);

			//add this entry to our know victims so we go onto other victims
			knownVicitms.put(v, new java.lang.Double(avgPathRating));

		}
	}

	public void startBot() {
		keepGoing = true;
	}

	public void stopBot() {
		keepGoing = false;
	}


	public synchronized void run() {

		keepGoing = true;

		//first things first, make one move randomly, to spread out a bit
		moveRandomly();

		//first, see if we should keep going
		while(keepGoing) {

			//first, read any messages that have come in, and take care of them
			readMessages();

			//now try to move, based on the move rules.
			move();

			//now that we have moved, find out if we can see any victims
			findAndAssesVictim();

			//now, just some housekeeping
			//we shouldn't hang onto shouts for too long
			heardShouts.clear();
			//also don't want to hang on to bot info for too long
			otherBotInfo.clear();

			//make sure we are still in the zones we think we are in
			if(currentZone == null || (! currentZone.contains(getCenterLocation()))) {
				currentZone = World.findZone(getCenterLocation());

				if(currentZone == null) {
					print("AHH! WE DON'T KNOW WHAT ZONE WE'RE IN!! - " + this.getCenterX() + ", " + getCenterY());
					print("Just moved: " + movementVector);
				}

				if(currentZone instanceof Fire) {
					print("AHHHH!!!! I'M MELTING!!!!");
				}


				//reasses the zones's status if we move to a new zones
				assessZone();
			}

			try {
				this.wait(1000);
			} catch(InterruptedException e) {}
		}
	}

	private void print(String message) {
		if(OVERALL_BOT_DEBUG) {
			System.out.println(botID + ":\t" + message);
			System.out.flush();
		}
	}
}