package simulation;
import java.awt.BasicStroke;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import main.java.be.humphreys.voronoi.GraphEdge;
import main.java.be.humphreys.voronoi.Voronoi;
import util.DPixel;
import util.Dijkstras;
import util.Utilities;
import zones.BaseZone;
import zones.BoundingBox;
import zones.DummyZone;
import zones.Zone;


public class World extends Canvas {

	private static final long serialVersionUID = -2526080354915012922L;

	/***************************************************************************
	 * CONSTANTS
	 **************************************************************************/
	public static final Random RANDOM_GENERATOR = new Random();
	private static final int MENUBAR_HEIGHT = 21;
	private static final int SEARCH_HEIGHT = 300;// + MENUBAR_HEIGHT;
	private static final int SEARCH_WIDTH = 300;
	public static final BoundingBox BOUNDING_BOX = new BoundingBox(0, 0, SEARCH_WIDTH, SEARCH_HEIGHT);

	private static final boolean WORLD_DEBUG = false;

	private static final int ZONE_COMPLEXITY = 200; //should be btwn ~ (Min(SEARCH_HEIGHT, SEARCH_WIDTH) / 10) and (SEARCH_HEIGHT * SEARCH_WIDTH)

	private static final Color BACKGROUND_COLOR = Color.white;
	private static final Color EXPLORER_BOT_COLOR = Color.green;
	private static final Color PATH_MARKER_BOT_COLOR = new Color(152,245,255);
	private static final Color DEACTIVATED_BOT_COLOR = new Color(97,97,97);
	private static final Color SURVIVOR_COLOR = Color.red;
	private static final Color SHOUT_COLOR = new Color(30, 144, 255);
	private static final Color VISIBLE_RANGE_COLOR = new Color(255,106,106);
	private static final Color AUDIO_RANGE_COLOR = new Color(205,102,0);
	private static final Color BROADCAST_RANGE_COLOR = Color.yellow;
	private static final Color LABEL_COLOR = Color.black;
	private static final Color ZONE_OUTLINE_COLOR = Color.black;
	private static final Color SURVIVOR_PATH_COLOR = new Color(0,154,205);
	private static final Color BOT_MOVEMENT_VECTOR_COLOR = Color.white;
	private static final Color OPTIMAL_SURVIVOR_PATH_COLOR = new Color(255,105,180);

	private static final Point BASE_ZONE_LOC = new Point((int)BOUNDING_BOX.getCenterX(), (int)BOUNDING_BOX.getCenterY());
	private static final double BASE_ZONE_BUFFER = 35;

	private static final Stroke SURVIVOR_PATH_STROKE = new BasicStroke((float) 2.0);

	private static final Font BOT_LABEL_FONT = new Font("Serif", Font.BOLD, 10);
	private static final Font ZONE_LABEL_FONT = new Font("Serif", Font.BOLD, 12);


	private static final String DATA_FILENAME = "data.txt";
	private static final String SCREENSHOTS_DIR_NAME = "screenshots";
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM,dd,yy-HH;mm;ss");
	private static final DecimalFormat DOUBLE_FORMAT = new DecimalFormat("#.####");

	/** VARIABLES */
	public static Hashtable<Integer, Zone> allZones; //The zones in the world - should be non-overlapping
	public static List<Bot> allBots; //List of the Bots, so we can do stuff with them
	public static List<Survivor> allSurvivors; //The survivors

	public static List<Shout> allShouts;

	//	public ListIterator<Bot> allBotSnapshot;
	//	public ListIterator<Survivor> allSurvivorSnapshot;
	//	public Dijkstras dijkstrasSnapshot;

	private BaseZone homeBase;

	//	public static List<Shape> debugShapesToDraw;
	//	public static List<Shape> debugSeperationVectors;
	//	public static List<Shape> debugRepulsionVectors;

	private static int currentTimestep; //keep track of what time it is
	private long timeBetweenTimesteps; //store the time in milliseconds
	private boolean drawBotRadii = false;

	private Dijkstras distancesToAllPoints;

	private Date firstStartTime = null;

	private String dataDirectory;

	public World() {
		this(40, 2);
	}

	public World(int numBots, File surDir) {
		super();

		setupFrame();
		initZones();
		initBots(numBots);
		initSurvivors(surDir);
		initMisc();
	}	

	public World(int numBots, int numSurvivors, File zoneDir) {
		super();

		setupFrame();
		initZones(zoneDir);
		//TODO lots of repetition here - not so good, but unavoidable because zones need to be made before anything else
		initBots(numBots);
		initSurvivors(numSurvivors);
		initMisc();

	}

	public World(int numBots, File surDir, File zoneDir) {
		super();

		setupFrame();
		initZones(zoneDir);
		initBots(numBots);
		initSurvivors(surDir);
		initMisc();
	}

	public World(int numBots, int numSurvivors) {
		super();
		//start with the frame.
		setupFrame();

		initZones();
		initBots(numBots);
		initSurvivors(numSurvivors);
		initMisc();
	}

	private void setupFrame() {
		setSize(SEARCH_WIDTH, SEARCH_HEIGHT);
//		setResizable(false);
//		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setBackground(BACKGROUND_COLOR);
	}

