package simulation;

import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
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

	//1 px = 2 m
	public static final double DEFAULT_BROADCAST_RADIUS = 40; //40 px = 80 m
	public static final double DEFAULT_VISIBILITY_RADIUS = 12; //12 px = 24 m
	public static final double DEFAULT_AUDITORY_RADIUS = 24; //24 px = 48 m
	public static final double DEFAULT_FOUND_RANGE = DEFAULT_VISIBILITY_RADIUS;
	public static final double DEFAULT_MAX_VELOCITY = 4; //4 px = 8 m/s

	private final static Random NUM_GEN = new Random();

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

	//order the role values in the order of importance, so that the more important roles win ties
	public final static int PATH_MARKER = 						0;	
	public final static int EXPLORER = 							1;
	public final static int DANGEROUS_EXPLORER = 				2;
	public final static int WAITING_FOR_ACTIVATION = 			3;

	private final static double TURNED_ON_THIS_TIMESTEP_PROB = .02;

	private final static double DISTANCE_FROM_SURVIVIOR_TO_START_MAKING_PATH = 1.0;
	private final static int NUM_TIMESTEPS_BTWN_PATH_CREATION = 25;

	private final static double DANGEROUS_EXPLORER_DANER_REDUCTION_FACTOR = 3.0;

	private final static double SHOULD_MARK_PATH_THRESHOLD_DIST = DEFAULT_BROADCAST_RADIUS / 3.0;
	private final static double ON_PATH_THRESHOLD_DISTANCE = Bot.DIMENSION;
	private static final double HIGH_DENSITY_PATH_MARKER_SWICH_MODE_PROB = .1;

	private final double PATH_MARK_IDEAL_DIST = 10;

	private final double PATH_MARK_MIN_DIST = PATH_MARK_IDEAL_DIST * 0.5;
	private final double PATH_MARK_MAX_DIST = PATH_MARK_IDEAL_DIST * 1.5;
	private final double PATH_MARK_CURVE_SHAPE = 2.5;
	private final double PATH_MARK_FACTOR = 50;

	private final int NUM_TIMESTEPS_TO_STORE_BROADCASTED_MESSAGES = 5;

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
	private List<Bot> botsWithinBroadcast; //bot's that are within broadcast radius this timestep

	//TODO Add damage value - increase as bot spends more time in fire area

	// private Bot previousBot;
	private Set<BotInfo> otherBotInfo; // storage of what information we know
	// about all of the other Bots
	private List<Message> messageBuffer; // keep a buffer of messages from other robots we have recieved in the last timestep
	private LinkedList<HashSet<Message>> alreadyBroadcastedMessages; //TODO make this keep only the messages sent in the last x timesteps
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

	private HashMap<Survivor, SurvivorPath> bestKnownCompletePaths;

	private SurvivorPath myPathToMark;

	//organized by mode number - the prob to changing to that role is stored in the idex that that role has
	private double[] roleChangeProbabilites;

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
		botsWithinBroadcast = new ArrayList<Bot>();

		// set up other variables with default values
		messageBuffer = new ArrayList<Message>();
		alreadyBroadcastedMessages = new LinkedList<HashSet<Message>>();

		heardShouts = new CopyOnWriteArrayList<Shout>();

		botID = _botID;

		baseZone = homeBase;

		knownSurvivors = new ArrayList<Survivor>();
		claimedSurvivors = new ArrayList<Survivor>();

		boundingBox = _bounds;

		movementVector = new Vector(this.getCenterLocation(), this.getCenterLocation());

		// start deactivaed
		botMode = WAITING_FOR_ACTIVATION;

		int roleChangeProbSize = Math.max(WAITING_FOR_ACTIVATION, Math.max(EXPLORER, Math.max(DANGEROUS_EXPLORER, PATH_MARKER))) + 1;
		roleChangeProbabilites = new double[roleChangeProbSize];
		Arrays.fill(roleChangeProbabilites, 0.0);
		roleChangeProbabilites[EXPLORER] = TURNED_ON_THIS_TIMESTEP_PROB;

		mySurvivor = null;

		bestKnownCompletePaths = new HashMap<Survivor, SurvivorPath>();

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
		return new BotInfo(this.getID(), this.getCenterX(), this.getCenterY(), botMode, currentZone.getPathWeightPerPixel());
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
	public HashMap<Survivor, SurvivorPath> getBestKnownCompletePaths() {
		return bestKnownCompletePaths;
	}

	public List<Survivor> getClaimedSurvivors() {
		return claimedSurvivors;
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
		//don't add this message to our buffer if it is one we have already sent
		boolean alreadyBroadcasted = false;
		for(HashSet mesList : alreadyBroadcastedMessages) {
			if(mesList.contains(message)) {
				alreadyBroadcasted = true;
			}
		}
		if(! alreadyBroadcasted) {
			messageBuffer.add(message);
		}
	}

	@SuppressWarnings("unchecked")
	private void broadcastMessage(Message mes) {

		//really firstly, make sure we haven't broadcasted this message before
		//if we have broadcastetd it before, don't do it again
		boolean alreadyBroadcasted = false;
		for(HashSet<Message> set : alreadyBroadcastedMessages) {
			if(set.contains(mes)) {
				alreadyBroadcasted = true;
			}
		}
		if(alreadyBroadcasted) {
			return;
		}

		//make sure we record that we are broadcasting this message
		alreadyBroadcastedMessages.get(0).add(mes);

		// first, get our broadcast range
		Shape broadcastRange = getBroadcastArea();

		// find any nearby bots
		if(botsWithinBroadcast.size() == 0) {
			botsWithinBroadcast = (List<Bot>) Utilities.findAreaIntersectionsInList(broadcastRange, World.allBots);
		}

		// send out the message to all the nearby bots
		for (Bot b : botsWithinBroadcast) {
			if (b.getID() == this.getID()) {
				continue;
			}
			b.recieveMessage(mes);
		}
	}

	private void readMessages() {
		//FIXME Still gets clogged up when making new paths or just after path creation?
		// go through all the messages

		// make a scanner to make going through the messages a bit easier
		Scanner s;
		// go through the messages and update the stored info about the other
		// bots

		int locNum = 0, surFound = 0, surClaim = 0, createPath = 0;

		Message mes;
		while(! messageBuffer.isEmpty()) {
			mes = messageBuffer.remove(0);

			s = new Scanner(mes.getText());

			String messageType = mes.getType();

			BotInfo newBotInfo;
			if (messageType.equals(Message.BOT_LOCATION_MESSAGE)) {

				locNum++;

				newBotInfo = mes.getSender();

				if(newBotInfo.getBotID() == this.botID) {
					continue;
				}

				otherBotInfo.add(newBotInfo);
			} else if (messageType.equals(Message.FOUND_SURVIVOR_MESSAGE)) {
				surFound++;
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
				surClaim++;
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
				createPath++;

				if(MESSAGE_BOT_DEBUG) {
					print("Got path message " + mes);
				}

				//remove the survivor path attachment
				SurvivorPath sp = new SurvivorPath((SurvivorPath) mes.getAttachment(0));

				//see how it compares to what path we know of that is best for this survivor
				Message passOnMessage = null;
				try {
					if(sp.isComplete()) {
						//if it is complete, see if we have a complete path to this survivor already
						SurvivorPath knownPathToThisSurvivor = bestKnownCompletePaths.get(sp.getSur());

						if(knownPathToThisSurvivor != null) {
							//see if the path we have is the same that we just got
							if(knownPathToThisSurvivor.equals(sp)) {
								//we know about this path already, and it is the best we've heard - don't rebroadcast
							} else {
								//the new path may be better or worse than the one we have - see which it is
								if(knownPathToThisSurvivor.getPathLength() > sp.getPathLength()) {
									//the new one is better is better
									passOnMessage = mes;
									bestKnownCompletePaths.put(sp.getSur(), new SurvivorPath(sp));
								} else {
									//our's is better
									passOnMessage = Message.constructCreatePathsMessage(this, knownPathToThisSurvivor);
								}
							}
						} else {
							//we have not heard of this path before
							//add it to our list, and pass on info about it
							bestKnownCompletePaths.put(sp.getSur(), new SurvivorPath(sp));
							passOnMessage = mes;
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

							//if we have a complete path to this survivor already
							//and the complete path is shorter than this partial path
							//then don't do anything more with it
							//tell neighbors about better, complete path
							if(bestKnownCompletePaths.containsKey(sp.getSur()) && bestKnownCompletePaths.get(sp.getSur()).getPathLength() < sp.getPathLength()) {
								passOnMessage = Message.constructCreatePathsMessage(this, bestKnownCompletePaths.get(sp.getSur()));
								continue;
							}

							//see if we are in the baseZone, i.e. if it should be complete
							if(baseZone.contains(this.getCenterLocation())) {
								sp.setComplete(true);
							}
							//add our current location to the path
							sp.addPoint(this.getBotInfo());

							//broadcast our version of the path
							passOnMessage = Message.constructCreatePathsMessage(this, sp);
						}
					}
				} finally {
					if(passOnMessage != null) {
						if(MESSAGE_BOT_DEBUG) {
							print("Passing on a path : " + passOnMessage);
						}
						broadcastMessage(passOnMessage);
					}
				}
			} else {
				continue; // this else matches up to figuring out what message type we have
			}

		}

		// once we are done reading, we should clear the buffer
		//the way we're doing it now (removing each message) should do that, but just make sure.
		messageBuffer.clear();

		//		if(MESSAGE_BOT_DEBUG) {
		print("Message totals: loc = "+ locNum + " found = " + surFound + " claim = " + surClaim + " path = " + createPath);
		//		}

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
				scalingFactor /= DANGEROUS_EXPLORER_DANER_REDUCTION_FACTOR;
			}

			//calculate the force from this side and add it to the net repulsion from the zone
			double distanceToVisSegMidpoint = this.getCenterLocation().distance(visSegMidpoint);
			if(Utilities.shouldEqualsZero(distanceToVisSegMidpoint)) {
				//ignore the force if we're on top of the midpoint - there's nothing we can really calculate in that case
				thisSideContribution = new Vector(this.getCenterLocation(), this.getCenterLocation());
			}
			else if(distanceToVisSegMidpoint < z.repulsionMinDist()) {
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

	private List<BotInfo> getClosestKnownPathNeighbors(int numNeighbors) {
		return getClosetKnownPathNeighbors(this.getBotInfo(), numNeighbors);
	}

	private List<BotInfo> getClosetKnownPathNeighbors(final BotInfo toThisBot, final int numNeighbors) {
		ArrayList<BotInfo> pathNeighbors = getKnownPathMarkers();

		//get the requested number of closest neighbors on the path
		LinkedList<BotInfo> closetNeighbors = new LinkedList<BotInfo>();

		for(BotInfo curNeighbor : pathNeighbors) {
			closetNeighbors.add(curNeighbor);

			if(closetNeighbors.size() > numNeighbors) {
				//get the list sorted
				Collections.sort(closetNeighbors, new Comparator<BotInfo>() {
					@Override
					public int compare(BotInfo o1, BotInfo o2) {
						double d1 = o1.getCenterLocation().distance(toThisBot.getCenterLocation());
						double d2 = o2.getCenterLocation().distance(toThisBot.getCenterLocation());

						return d1 < d2 ? -1 : (d1 == d2 ? 0 : 1);
					}
				});
				//remove the last element in the list
				closetNeighbors.removeLast();
			}
		}

		print("I have " + pathNeighbors.size() + " path neighbors, and " + closetNeighbors.size() +"  are closest to me out of requested " + numNeighbors);
		
		return closetNeighbors;

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
		List<BotInfo> closetNeighbors = getClosestKnownPathNeighbors(2);

		Vector pathSegVector = new Vector(closestSegmentOfPath);
		Vector movementVector = new Vector(this.getCenterLocation(), this.getCenterLocation());

		for(BotInfo curNeighbor : closetNeighbors) {
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

		if(closetNeighbors.size() != 0) {
			movementVector = movementVector.rescaleRatio(1.0 / closetNeighbors.size());
		}

		if(Utilities.shouldEqualsZero(movementVector.getMagnitude())) {
			movementVector = movementVector.rescale(0.0);
		}

		actuallyMoveAlong(movementVector);
	}

	private ArrayList<BotInfo> getKnownPathMarkers() {

		ArrayList<BotInfo> neighbors = new ArrayList<BotInfo>();
		for(BotInfo potentialNeighbor : otherBotInfo) {
			if(potentialNeighbor.isPathMarker()) {
				neighbors.add(potentialNeighbor);
			}
		}
		return neighbors;
	}

	private double getAvgDistToClosestPathNeighbors(int numNeighbors) {
		if(botMode != PATH_MARKER) {
			//we shouldn't be asking this question
			throw new IllegalStateException(this.getID() + " is not a path marker");
		}

		List<BotInfo> closestNeighbors = getClosestKnownPathNeighbors(numNeighbors);

		if(closestNeighbors.size() == 0) {
			return -1.0;
		}

		//average their distances
		double distSum = 0.0;
		for(BotInfo b : closestNeighbors) {
			if(b.getBotID() == this.getID()) {
				continue;
			}
			distSum += b.getCenterLocation().distance(this.getCenterLocation());
		}

		return distSum / closestNeighbors.size();
	}

	public boolean isPathDensityAcceptable() {
		double avgDistToNeighbors = getAvgDistToClosestPathNeighbors(2);
		if(avgDistToNeighbors < 0) return false;
		print("Checking path density acceptablity - dist to 2 closest neighbors = " + avgDistToNeighbors);
		boolean isDensityAcceptable = (avgDistToNeighbors <= PATH_MARK_IDEAL_DIST * 1.2) && (avgDistToNeighbors >= PATH_MARK_IDEAL_DIST * .8);
		if(isDensityAcceptable) {
			print("I HAVE ACCEPTABLE DENSITY");
		}
		return isDensityAcceptable;
	}	

	private void handlePathDensity() {
		//		//see what the average distance to neighboring bots on path is
		//		double avgDist = getAvgDistFromPathNeighbors();
		//see how close the closest neighbor is
		double closeDist = getAvgDistToClosestPathNeighbors(2);

		timestepAvgDistBtwnPathNeighbors += closeDist;
		timestepNumBotOnPaths++;
	}

	private void adjustRoleChangeProb(int index, boolean positive) {
		if(positive) {
			adjustRoleChangeProb(index, .1);
		} else {
			adjustRoleChangeProb(index, -.1);
		}
	}

	private void adjustRoleChangeProb(int index, double incrementAmt) {
		if(index >= roleChangeProbabilites.length || index < 0) {
			throw new IndexOutOfBoundsException();
		}

		roleChangeProbabilites[index] += incrementAmt;
		if(roleChangeProbabilites[index] < 0.0) {
			roleChangeProbabilites[index] = 0.0;
		}
		if(roleChangeProbabilites[index] > 1.0) {
			roleChangeProbabilites[index] = 1.0;
		}
	}

	private void reevaluateBotMode() {	
		//FIXME need to make more regular explorers become dangerous explorers
		//first, adjust the probabilities
		//if we're some sort of explorer, adjust the probability that we should become a path marker
		SurvivorPath closestPath = null;
		if(botMode == EXPLORER || botMode == DANGEROUS_EXPLORER) {
			//firstly, don't switch to being a path marker if we're in a base zone
			if(currentZone instanceof BaseZone) {
				adjustRoleChangeProb(PATH_MARKER, false);
				adjustRoleChangeProb(EXPLORER, true);
				adjustRoleChangeProb(DANGEROUS_EXPLORER, false);
			} else {
				//see if we are near a path, and thus if we should be exploring the possibility of switching to being a path marker
				double minPathDistance = java.lang.Double.MAX_VALUE;

				for(SurvivorPath potentialPathToMark : bestKnownCompletePaths.values()) {
					double distToCurPath = potentialPathToMark.ptPathDist(this.getCenterLocation());
					if(distToCurPath < minPathDistance) {
						minPathDistance = distToCurPath;
						closestPath = potentialPathToMark;
					}
				}

				//see if we are close enough to this path
				if(minPathDistance < SHOULD_MARK_PATH_THRESHOLD_DIST) {
					//see how the path coverage is
					List<BotInfo> knownPathMarkers = getKnownPathMarkers();

					//if we can't see any path makers, up the probability to switch by a lot
					if(knownPathMarkers.size() == 0) {
						adjustRoleChangeProb(PATH_MARKER, true);
						adjustRoleChangeProb(EXPLORER, false);
						adjustRoleChangeProb(DANGEROUS_EXPLORER, false);
					} else {
						//find the average distance between closest path marker neighbors
						double distanceBtwnPathNeighborSum = 0.0;

						for(BotInfo curBot : knownPathMarkers) {
							List<BotInfo> curBotClosestNeighbors = getClosetKnownPathNeighbors(curBot, 2);
							//average the differences between the current bot and it's 2 closest neighbors
							double curBotDistanceSum = 0.0;
							for(BotInfo curNei : curBotClosestNeighbors) {
								curBotDistanceSum += curNei.getCenterLocation().distance(curBot.getCenterLocation());
							}
							//if this bot doesn't have 2 neighbors, make up the difference with a maximum distance
							if(curBotClosestNeighbors.size() < 2) {
								for(int i = 0; i < 2 - curBotClosestNeighbors.size(); i++) {
									curBotDistanceSum += currentZone.getBroadcastRange();
								}
							}
							double curBotNeigAvgDist = curBotDistanceSum / 2.0;
							distanceBtwnPathNeighborSum += curBotNeigAvgDist;
						}

						double avgDistBtwnPathNeighbors = distanceBtwnPathNeighborSum / knownPathMarkers.size();

						if(avgDistBtwnPathNeighbors > PATH_MARK_IDEAL_DIST) {
							//they need more path makers
							adjustRoleChangeProb(PATH_MARKER, .2);
							adjustRoleChangeProb(DANGEROUS_EXPLORER, false);
							adjustRoleChangeProb(EXPLORER, false);
						} else {
							//they don't need as many path makers
							adjustRoleChangeProb(PATH_MARKER, false);
						}
					}
				}
			}
		}
		//if we're a normal explorer, adjust the probability that we become a dangerous explorer
		if(botMode == EXPLORER || botMode == DANGEROUS_EXPLORER) {
			if(currentZone instanceof DangerZone) {
				//we should be a dangerous explorer
				adjustRoleChangeProb(DANGEROUS_EXPLORER, true);
				adjustRoleChangeProb(EXPLORER, false);
			} else {
				//if we can see a Dangerous zone, and most of our neighbors are not exploring it, up the prob we'll become a dangerous explorer
				//otherwise, lower that probability
				List<? extends Shape> visibleZones = Utilities.findAreaIntersectionsInList(getVisibleArea(), World.allZones.values());
				boolean canSeeDangerousZone = false;
				for(Shape s : visibleZones) {
					if(s instanceof Zone) {
						Zone z = (Zone) s;
						if(z instanceof DangerZone) {
							canSeeDangerousZone = true;
							break;
						}
					}
				}

				if(canSeeDangerousZone) {
					//count up how many of our neighbors are normal explorers vs dangerous explorers
					int normalCount = 0, dangerCount = 0;
					for(BotInfo b : otherBotInfo) {
						if(b.getBotID() == this.getID()) {
							continue;
						}
						if(b.isNormalExplorer()) {
							normalCount++;
						}
						if(b.isDangerExplorer()) {
							dangerCount++;
						}
					}
					//if there are more normals that dangerouses, we should up our prob of becoming a dangerous explorer
					if(normalCount > dangerCount) {
						adjustRoleChangeProb(DANGEROUS_EXPLORER, .2);
						adjustRoleChangeProb(EXPLORER, false);
					} else {
						//there are more dangerous than normal
						//lower the chance than we become dangerous
						adjustRoleChangeProb(DANGEROUS_EXPLORER, false);
						adjustRoleChangeProb(EXPLORER, true);
					}
				} else {
					//we can't see a dangerous zone
					//lower the probability that we'll become a dangerous explorer
					adjustRoleChangeProb(DANGEROUS_EXPLORER, false);
					adjustRoleChangeProb(EXPLORER, true);
				}
			}
		}

		//adjust the probability that we will switch from being a path marker to an explorer
		if(botMode == PATH_MARKER) {
			//get the average distance to our 2 closest neighbors
			double avgNeiDist = getAvgDistToClosestPathNeighbors(2);

			//if the average is negative, than we don't have any neighbors
			//in that case, reduce the chance that we become an explorer
			if(avgNeiDist < 0) {
				adjustRoleChangeProb(EXPLORER, false);
				adjustRoleChangeProb(DANGEROUS_EXPLORER, false);
				adjustRoleChangeProb(PATH_MARKER, .2);
			} else {
				//we have at least 1 neighboring path marker
				//depending on if the averge distance is greater than or less than the ideal distance, we want to increase or decrease or chance of becoming an explorer
				//TODO add a buffer region where everything is just right?
				if(avgNeiDist > PATH_MARK_IDEAL_DIST) {
					//we want to stay a path marker
					adjustRoleChangeProb(PATH_MARKER, true);
					adjustRoleChangeProb(EXPLORER, false);
					adjustRoleChangeProb(DANGEROUS_EXPLORER, false);
				} else {
					//there are too many path markers
					adjustRoleChangeProb(PATH_MARKER, -.05);
					adjustRoleChangeProb(EXPLORER, .05);
					adjustRoleChangeProb(DANGEROUS_EXPLORER, .05);
				}
			}
		}

		//first, do the probability check for activation
		if(botMode == WAITING_FOR_ACTIVATION) {
			if(NUM_GEN.nextDouble() <= roleChangeProbabilites[EXPLORER]) {
				botMode = EXPLORER;
			}
		}
		//if we have been activated already, see whether or not we should become/stay a path marker
		//TODO don't let path markers form in base zone?
		//also don't become a path marker if we have claimed a survivor
		else if(closestPath != null && mySurvivor == null && NUM_GEN.nextDouble() <= roleChangeProbabilites[PATH_MARKER]) {
			//we should become /stay a path marker
			myPathToMark = closestPath;
			botMode = PATH_MARKER;
		} else {
			//we should not be a path marker right now
			//if we are in a dangerous area, we should become a dangerous explorer
			if(currentZone instanceof DangerZone) {
				botMode = DANGEROUS_EXPLORER;
			} else {
				//otherwise, we should look at the probabilities of becoming a normal or dangerous explorer
				//look at the higher probability
				int higherProbIndex = roleChangeProbabilites[EXPLORER] > roleChangeProbabilites[DANGEROUS_EXPLORER] ? EXPLORER : DANGEROUS_EXPLORER;
				if(NUM_GEN.nextDouble() <= roleChangeProbabilites[higherProbIndex]) {
					botMode = higherProbIndex;
				} else {
					botMode = higherProbIndex == EXPLORER ? DANGEROUS_EXPLORER : EXPLORER;
				}
			}
		}
	}

	private void print(String message) {
		if (OVERALL_BOT_DEBUG) {
			System.out.println(botID + ":\t" + message);
			System.out.flush();
		}
	}

	public void doOneTimestep() {
		//make sure we aren't trying to mark a path if we shouldn't be
		if(botMode != PATH_MARKER) {
			myPathToMark = null;
		}

		// now, just some housekeeping
		// we shouldn't hang onto any of these for more than 1 time step
		heardShouts.clear();
		// also don't want to hang on to bot info for too long
		otherBotInfo.clear();
		//and don't want to hang onto the list of bots within broadcast
		botsWithinBroadcast.clear();

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

		alreadyBroadcastedMessages.add(new HashSet<Message>());
		if(alreadyBroadcastedMessages.size() > NUM_TIMESTEPS_TO_STORE_BROADCASTED_MESSAGES) {
			alreadyBroadcastedMessages.removeLast();
		}


		// make sure we are still in the zones we think we are in
		if (currentZone == null || (!currentZone.contains(getCenterLocation()))) {
			updateZoneInfo();
		}
		if (MOVE_BOT_DEBUG) {
			print("");
		}
	}
}