package simulation;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

import util.Utilities;
import util.Vector;
import util.shapes.Circle2D;
import util.shapes.LineSegment;
import zones.BaseZone;
import zones.BoundingBox;
import zones.Fire;
import zones.SafeZone;
import zones.Zone;


public class Bot extends Rectangle {

	private static final long serialVersionUID = -3272426964314356266L;

	/***************************************************************************
	 * CONSTANTS
	 **************************************************************************/
	private final int DIMENSION = 6;
	private final double VISUAL_ID_VICTIM_PROB = .70;
	private final double HEAR_VICTIM_PROB = .75;
	private final double ASSES_VICTIM_CORRECTLY_PROB = .9;
	private final double CORRECT_ZONE_ASSESMENT_PROB = .8; //the probability that the bot will asses the zones correctly

	public static final double DEFAULT_BROADCAST_RADIUS = 95;
	public static final double DEFALUT_VISIBILITY_RADIUS = 15;
	public static final double DEFAULT_AUDITORY_RADIUS = 50;
	public static final double DEFAULT_FOUND_RANGE = DEFALUT_VISIBILITY_RADIUS;
	public static final double DEFAULT_MAX_VELOCITY = 8;

	private final double AVOID_OBSTACLES_OVERROTATE_RADIANS = 2.0 * Math.PI / 180.0;
	private final double RADIAL_SWEEP_INCREMENT_RADIANS = AVOID_OBSTACLES_OVERROTATE_RADIANS / 2.0;

	private final int ZONE_SAFE = 1;
	private final int ZONE_DANGEROUS = 2;
	private final int ZONE_BASE = 3;

	private final double DANGER_MULTIPLIER = 2;

	private static double REPULSION_FACTOR_FROM_OTHER_BOTS = 800;
	private static double REPULSION_FACTOR_FROM_HOME_BASES = 2500;


	private boolean OVERALL_BOT_DEBUG = 	true;
	private boolean LISTEN_BOT_DEBUG = 		false;
	private boolean LOOK_BOT_DEBUG = 		false;
	private boolean MESSAGE_BOT_DEBUG = 	false;
	private boolean MOVE_BOT_DEBUG = 		true;
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
	private List<BotPathMemoryPoint> pathMemory;


	private List<BotInfo> otherBotInfo; //storage of what information we know about all of the other Bots
	private String messageBuffer; //keep a buffer of messages from other robots
	private int botID;
	private int zoneAssesment; //stores the bot's assesment of what sort of zones it is in
	private Zone baseZone; //the home base zones.
	private List<Survivor> knownSurvivors; //keep a list of vicitms that have already been found, so we don't double up on one vic
	private World world;


	/***************************************************************************
	 * CONSTRUCTORS
	 **************************************************************************/
	public Bot(World _world, double centerX, double centerY, int _numBots, int _botID, Zone homeBase, BoundingBox _bounds) {
		super();

		world = _world;

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

		knownSurvivors = new ArrayList<Survivor>();

		boundingBox = _bounds;

		movementVector = new Vector(this.getCenterLocation(), this.getCenterLocation());


		pathMemory = new ArrayList<BotPathMemoryPoint>();

		//for now, assume we're starting in a base zone
		zoneAssesment = ZONE_BASE;

		//find out what zones we start in, and try to determine how safe it is
		updateZoneInfo();
	}

	/***************************************************************************
	 * GETTERS
	 **************************************************************************/
	public Point2D getCenterLocation() {
		return new Point2D.Double(getCenterX(), getCenterY());
	}

	public Circle2D getBroadcastArea() {
		//see how far the current zones thinks we can broadcast
		return currentZone.getBroadcastArea(getCenterLocation());
	}

	public Circle2D getVisibleArea() {
		//see how far the current zones thinks we can see
		return currentZone.getVisibilityArea(getCenterLocation());
	}

	public double getVisibityRange() {
		return currentZone.getVisiblityRange(getCenterLocation());
	}

	public Circle2D getAuditbleArea() {
		//see how far the current zones thinks we can hear
		return currentZone.getAudibleArea(getCenterLocation());
	}

	public ListIterator<Shout> getShoutIterator() {
		return heardShouts.listIterator();
	}

	public double getMaxVelocity() {
		return DEFAULT_MAX_VELOCITY;
	}

	public ListIterator<BotPathMemoryPoint> getPathMemoryIterator() {
		return pathMemory.listIterator();
	}