	private void initZones(File zoneDir) {

		//check that the Zone directory actually is a directory
		if(! zoneDir.isDirectory()) {
			throw new IllegalArgumentException("Passed File is not a directory");
		}

		allZones = new Hashtable<Integer, Zone>();
		//for each file in the directory, if it has the correct extension, create a zone and add it to our Table
		File[] zoneFileList = zoneDir.listFiles(new FileFilter() {	
			@Override
			public boolean accept(File pathname) {
				return Zone.zoneFileExtensionFilter.accept(pathname);
			}
		});

		for(File curZoneFile : zoneFileList) {
			//deserealize the file, and add it to our table
			ObjectInputStream zoneIn;
			try {
				zoneIn = new ObjectInputStream(new FileInputStream(curZoneFile));
				//based on the file name, set the type
				//look from the end of the file - the ID char should be the char before the . before the extension
				char zoneTypeChar = curZoneFile.getName().charAt(curZoneFile.getName().length() - Zone.zoneFileExtensionFilter.getExtensions()[0].length() - 2);
				Class<? extends Zone> zoneClass = Zone.decodeZoneTypeChar(zoneTypeChar);
				Zone z = zoneClass.cast(zoneIn.readObject());
				if(z instanceof BaseZone) {
					homeBase = (BaseZone) z;
				}
				allZones.put(new Integer(z.getID()), z);
				zoneIn.close();
			} catch (FileNotFoundException e) {
				System.out.println("Cannot find zone file");
				e.printStackTrace();
			} catch (IOException e) {
				System.out.println("IOException while deserializing zone");
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				System.out.println("Cannot find Zone class when deserializing Zone");
				e.printStackTrace();
			}
		}

	}

	//init zones randomly
	private void initZones() {
		//initialize the zones
		allZones = new Hashtable<Integer, Zone>();

		//set them up using the Voronoi Algorithm
		voronoiZones();
	}

	private void initBots(int numBots) {
		//initialize the bots
		allBots = Collections.synchronizedList(new ArrayList<Bot>());

		for(int i = 0; i < numBots; i++) {
			allBots.add(new Bot(this, homeBase.getCenterX(), homeBase.getCenterY(), numBots, i, homeBase, BOUNDING_BOX));
		}
	}

	private void initSurvivors(int numSurvivors) {
		//initialize the survivors
		allSurvivors = Collections.synchronizedList(new ArrayList<Survivor>());
		Survivor curSurvivor;

		for(int i = 0; i < numSurvivors; i++) {
			//don't let survivors be in the basezone
			do {
				curSurvivor = new Survivor(RANDOM_GENERATOR.nextDouble()*SEARCH_WIDTH, RANDOM_GENERATOR.nextDouble()*(SEARCH_HEIGHT), RANDOM_GENERATOR.nextDouble());
				//make sure we don't have survivors too close to the base zone and we don't duplicate survivors
			} while(homeBase.getCenterLocation().distance(curSurvivor.getCenterLocation()) < BASE_ZONE_BUFFER || allSurvivors.contains(curSurvivor));

			allSurvivors.add(curSurvivor);
		}
	}

