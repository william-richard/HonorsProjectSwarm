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
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

import javax.swing.JFrame;

import util.Utilities;
import zones.BaseZone;
import zones.BoundingBox;
import zones.DangerZone;
import zones.Fire;
import zones.SafeZone;
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

	private static final boolean DRAW_BOT_RADII = false;

	private static final boolean WORLD_DEBUG = false;

	private static final int ZONE_COMPLEXITY = 10;

	private static final Color BACKGROUND_COLOR = Color.white;
	private static final Color BOT_COLOR = Color.green;
	private static final Color SURVIVOR_COLOR = Color.red;
	private static final Color SHOUT_COLOR = new Color(30, 144, 255);
	private static final Color VISIBLE_RANGE_COLOR = new Color(255,106,106);
	private static final Color AUDIO_RANGE_COLOR = new Color(205,102,0);
	private static final Color BROADCAST_RANGE_COLOR = Color.yellow;
	private static final Color BOT_LABEL_COLOR = Color.black;
	private static final Color ZONE_OUTLINE_COLOR = Color.black;
	private static final Color SURVIVOR_PATH_COLOR = new Color(0,191,255);
	private static final Color BOT_MOVEMENT_VECTOR_COLOR = Color.white;

	private static final Stroke SURVIVOR_PATH_STROKE = new BasicStroke((float) 2.0);

	private static final Font BOT_LABEL_FONT = new Font("Serif", Font.BOLD, 10);
	private static final Font ZONE_LABEL_FONT = new Font("Serif", Font.BOLD, 12);

	/** VARIABLES */
	public static List<Zone> allZones; //The zones in the world - should be non-overlapping
	public static List<Bot> allBots; //List of the Bots, so we can do stuff with them
	public static List<Survivor> allSurvivors; //The survivors
	public ListIterator<Bot> allBotSnapshot;
	public ListIterator<Survivor> allSurvivorSnapshot;

	private BaseZone homeBase;

	public static List<Shape> debugShapesToDraw;
	public static List<Shape> debugSeperationVectors;
	public static List<Shape> debugRepulsionVectors;

	private static int currentTimestep; //keep track of what time it is
	private long timeBetweenTimesteps; //store the time in milliseconds

	public World() {
		this(40, 2, 5000);
	}

	public World(int numBots, int numSurvivors, long _timeBetweenTimesteps) {
		super("Swarm Simulation");
		//start with the frame.
		setupFrame();

		//initialize the zones
		allZones = new ArrayList<Zone>();

		//add all the default zones
		addAllSetZones();
	
		//fill in the rest randomly
		fillInZones();

		checkZoneSanity();

		//initialize the bots
		allBots = new ArrayList<Bot>();

		Rectangle2D startingZoneBoundingBox = homeBase.getBounds2D();

		for(int i = 0; i < numBots; i++) {
			allBots.add(new Bot(this, startingZoneBoundingBox.getCenterX(), startingZoneBoundingBox.getCenterY(), numBots, i, homeBase, BOUNDING_BOX));
		}

		//need to randomly distribute the bots a bit
		for(Bot b : allBots) {
			b.moveRandomly();
		}

		//initialize the survivors
		allSurvivors = new ArrayList<Survivor>();
		Survivor curSurvivor;

		for(int i = 0; i < numSurvivors; i++) {
			curSurvivor = new Survivor(RANDOM_GENERATOR.nextDouble()*FRAME_WIDTH, RANDOM_GENERATOR.nextDouble()*(FRAME_HEIGHT-MENUBAR_HEIGHT) + MENUBAR_HEIGHT, RANDOM_GENERATOR.nextDouble());
			allSurvivors.add(curSurvivor);
		}

		debugShapesToDraw = new ArrayList<Shape>();
		debugSeperationVectors = new ArrayList<Shape>();
		debugRepulsionVectors = new ArrayList<Shape>();

		currentTimestep = 0;
		setTimeBetweenTimesteps(_timeBetweenTimesteps);
	}

	private void setupFrame() {
		setSize(FRAME_WIDTH, FRAME_HEIGHT);
		setResizable(false);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setBackground(BACKGROUND_COLOR);
	}

	public void checkZoneSanity() {
		//check each zones's area with all the rest to make sure they don't overlap
		for(int i = 0; i < allZones.size(); i++) {
			//calculate if there are any intersections
			List<? extends Shape> intersections = Utilities.findAreaIntersectionsInList(allZones.get(i), allZones.subList(i+1, allZones.size()));
			//if there are, freak out
			if(intersections.size() > 0) {
				System.out.println("ZONES ARE NOT SANE!!!!");
				System.exit(0);
			}
		}

		//make sure the whole area is covered
		Area zoneArea = new Area();
		for(Zone z : allZones) {
			zoneArea.add(new Area(z));
		}

		if(! zoneArea.equals(new Area(BOUNDING_BOX))) {
			System.out.println("Zones don't cover all area");
			System.exit(0);
		}


	}

	private void addAllSetZones() {
		//start with the base zone
		addBaseZone();
		
		//the rest are not as necessary
		//put some triangles of safe zones around the base zone
		int[] xPointsSafe = {225, 250, 275};
		int[] yPointsSafe = {225, 200, 225};
		SafeZone safe = new SafeZone(xPointsSafe, yPointsSafe, 3, allZones.size());
		allZones.add(safe);
		
		xPointsSafe = new int[] {275, 300, 275};
		yPointsSafe = new int[] {225, 250, 275};
		safe = new SafeZone(xPointsSafe, yPointsSafe, 3, allZones.size());
		allZones.add(safe);
		
		xPointsSafe = new int[] {275, 250, 225};
		yPointsSafe = new int[] {275, 300, 275};
		safe = new SafeZone(xPointsSafe, yPointsSafe, 3, allZones.size());
		allZones.add(safe);
		
		xPointsSafe = new int[] {225, 200, 225};
		yPointsSafe = new int[] {225, 250, 275};
		safe = new SafeZone(xPointsSafe, yPointsSafe, 3, allZones.size());
		allZones.add(safe);


	}
	
	private void addBaseZone() {
		int[] xPointsBase = {225, 275, 275, 225};
		int[] yPointsBase = {225, 225, 275, 275};
		BaseZone homeBase = new BaseZone(xPointsBase, yPointsBase, 4, 0);
		allZones.add(homeBase);
		this.homeBase = homeBase;
	}
	
	
	//TODO combine same type zones?

	private void fillInZones() {
		//first, get all unfilled zones
		Area filledAreas = new Area();
		for(Zone z : allZones) {
			filledAreas.add(new Area(z));
		}

		Area unfilledArea = new Area(BOUNDING_BOX);
		unfilledArea.subtract(filledAreas);

		List<Point> zoneVerticies = Utilities.getVerticies(unfilledArea);

		//the ZONE_COMPLEXITY constant basically defines how many extra verticies in Zones we should add
		//so add them
		int numLeftToAdd = ZONE_COMPLEXITY;
		while(numLeftToAdd > 0) {
			//make a random point
			Point newPoint = new Point(RANDOM_GENERATOR.nextInt(FRAME_WIDTH), RANDOM_GENERATOR.nextInt(FRAME_HEIGHT));
			//make sure the point is in the area
			if(! unfilledArea.contains(newPoint)) {
				continue;
			}
			//make sure it isn't already added
			if(zoneVerticies.contains(newPoint)) {
				continue;
			}
			//if we've gotten here, the point is safe to add
			zoneVerticies.add(newPoint);
			numLeftToAdd--;
		}

		//now, start making arbitrary triangles and see if they overlap with any existing zones
		//if they don't, add them to the zones list
		while(! unfilledArea.isEmpty()) {
			//choose 3 points randomly
			Point p1 = zoneVerticies.remove(RANDOM_GENERATOR.nextInt(zoneVerticies.size()));
			Point p2 = zoneVerticies.remove(RANDOM_GENERATOR.nextInt(zoneVerticies.size()));
			Point p3 = zoneVerticies.remove(RANDOM_GENERATOR.nextInt(zoneVerticies.size()));
			
			int[] xPoints = {(int) p1.getX(), (int) p2.getX(), (int) p3.getX()};
			int[] yPoints = {(int) p1.getY(), (int) p2.getY(), (int) p3.getY()};

			//make a zones out of them
			Zone newZone;

			
			switch(RANDOM_GENERATOR.nextInt(3)) {
				case 0: newZone = new SafeZone(xPoints, yPoints, 3, allZones.size()); break; 
				case 1: newZone = new DangerZone(xPoints, yPoints, 3, allZones.size()); break;
				case 2: newZone = new Fire(xPoints, yPoints, 3, allZones.size()); break;
				default: newZone = new SafeZone(xPoints, yPoints, 3, allZones.size()); break;  
			}

			//make sure it doesn't intersect any existing zones
			try {
				if(Utilities.findAreaIntersectionsInList(newZone, allZones).size() > 0) {
					zoneVerticies.add(p1);
					zoneVerticies.add(p2);
					zoneVerticies.add(p3);
					continue;
				}
			} catch(IllegalArgumentException e) {
				//if we got an IllegalArgument exception, it means we tried to pass an arealess shape
				//try again
				zoneVerticies.add(p1);
				zoneVerticies.add(p2);
				zoneVerticies.add(p3);
				continue;
			}
			
			//it checks out - add it
			allZones.add(newZone);
			//remove it's area from the unfilled area
			unfilledArea.subtract(new Area(newZone));
			
			//TODO find a way to combine same-type zones that share sides

			//only put points back into the array if they are still in the unfilled area
			zoneVerticies.add(p1);

			zoneVerticies.add(p2);

			zoneVerticies.add(p3);
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

	private boolean keepGoing = false;

	public boolean isGoing() {
		return keepGoing;
	}

	public synchronized void go() {		
		repaint();

		keepGoing = true;

		long timestepStartTime, timestepStopTime, timestepDuration;

		//then, start with timesteps
		for(; keepGoing; currentTimestep++) {			
			System.out.println("************************************");
			System.out.println("On timestep " + currentTimestep);

			timestepStartTime = System.currentTimeMillis();

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

			//repaint the scenario
			repaint();
			System.out.println("Done with repaint");

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

		//draw the zones
		g2d.setFont(ZONE_LABEL_FONT);
		for(Zone z : allZones) {
			g2d.setColor(z.getColor());
			g2d.fill(z);
			g2d.setColor(ZONE_OUTLINE_COLOR);
			g2d.draw(z);
		}

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
		
		//draw all the bots and their radii and their labels
		g2d.setFont(BOT_LABEL_FONT);
		while(allBotSnapshot.hasNext()) {
			Bot curBot = allBotSnapshot.next();

//			g2d.setColor(BOT_COLOR);
//			g2d.fill(curBot);

			if(DRAW_BOT_RADII) {
				g2d.setColor(AUDIO_RANGE_COLOR);
				g2d.draw(curBot.getAuditbleArea());

				g2d.setColor(VISIBLE_RANGE_COLOR);
				g2d.draw(curBot.getVisibleArea());

				g2d.setColor(BROADCAST_RANGE_COLOR);
				g2d.draw(curBot.getBroadcastArea());
			}

//			g2d.setColor(BOT_LABEL_COLOR);
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

			g2d.setColor(BOT_COLOR);
			g2d.fill(curBot);
			
			g2d.setColor(BOT_LABEL_COLOR);
			g2d.drawString("" + curBot.getID(), (float) (curBot.getX()), (float) (curBot.getY() + curBot.getHeight()));
		}
		
		
		//paint all the survivor paths
		g2d.setColor(SURVIVOR_PATH_COLOR);
		g2d.setStroke(SURVIVOR_PATH_STROKE);
		//only basezones have survivorPaths
		for(Zone z : allZones) {
			//skip everything that isn't a BaseZone
			if(! (z instanceof BaseZone)) continue;

			System.out.println("Got a base zone - printing out it's paths");
			
			BaseZone bz = (BaseZone) z;

			//draw all of is paths
			List<SurvivorPath> survivorPaths= bz.getBestSurvivorPaths();

			for(SurvivorPath sp : survivorPaths) {
				g2d.draw(sp);
			}

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


	}

	//figures out which zones the passed point is in, and returns it.
	//zones should not overlap, so there should only be one solution
	public static Zone findZone(Point2D point) {

		for(Zone z : allZones) {
			if(z.contains(point)) {
				return z;
			}
		}

		System.out.println("Could not find a zone for " + point.getX() + ", " + point.getY());

		return null;
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
