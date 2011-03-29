package simulation;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

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


public class World extends JFrame implements WindowListener {

	private static final long serialVersionUID = -2526080354915012922L;

	/***************************************************************************
	 * CONSTANTS
	 **************************************************************************/
	public static final Random RANDOM_GENERATOR = new Random();
	private static final int MENUBAR_HEIGHT = 21;
	private static final int FRAME_HEIGHT = 500 + MENUBAR_HEIGHT;
	private static final int FRAME_WIDTH = 500;
	public static final BoundingBox BOUNDING_BOX = new BoundingBox(0, MENUBAR_HEIGHT, FRAME_WIDTH, FRAME_HEIGHT - MENUBAR_HEIGHT);

	private static final boolean WORLD_DEBUG = false;

	private static final int ZONE_COMPLEXITY = 100; //should be btwn ~ (Min(FRAME_HEIGHT, FRAME_WIDTH) / 10) and (FRAME_HEIGHT * FRAME_WIDTH)

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
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM.dd.yy-HH;mm;ss");
	private static final DecimalFormat DOUBLE_FORMAT = new DecimalFormat("#.####");

	/** VARIABLES */
	public static Hashtable<Integer, Zone> allZones; //The zones in the world - should be non-overlapping
	public static List<Bot> allBots; //List of the Bots, so we can do stuff with them
	public static List<Survivor> allSurvivors; //The survivors

	public ListIterator<Bot> allBotSnapshot;
	public ListIterator<Survivor> allSurvivorSnapshot;
//	public Dijkstras dijkstrasSnapshot;

	private BaseZone homeBase;

	public static List<Shape> debugShapesToDraw;
	public static List<Shape> debugSeperationVectors;
	public static List<Shape> debugRepulsionVectors;

	private static int currentTimestep; //keep track of what time it is
	private long timeBetweenTimesteps; //store the time in milliseconds
	private boolean drawBotRadii = false;

	private Dijkstras distancesToAllPoints;

	private Date firstStartTime = null;

	private String dataDirectory;

	public World() {
		this(40, 2, 5000, false);
	}

	public World(int numBots, int numSurvivors, long _timeBetweenTimesteps, boolean _drawBotRadii) {
		super("Swarm Simulation");
		//start with the frame.
		setupFrame();

		//initialize the zones
		allZones = new Hashtable<Integer, Zone>();

		//set them up using the Voronoi Algorithm
		voronoiZones();
		//		checkZoneSanity();

		//initialize the bots
		allBots = new ArrayList<Bot>();

		Rectangle2D startingZoneBoundingBox = homeBase.getBounds2D();

		for(int i = 0; i < numBots; i++) {
			allBots.add(new Bot(this, startingZoneBoundingBox.getCenterX(), startingZoneBoundingBox.getCenterY(), numBots, i, homeBase, BOUNDING_BOX));
		}

		//initialize the survivors
		allSurvivors = new ArrayList<Survivor>();
		Survivor curSurvivor;

		for(int i = 0; i < numSurvivors; i++) {
			//don't let survivors be in the basezone
			do {
				curSurvivor = new Survivor(RANDOM_GENERATOR.nextDouble()*FRAME_WIDTH, RANDOM_GENERATOR.nextDouble()*(FRAME_HEIGHT-MENUBAR_HEIGHT) + MENUBAR_HEIGHT, RANDOM_GENERATOR.nextDouble());
			} while( homeBase.contains(curSurvivor.getCenterLocation()));

			allSurvivors.add(curSurvivor);
		}

		debugShapesToDraw = new ArrayList<Shape>();
		debugSeperationVectors = new ArrayList<Shape>();
		debugRepulsionVectors = new ArrayList<Shape>();

		currentTimestep = 0;
		setTimeBetweenTimesteps(_timeBetweenTimesteps);
		setDrawBotRadii(_drawBotRadii);

		//		distancesToAllPoints = new Dijkstras(0, FRAME_WIDTH, MENUBAR_HEIGHT, FRAME_HEIGHT);
		distancesToAllPoints = new Dijkstras(0, FRAME_WIDTH, MENUBAR_HEIGHT, FRAME_HEIGHT);		
		distancesToAllPoints.dijkstras(BASE_ZONE_LOC);
	}

	private void setupFrame() {
		setSize(FRAME_WIDTH, FRAME_HEIGHT);
		setResizable(false);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setBackground(BACKGROUND_COLOR);
	}