	private void initSurvivors(File surDir) {

		//check that the Survivor directory actually is a directory
		if(! surDir.isDirectory()) {
			throw new IllegalArgumentException("Passed File is not a directory");
		}

		allSurvivors = Collections.synchronizedList(new ArrayList<Survivor>());
		//for each file in the directory with the correct extension, create a survivor and add it to the list
		File[] survivorFileList = surDir.listFiles(new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				return Survivor.survivorFileExtensionFilter.accept(pathname);
			}

		});

		for(File curSurFile : survivorFileList) {
			//deserealize the file and add it to the list
			ObjectInputStream surIn;
			try {
				surIn = new ObjectInputStream(new FileInputStream(curSurFile));
				Survivor s = (Survivor) surIn.readObject();
				allSurvivors.add(s);
				surIn.close();
			} catch (FileNotFoundException e) {
				System.out.println("Cannot find survivor file");
				e.printStackTrace();
			} catch (IOException e) {
				System.out.println("IOException trying to read survivor from file");
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				System.out.println("Cannot find Survivor class when deserializing Survivor from file");
				e.printStackTrace();
			}
		}
	}

	private void initMisc() {		
		distancesToAllPoints = new Dijkstras(0, SEARCH_WIDTH, 0, SEARCH_HEIGHT);		
		distancesToAllPoints.dijkstras(BASE_ZONE_LOC);

		//		debugShapesToDraw = new ArrayList<Shape>();
		//		debugSeperationVectors = new ArrayList<Shape>();
		//		debugRepulsionVectors = new ArrayList<Shape>();

		allShouts = new Vector<Shout>();

		currentTimestep = 0;
	}

	private void setupFiles() {
		System.out.println("Setting up files");
		//make the directory for this run
		dataDirectory = "data/" + allSurvivors.size() + "/" + allBots.size() + "/" + DATE_FORMAT.format(firstStartTime) + "/";
		new File(dataDirectory).mkdirs();
		//also make the directory for the screenshots
		new File(dataDirectory + SCREENSHOTS_DIR_NAME).mkdir();
		//create the information about number of bots, survivors etc
		try {
			//write an info file about the number of bots and survivors, to start
			BufferedWriter infoWriter = new BufferedWriter(new FileWriter(dataDirectory + "info.txt"));
			infoWriter.write("bots = " + allBots.size());
			infoWriter.newLine();
			infoWriter.write("sur = " + allSurvivors.size());
			infoWriter.newLine();
			infoWriter.close();

			//write a folder with a file for each zone - serialize them
			String zoneDirString = dataDirectory + "/zones/";
			File zoneDirectoryFile = new File(zoneDirString);
			zoneDirectoryFile.mkdir();
			//write about each of the zones
			//they should all be searializable
			for(Integer curKey : allZones.keySet()) {
				Zone curZone = allZones.get(curKey);
				ObjectOutputStream zoneOut = new ObjectOutputStream(new FileOutputStream(zoneDirString + curKey + "_" + curZone.getZoneTypeChar() + "." + Zone.zoneFileExtensionFilter.getExtensions()[0]));
				zoneOut.writeObject(curZone);
				zoneOut.close();
			}

			//write a folder with a file for each survivor - serialize them too
			String surDirString = dataDirectory + "/survivors/";
			File surDirFile = new File(surDirString);
			surDirFile.mkdir();
			synchronized (allSurvivors) {
				Iterator<Survivor> iter = allSurvivors.iterator();

				for(int i = 0; iter.hasNext(); i++) {
					Survivor curSur = iter.next();
					ObjectOutputStream surOut = new ObjectOutputStream(new FileOutputStream(surDirString + i + "." + Survivor.survivorFileExtensionFilter.getExtensions()[0]));
					surOut.writeObject(curSur);
					surOut.close();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private double calcPercentSurFound() {
		HashSet<Survivor> allClaimedSurvivors = new HashSet<Survivor>();
		synchronized (allBots) {
			Iterator<Bot> i = allBots.iterator();
			Bot b;
			while(i.hasNext()) {
				b = i.next();
				allClaimedSurvivors.addAll(b.getClaimedSurvivors());
			}
		}
		return ((double)allClaimedSurvivors.size()) / allSurvivors.size();
	}

	private double calcAvgPathQuality() {
		HashMap<Survivor, SurvivorPath> bestCompletePaths = new HashMap<Survivor, SurvivorPath>();
		synchronized (allBots) {
			Iterator<Bot> i = allBots.iterator();
			Bot b;
			while(i.hasNext()) {
				b = i.next();
				HashMap<Survivor, SurvivorPath> thisBotsPaths = b.getBestKnownCompletePaths();
				for(Survivor sur : thisBotsPaths.keySet()) {
					if(bestCompletePaths.containsKey(sur)) {
						//take the path that is better
						if(bestCompletePaths.get(sur).getPathLength() > thisBotsPaths.get(sur).getPathLength()) {
							bestCompletePaths.put(sur, thisBotsPaths.get(sur));
						}
					} else {
						bestCompletePaths.put(sur, thisBotsPaths.get(sur));
					}
				}
			}
		}

		//now we should have all the best paths known by any bot to each survivor
		//get the length of each of these paths, and compare them to the optimal lengths computed by Dijkstra's
		HashMap<Survivor, Double> realPathLengths = new HashMap<Survivor, Double>(bestCompletePaths.size());
		for(Survivor sur : bestCompletePaths.keySet()) {
			//get the path that the bots thing is best
			SurvivorPath botsBest = bestCompletePaths.get(sur);
			//calculate it's real length
			Double realLength = new Double(botsBest.getRealPathLength());
			//put it into the real length hash
			realPathLengths.put(sur, realLength);
		}

		//use the real lengths in these percentages
		double pathPercentagesSum = 0.0;
		for(Survivor sur : bestCompletePaths.keySet()) {
			SurvivorPath botPath = bestCompletePaths.get(sur);
			Double realLength = realPathLengths.get(sur);
			double optimalLength = distancesToAllPoints.getDistanceTo(botPath.getSur().getCenterLocation());
			//			System.out.println("Bots think path has length " + botPath.getPathLength() + " real bot path length = " + realLength + " optimal path has length " + optimalLength);	
			pathPercentagesSum += (realLength.doubleValue() / optimalLength);
		}
		//average the percentages
		double avgPercent = pathPercentagesSum / bestCompletePaths.size();
		if(Double.isInfinite(avgPercent) || Double.isNaN(avgPercent)) {
			return 0.0;
		} else {
			return avgPercent;
		}
	}

	private double calcPathCoverageMetric() {
		int coverageMetricSum = 0;
		int numPathMarkers = 0;

		synchronized (allBots) {
			Iterator<Bot> i = allBots.iterator();
			Bot b;
			while(i.hasNext()) {
				b = i.next();
				if(b.getBotMode() == Bot.PATH_MARKER) {
					//bad is high, good is low
					coverageMetricSum += b.isPathDensityAcceptable() ? 0 : 1;
					numPathMarkers++;
				}
			}
		}

		System.out.println("coverage sum = " + coverageMetricSum + "\tnum path markers = " + numPathMarkers);
		//calculate the average value
		double pathCoverage = ((double) coverageMetricSum) / numPathMarkers;
		//scale it so that it goes from 1 to 1/10 of path quality vaules
		//as a first approximation, setting this to be from 1 to 1.5
		//as it goes from 0 to 1 currently, we just need to multiply by .5 and add 1
		pathCoverage *= .5;
		pathCoverage += 1.0;
		if(Double.isInfinite(pathCoverage) || Double.isNaN(pathCoverage)) {
			return 0.0;
		} else {
			return pathCoverage;
		}
	}

	private double calcOverallMetric(double perSurFound, double pathQuality, double pathCoverage) {
		double overallMetric = perSurFound / (pathQuality * pathCoverage);
		if(Double.isInfinite(overallMetric) || Double.isNaN(overallMetric)) {
			return 0.0;
		} else {
			return overallMetric;
		}
	}


	private void writeADatapoint() {
		try {
			BufferedWriter dataWriter = new BufferedWriter(new FileWriter(dataDirectory + DATA_FILENAME, true));
			//write:
			//<timestep>	<% sur found>	<path quality>	<coverage metric>	<overall metric>
			double perSurFound = calcPercentSurFound();
			double pathQuality = calcAvgPathQuality();
			double pathCoverage = calcPathCoverageMetric();
			double overallMetric = calcOverallMetric(perSurFound, pathQuality, pathCoverage);
			dataWriter.write(World.getCurrentTimestep() + "\t" + System.currentTimeMillis() + "\t" + DOUBLE_FORMAT.format(perSurFound) + '\t' + DOUBLE_FORMAT.format(pathQuality) + '\t' + DOUBLE_FORMAT.format(pathCoverage) + '\t' + DOUBLE_FORMAT.format(overallMetric));
			dataWriter.newLine();
			dataWriter.close();

			//also write a screenshot
			//			Robot screenCapRobot = new Robot();
			//			BufferedImage curTimestepShot = screenCapRobot.createScreenCapture(BOUNDING_BOX.getBounds());

			BufferedImage curTimestepShot = new BufferedImage(SEARCH_WIDTH, SEARCH_HEIGHT, BufferedImage.TYPE_INT_RGB);
			this.paint(curTimestepShot.createGraphics());

			//TODO replace this with video output? Xuggle?
			File outputFile = new File(dataDirectory + SCREENSHOTS_DIR_NAME + "/" + getCurrentTimestep() + ".jpeg");
			ImageIO.write(curTimestepShot, "jpeg", outputFile);
		} catch (IOException e) {
			System.out.println("IOException writing a datapoint");
			e.printStackTrace();
		}
	}

	public void checkZoneSanity() {
		//check each zones's area with all the rest to make sure they don't overlap
		for(int i = 0; i < allZones.size(); i++) {
			//calculate if there are any intersections
			List<? extends Shape> intersections = Utilities.findAreaIntersectionsInList(allZones.get(i), allZones.values());
			//if there are, freak out
			if(intersections.size() > 0) {
				System.out.println("ZONES ARE NOT SANE!!!!");
				System.exit(0);
			}
		}

		//make sure the whole area is covered
		Area zoneArea = new Area();
		for(Zone z : allZones.values()) {
			zoneArea.add(new Area(z));
		}

		if(! zoneArea.equals(new Area(BOUNDING_BOX))) {
			System.out.println("Zones don't cover all area");
			System.exit(0);
		}


	}


	private void voronoiZones() {
		//create our points
		double[] xValues = new double[ZONE_COMPLEXITY];
		double[] yValues = new double[ZONE_COMPLEXITY];

		//start with the basezone center location
		xValues[0] = (double) BASE_ZONE_LOC.getX();
		yValues[0] = (double) BASE_ZONE_LOC.getY();

		//add random points
		//make sure they are not inside the basezone
		Point curPoint;
		ArrayList<Point> allPointsToAdd = new ArrayList<Point>();
		allPointsToAdd.add(BASE_ZONE_LOC);
		for(int i = 1; i < ZONE_COMPLEXITY; i++) {
			do {
				//make a random point
				curPoint = new Point((int) (RANDOM_GENERATOR.nextInt((int) (BOUNDING_BOX.getMaxX()-BOUNDING_BOX.getMinX())) + BOUNDING_BOX.getMinX()),
						(int) (RANDOM_GENERATOR.nextInt((int) (BOUNDING_BOX.getMaxY()-BOUNDING_BOX.getMinY())) + BOUNDING_BOX.getMinY()));
			} while(allPointsToAdd.contains(curPoint) || curPoint.distance(BASE_ZONE_LOC) < BASE_ZONE_BUFFER);
			//add it to the list
			xValues[i] = curPoint.x;
			yValues[i] = curPoint.y;
		}

		//now, get the edges from the Voronoi algorithm
		Voronoi vor = new Voronoi(1.0);
		List<GraphEdge> voronoiEdges = vor.generateVoronoi(xValues, yValues, BOUNDING_BOX.getMinX(), BOUNDING_BOX.getMaxX(), BOUNDING_BOX.getMinY(), BOUNDING_BOX.getMaxY());

		//we should have <ZONE_COMPLEXITY> shapes
		@SuppressWarnings("unchecked")
		List<GraphEdge>[] voronoiEdgesOrganizedByShape = (ArrayList<GraphEdge>[]) new ArrayList[ZONE_COMPLEXITY];
		//intitialize them
		for(int i = 0; i < voronoiEdgesOrganizedByShape.length; i++) {
			voronoiEdgesOrganizedByShape[i] = new ArrayList<GraphEdge>();
		}

		//organize the edges into the various shapes
		for(GraphEdge curEdge : voronoiEdges) {
			voronoiEdgesOrganizedByShape[curEdge.site1].add(curEdge);
			voronoiEdgesOrganizedByShape[curEdge.site2].add(curEdge);
		}

		//make them into Zones
		//start with DummyZones
		for(int zoneId = 0; zoneId < voronoiEdgesOrganizedByShape.length; zoneId++) {
			allZones.put(new Integer(zoneId), new DummyZone(voronoiEdgesOrganizedByShape[zoneId], zoneId, new Point2D.Double(xValues[zoneId], yValues[zoneId]), World.BOUNDING_BOX));
		}

		//now, we need to reassign them to non-dummy zones
		for(int zoneId = 0; zoneId < allZones.values().size(); zoneId++) {
			//get the zone
			Integer zoneIdInteger = new Integer(zoneId);
			Zone curZone = allZones.get(zoneIdInteger);

			//see if it should be the BaseZone
			if(curZone.contains(BASE_ZONE_LOC)) {
				allZones.put(zoneIdInteger, new BaseZone(curZone));
				homeBase = (BaseZone) allZones.get(zoneIdInteger);
				continue;
			}
			//otherwise, take a look at it's neighbors and change accordingly
			allZones.put(zoneIdInteger, Zone.changeZoneBasedOnNeighbors(curZone));

		}		
	}

	/**
	 * @return the currentTimestep
	 */
	public static int getCurrentTimestep() {
		return currentTimestep;
	}

	/**
	 * @return the timeBetweenTimesteps
	 */
	public long getTimeBetweenTimesteps() {
		return timeBetweenTimesteps;
	}

	/**
	 * @param timeBetweenTimesteps the timeBetweenTimesteps to set
	 */
	public void setTimeBetweenTimesteps(long timeBetweenTimesteps) {
		this.timeBetweenTimesteps = timeBetweenTimesteps;
	}

	public boolean getDrawBotRadii() {
		return drawBotRadii;
	}


	public void setDrawBotRadii(boolean setValue) {
		drawBotRadii = setValue;
		repaint();
	}

	private boolean keepGoing = false;

	public boolean isGoing() {
		return keepGoing;
	}

	public synchronized void go() {
		//a bit of a hack, but it will work
		this.go(-1, Long.MAX_VALUE);
	}


	public synchronized void go(int numTimestepsToRun, long maxRunTimeMili) {		
		repaint();

		keepGoing = true;

		long timestepStartTime, timestepStopTime, timestepDuration;
		long overallStartTime = System.currentTimeMillis();

		if(firstStartTime == null) {
			firstStartTime = new Date();
			setupFiles();
		}

		//then, start with timesteps
		int endTimestep;
		if(numTimestepsToRun < 0) {
			endTimestep = Integer.MAX_VALUE;
		} else {
			endTimestep = currentTimestep + numTimestepsToRun;
		}

		for(; keepGoing && currentTimestep <= endTimestep && System.currentTimeMillis() - overallStartTime < maxRunTimeMili; currentTimestep++) {			
			System.out.println("************************************");
			System.out.println("On timestep " + currentTimestep);
			System.out.println("Starting at " + new Date());

			timestepStartTime = System.currentTimeMillis();

			//do the zones
			boolean aZoneChanged = false;
			for(Zone z : allZones.values()) {
				//needs to be implimented here rather than in a "doOneTimestep" method because we need to store the results
				//with some probability, each zone is going to change
				//except for the base Zone
				if(z instanceof BaseZone) {
					continue;
				}

				if(World.RANDOM_GENERATOR.nextDouble() < Zone.CHANGE_PROBABILITY) {
					Zone newZone = Zone.changeZoneBasedOnNeighbors(z);
					allZones.put(new Integer(z.getID()), newZone);
					if(! newZone.getClass().equals(z.getClass())) {
						aZoneChanged = true;
					}
				}	
			}

			if(aZoneChanged) {
				//recalculate optimal paths to all points, so we know optimal paths to survivors
				distancesToAllPoints.dijkstras(BASE_ZONE_LOC);
			}

			System.out.println("Done with zones");

			//clean up the list of shouts before the survivors might shout again
			allShouts.clear();

			//do all the survivors
			synchronized (allSurvivors) {
				Iterator<Survivor> iter = allSurvivors.iterator();
				Survivor s;
				while(iter.hasNext()) {
					s = iter.next();
					s.doOneTimestep();
				}
			}
			System.out.println("Done with survivors");

			//do all the bots
			//print out percent checkpoints
			int lastPercentCheckpoint = 0;

			Bot.timestepCohesionMagnitudeTotal = 0.0;
			Bot.timestepSeperationMagnitudeTotal = 0.0;
			Bot.timestepCountOfBotsAffectedBySepOrCohesion = 0;
			Bot.timestepBotsRepelledByZones = 0.0;
			Bot.timestepZoneRepulsionMagnitudeTotal = 0.0;
			Bot.timestepVisibleZoneSideTotal = 0.0;
			Bot.timestepNumVisibleZoneSides = 0;
			Bot.timestepAverageDistanceApartTotal = 0.0;
			Bot.timestepAvgDistBtwnPathNeighbors = 0.0;
			Bot.timestepNumBotOnPaths = 0;


			synchronized (allBots) {
				Iterator<Bot> i = allBots.iterator();
				Bot b;
				while(i.hasNext()) {
					b = i.next();
					b.doOneTimestep();
					if( (b.getID() * 100.0 / allBots.size()) > (lastPercentCheckpoint + 5)) {
						//need to do another checkpoint
						lastPercentCheckpoint += 5;
						System.out.print(lastPercentCheckpoint + "% ");
					}
				}
			}
			System.out.println("");
			System.out.println("Done with bots");

			//			System.out.println("Average seperation vector mag = " + (Bot.timestepSeperationMagnitudeTotal / Bot.timestepCountOfBotsAffectedBySepOrCohesion));
			//			System.out.println("Average cohesion vector mag = " + (Bot.timestepCohesionMagnitudeTotal / Bot.timestepCountOfBotsAffectedBySepOrCohesion));
			//			System.out.println("Average distance between bots = " + (Bot.timestepAverageDistanceApartTotal / Bot.timestepCountOfBotsAffectedBySepOrCohesion));
			//			System.out.println("Average zone repulsion vector mag (for bots near zones) = " + (Bot.timestepZoneRepulsionMagnitudeTotal / Bot.timestepBotsRepelledByZones));
			//			System.out.println("Average visible side segment length =  " + (Bot.timestepVisibleZoneSideTotal / Bot.timestepNumVisibleZoneSides));
			//			System.out.println("");
			//			System.out.println("Avg dist btwn bots on paths = " + (Bot.timestepAvgDistBtwnPathNeighbors / Bot.timestepNumBotOnPaths));
			//			System.out.println(Bot.timestepNumBotOnPaths + " bots marking paths");

			writeADatapoint();

			//repaint the scenario
			repaint(timeBetweenTimesteps);

			timestepStopTime = System.currentTimeMillis();
			timestepDuration = timestepStopTime - timestepStartTime;

			if(timestepDuration < timeBetweenTimesteps) {
				//we need to wait longer
				try {
					wait(timeBetweenTimesteps - timestepDuration);
				} catch (InterruptedException e) {}
			} else {
				//wait just a bit so that the simulation has time to repaint
				try {
					wait(0, 10);
				} catch (InterruptedException e)  {}
			}
		}

	}

	public void stopSimulation() {
		keepGoing = false;
	}


	public void paint(Graphics g) {		
		super.paint(g);

		System.out.println("Starting repaint");

		//		g = this.getGraphics();
		Graphics2D g2d = (Graphics2D) g;

		//clear everything
		g2d.setColor(BACKGROUND_COLOR);
		g2d.fill(BOUNDING_BOX);

		////		get a snapshot of the bots and survivors
		//		allBotSnapshot = allBots.listIterator();
		//		allSurvivorSnapshot = allSurvivors.listIterator();
		//		dijkstrasSnapshot = new Dijkstras(distancesToAllPoints);

		//draw the zones
		g2d.setFont(ZONE_LABEL_FONT);
		for(Zone z : allZones.values()) {
			g2d.setColor(z.getColor());
			g2d.fill(z);
			g2d.setColor(ZONE_OUTLINE_COLOR);
			g2d.draw(z);
		}
		//		for(Zone z : allZones.values()) {
		//			g2d.setColor(LABEL_COLOR);
		//			g2d.drawString("" + z.getID(), (int)z.getCenterX(), (int)z.getCenterY());
		//		}

		//all bots should know about all shouts, so draw them all based on what the first bot knows
		//			Bot firstBot = allBotSnapshot.next();

		//			//go previous one, so that when we start to draw the bots, we'll start at the beginning
		//			if(allBotSnapshot.hasPrevious()) {
		//				allBotSnapshot.previous();
		//			}

		Bot firstBot = allBots.get(0);
		//now, drow all of the shouts
		g2d.setColor(SHOUT_COLOR);
		//		ListIterator<Shout> shoutIterator = firstBot.getShoutIterator();
		//		while(shoutIterator.hasNext()) {
		//			g2d.draw(shoutIterator.next());
		//		}
		synchronized (allShouts) {
			Iterator<Shout> i = allShouts.iterator();
			Shout s;
			while(i.hasNext()) {
				s = i.next();
				g2d.draw(s);
			}
		}

		//draw optimal paths to all survivors
		g2d.setColor(OPTIMAL_SURVIVOR_PATH_COLOR);
		g2d.setStroke(SURVIVOR_PATH_STROKE);
		synchronized (allSurvivors) {
			Iterator<Survivor> i = allSurvivors.iterator();
			Survivor curSur;
			while(i.hasNext()) {
				curSur = i.next();
				//get the DPixel for this survivor
				DPixel curSurPix = distancesToAllPoints.getClosestPixel(curSur.getCenterLocation());
				//draw the first line and it's endpoints
				g2d.drawLine((int)curSur.getCenterX(), (int)curSur.getCenterY(), curSurPix.getX(), curSurPix.getY());
				//make a rectangle for each endpoint
				double ptWidth = 4, ptHeight = 4;
				Rectangle2D endpt = new Rectangle2D.Double(curSur.getCenterX() - (ptWidth/2), curSur.getCenterY() - (ptHeight/2), ptWidth, ptHeight);
				g2d.fill(endpt);
				while(curSurPix.getPrevious() != null) {
					//draw subsequent lines and the endpoint
					//need only draw 1 endpoint, because we'll draw the other endpoint in the next iteration
					g2d.draw(new Line2D.Double(curSurPix.getX(), curSurPix.getY(), curSurPix.getPrevious().getX(), curSurPix.getPrevious().getY()));
					endpt.setRect(curSurPix.getX() - (ptWidth / 2), curSurPix.getY() - (ptHeight/2), ptWidth, ptHeight);
					g2d.fill(endpt);
					curSurPix = distancesToAllPoints.getPixel(curSurPix.getPrevious());
				}
			}
		}

		//paint all the survivor paths
		g2d.setColor(SURVIVOR_PATH_COLOR);
		g2d.setStroke(SURVIVOR_PATH_STROKE);

		//go through each of the bots, looking at their known complete paths
		//keep a list of one's we've drawn, so we don't draw more than once
		List<SurvivorPath> pathsDrawn = new ArrayList<SurvivorPath>();
		synchronized (allBots) {
			Iterator<Bot> iter = allBots.iterator();
			Bot b;
			while(iter.hasNext()) {
				b = iter.next();
				//draw all of it's paths
				Collection<SurvivorPath> survivorPaths = b.getBestKnownCompletePaths().values();

				for(SurvivorPath sp : survivorPaths) {
					if(! pathsDrawn.contains(sp)) {
						//draw each segment in the path
						if(sp.getPoints().size() > 1) {
							for(int i = 1; i < sp.getPoints().size(); i++) {
								g2d.draw(new Line2D.Double(sp.getPoints().get(i-1).getCenterLocation(), sp.getPoints().get(i).getCenterLocation()));
							}
						}
						g2d.draw(new Line2D.Double(sp.getPoints().get(sp.getPoints().size() - 1).getCenterLocation(), sp.getEndPoint()));
						pathsDrawn.add(sp);
					}
				}
			}
		}


		g2d.setStroke(new BasicStroke());

		//draw all the survivors
		g2d.setColor(SURVIVOR_COLOR);
		//		while(allSurvivorSnapshot.hasNext()) {
		//			Survivor curSur = allSurvivorSnapshot.next();
		//
		//			g2d.fill(curSur);
		//		}
		synchronized (allSurvivors) {
			Iterator<Survivor> i = allSurvivors.iterator();
			Survivor curSur;
			while(i.hasNext()) {
				curSur = i.next();
				g2d.fill(curSur);
			}
		}

		//draw all the bots and their radii and their labels
		g2d.setFont(BOT_LABEL_FONT);
		//		while(allBotSnapshot.hasNext()) {
		//			Bot curBot = allBotSnapshot.next();
		synchronized (allBots) {
			Iterator<Bot> iter = allBots.iterator();
			Bot curBot;
			while(iter.hasNext()) {
				curBot = iter.next();

				//			g2d.setColor(BOT_COLOR);
				//			g2d.fill(curBot);

				if(drawBotRadii) {
					g2d.setColor(AUDIO_RANGE_COLOR);
					g2d.draw(curBot.getAuditbleArea());

					g2d.setColor(VISIBLE_RANGE_COLOR);
					g2d.draw(curBot.getVisibleArea());

					g2d.setColor(BROADCAST_RANGE_COLOR);
					g2d.draw(curBot.getBroadcastArea());
				}

				//			g2d.setColor(LABEL_COLOR);
				//			g2d.drawString("" + curBot.getID(), (float) (curBot.getX()), (float) (curBot.getY() + curBot.getHeight()));

				g2d.setColor(BOT_MOVEMENT_VECTOR_COLOR);
				//only draw it if it has non-zero length
				if(! Utilities.shouldEqualsZero(curBot.getMovementVector().getMagnitude())) {
					g2d.draw(curBot.getMovementVector().rescale(-5.0));
				}
			}
		}

		//		allBotSnapshot = allBots.listIterator();
		//		while(allBotSnapshot.hasNext()) {
		//			Bot curBot = allBotSnapshot.next();
		synchronized (allBots) {
			Iterator<Bot> iter = allBots.iterator();
			Bot curBot;
			while(iter.hasNext()) {
				curBot = iter.next();

				switch(curBot.getBotMode()) {
					case(Bot.WAITING_FOR_ACTIVATION):
						g2d.setColor(DEACTIVATED_BOT_COLOR);
					break;
					case(Bot.EXPLORER):
						g2d.setColor(EXPLORER_BOT_COLOR);
					break;
					case(Bot.PATH_MARKER):
						g2d.setColor(PATH_MARKER_BOT_COLOR);
					break;
				}
				g2d.fill(curBot);

				g2d.setColor(LABEL_COLOR);
				g2d.drawString("" + curBot.getID(), (float) (curBot.getX()), (float) (curBot.getY() + curBot.getHeight()));
			}
		}


		//		if(WORLD_DEBUG) {
		//			//draw the shapes in the debug arraylist
		//			g2d.setColor(Color.cyan);
		//			g2d.setStroke(new BasicStroke(1));
		//			for(Shape s : debugShapesToDraw) {
		//				g2d.draw(s);
		//			}
		//
		//			//			g2d.setColor(Color.red);
		//			//			for(Shape s : debugSeperationVectors) {
		//			//				g2d.draw(s);
		//			//			}
		//			//			debugSeperationVectors.clear();
		//			//			
		//			//			g2d.setColor(Color.blue);
		//			//			for(Shape s : debugRepulsionVectors) {
		//			//				g2d.draw(s);
		//			//			}
		//			//			debugRepulsionVectors.clear();
		//		}

		//		debugShapesToDraw.clear();


		System.out.println("Done with repaint");
	}

	//figures out which zones the passed point is in, and returns it.
	//zones should not overlap, so there should only be one solution
	public static Zone findZone(Point2D point) {

		for(Zone z : allZones.values()) {
			if(z.contains(point)) {
				return z;
			}
		}

		//from some weird reason, the Veronoi algorithm will sometimes leave out a few pixels
		//if we get here, that's happened
		//basically, do the voronoi for it - try to find which zone's point is closest to this point
		double minimumDistance = Double.MAX_VALUE;
		Zone closestZone = null;
		for(Zone z : allZones.values()) {
			double curDist = point.distance(z.getCenterLocation());
			if(curDist < minimumDistance) {
				minimumDistance = curDist;
				closestZone = z;
			}
		}

		return closestZone;
	}

	public static JFrame createAndShowGUI(World w) {
		//create a new World Frame
//		World w = new World();		
		JFrame frame = new JFrame("Swarm Simulation");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		frame.setSize(w.getWidth(), w.getHeight() + MENUBAR_HEIGHT);
//		frame.setResizable(false);
		frame.add(w);
		w.setLocation(0, 0);

		frame.pack();
//		frame.setVisible(true);		
		return frame;
	}

	public static void main(String[] args) {
		//		//ask for a specific zone and survivor combination
		//		File zoneDir = null, surDir = null;
		//		JFileChooser zoneDirChooser = new JFileChooser();
		//		zoneDirChooser.setCurrentDirectory(new File("."));
		//		zoneDirChooser.setDialogTitle("Choose a zone directory, or cancel for random creation");
		//		zoneDirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		//		zoneDirChooser.setAcceptAllFileFilterUsed(false);
		//
		//		if(zoneDirChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
		//			zoneDir = zoneDirChooser.getSelectedFile();
		//		} 
		//
		//		JFileChooser surDirChooser = new JFileChooser();
		//		surDirChooser.setCurrentDirectory(new File("."));
		//		surDirChooser.setDialogTitle("Choose a survivor directory, or cancel to place them randomly");
		//		surDirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		//		surDirChooser.setAcceptAllFileFilterUsed(false);
		//
		//		if(surDirChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
		//			surDir = surDirChooser.getSelectedFile();
		//		}

		//get zones dir from args
		if(args.length != 1) {
			System.out.println("Incorrect number of arguments passed");
			System.exit(0);
		}

		File zoneDir = new File(args[0]);
		File surDir = null;

		//keep a max run time of about 15 minutes
		//these are running in about 2, so 15 min is enough
		//15 min = 900,000 miliseconds
		final long maxRunTime = 900000;


		World world;

		for(int numSur = 1; numSur <= 10; numSur+=1) {
			for(int numBots = 10; numBots <= 100; numBots += 10) {
				//run each test 5 times, so that we get a good range of numbers
				for(int i = 0; i < 5; i++) {
					if(zoneDir != null) {
						if(surDir != null) {
							world = new World(numBots, surDir, zoneDir);
						} else {
							world = new World(numBots, numSur, zoneDir);
						}
					} else {
						if(surDir != null) {
							world = new World(numBots, surDir);
						} else {
							world = new World(numBots, numSur);
						}
					}
					//TODO add a set location?
					//world.setLocation(200, 200);
//					world.setVisible(true);
					//do a gc to clean up?
					System.gc();
					//go for 1800 timesteps = 30 min - should be enough time to settle down
					world.go(1800, maxRunTime);
//					world.dispose();
				}
			}
		}
	}
}
