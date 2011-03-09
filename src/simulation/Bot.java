package simulation;

import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import util.Utilities;
import util.Vector;
import util.shapes.Circle2D;
import util.shapes.LineSegment;
import zones.BaseZone;
import zones.BoundingBox;
import zones.DangerZone;
import zones.SafeZone;
import zones.Zone;

public class Bot extends Rectangle2D.Double {

	private static final long serialVersionUID = -3272426964314356266L;

	/***************************************************************************
	 * CONSTANTS
	 **************************************************************************/
	private static final int DIMENSION = 2;
	private static final double VISUAL_ID_SURVIVOR_PROB = .70;
	private static final double HEAR_SURVIVOR_PROB = .75;
	private static final double ASSES_SURVIVOR_CORRECTLY_PROB = .9;
	private static final double CORRECT_ZONE_ASSESMENT_PROB = .8; // the probability that the bot will asses the zones correctly

	//1 px = 2 m
	public static final double DEFAULT_BROADCAST_RADIUS = 40; //40 px = 80 m
	public static final double DEFAULT_VISIBILITY_RADIUS = 12; //12 px = 24 m
	public static final double DEFAULT_AUDITORY_RADIUS = 24; //24 px = 48 m
	public static final double DEFAULT_FOUND_RANGE = DEFAULT_VISIBILITY_RADIUS;
	public static final double DEFAULT_MAX_VELOCITY = 4; //4 px = 8 m/s

	private final static Random NUM_GEN = new Random();

	private final int ZONE_SAFE = 1;
	private final int ZONE_DANGEROUS = 2;
	private final int ZONE_BASE = 3;

	//TODO Scale force exerted on other bots based on how many neighobors each bot has - so bots with more neighbors will push more?
	private final double NORMAL_SEPERATION_FACTOR = 									50;
	private final double COHESION_FACTOR = 										.5; //cohesion factor should never me more than 1

	private final double SEPERATION_MIN_DIST = 5;
	private final double SEPERATION_MAX_DIST = DEFAULT_BROADCAST_RADIUS*2;
	private final double SEPERATION_CURVE_SHAPE = 2.5;

	public static double timestepSeperationMagnitudeTotal;
	public static double timestepCohesionMagnitudeTotal;
	public static double timestepAverageDistanceApartTotal;
	public static int timestepCountOfBotsAffectedBySepOrCohesion;

	public static double timestepZoneRepulsionMagnitudeTotal;
	public static double timestepBotsRepelledByZones;
	public static double timestepVisibleZoneSideTotal;
	public static int timestepNumVisibleZoneSides;

	public static double timestepAvgDistBtwnPathNeighbors;
	public static int timestepNumBotOnPaths;

	public final static int WAITING_FOR_ACTIVATION = 			0;
	public final static int EXPLORER = 							1;
	public final static int DANGEROUS_EXPLORER = 				2;
	public final static int PATH_MARKER = 						3;

	private final static double TURNED_ON_THIS_TIMESTEP_PROB = .02;

	private final static double DISTANCE_FROM_SURVIVIOR_TO_START_MAKING_PATH = 1.0;
	private final static int NUM_TIMESTEPS_BTWN_PATH_CREATION = 25;

	private final static double DANEROUS_EXPLORER_DANER_REDUCTION_FACTOR = 3.0;

	private final static double SHOULD_MARK_PATH_THRESHOLD_DIST = DEFAULT_BROADCAST_RADIUS / 3.0;
	private final static double ON_PATH_THRESHOLD_DISTANCE = Bot.DIMENSION;
	private static final double HIGH_DENSITY_PATH_MARKER_SWICH_MODE_PROB = .1;

	private final double PATH_MARK_IDEAL_DIST = 10;

	private final double PATH_MARK_MIN_DIST = PATH_MARK_IDEAL_DIST * 0.5;
	private final double PATH_MARK_MAX_DIST = PATH_MARK_IDEAL_DIST * 1.5;
	private final double PATH_MARK_CURVE_SHAPE = 2.5;
	private final double PATH_MARK_FACTOR = 50;

	private final int TIMESTEPS_TO_WAIT_BEFORE_MARKING_PATHS = 3;

	private boolean OVERALL_BOT_DEBUG = true;
	private boolean LISTEN_BOT_DEBUG = false;
	private boolean LOOK_BOT_DEBUG = false;
	private boolean MESSAGE_BOT_DEBUG = false;
	private boolean MOVE_BOT_DEBUG = false;

	/***************************************************************************
	 * VARIABLES
	 **************************************************************************/

	/**
	 * These variables the bot does not know about - we store them for our
	 * convience.
	 */
	private Zone currentZone; // what zones we actually are in can change some behavior
	private List<Shout> heardShouts; // the shouts that have been heard recently
	private Vector movementVector;
	private BoundingBox boundingBox;
	//TODO Add damage value - increase as bot spends more time in fire area

	// private Bot previousBot;
	private Set<BotInfo> otherBotInfo; // storage of what information we know
	// about all of the other Bots
	private List<Message> messageBuffer; // keep a buffer of messages from other robots we have recieved in the last timestep
	private HashSet<Message> alreadyBroadcastedMessages; //TODO make this keep only the messages sent in the last x timesteps
	private int botID;
	// zones it is in
	private Zone baseZone; // the home base zones.
	private List<Survivor> knownSurvivors; // keep a list of survivors that have
	// already been found, so can go
	// claim them
	// TODO handle reclaiming survivors in the case of robot death
	private List<Survivor> claimedSurvivors; // keep a list of survivors that
	// have been claimed, so that we
	// don't double up on one survivor
	private Survivor mySurvivor; // the survivor I have claimed - don't know if
	// this is useful, but it might be
	private int mySurvivorClaimTime;
	private World world;
	private int botMode;

	private boolean startedCreatingMyPath = false;
	private int numTimestepsToNextPathCreation = NUM_TIMESTEPS_BTWN_PATH_CREATION;

	private Set<SurvivorPath> bestKnownCompletePaths;

	private boolean possiblySwitchToMarkingPathsThisStep;
	private int lastToldNotToMarkPaths;

	private SurvivorPath myPathToMark;