	public double getObstacleBufferRange() {
		return DEFALUT_VISIBILITY_RADIUS / 4.0;
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
	public void recieveMessage(String message) {
		messageBuffer = messageBuffer + message;
	}

	private void readMessages() {
		//go through all the messages
		//messages should be split up by '\n'
		String[] messageArray = messageBuffer.split("\n");

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
				//TODO need to sort out this message case
			} else if(messageType.equals("fv")) {
				if(FIND_VICTIM_DEBUG)
					print("Reading message '" + mes + "'");

				double vicId = s.nextDouble();
				double vicStatus = s.nextDouble();
				double vicX = s.nextDouble();
				double vicY = s.nextDouble();
				int foundTime = s.nextInt();

				//see if we have seen this victim yet, and if this is a better path than the one we know about
				Survivor sur = new Survivor(vicX, vicY, vicStatus);

				//check if we know about this survivor already
				//if we don't, store it's info
				if(! knownSurvivors.contains(sur)) {
					knownSurvivors.add(sur);
				}

				//TODO determine how or if we should rebroadcast this


				//				//make the message
				//				//start it off with the vic info
				//				String message = "fv " + vicStatus + " " + vicX + " " + vicY + " " + newPathLength + " " + newPathRating + " " + newAvgRating + " ";
				//				//add all the bots on the path so far
				//				for(BotInfo bi : pathBots) {
				//					message = message + bi.getBotID() + " " + bi.getCenterX() + " " + bi.getCenterY() + " " + bi.getZoneAssessment() + " ";
				//				}
				//				//add this bot
				//				message = message + this.getID() + " " + this.getCenterX() + " " + this.getCenterY() + " " + this.zoneAssesment + "\n";				
				//
				//				//broadcast the message
				//				broadcastMessage(message);

			} else continue;

		}