	private void setupFiles() {
		//make the directory for this run
		dataDirectory = "data/" + DATE_FORMAT.format(firstStartTime) + "/";
		new File(dataDirectory).mkdir();
		//create the information about number of bots, survivors etc
		try {
			BufferedWriter infoWriter = new BufferedWriter(new FileWriter(dataDirectory + "info.txt"));
			infoWriter.write("bots = " + allBots.size());
			infoWriter.newLine();
			infoWriter.write("sur = " + allSurvivors.size());
			infoWriter.newLine();
			//TODO eventually, put map info here too?
			infoWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private double calcPercentSurFound() {
		HashSet<Survivor> allClaimedSurvivors = new HashSet<Survivor>();
		for(Bot b : allBots) {
			allClaimedSurvivors.addAll(b.getClaimedSurvivors());
		}
		return ((double)allClaimedSurvivors.size()) / allSurvivors.size();
	}

	private double calcAvgPathQuality() {
		HashMap<Survivor, SurvivorPath> bestCompletePaths = new HashMap<Survivor, SurvivorPath>();
		for(Bot b : allBots) {
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

		//now we should have all the best paths known by any bot to each survivor
		//get the length of each of these paths, and compare them to the optimal lengths computed by Dijkstra's
		double pathPercentagesSum = 0.0;
		for(Survivor sur : bestCompletePaths.keySet()) {
			SurvivorPath botPath = bestCompletePaths.get(sur);
			pathPercentagesSum += (botPath.getPathLength() / distancesToAllPoints.getDistanceTo(botPath.getSur().getCenterLocation()));
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
		for(Bot b : allBots) {
			if(b.getBotMode() == Bot.PATH_MARKER) {
				coverageMetricSum += b.isPathDensityAcceptable() ? 1 : 0;
				numPathMarkers++;
			}
		}
		
		System.out.println("coverage sum = " + coverageMetricSum + "\tnum path markers = " + numPathMarkers);
		//return the average value
		double pathCoverage = ((double) coverageMetricSum) / numPathMarkers;
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
			dataWriter.write(World.getCurrentTimestep() + "\t" + DOUBLE_FORMAT.format(perSurFound) + '\t' + DOUBLE_FORMAT.format(pathQuality) + '\t' + DOUBLE_FORMAT.format(pathCoverage) + '\t' + DOUBLE_FORMAT.format(overallMetric));
			dataWriter.newLine();
			dataWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
				//TODO don't allow new points if they are too close to BASE_ZONE_LOC
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
	}
	
	private boolean keepGoing = false;

	public boolean isGoing() {
		return keepGoing;
	}

	public synchronized void go() {		
		repaint();

		keepGoing = true;

		long timestepStartTime, timestepStopTime, timestepDuration;

		if(firstStartTime == null) {
			firstStartTime = new Date();
			setupFiles();
		}

		//then, start with timesteps
		for(; keepGoing; currentTimestep++) {			
			System.out.println("************************************");
			System.out.println("On timestep " + currentTimestep);

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



			//do all the survivors
			for(Survivor s : allSurvivors) {
				s.doOneTimestep();
			}
			System.out.println("Done with survivors");

			//do all the bots
			//print out percent checkpoints
			double lastPercentCheckpoint = 0.0;

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


			for(Bot b : allBots) {
				b.doOneTimestep();
				if( (b.getID() * 100.0 / allBots.size()) > (lastPercentCheckpoint + 10)) {
					//need to do another checkpoint
					lastPercentCheckpoint += 10;
					System.out.print(lastPercentCheckpoint + "% ");
				}
			}
			System.out.println("");
			System.out.println("Done with bots");

			System.out.println("Average seperation vector mag = " + (Bot.timestepSeperationMagnitudeTotal / Bot.timestepCountOfBotsAffectedBySepOrCohesion));
			System.out.println("Average cohesion vector mag = " + (Bot.timestepCohesionMagnitudeTotal / Bot.timestepCountOfBotsAffectedBySepOrCohesion));
			System.out.println("Average distance between bots = " + (Bot.timestepAverageDistanceApartTotal / Bot.timestepCountOfBotsAffectedBySepOrCohesion));
			System.out.println("Average zone repulsion vector mag (for bots near zones) = " + (Bot.timestepZoneRepulsionMagnitudeTotal / Bot.timestepBotsRepelledByZones));
			System.out.println("Average visible side segment length =  " + (Bot.timestepVisibleZoneSideTotal / Bot.timestepNumVisibleZoneSides));
			System.out.println("");
			System.out.println("Avg dist btwn bots on paths = " + (Bot.timestepAvgDistBtwnPathNeighbors / Bot.timestepNumBotOnPaths));
			System.out.println(Bot.timestepNumBotOnPaths + " bots marking paths");

			writeADatapoint();

			//repaint the scenario
			repaint(timeBetweenTimesteps);

			int qLength = distancesToAllPoints.rwl.getQueueLength();
			System.out.println("Dijkstra lock queue length = " + qLength);

			timestepStopTime = System.currentTimeMillis();
			timestepDuration = timestepStopTime - timestepStartTime;

			//			try {
			//				wait(timeBetweenTimesteps, 1);
			//			} catch (InterruptedException e) {}
			if(timestepDuration < timeBetweenTimesteps) {
				//we need to wait longer
				try {
					wait(timeBetweenTimesteps - timestepDuration);
				} catch (InterruptedException e) {}
			} else {
				//wai just a bit so that the simulation has time to repaint
				try {
					wait(0, 1);
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

		g = this.getGraphics();
		Graphics2D g2d = (Graphics2D) g;

		//clear everything
		g2d.setColor(BACKGROUND_COLOR);
		g2d.fill(BOUNDING_BOX);

		//get a snapshot of the bots and survivors
		allBotSnapshot = allBots.listIterator();
		allSurvivorSnapshot = allSurvivors.listIterator();
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
		Bot firstBot = (Bot) allBotSnapshot.next().clone();
		//go previous one, so that when we start to draw the bots, we'll start at the beginning
		allBotSnapshot.previous();

		//now, drow all of the shouts
		g2d.setColor(SHOUT_COLOR);
		ListIterator<Shout> shoutIterator = firstBot.getShoutIterator();
		while(shoutIterator.hasNext()) {
			g2d.draw(shoutIterator.next());
		}

		//draw all the survivors
		g2d.setColor(SURVIVOR_COLOR);
		while(allSurvivorSnapshot.hasNext()) {
			Survivor curSur = allSurvivorSnapshot.next();

			g2d.fill(curSur);
		}

		//draw optimal paths to all survivors
		g2d.setColor(OPTIMAL_SURVIVOR_PATH_COLOR);
		g2d.setStroke(SURVIVOR_PATH_STROKE);
		for(Survivor curSur : allSurvivors) {
			System.out.println("Painting " + curSur);
			//get the DPixel for this survivor
			DPixel curSurPix = distancesToAllPoints.getClosestPixel(curSur.getCenterLocation());
			//draw the first line
			g2d.drawLine((int)curSur.getCenterX(), (int)curSur.getCenterY(), curSurPix.getX(), curSurPix.getY());
			while(curSurPix.getPrevious() != null) {
				System.out.println("While painting " + curSur + ", painting DPixel " + curSurPix);
				g2d.draw(new Line2D.Double(curSurPix.getX(), curSurPix.getY(), curSurPix.getPrevious().getX(), curSurPix.getPrevious().getY()));
				curSurPix = distancesToAllPoints.getPixel(curSurPix.getPrevious());
			}			
			System.out.println("Done painting " + curSur);
		}
		
		//paint all the survivor paths
		g2d.setColor(SURVIVOR_PATH_COLOR);
		g2d.setStroke(SURVIVOR_PATH_STROKE);

		//go through each of the bots, looking at their known complete paths
		//keep a list of one's we've drawn, so we don't draw more than once
		List<SurvivorPath> pathsDrawn = new ArrayList<SurvivorPath>();
		for(Bot b : allBots) {
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


		g2d.setStroke(new BasicStroke());

		//draw all the bots and their radii and their labels
		g2d.setFont(BOT_LABEL_FONT);
		while(allBotSnapshot.hasNext()) {
			Bot curBot = allBotSnapshot.next();

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

		allBotSnapshot = allBots.listIterator();
		while(allBotSnapshot.hasNext()) {
			Bot curBot = allBotSnapshot.next();

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


		if(WORLD_DEBUG) {
			//draw the shapes in the debug arraylist
			g2d.setColor(Color.cyan);
			g2d.setStroke(new BasicStroke(1));
			for(Shape s : debugShapesToDraw) {
				g2d.draw(s);
			}
			debugShapesToDraw.clear();

			//			g2d.setColor(Color.red);
			//			for(Shape s : debugSeperationVectors) {
			//				g2d.draw(s);
			//			}
			//			debugSeperationVectors.clear();
			//			
			//			g2d.setColor(Color.blue);
			//			for(Shape s : debugRepulsionVectors) {
			//				g2d.draw(s);
			//			}
			//			debugRepulsionVectors.clear();
		}
		
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
		//TODO MAYBE? Have zones store these extra points so we only have to do this once? Probably will only do this a handful of times anyway..... maybe not worth it
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

	public static void createAndShowGUI() {
		//create a new World
		World w = new World();
		w.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		w.pack();
		w.setVisible(true);		
	}

	@Override
	public void windowActivated(WindowEvent e) {
		repaint();
	}

	@Override
	public void windowClosed(WindowEvent e) {}

	@Override
	public void windowClosing(WindowEvent e) {
		stopSimulation();
		setVisible(false);
	}
	@Override
	public void windowDeactivated(WindowEvent e) {
		repaint();
	}

	@Override
	public void windowDeiconified(WindowEvent e) {}

	@Override
	public void windowIconified(WindowEvent e) {}

	@Override
	public void windowOpened(WindowEvent e) {}

}