	/***************************************************************************
	 * CONSTRUCTORS
	 **************************************************************************/
	public Bot(World _world, double centerX, double centerY, int _numBots, int _botID, Zone homeBase, BoundingBox _bounds) {
		super();

		world = _world;

		// first, in order to store our location, we need to find our top left
		// corner
		double cornerX = centerX - DIMENSION / 2;
		double cornerY = centerY - DIMENSION / 2;

		// set our location and size
		setFrame(cornerX, cornerY, DIMENSION, DIMENSION);

		// now, set up the list of other bot information
		otherBotInfo = new HashSet<BotInfo>();

		// set up other variables with default values
		messageBuffer = new ArrayList<Message>();
		alreadyBroadcastedMessages = new HashSet<Message>();

		heardShouts = new CopyOnWriteArrayList<Shout>();

		botID = _botID;

		baseZone = homeBase;

		knownSurvivors = new ArrayList<Survivor>();
		claimedSurvivors = new ArrayList<Survivor>();

		boundingBox = _bounds;

		movementVector = new Vector(this.getCenterLocation(), this
				.getCenterLocation());

		// start deactivaed
		botMode = WAITING_FOR_ACTIVATION;

		mySurvivor = null;

		bestKnownCompletePaths = new HashSet<SurvivorPath>();

		//we can start marking paths now if we want to
		lastToldNotToMarkPaths = 0 - TIMESTEPS_TO_WAIT_BEFORE_MARKING_PATHS;

		// find out what zones we start in, and try to determine how safe it is
		updateZoneInfo();		
	}

	/***************************************************************************
	 * GETTERS
	 **************************************************************************/
	public Point2D getCenterLocation() {
		return new Point2D.Double(getCenterX(), getCenterY());
	}

	public Circle2D getBroadcastArea() {
		// see how far the current zones thinks we can broadcast
		return currentZone.getBroadcastArea(getCenterLocation());
	}

	public Circle2D getVisibleArea() {
		// see how far the current zones thinks we can see
		return currentZone.getVisibilityArea(getCenterLocation());
	}

	public double getVisibityRange() {
		return currentZone.getVisiblityRange();
	}

	public Circle2D getAuditbleArea() {
		// see how far the current zones thinks we can hear
		return currentZone.getAudibleArea(getCenterLocation());
	}

	public ListIterator<Shout> getShoutIterator() {
		return heardShouts.listIterator();
	}

	public double getMaxVelocity() {
		return DEFAULT_MAX_VELOCITY;
	}

	public double getObstacleBufferRange() {
		return DEFAULT_VISIBILITY_RADIUS / 4.0;
	}

	public int getID() {
		return botID;
	}

	/**
	 * @return the botMode
	 */
	public int getBotMode() {
		return botMode;
	}

	public BotInfo getBotInfo() {
		return new BotInfo(this.getID(), this.getCenterX(), this.getCenterY(), botMode);
	}


	public Vector getMovementVector() {
		return movementVector;
	}

	/**
	 * @return the mySurvivor
	 */
	public Survivor getMySurvivor() {
		return mySurvivor;
	}

	/**
	 * @return the bestKnownCompletePaths
	 */
	public Set<SurvivorPath> getKnownCompletePaths() {
		return bestKnownCompletePaths;
	}

	/**
	 * @return the mySurvivorClaimTime
	 */
	public int getMySurvivorClaimTime() {
		return mySurvivorClaimTime;
	}