		//once we are done reading, we should clear the buffer
		messageBuffer = "";
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
			List<Survivor> visibleSurs = lookForSurvivors();
			//if we find some, go towards one of them
			if(visibleSurs.size() > 0) {
				//want to go towards the nearest victim
				Vector nearestVicVect = null;
				double nearestDistSquare = java.lang.Double.MAX_VALUE;


				//so, we need to figure out which one is the nearest one
				for(Survivor s : visibleSurs) {
					Vector surVect = new Vector(this.getCenterLocation(), s.getCenterLocation());
					if(surVect.getMagSquare() < nearestDistSquare) {
						nearestVicVect = surVect;
						nearestDistSquare = surVect.getMagSquare();
					}
				}

				//make a bee-line for that victim!
				actuallyMoveAlong(nearestVicVect);

				haveMoved = true;
			}
		}

		if(!haveMoved) {
			List<Shout> audibleShouts = listenForSurvivors();
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

	protected void moveRandomly() {
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

		//make sure the vector isn't too long i.e. assert our max velocity
		//this basically allows us to move to the end of the vector as 1 step
		v = verifyMovementVector(v);

		if(MOVE_BOT_DEBUG) 
			print("Moving along vector '" + v + "'");

		//don't hit the walls of the bounding box 
		if(Utilities.edgeIntersects(this.boundingBox, currentZone.getVisibilityArea(getCenterLocation()))) {
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
		if(v.getMagSquare() > getMaxVelocity()) {
			v = v.rescale(getMaxVelocity());
		}
		return v;
	}

	private Vector avoidObstacles(Vector intendedPath) {

		if(MOVE_BOT_DEBUG)
			print("Starting avoidObstacles");

		//first, look for any obstacles nearby
		List<? extends Shape> visibleZones = Utilities.findAreaIntersectionsInList(this.getVisibleArea(), World.allZones);

		List<Zone> visibleObstacles = new ArrayList<Zone>();
		for(Shape s : visibleZones) {
			if(s instanceof Zone && ((Zone) s).isObstacle()) {
				visibleObstacles.add((Zone)s);
			}
		}

		//don't do anything if we're not going to run into anything.
		if(visibleObstacles.size() == 0) return intendedPath;

		if(MOVE_BOT_DEBUG) {
			print(visibleZones.size() + " visible zones.\t" + visibleObstacles.size() + " are obstacles");
		}

		//we may need to avoid at least 1 obstacle
		//get all the visible segments of those obstacles
		List<LineSegment> visibleObstacleEdges = getVisibleObstacleEdges(visibleObstacles);

		if(MOVE_BOT_DEBUG) {
			for(LineSegment s : visibleObstacleEdges) {
				print("Can see : " + Utilities.lineToString(s));
				World.debugShapesToDraw.add(s);
			}
		}

		//see if our path goes into any of them
		//if it does, adjust our path so that it misses that segment and any other segment it may run into
		for(LineSegment s : visibleObstacleEdges) {
			if(s.segmentsIntersect(intendedPath)) {
				//we need to adjust our path
				//see which end of the line segment is closer to our intended position by angle
				Point2D closerPoint = (Math.abs(intendedPath.getAngleBetween(s.getP1())) < Math.abs(intendedPath.getAngleBetween(s.getP2())) ? s.getP1() : s.getP2());
				//see if any other sides would get in the way of making a path going toward that closer point
				Vector newIntendedPath = new Vector(this.getCenterLocation(), closerPoint);
				if(MOVE_BOT_DEBUG) {
					print("Originally, considering " + s);
					print("new intended path before overrotating = " + newIntendedPath);
					print("Angle between this intended path and the original path is " + intendedPath.getAngleBetween(newIntendedPath));
				}

				//go a bit farther, just in case
				boolean goInPositiveDirection = true; //assume we should
				//see which way is clear
				Vector overshootPath = newIntendedPath.rotate(AVOID_OBSTACLES_OVERROTATE_RADIANS);
				
				for(Zone o : visibleObstacles) {
					if(o.contains(overshootPath.getP2())) {
						//don't go positive
						goInPositiveDirection = false;
					}
				}
				
				
				
//				goInPositiveDirection = (intendedPath.getAngleBetween(newIntendedPath) > 0);
				
				//go whichever way we decided was best, maybe
				newIntendedPath = newIntendedPath.rotate((goInPositiveDirection ? 1.0 : -1.0) * AVOID_OBSTACLES_OVERROTATE_RADIANS);

				if(MOVE_BOT_DEBUG) {
					print("Unscaled movement vector is " + newIntendedPath);
					double angleBetweenNewPathAndOldPath = intendedPath.getAngleBetween(newIntendedPath);
					print("Angle between old path and new path is " + angleBetweenNewPathAndOldPath);
					print("Rotating in " + (goInPositiveDirection ? "POSITIVE" : "NEGATIVE") + " direction");
				}

				newIntendedPath = verifyMovementVector(newIntendedPath);

				//go through all of the segments, continuing to go in the same direction until we have a clear path
				//to go in the same direction, try to go to the far end of the line
				boolean passNeeded = true;
				//				LineSegment previousSegment = s;

//				int numPasses = 0;
				
				while(passNeeded && ! Utilities.shouldEqualsZero(newIntendedPath.getMagnitude())) {
					passNeeded = false;
					
//					numPasses++;
					
//					if(numPasses > (2*Math.PI / AVOID_OBSTACLES_OVERROTATE_RADIANS) ) {
//						//gone in a full circle (ish) - something is wrong
//						//try shortening the vector a bit
//						newIntendedPath = newIntendedPath.rescaleRatio(.1);
//						numPasses = 0;
//					}
					
					

					for(LineSegment line : visibleObstacleEdges) {

						//						//ignore the last segment we intersected
						//						if(line.equals(previousSegment)) {
						//							continue;
						//						}

						if(line.segmentsIntersect(newIntendedPath)) {
							passNeeded = true;
							//							previousSegment = line;

							//rotate to the farther endpoint of the line
							double angleToP1 = newIntendedPath.getAngleBetween(line.getP1());
							double angleToP2 = newIntendedPath.getAngleBetween(line.getP2());

//							if(Utilities.shouldEqualsZero(angleToP1)) {
//								angleToP1 = 0.0;
//							}
//							if(Utilities.shouldEqualsZero(angleToP2)) {
//								angleToP2 = 0.0;
//							}							
							
							//I *THINK* one *SHOULD* be positive and the other negative
							//print just to be sure
							if(MOVE_BOT_DEBUG) {
								print("Looking at " + line);
								print("Current movement vector is " + newIntendedPath);
								print("angle 1 = " + angleToP1 + "\tangle 2 = " + angleToP2);
							}
							

							double morePositiveAngle = (angleToP1 > angleToP2 ? angleToP1 : angleToP2);
							double moreNegativeAngle = (angleToP1 < angleToP2 ? angleToP1 : angleToP2);

							//							if(morePositiveAngle < 0 || moreNegativeAngle > 0) {
							//								world.stopSimulation();
							//							}

							double angleToRotate = (goInPositiveDirection ? morePositiveAngle : moreNegativeAngle);

							newIntendedPath = newIntendedPath.rotate(angleToRotate);

							//over-rotate to make sure we're not on this line anymore
							double overrotationAngle = (goInPositiveDirection ? 1.0 : -1.0) * AVOID_OBSTACLES_OVERROTATE_RADIANS;
							print("Rotating by angle " + (overrotationAngle+angleToRotate));

							newIntendedPath = newIntendedPath.rotate(overrotationAngle);
							
							break;
						}
					}
				}
				
				if(MOVE_BOT_DEBUG) {
					print("Avoiding obstacles vector is '" + newIntendedPath + "'");
				}
				
				//make sure it actually will work
				for(Zone o : visibleObstacles) {
					if(o.contains(newIntendedPath.getP2())) {
						print("AVOIDING OBSTACLES DIDN'T WORK! TRY AGAIN");
						return avoidObstacles(newIntendedPath);
					}
				}

				return newIntendedPath;
			}
		}

		//no changes need to be made
		return intendedPath;
	}

	private class SegmentAngles {

		Point2D pov;
		LineSegment segment;
		double angle1;
		double angle2;

		public SegmentAngles(Point2D _pov, LineSegment _segment) {
			pov = _pov;
			segment = _segment;
			Vector povUnitVector = Vector.getHorizontalUnitVector(pov);

			//mod the angles to keep them between 0 and 2pi
			angle1 = povUnitVector.getAngleBetween(segment.getP1()) % (2*Math.PI);
			angle2 = povUnitVector.getAngleBetween(segment.getP2()) % (2*Math.PI);
		}

		/**
		 * @return the segment
		 */
		public LineSegment getSegment() {
			return segment;
		}

		/**
		 * @return the angle1
		 */
		public double getAngle1() {
			return angle1;
		}

		/**
		 * @return the angle2
		 */
		public double getAngle2() {
			return angle2;
		}

		public double getSmallerAngle() {
			return (angle1 < angle2 ? angle1 : angle2);
		}

		public double getLargerAngle() {
			return (angle1 > angle2 ? angle1 : angle2);
		}

		/**
		 * @param other
		 * @return if this Segment totally obstructs the other segment, according to the point of view
		 *i.e., the other segment cannot be seen because this segment is in the way
		 */
		public boolean obstructs(SegmentAngles other) {
			//all angles are between 0 and 2pi
			//need to check if this segment goes over the 0 rad line
			//i.e. if it has one angle on one side of 0 and another angle on the other side of 0
			//do this with a ray shooting down the 0 rad line
			Vector zeroRadVect = Vector.getHorizontalUnitVector(pov).rescale(1000000);

			if(zeroRadVect.intersectsLine(segment)) {
				//in this case, this line has one foot on either side of 0
				//should test if our smaller angle is bigger than the other's smaller angle
				//and if our bigger angle is smaller than the other's bigger angle
				return this.getSmallerAngle() > other.getSmallerAngle() &&
				this.getLargerAngle() < other.getLargerAngle();
			} else {
				//need to test if our smaller angel is smaller than the other's smaller angle
				//and if our larger angle is bigger than the other's larger angle
				return this.getSmallerAngle() < other.getSmallerAngle() &&
				this.getLargerAngle() > other.getLargerAngle();
			}
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result
			+ ((segment == null) ? 0 : segment.hashCode());
			return result;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof SegmentAngles))
				return false;
			SegmentAngles other = (SegmentAngles) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (segment == null) {
				if (other.segment != null)
					return false;
			} else if (!segment.equals(other.segment))
				return false;
			return true;
		}

		private Bot getOuterType() {
			return Bot.this;
		}


	}

	private List<LineSegment> getVisibleObstacleEdges(List<Zone> obstacles) {

		//get all the visible segments of the obstacles that we can see
		List<LineSegment> potentiallyVisibleEdges = new ArrayList<LineSegment>();

		for(Zone o : obstacles) {
			potentiallyVisibleEdges.addAll(Utilities.getDiscontinuitySegments(this.getVisibleArea(), o));
		}

		//for some reason, we're getting duplicates
		//remove them
		boolean passNeeded = true;;
		LineSegment outerSegment, innerSegment;
		while(passNeeded) {
			passNeeded = false;
			for(int outer = 0; outer < potentiallyVisibleEdges.size() - 1; outer++) {
				outerSegment = potentiallyVisibleEdges.get(outer);
				for(int inner = outer + 1; inner < potentiallyVisibleEdges.size(); inner++) {
					innerSegment = potentiallyVisibleEdges.get(inner);
					if(innerSegment.equals(outerSegment)) {
						passNeeded = true;
						potentiallyVisibleEdges.remove(inner);
						continue;
					}
				}
			}
		}

//		//around corners, we will have overlapping edges
//		//do it roughly for now, see if it works OK
//		//remove edges that are completely obstructed by another edge
//		//start off by calculating the start and end angles of each segment using the SegmentAngles helper class
//		List<SegmentAngles> potentiallyVisibleEdgesWithAngles = new ArrayList<SegmentAngles>();
//		for(LineSegment s : potentiallyVisibleEdges) {
//			potentiallyVisibleEdgesWithAngles.add(new SegmentAngles(this.getCenterLocation(), s));
//		}

		//TODO ray tracing to figure out what parts of what segments we can see
		
		//make a ray that is the length of the distance that we can see
		Vector visibleRay = Vector.getHorizontalUnitVector(this.getCenterLocation()).rescale(getVisibityRange());
		
		//swing it around until we hit a point where there is nothing in the way
		double rayAngle = 0.0;
		for(; rayAngle < 2 * Math.PI; rayAngle += RADIAL_SWEEP_INCREMENT_RADIANS) {
			if(closestVisibleObstacleAlongVector(visibleRay, potentiallyVisibleEdges) == null) {
				//stop when we find an open direction
				break;
			}
		}
		
		//now that we have found an open direction, start creating edges
		
		
		
//		//now, go through the list one segment at a time
//		//and compare that segment to all other segments in the list
//		//if that segment completely obstructs the other segment, remove the other segment from the list
//		ListIterator<SegmentAngles> obstructorItertaor = potentiallyVisibleEdgesWithAngles.listIterator();
//		SegmentAngles obstructor;
//		while(obstructorItertaor.hasNext()) {
//			obstructor = obstructorItertaor.next();
//
//			//now, go through all the other ones
//			ListIterator<SegmentAngles> angleIterator = potentiallyVisibleEdgesWithAngles.listIterator();
//			SegmentAngles maybeHiddenSegment;
//			while(angleIterator.hasNext()) {
//				maybeHiddenSegment = angleIterator.next();
//				if(obstructor.equals(maybeHiddenSegment)) {
//					//don't want to remove ourselves
//					continue;
//				}
//				if(obstructor.obstructs(maybeHiddenSegment)) {
//					potentiallyVisibleEdges.remove(maybeHiddenSegment.getSegment());
//				}
//			}
//		}

		return potentiallyVisibleEdges;
	}
	
	private Point2D closestVisibleObstacleAlongVector(Vector visibleRay, List<LineSegment> obstacleEdges) {
		List<Point2D> visiblePoints = new ArrayList<Point2D>();
				
		for(LineSegment curEdge : obstacleEdges) {
			if(curEdge.segmentsIntersect(visibleRay)) {
				visiblePoints.add(curEdge.intersectionPoint(curEdge));
			}
		}
		
		if(visiblePoints.size() == 0) {
			return null;
		}
		if(visiblePoints.size() == 1) {
			return visiblePoints.get(0);
		}
		
		//find the closer point
		Point2D closestPoint = visiblePoints.get(0);
		double closestPointDistance = closestPoint.distance(getCenterLocation());
		for(Point2D curPoint : visiblePoints) {
			double curDistance = curPoint.distance(getCenterLocation());
			if(curDistance < closestPointDistance) {
				closestPoint = curPoint;
				closestPointDistance = curDistance;
			}
		}
		
		return closestPoint;	
	}

	@Deprecated
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
			b.recieveMessage(mes);
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
	private List<Survivor> lookForSurvivors() {
		//first, get our visibility radius
		Shape visibilityRange = getVisibleArea();

		//see if the location of any of our victims intersects this range
		List<Survivor> visibleVictims = (List<Survivor>) Utilities.findAreaIntersectionsInList((Shape)visibilityRange, World.allSurvivors);

		if(LOOK_BOT_DEBUG)
			print("In perfect world, would have just seen " + visibleVictims.size() + " victims");

		//ignore any vicitms we already know about
		//use a list iterator for the reasons described below
		ListIterator<Survivor> vicIteratior = visibleVictims.listIterator();
		while(vicIteratior.hasNext()) {
			Survivor curSur = vicIteratior.next();
			if(knownSurvivors.contains(curSur)) {
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
	private List<Shout> listenForSurvivors() {
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
			if(knownSurvivors.contains(curShout.getShouter())) {
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

	private void updateZoneInfo() {
		currentZone = World.findZone(getCenterLocation());

		if(currentZone == null) {
			print("AHH! WE DON'T KNOW WHAT ZONE WE'RE IN!! - " + this.getCenterX() + ", " + getCenterY());
			print("Just moved: " + movementVector);
		}

		if(currentZone instanceof Fire) {
			print("AHHHH!!!! I'M MELTING!!!!");
			world.stopSimulation();
		}

		int formorZoneAssessment = this.zoneAssesment;

		//reasses the zones's status if we move to a new zones
		assessZone();

		//store the transition information
		//first, need to find the point of transition that we passed
		//for simplicity, assume that the transition took place halfway through our last step
		//also, this will always work in all cases
		Point2D transitionPoint = getMovementVector().getMidpoint();
		//make our from vector by making a vector from the transition point to P1 of the movement vector
		Vector fromVector = new Vector(transitionPoint, getMovementVector().getP1());
		//make the to vector in a similar way
		Vector toVector = new Vector(transitionPoint, getMovementVector().getP2());
		//make the memory point
		BotPathMemoryPoint newTransitionMemory = new BotPathMemoryPoint(transitionPoint, fromVector, toVector, formorZoneAssessment, zoneAssesment);
		//store the point
		pathMemory.add(newTransitionMemory);
	}


	private void assessZone() {
		//with some probability, the bot will asses the zones correctly
		if(numGen.nextDouble() < CORRECT_ZONE_ASSESMENT_PROB) {
			if(currentZone instanceof SafeZone) {
				zoneAssesment = ZONE_SAFE;
			} else if(currentZone instanceof BaseZone) {
				zoneAssesment = ZONE_BASE;
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

	private double assesSurvivor(Survivor s) {
		//with some probability we'll get it wrong
		if(numGen.nextDouble() < ASSES_VICTIM_CORRECTLY_PROB) {
			return s.getDamage();
		} else {
			return numGen.nextInt(101)/100.0;
		}
	}


	private void findAndAssesSurvivor() {
		//first, see if there are any victims that we can see
		List<Survivor> visibleSurvivors = lookForSurvivors();

		//see if any of them are within the FOUND_RANGE
		List<Survivor> foundSurvivors = new ArrayList<Survivor>();

		for(Survivor v : visibleSurvivors) {
			if(v.getCenterLocation().distance(this.getCenterLocation()) < DEFAULT_FOUND_RANGE) {
				foundSurvivors.add(v);
			}
		}


		//we now know what victims we have found
		//evaluate each of them in turn
		for(Survivor s : foundSurvivors) {
			double surDamage = assesSurvivor(s);

			//send out a message letting everyone know where the victim is, what condition they are in, and how safe the zones is
			double vicDistance = s.getCenterLocation().distance(this.getCenterLocation());
			double currentSegmentRating;
			if(zoneAssesment == ZONE_SAFE) currentSegmentRating = vicDistance;
			else currentSegmentRating = vicDistance * DANGER_MULTIPLIER;

			double avgPathRating = currentSegmentRating/(vicDistance*DANGER_MULTIPLIER);

			String message = "fv " + this.getID() + " " + surDamage + " " + s.getCenterX() + " " + s.getCenterY() + World.getCurrentTimestep() + "\n";

			broadcastMessage(message);

			//TODO handle this survivor stuff too
			knownSurvivors.add(s);

		}
	}

	//	private List<BotPathMemoryPoint> getPathToVictim(String victimPathMessage) {
	//		
	//	}

	public void doOneTimestep() {
		//first, read any messages that have come in, and take care of them
		readMessages();

		//now try to move, based on the move rules.
		move();

		//now that we have moved, find out if we can see any victims
		findAndAssesSurvivor();

		//now, just some housekeeping
		//we shouldn't hang onto shouts for too long
		heardShouts.clear();
		//also don't want to hang on to bot info for too long
		otherBotInfo.clear();


		//make sure we are still in the zones we think we are in
		if(currentZone == null || (! currentZone.contains(getCenterLocation()))) {
			updateZoneInfo();
		}
		
		print("");
	}

	private void print(String message) {
		if(OVERALL_BOT_DEBUG) {
			System.out.println(botID + ":\t" + message);
			System.out.flush();
		}
	}
}