	public void setCenterLocation(Point2D newCenterLoc) {
		// need find the new upper-left corner location
		double cornerX = newCenterLoc.getX() - (DIMENSION / 2.0);
		double cornerY = newCenterLoc.getY() - (DIMENSION / 2.0);
		this.setRect(cornerX, cornerY, DIMENSION, DIMENSION);
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
	public void recieveMessage(Message message) {
		messageBuffer.add(message);
	}

	@SuppressWarnings("unchecked")
	private void broadcastMessage(Message mes) {

		//really firstly, make sure we haven't broadcasted this message before
		//if we have broadcastetd it before, don't do it again
		if(alreadyBroadcastedMessages.contains(mes)) {
			return;
		}

		//make sure we record that we are broadcasting this message
		alreadyBroadcastedMessages.add(mes);

		// first, get our broadcast range
		Shape broadcastRange = getBroadcastArea();

		// find any nearby bots
		List<Bot> nearbyBots = (List<Bot>) Utilities.findAreaIntersectionsInList(broadcastRange, World.allBots);

		// send out the message to all the nearby bots
		for (Bot b : nearbyBots) {
			if (b.getID() == this.getID()) {
				continue;
			}
			b.recieveMessage(mes);
		}
	}

	private void readMessages() {
		// go through all the messages

		// make a scanner to make going through the messages a bit easier
		Scanner s;
		// go through the messages and update the stored info about the other
		// bots

		for (Message mes : messageBuffer) {

			s = new Scanner(mes.getText());

			String messageType = mes.getType();

			BotInfo newBotInfo;
			if (messageType.equals(Message.BOT_LOCATION_MESSAGE)) {

				newBotInfo = mes.getSender();

				if(newBotInfo.getBotID() == this.botID) {
					continue;
				}

				otherBotInfo.add(newBotInfo);
			} else if (messageType.equals(Message.FOUND_SURVIVOR_MESSAGE)) {
				// get all the information off the message
				int finderID = s.nextInt();

				if (finderID == this.getID()) {
					continue;
				}

				int sentTime = s.nextInt();

				if (MESSAGE_BOT_DEBUG) {
					print("Got a FOUND message from " + finderID);
				}

				double survivorDamage = s.nextDouble();
				double survivorX = s.nextDouble();
				double survivorY = s.nextDouble();

				// make that into a survivor entry
				Survivor foundSurvivor = new Survivor(survivorX, survivorY, survivorDamage);
				// figure out if we know about it already - update if we do, add
				// to our records if we don't
				if (knownSurvivors.contains(foundSurvivor)) {
					// update it
					knownSurvivors.set(knownSurvivors.indexOf(foundSurvivor),
							foundSurvivor);
				} else {
					// add it
					knownSurvivors.add(foundSurvivor);
				}

				// rebroadcast the message if we haven't already
				broadcastMessage(mes);
			} else if (messageType.equals(Message.CLAIM_SURVIVOR_MESSAGE)) {
				// remember to give up and reset mySurvivor if someone else
				// finds them first
				// and maybe rebroadcast our claim message so that they get the
				// idea
				// otherwise, we want to rebroadcast their claim message
				// or any found messages we get

				// get the information out of the message
				int claimerID = s.nextInt();
				// if it is us, ignore it
				if (claimerID == getID()) {
					continue;
				}

				int sentTime = s.nextInt();

				if (MESSAGE_BOT_DEBUG) {
					print("Got a Claim message from " + claimerID);
				}

				double survivorX = s.nextDouble();
				double survivorY = s.nextDouble();
				int claimTime = s.nextInt();

				Survivor claimedSurvivor = new Survivor(survivorX, survivorY, 0);
				if (claimedSurvivor.equals(mySurvivor)) {
					// someone else is claiming my survivor
					// is this valid?
					if (claimTime < mySurvivorClaimTime) {
						// the other guy made the claim first
						// give it to them
						mySurvivor = null;
						// rebroadcast his message
						broadcastMessage(mes);
					} else if (claimTime == mySurvivorClaimTime) {
						// the guy with the lower ID gets it
						if (getID() < claimerID) {
							// I get it
							// rebroadcast my claim message
							Message myClaimMessage = Message.constructClaimMessage(this);
							if(myClaimMessage != null) {
								broadcastMessage(myClaimMessage);
							}
						} else {
							// he gets it
							mySurvivor = null;
							// rebroadcast his message
							broadcastMessage(mes);
						}
					}
				} else {
					// store/update the survivor that they have found
					if (claimedSurvivors.contains(claimedSurvivor)) {
						claimedSurvivors.set(claimedSurvivors
								.indexOf(claimedSurvivor), claimedSurvivor);
					} else {
						claimedSurvivors.add(claimedSurvivor);
					}
					// rebroadcast their message
					broadcastMessage(mes);
				}
			} else if(messageType.equals(Message.CREATE_PATH_MESSAGE)) {

				//TODO don't let danger seeking bots add to paths???


				if(MESSAGE_BOT_DEBUG) {
					print("Got path message " + mes);
				}

				//remove the survivor path attachment
				SurvivorPath sp = new SurvivorPath((SurvivorPath) mes.getAttachment(0));

				//see how it compares to what path we know of that is best for this survivor

				Message passOnMessage = null;
				if(sp.isComplete()) {
					//if it is complete, see if we have a complete path to this survivor already
					SurvivorPath knownPathToThisSurvivor = null;
					for(SurvivorPath curKnownPath : bestKnownCompletePaths) {
						if(curKnownPath.getSur().equals(sp.getSur())) {
							knownPathToThisSurvivor = curKnownPath;
							break;
						}
					}
					if(knownPathToThisSurvivor != null) {
						//see if the path we have is the same that we just got
						if(knownPathToThisSurvivor.equals(sp)) {
							//we know about this path already, and it is the best we've heard - don't rebroadcast
						} else {
							//the new path may be better or worse than the one we have - see which it is
							if(knownPathToThisSurvivor.getPathLength() > sp.getPathLength()) {
								//the new one is better is better
								passOnMessage = Message.constructCreatePathsMessage(this, sp);
								bestKnownCompletePaths.remove(knownPathToThisSurvivor);
								bestKnownCompletePaths.add(new SurvivorPath(sp));
							} else {
								//our's is better
								passOnMessage = Message.constructCreatePathsMessage(this, knownPathToThisSurvivor);
							}
						}
					} else {
						//we have not heard of this path before
						//add it to our list, and pass on info about it
						bestKnownCompletePaths.add(new SurvivorPath(sp));
						passOnMessage = Message.constructCreatePathsMessage(this, sp);
					}
				} else {
					//the path we just got is not complete

					//make our changes to it, and pass it on if we are not a path marker
					if(botMode == PATH_MARKER) {
						//in this case, just pass on the incomplete path
						passOnMessage = mes;
					} else {
						//first, make sure we have not already contributed to this path
						//if we have, we should not do anything more with it
						if(sp.getPoints().contains(this.getBotInfo())) {
							continue;
						}
						//make a new version
						SurvivorPath ourVersion = new SurvivorPath(sp);

						//see if we are in the baseZone, i.e. if it should be complete
						if(baseZone.contains(this.getCenterLocation())) {
							ourVersion.setComplete(true);
						}
						//add our current location to the path
						ourVersion.addPoint(this.getBotInfo());

						//broadcast our version of the path
						passOnMessage = Message.constructCreatePathsMessage(this, ourVersion);
					}
				}

				if(passOnMessage != null) {
					if(MESSAGE_BOT_DEBUG) {
						print("Passing on a path : " + passOnMessage);
					}
					broadcastMessage(passOnMessage);
				}
			} else if(messageType.equals(Message.STOP_ADDING_NEW_PATH_MARKERS)) {
				int senderID = s.nextInt();

				if(senderID == this.getID()) continue;

				int sentTime = s.nextInt();

				//only store this time if it is after the stored time we have
				if(sentTime > lastToldNotToMarkPaths) {
					lastToldNotToMarkPaths = sentTime;
				}

				//don't mark any paths this timestep
				possiblySwitchToMarkingPathsThisStep = false;

				//don't pass on this message - only bots near a path need to know not to mark it this timestep
				//this also lets local need for bots be met without having to get the whole path to work together

			} else {
				continue; // this else matches up to figuring out what message type we have
			}

		}

		// once we are done reading, we should clear the buffer
		messageBuffer.clear();

	}

	public void hearShout(Shout s) throws InterruptedException {
		heardShouts.add(s);
	}

	private List<Survivor> getKnowButUnclaimedSurvivors() {
		List<Survivor> results = new ArrayList<Survivor>();
		results.addAll(knownSurvivors);
		results.removeAll(claimedSurvivors);
		return results;
	}

	private void exploreMove() {
		// //store our current state, so we can undo it if necessary.
		// this.previousBot = (Bot) this.clone();

		/*
		 * determine what actions we want to take there are levels to what we
		 * want to do 0) If we have claimed a survivor, head towards them 1) See
		 * if we can detect a survivor, first by sight and then by sound head
		 * towards them if we can 2) See if we are within broadcast range of any
		 * other robots by seeing what messages have come in since we last
		 * checked. a) If there are robots nearby, try to maximize distance from
		 * them and from the base b) If there are not, try to find robots by
		 * heading back towards base.
		 */

		// TODO the haveMoved thing seems bad form to me - try to do it better
		boolean haveMoved = false; // once we have made a movement, this will be
		// set to true

		// 0) If we have claimed a survivor, move towards them
		if (!haveMoved && mySurvivor != null) {
			// make a vector towards them
			Vector surVect = new Vector(this.getCenterLocation(), mySurvivor
					.getCenterLocation());
			actuallyMoveAlong(surVect);
			haveMoved = true;
		}

		// 1) See if we can detect a survivor, first by sight and then by sound
		// head towards them if we can
		if (!haveMoved) {
			List<Survivor> visibleSurs = lookForSurvivors();
			// if we find some, go towards one of them
			if (visibleSurs.size() > 0) {
				// want to go towards the nearest survivor
				Vector nearestVicVect = null;
				double nearestDistSquare = java.lang.Double.MAX_VALUE;

				// so, we need to figure out which one is the nearest one
				for (Survivor s : visibleSurs) {
					Vector surVect = new Vector(this.getCenterLocation(), s
							.getCenterLocation());
					if (surVect.getMagnitudeSquared() < nearestDistSquare) {
						nearestVicVect = surVect;
						nearestDistSquare = surVect.getMagnitudeSquared();
					}
				}

				// make a bee-line for that survivor!
				actuallyMoveAlong(nearestVicVect);

				haveMoved = true;
			}
		}

		if (!haveMoved) {
			List<Shout> audibleShouts = listenForSurvivors();
			// if we can hear anything, go towards one of them
			if (audibleShouts.size() > 0) {
				// want to go towards the nearest shout
				Vector nearestShoutVect = null;
				double nearestDistSquare = java.lang.Double.MAX_VALUE;

				// so, we need to figure out which one is the nearest one
				for (Shout s : audibleShouts) {
					Vector shoutVect = new Vector(this.getCenterLocation(), s
							.getCenterLocation());
					if (shoutVect.getMagnitudeSquared() < nearestDistSquare) {
						nearestShoutVect = shoutVect;
						nearestDistSquare = s.getCenterLocation().distanceSq(
								this.getCenterLocation());
					}
				}

				// make a bee-line for that survivor!
				actuallyMoveAlong(nearestShoutVect);

				haveMoved = true;
			}
		}

		/*
		 * 2) See if we are within broadcast range of any other robots by seeing
		 * what messages have come in since we last checked. a) If there are
		 * robots nearby, try to maximize distance from them and from base zones
		 */

		if (MOVE_BOT_DEBUG) {
			print("I know about " + otherBotInfo.size() + " other bots");
		}

		//figure out how many explorers are nearby
		//only want to move based on explorers
		Set<BotInfo> otherExplorerBots = new HashSet<BotInfo>();
		for(BotInfo bi : otherBotInfo) {
			if(bi.isExplorer()) {
				otherExplorerBots.add(bi);
			}
		}


		if ((!haveMoved) && (otherExplorerBots.size() > 0)) {

			Vector botSeperationVector = new Vector(this.getCenterLocation(), this.getCenterLocation());
			double averageDistanceToNeighbors = 0.0;

			for (BotInfo bi : otherExplorerBots) {
				//get the location of the other bot
				Point2D curBotLoc = bi.getCenterLocation();
				double distToCurBot = this.getCenterLocation().distance(curBotLoc);

				averageDistanceToNeighbors += distToCurBot;

				//put the other bot at a random nearby location if the other bot is right on top of us
				if(Utilities.shouldEqualsZero(distToCurBot)) {
					Vector randomDirVect = Vector.getHorizontalUnitVector(this.getCenterLocation());
					randomDirVect = randomDirVect.rotate(Bot.NUM_GEN.nextDouble() * 2.0 * Math.PI);
					curBotLoc = randomDirVect.getP2();
				}

				Vector curBotVect;
				if(distToCurBot < SEPERATION_MIN_DIST) {
					//too close
					//just make a vector pointing away of length 1, since the other vector calculation will be normalized to be between 0 and 1
					curBotVect = new Vector(this.getCenterLocation(), curBotLoc, -1 * NORMAL_SEPERATION_FACTOR);
				} else if (distToCurBot > SEPERATION_MAX_DIST) { 
					//too far
					//don't consider this bot
					continue;
				} else {
					curBotVect = calculateFractionalPotentialVector(curBotLoc, SEPERATION_MIN_DIST, SEPERATION_MAX_DIST, SEPERATION_CURVE_SHAPE, NORMAL_SEPERATION_FACTOR);
				}

				// now add it to the seperation vector
				botSeperationVector = botSeperationVector.add(curBotVect);
			}

			//since there are several other bots, need to divide by the number of bots to end up with an average location
			botSeperationVector = botSeperationVector.rescaleRatio(1.0/(otherExplorerBots.size()));
			averageDistanceToNeighbors = averageDistanceToNeighbors / otherExplorerBots.size();
			timestepAverageDistanceApartTotal+= averageDistanceToNeighbors;

			//			print("Final sep vect mag = " + seperationVector.getMagnitude());

			if(Utilities.shouldEqualsZero(botSeperationVector.getMagnitude())) {
				botSeperationVector = botSeperationVector.rescale(0.0);
			} else {
				World.debugSeperationVectors.add(botSeperationVector.rescaleRatio(10.0));
			}

			//also, make a cohesion vector, that points toward the average location of the neighboring bots
			//start by calculating the average location of all the bots
			double xSum = 0.0, ySum = 0.0;
			for(BotInfo curBotInfo : otherExplorerBots) {
				xSum += curBotInfo.getCenterX();
				ySum += curBotInfo.getCenterY();
			}
			double avgX = xSum / otherExplorerBots.size();
			double avgY = ySum / otherExplorerBots.size();

			Point2D averageNeighborLocation = new Point2D.Double(avgX, avgY);

			Vector cohesionVector = new Vector(this.getCenterLocation(), averageNeighborLocation);

			//scale the cohesion vector based on it's scaling factor
			cohesionVector = cohesionVector.rescaleRatio(COHESION_FACTOR);

			//also, get a vector pushing us away from bad places
			Vector zoneRepulsionVector = getAllZonesRepulsionVector();

			if(Utilities.shouldEqualsZero(zoneRepulsionVector.getMagnitude())) {
				zoneRepulsionVector = zoneRepulsionVector.rescale(0.0);
			} else {
				World.debugRepulsionVectors.add(zoneRepulsionVector.rescaleRatio(10.0));
			}			

			//			print("Num neighbors = " + otherBotInfo.size() + "\tsep = " + botSeperationVector.getMagnitude() + "\tzone = " + zoneRepulsionVector.getMagnitude());

			//we want to move along the sum of these vectors
			Vector movementVector = botSeperationVector.add(cohesionVector).add(zoneRepulsionVector);			

			timestepSeperationMagnitudeTotal += botSeperationVector.getMagnitude();
			timestepCohesionMagnitudeTotal += cohesionVector.getMagnitude();
			timestepCountOfBotsAffectedBySepOrCohesion++;

			if(! Utilities.shouldEqualsZero(zoneRepulsionVector.getMagnitude())) {
				timestepZoneRepulsionMagnitudeTotal += zoneRepulsionVector.getMagnitude();
				timestepBotsRepelledByZones++;
			}

			// move along the vector we made
			actuallyMoveAlong(movementVector);

			haveMoved = true;
		}

		if (!haveMoved) {
			// move toward the base, hopefully finding other robots and/or
			// getting messages about paths to follow
			//			if (MOVE_BOT_DEBUG) {
			print("No bots within broadcast distance - move back towards base");
			//			}

			Vector baseZoneVect = new Vector(this.getCenterLocation(), baseZone
					.getCenterLocation());

			actuallyMoveAlong(baseZoneVect);

			haveMoved = true;

			//			world.stopSimulation();
		}
	}

	@Deprecated
	protected void moveRandomly() {
		//calculate a random vector
		//start with a vector to the right
		Vector randomMove = Vector.getHorizontalUnitVector(this.getCenterLocation());
		//make it a random length between our min velocity and max velocity
		randomMove = randomMove.rescale(Bot.NUM_GEN.nextDouble() * this.getMaxVelocity());
		//rotate it to a random direction
		randomMove = randomMove.rotate(Bot.NUM_GEN.nextDouble() * 2.0 * Math.PI);
		//move along it
		actuallyMoveAlong(randomMove);
	}

	private Vector calculateFractionalPotentialVector(Point2D awayFrom, double minDist, double maxDist, double curveShape, double scalingFactor) {
		//check distances are OK
		if(minDist <= 0.0 || maxDist <= 0.0 || maxDist < minDist) {
			throw new IllegalArgumentException("Distances impossible");
		}

		//check that curve shape is OK
		if(curveShape < 0.0 || curveShape == 2.0) {
			throw new IllegalArgumentException("Illegal Curve Shape");
		}

		double distaceAway = this.getCenterLocation().distance(awayFrom);

		//if our current distance is outside the possible range, throw an error	
		if(distaceAway > maxDist) {
			throw new IllegalArgumentException("Current distance is too big - max = " + maxDist + "\t cur = " + distaceAway);
		}

		if(distaceAway < minDist) {
			throw new IllegalArgumentException("Current distance is too small");
		}

		//make a new vector pointing toward the point
		Vector awayVect = new Vector(this.getCenterLocation(), awayFrom, -1.0);

		//calculate the magnitude of the vector
		double exponent = curveShape - 2.0;
		double vectMag = (Math.pow(distaceAway, exponent) - Math.pow(maxDist, exponent)) / (Math.pow(minDist, exponent) - Math.pow(maxDist, exponent));

		//make sure the magnitude is positive, so we don't flip the vector's direction
		vectMag = Math.copySign(vectMag, 1.0);

		//		print("Partial vect mag = " + vectMag);

		return awayVect.rescale(vectMag * scalingFactor);
	}


	private Vector getAllZonesRepulsionVector() {
		//see which zone-shapes we can see
		List<? extends Shape> visibleShapes = Utilities.findAreaIntersectionsInList(this.getVisibleArea(), World.allZones.values());

		//go through each of them and get the repulsion from each one
		Vector netRepulsionFromAllZones = new Vector(this.getCenterLocation(), this.getCenterLocation());
		int numContributingShapes = 0;

		for(Shape s : visibleShapes) {
			//should all be zones
			Vector curShapeContribution = getRepulsionContributionFromOneZone((Zone) s);

			if(! Utilities.shouldEqualsZero(curShapeContribution.getMagnitude())) {
				netRepulsionFromAllZones = netRepulsionFromAllZones.add(curShapeContribution);
				numContributingShapes++;
			}
		}

		if(numContributingShapes > 0) {
			//now, need to normalize it
			netRepulsionFromAllZones = netRepulsionFromAllZones.rescaleRatio(1.0/numContributingShapes);
		}

		return netRepulsionFromAllZones;
	}

	private Vector getRepulsionContributionFromOneZone(Zone z) {
		//make sure it is a zone that creates a repulsion
		if(! z.causesRepulsion()) {
			return new Vector(this.getCenterLocation(), this.getCenterLocation());
		}

		Vector netRepulsionFromZone = new Vector(this.getCenterLocation(), this.getCenterLocation());

		//get the repulsion vectors from each of the sides
		List<LineSegment> dzSides = Utilities.getSides(z);

		LineSegment visibleSegment;
		Point2D visSegMidpoint;
		Vector thisSideContribution;
		int numContributingSides = 0;

		for(LineSegment s : dzSides) {
			//get the part of the segment that we can see, since that is the only part that should be exerting a force
			visibleSegment = this.getVisibleArea().getLineIntersectionSegment(s);

			if(visibleSegment == null) {
				//no contribution from this segment
				continue;
			}

			World.debugShapesToDraw.add(visibleSegment);

			timestepVisibleZoneSideTotal += visibleSegment.getLength();
			timestepNumVisibleZoneSides++;

			visSegMidpoint = visibleSegment.getMidpoint();

			//set the scaling factor based on if we are a dangerous explorer or not
			double scalingFactor = z.repulsionScalingFactor();
			if(botMode == DANGEROUS_EXPLORER) {
				scalingFactor /= DANEROUS_EXPLORER_DANER_REDUCTION_FACTOR;
			}

			//calculate the force from this side and add it to the net repulsion from the zone
			if(this.getCenterLocation().distance(visSegMidpoint) < z.repulsionMinDist()) {
				//distance too small
				//just have a vector of maximum size pointing away from the midpoint
				thisSideContribution = new Vector(this.getCenterLocation(), visSegMidpoint, -1.0 * scalingFactor);
			} else {
				thisSideContribution = calculateFractionalPotentialVector(visSegMidpoint, z.repulsionMinDist(), z.repulsionMaxDist(), z.repulsionCurveShape(), scalingFactor);
			}

			//reverse the vector if we are inside the zone
			if(z.contains(this.getCenterLocation())) {
				thisSideContribution = thisSideContribution.rescaleRatio(-1.0);
			}

			//add it if it has a non-zero addition

			netRepulsionFromZone = netRepulsionFromZone.add(thisSideContribution);
			numContributingSides++;
		}

		if(numContributingSides > 0) {
			//need to divide by the number of sides to average it out
			netRepulsionFromZone = netRepulsionFromZone.rescaleRatio(1.0/numContributingSides);
		}

		//		print("Net repulsion vector = " + netRepulsionFromZone.getMagnitude());

		return netRepulsionFromZone;
	}





	private void actuallyMoveAlong(Vector v) {
		if (MOVE_BOT_DEBUG)
			print("Current location : " + this.getCenterLocation());

		// make sure the vector starts in the right place
		if (!v.getP1().equals(this.getCenterLocation())) {
			// move the vector to fix this
			print("HAD TO ADJUST MOVE VECTOR");
			v = v.moveTo(this.getCenterLocation());
		}

		// make sure the vector isn't too long i.e. assert our max velocity
		// this basically allows us to move to the end of the vector as 1 step
		v = verifyMovementVector(v);

		if (MOVE_BOT_DEBUG)
			print("Moving along vector '" + v + "'");

		// don't hit the walls of the bounding box
		if (Utilities.edgeIntersects(this.boundingBox, currentZone
				.getVisibilityArea(getCenterLocation()))) {
			// this means we can "see" the edge of the bounding box
			// try to move such that we don't hit it
			v = boundingBox.getPathThatStaysInside(v);
		}

		// again, make sure our movement vector is legal
		v = verifyMovementVector(v);

		if (MOVE_BOT_DEBUG)
			print("rescaled vector is " + v);

		// now that everything is all set with the vector, we can move to the
		// other end of it
		this.setCenterLocation(v.getP2());

		movementVector = v;

		//tell everyone where we are
		Message locationMessage = Message.constructLocationMessage(this);
		broadcastMessage(locationMessage);
	}

	// makes sure that the passed movement vector is OK i.e. it isn't too long
	private Vector verifyMovementVector(Vector v) {
		if (v.getMagnitude() > getMaxVelocity()) {
			v = v.rescale(getMaxVelocity());
		}
		return v;
	}

	@SuppressWarnings("unchecked")
	private List<Survivor> lookForSurvivors() {
		// first, get our visibility radius
		Shape visibilityRange = getVisibleArea();

		// see if the location of any of our survivors intersects this range
		List<Survivor> visiblesurvivors = (List<Survivor>) Utilities
		.findAreaIntersectionsInList((Shape) visibilityRange,
				World.allSurvivors);

		if (LOOK_BOT_DEBUG)
			print("In perfect world, would have just seen "
					+ visiblesurvivors.size() + " survivors");

		// ignore any vicitms we know have been claimed
		// use a list iterator for the reasons described below
		ListIterator<Survivor> vicIteratior = visiblesurvivors.listIterator();
		while (vicIteratior.hasNext()) {
			Survivor curSur = vicIteratior.next();
			if (claimedSurvivors.contains(curSur)) {
				vicIteratior.remove();
			}
		}

		// visibilesurvivors is now a list of all the survivors the robot could
		// see and dosen't already know about
		// if it was perfect but it's not, and there is some probability that it
		// will miss some of the survivors
		// so, go through the list and remove some or all of the survivors with
		// that probability.
		// need to use an iterator, because it won't let us remove as we go
		// otherwise
		vicIteratior = visiblesurvivors.listIterator();
		while (vicIteratior.hasNext()) {
			vicIteratior.next();
			if (!(Bot.NUM_GEN.nextDouble() <= VISUAL_ID_SURVIVOR_PROB)) {
				vicIteratior.remove(); // removes from the iterator AND the list
			}
		}

		if (LOOK_BOT_DEBUG)
			print("Actually able to see " + visiblesurvivors.size()
					+ " survivors");

		// we have our list survivors that the Bot saw - return it
		return visiblesurvivors;
	}

	@SuppressWarnings("unchecked")
	private List<Shout> listenForSurvivors() {
		// first, get our auditory radius
		Shape auditoryRange = getAuditbleArea();

		// see if any of the shouts we know about intersect this range
		List<Shout> audibleShouts = (List<Shout>) Utilities
		.findAreaIntersectionsInList((Shape) auditoryRange, heardShouts);

		if (LISTEN_BOT_DEBUG)
			print("In perfect world, would have just heard "
					+ audibleShouts.size() + " vicitms");

		// ignore any shouts that sound like they're coming from vicitims have
		// already been claimed
		// use a list iterator for the reasons described below
		ListIterator<Shout> shoutIterator = audibleShouts.listIterator();
		while (shoutIterator.hasNext()) {
			Shout curShout = shoutIterator.next();
			if (claimedSurvivors.contains(curShout.getShouter())) {
				shoutIterator.remove();
			}
		}

		// audible shouts is now all the shouts we could hear if the robot could
		// hear perfectly
		// but it can't - we're using a probability to model this fact
		// so, go through the list and remove the ones that probability says we
		// can't identify
		shoutIterator = audibleShouts.listIterator();
		while (shoutIterator.hasNext()) {
			shoutIterator.next();
			if (!(Bot.NUM_GEN.nextDouble() <= HEAR_SURVIVOR_PROB)) {
				shoutIterator.remove(); // remove from iterator AND list
			}
		}

		if (LOOK_BOT_DEBUG)
			print("Actually just heard " + audibleShouts.size() + " survivors");

		// we have our list of shouts - return it
		return audibleShouts;
	}

	private void updateZoneInfo() {
		currentZone = World.findZone(getCenterLocation());

		if (currentZone == null) {
			print("AHH! WE DON'T KNOW WHAT ZONE WE'RE IN!! - "
					+ this.getCenterX() + ", " + getCenterY());
			print("Just moved: " + movementVector);
			world.stopSimulation();
			return;
		}

		if (currentZone.isObstacle()) {
			print("AHHHH!!!! I'M MELTING!!!!");
			world.stopSimulation();
		}
	}

	private double assesSurvivor(Survivor s) {
		// with some probability we'll get it wrong
		if (Bot.NUM_GEN.nextDouble() < ASSES_SURVIVOR_CORRECTLY_PROB) {
			return s.getDamage();
		} else {
			return Bot.NUM_GEN.nextInt(101) / 100.0;
		}
	}

	private void findAndAssesSurvivor() {
		// first, see if there are any survivors that we can see
		List<Survivor> visibleSurvivors = lookForSurvivors();

		// see if any of them are within the FOUND_RANGE
		List<Survivor> foundSurvivors = new ArrayList<Survivor>();

		for (Survivor v : visibleSurvivors) {
			if (v.getCenterLocation().distance(this.getCenterLocation()) < DEFAULT_FOUND_RANGE) {
				foundSurvivors.add(v);
			}
		}

		// we now know what survivors we have found
		// evaluate each of them in turn
		for (Survivor s : foundSurvivors) {
			double surDamage = assesSurvivor(s);

			// if someone has already claimed this survivor, don't bother
			// letting everyone know about them
			if (claimedSurvivors.contains(s)) {
				continue;
			}

			// send out a message letting everyone know where the survivor is,
			// what condition they are in, and how safe the zones is
			Message message = Message.constructFoundMessage(this, s, surDamage);

			broadcastMessage(message);

			knownSurvivors.add(s);
		}

		// claim the one that is closest and that has not yet been claimed
		List<Survivor> claimableSurvivors = new ArrayList<Survivor>(
				foundSurvivors);
		claimableSurvivors.removeAll(claimedSurvivors);

		if (claimableSurvivors.size() > 0) {
			// figure out which one is closets
			Survivor closestSurvivor = claimableSurvivors.get(0);
			double closestSurvivorDist = getCenterLocation().distance(
					closestSurvivor.getCenterLocation());
			for (Survivor curSur : claimableSurvivors) {
				double curDist = getCenterLocation().distance(
						curSur.getCenterLocation());
				if (curDist < closestSurvivorDist) {
					closestSurvivor = curSur;
					closestSurvivorDist = curDist;
				}
			}

			// claim the closest one
			mySurvivor = closestSurvivor;
			Message message = Message.constructClaimMessage(this);
			mySurvivorClaimTime = World.getCurrentTimestep();
			if(message != null) {
				broadcastMessage(message);
			}
		}
	}

	private void handlePathToMySurvivor() {
		//double check that we have a survivior
		if(mySurvivor == null) return;

		//what we do depends on if we've already initiated creating our survivor's path
		if(! startedCreatingMyPath || numTimestepsToNextPathCreation == 0) {
			//see if we are close to them
			if(this.getCenterLocation().distance(mySurvivor.getCenterLocation()) <= DISTANCE_FROM_SURVIVIOR_TO_START_MAKING_PATH) {
				print("Starting to make a path to my survivor");
				//start making the path
				startedCreatingMyPath = true;

				List<BotInfo> pointList = new ArrayList<BotInfo>();
				pointList.add(this.getBotInfo());

				SurvivorPath initialPath = new SurvivorPath(mySurvivor, pointList, baseZone.getCenterLocation(), baseZone.contains(this.getCenterLocation()));
				broadcastMessage(Message.constructCreatePathsMessage(this, initialPath));

				numTimestepsToNextPathCreation = NUM_TIMESTEPS_BTWN_PATH_CREATION;
			}

		} else { //we have already started creating the path to our survivor
			numTimestepsToNextPathCreation--;
		}

	}

	private void pathMarkMove() {
		//want to move toward our path, or distribute ourselves on the path
		//first, try to move toward our path
		//if we can't move any closer, start to move away from neighbors on path

		LineSegment closestSegmentOfPath = myPathToMark.getNearestSegment(this.getCenterLocation());

		Point2D closestPointOnPath = Utilities.getNearestPoint(closestSegmentOfPath, this.getCenterLocation());

		Vector towardsPathVector = new Vector(this.getCenterLocation(), closestPointOnPath);

		if(towardsPathVector.getMagnitude() > ON_PATH_THRESHOLD_DISTANCE) {
			actuallyMoveAlong(towardsPathVector);
			return;
		}

		//if we got here, we are "on" the path
		//we should try to distribute ourselves from our neighbors


		//try to distribute ourself equally between these two neighbors
		//along the path
		ArrayList<BotInfo> pathNeighbors = getPathNeighbors();

		Vector pathSegVector = new Vector(closestSegmentOfPath);
		Vector movementVector = new Vector(this.getCenterLocation(), this.getCenterLocation());

		for(BotInfo curNeighbor : pathNeighbors) {
			//TODO DUPLICATE CODE! We should just have a method that gives us a vector away from a neighbor for both move methods
			Point2D curBotLoc = curNeighbor.getCenterLocation();
			double distToCurNeighbor = curNeighbor.getCenterLocation().distance(this.getCenterLocation());

			if(Utilities.shouldEqualsZero(distToCurNeighbor)) {
				//put them in a random location
				Vector randomDir = Vector.getHorizontalUnitVector(this.getCenterLocation());
				randomDir = randomDir.rotate(Bot.NUM_GEN.nextDouble() * 2.0 * Math.PI);
				curBotLoc = randomDir.getP2();
			}

			Vector curBotVect;
			if(distToCurNeighbor < PATH_MARK_MIN_DIST) {
				//make a vector of length 1 away from them
				curBotVect = new Vector(this.getCenterLocation(), curBotLoc, -1.0 * PATH_MARK_FACTOR);
			} else if(distToCurNeighbor > PATH_MARK_MAX_DIST) {
				//just have 0 force
				curBotVect = new Vector(this.getCenterLocation(), this.getCenterLocation());
			} else {
				curBotVect = calculateFractionalPotentialVector(curBotLoc, PATH_MARK_MIN_DIST, PATH_MARK_MAX_DIST, PATH_MARK_CURVE_SHAPE, PATH_MARK_FACTOR);
			}

			//we only want the part along the path
			curBotVect  = pathSegVector.rescale(curBotVect.scalerProjectionOnto(pathSegVector));

			movementVector = movementVector.add(curBotVect);
		}

		if(pathNeighbors.size() != 0) {
			movementVector = movementVector.rescaleRatio(1.0 / pathNeighbors.size());
		}

		if(Utilities.shouldEqualsZero(movementVector.getMagnitude())) {
			movementVector = movementVector.rescale(0.0);
		}

		actuallyMoveAlong(movementVector);
	}

	private ArrayList<BotInfo> getPathNeighbors() {

		ArrayList<BotInfo> neighbors = new ArrayList<BotInfo>();
		for(BotInfo potentialNeighbor : otherBotInfo) {
			if(potentialNeighbor.isPathMarker()) {
				neighbors.add(potentialNeighbor);
			}
		}
		return neighbors;
	}

	private double getAvgDistFromPathNeighbors() {
		ArrayList<BotInfo> pathNeighbors = getPathNeighbors();

		double distSum = 0.0;
		for(BotInfo curNeighbor : pathNeighbors) {
			distSum += curNeighbor.getCenterLocation().distance(this.getCenterLocation());
		}

		return distSum / (double)pathNeighbors.size();
	}

	private double getDistToClosestPathNeighbor() {
		ArrayList<BotInfo> neighbors = getPathNeighbors();

		double closestDist = java.lang.Double.MAX_VALUE;
		double curDist;
		for(BotInfo curNeighbor : neighbors) {
			curDist = this.getCenterLocation().distance(curNeighbor.getCenterLocation());
			if(curDist < closestDist) {
				closestDist = curDist;
			}
		}

		return closestDist;
	}



	private void handlePathDensity() {
		//		//see what the average distance to neighboring bots on path is
		//		double avgDist = getAvgDistFromPathNeighbors();
		//see how close the closest neighbor is
		double closeDist = getDistToClosestPathNeighbor();

		if(closeDist <= PATH_MARK_IDEAL_DIST) {
			//no need for more bots on this part of the path
			broadcastMessage(Message.constructStopAddNewPathMarkersMessage(this));
		}

		timestepAvgDistBtwnPathNeighbors += closeDist;
		timestepNumBotOnPaths++;
	}

	private int numTimestepsToWaitBeforeMarkingPaths() {
		int timeToStart = lastToldNotToMarkPaths + TIMESTEPS_TO_WAIT_BEFORE_MARKING_PATHS;
		int timeToWait = timeToStart - World.getCurrentTimestep();
		return timeToWait;
	}

	private boolean canPossiblyMarkPathsNow() {
		return numTimestepsToWaitBeforeMarkingPaths() <= 0;
	}


	private void reevaluateBotMode() {

		//depending on which mode we're in, we're going to reevaluate differently
		//if we end up with more than the modes we have now (2/13/11) we're going to have problems with this method
		//cross that bridge when you come to it

		int decision = WAITING_FOR_ACTIVATION;

		try {
			if(botMode == WAITING_FOR_ACTIVATION) {
				//with some probability, we'll be turned on this timestep
				//if that probability is right, turn on and move to the next phase
				if(Bot.NUM_GEN.nextDouble() <= TURNED_ON_THIS_TIMESTEP_PROB) {
					decision = EXPLORER;
					return;
				}
			} 
			
			//we didn't switch yet, see if we are near paths that we should mark
			double minPathDistance = java.lang.Double.MAX_VALUE;
			SurvivorPath nearestPath = null;

			for(SurvivorPath potentialPathToMark : bestKnownCompletePaths) {
				double distToCurPath = potentialPathToMark.ptPathDist(this.getCenterLocation());
				if(distToCurPath < minPathDistance) {
					minPathDistance = distToCurPath;
					nearestPath = potentialPathToMark;
				}
			}

			//			print("Last told no mark = " + lastToldNotToMarkPaths + "\tTime til path marker = " + numTimestepsToWaitBeforeMarkingPaths());


			//if we are currently an explorer, then see if we should start marking this path
			if(botMode == EXPLORER || botMode == DANGEROUS_EXPLORER) {
				//				if(minPathDistance < SHOULD_MARK_PATH_THRESHOLD_DIST && possiblySwitchToMarkingPathsThisStep) {
				if(minPathDistance < SHOULD_MARK_PATH_THRESHOLD_DIST && canPossiblyMarkPathsNow()) {
					decision = PATH_MARKER;
					myPathToMark = new SurvivorPath(nearestPath);
					return;
				}
			} else if(botMode == PATH_MARKER) {
				//if we are already a path marker, make sure we are making the path we are closest to
				myPathToMark = new SurvivorPath(nearestPath);

				//TODO need to find a way to force it to stay a explorer for a few steps
				//right now, when someone leaves, most of the time that will force the space between their previous neighbors
				//to skyrocket, seemingly requiring a need for them to come back

				//with some probability, if the density is too high, stop being a marker
				if(! canPossiblyMarkPathsNow()) {
					if(Bot.NUM_GEN.nextDouble() < HIGH_DENSITY_PATH_MARKER_SWICH_MODE_PROB) {
						decision = EXPLORER;
						return;
					}
				}

				//also, if our path is no longer a best known path, switch away from being a path marker
				if(! bestKnownCompletePaths.contains(myPathToMark)) {
					decision = EXPLORER;
					return;
				}
			}
			
			//if we still haven't switched, see if we are an explorer and if we should go to more dangerous areas
			//first, see if we are currently in a dangerous area, thuse requiring that we go dangerous
			if(currentZone instanceof DangerZone) {
				decision = DANGEROUS_EXPLORER;
				return;
			}
			
			//do this test for both normal and dangerous explorers - if we happen to have gone dangerous and then find ourselves surrounded again, don't be dangerous anymore
			if(botMode == EXPLORER || botMode == DANGEROUS_EXPLORER) {
				//see if our neighbors all all in one direction
				//i.e. if we are on the fringe of the group
				Vector netNeighborDirection = new Vector(this.getCenterLocation(), this.getCenterLocation());
				Vector curNeighborVect;
				for(BotInfo neighbor : otherBotInfo) {
					//get a unit vector in the direction of this neighbor
					curNeighborVect = new Vector(this.getCenterLocation(), neighbor.getCenterLocation());
					if(Utilities.shouldEqualsZero(curNeighborVect.getMagnitude())) {
						continue;
					}
					curNeighborVect = curNeighborVect.getUnitVector();
					netNeighborDirection = netNeighborDirection.add(curNeighborVect);
				}
				
				//see if the vector has a non-close to 0 magnitude
				//TODO possibly have a threshold here i.e. if the mag is > .5, then we count as too many in one direction
				if(! Utilities.shouldEqualsZero(netNeighborDirection.getMagnitude())) {
					decision = DANGEROUS_EXPLORER;
				} else {
					decision = EXPLORER;
				}
				return;
			}


		} finally {
			botMode = decision;
		}
	}


	private void print(String message) {
		if (OVERALL_BOT_DEBUG) {
			System.out.println(botID + ":\t" + message);
			System.out.flush();
		}
	}

	public void doOneTimestep() {
		//before anything else, reset any values that need resetting
		possiblySwitchToMarkingPathsThisStep = true;

		//make sure we aren't trying to mark a path if we shouldn't be
		if(botMode != PATH_MARKER) {
			myPathToMark = null;
		}

		// first, read any messages that have come in, and take care of them
		readMessages();

		switch (botMode) {
			case (WAITING_FOR_ACTIVATION):

				//don't do anything in this loop - the transition a different mode
				//will take place in the "reevaluateBotMode()" method

				break;
			case (EXPLORER) :
			case (DANGEROUS_EXPLORER) :
				// now try to move, based on the move rules.
				exploreMove();
			// if we have not already claimed a survivor, find out if we can see any survivors
			// TODO if they have heard a survivor, check it out for a few steps
			if (mySurvivor == null) {
				findAndAssesSurvivor();
			} else {
				//if we are close enough to them, we should start creating a path to the survivor
				handlePathToMySurvivor();
			}
			break;
			case(PATH_MARKER) :
				//move toward/on the path
				pathMarkMove();

			//check path density, and stop more bots from approaching if need be
			handlePathDensity();

			break;
			default :
				botMode = WAITING_FOR_ACTIVATION;
				break;
		}

		reevaluateBotMode();

		// now, just some housekeeping
		// we shouldn't hang onto shouts for too long
		heardShouts.clear();
		// also don't want to hang on to bot info for too long
		otherBotInfo.clear();

		// make sure we are still in the zones we think we are in
		if (currentZone == null || (!currentZone.contains(getCenterLocation()))) {
			updateZoneInfo();
		}
		if (MOVE_BOT_DEBUG) {
			print("");
		}
	}
}