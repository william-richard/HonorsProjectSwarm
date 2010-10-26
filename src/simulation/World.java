package simulation;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
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
import zones.Building;
import zones.DangerDebris;
import zones.DangerZone;
import zones.SafeDebris;
import zones.SafeZone;
import zones.Zone;


public class World extends JFrame {

	/***************************************************************************
	 * CONSTANTS
	 **************************************************************************/
	public static final Random RAMOM_GENERATOR = new Random();
	private static final int MENUBAR_HEIGHT = 21;
	private static final int FRAME_HEIGHT = 500 + MENUBAR_HEIGHT;
	private static final int FRAME_WIDTH = 500;
	public static final BoundingBox BOUNDING_BOX = new BoundingBox(0, MENUBAR_HEIGHT, FRAME_WIDTH, FRAME_HEIGHT - MENUBAR_HEIGHT);

	private static final boolean DRAW_BOT_RADII = true;

	private static final boolean WORLD_DEBUG = true;

	private static final int ZONE_COMPLEXITY = 20;

	private static final Color BACKGROUND_COLOR = Color.white;
	private static final Color BOT_COLOR = Color.green;
	private static final Color VICTIM_COLOR = Color.red;
	private static final Color SHOUT_COLOR = new Color(30, 144, 255);
	private static final Color VISIBLE_RANGE_COLOR = new Color(255,106,106);
	private static final Color AUDIO_RANGE_COLOR = new Color(205,102,0);
	private static final Color BROADCAST_RANGE_COLOR = Color.yellow;
	private static final Color BOT_LABEL_COLOR = Color.black;
	private static final Color ZONE_LABEL_COLOR = Color.black;
	private static final Color ZONE_OUTLINE_COLOR = Color.black;
	private static final Color VICTIM_PATH_COLOR = new Color(0,191,255);
	private static final Color BOT_MOVEMENT_VECTOR_COLOR = Color.white;

	private static final Stroke VICTIM_PATH_STROKE = new BasicStroke((float) 2.0);	

	private static final Font BOT_LABEL_FONT = new Font("Serif", Font.BOLD, 10);
	private static final Font ZONE_LABEL_FONT = new Font("Serif", Font.BOLD, 12);

	private static final long serialVersionUID = 1L;

	/** VARIABLES */
	public static List<Zone> allZones; //The zones in the world - should be non-overlapping
	public static List<Bot> allBots; //List of the Bots, so we can do stuff with them
	public static List<Survivor> allSurvivors; //The Victims
	public ListIterator<Bot> allBotSnapshot;
	public ListIterator<Survivor> allSurvivorSnapshot;

	public static List<Shape> debugShapesToDraw;

	private Zone baseZone;
	
	public static int currentTimestep; //keep track of what time it is


	public World() {
		super("Swarm Simulation");
		//start with the frame.
		setupFrame();

		//this is with default values, mostly for debugging
		int numBots = 40;
		int numVic = 2;

		//initialize the zones
		allZones = new ArrayList<Zone>();

		int[] xPointsBase = {225, 275, 275, 225};
		int[] yPointsBase = {225, 225, 275, 275};
		//		int[] xPointsBase = {MENUBAR_HEIGHT, 50, 50, MENUBAR_HEIGHT};
		//		int[] yPointsBase = {MENUBAR_HEIGHT, MENUBAR_HEIGHT, 50, 50};
		Zone homeBase = new BaseZone(xPointsBase, yPointsBase, 4, 0);
		baseZone = homeBase;
		allZones.add(homeBase);

		//		Zone building = new Building(150, 150, 150, 150, allZones.size());
		//		allZones.add(building);


		//		int[] xPointsFire = {0, 			275, FRAME_WIDTH,		FRAME_WIDTH};
		//		int[] yPointsFire = {FRAME_HEIGHT,  275, MENUBAR_HEIGHT,	FRAME_HEIGHT};
		//
		//		Zone fireZone = new Fire(xPointsFire, yPointsFire, 4, allZones.size());
		//		allZones.add(fireZone);

		fillInZones();

		checkZoneSanity();

		//initialize the bots
		allBots = new ArrayList<Bot>();

		Rectangle2D startingZoneBoundingBox = homeBase.getBounds2D();

		for(int i = 0; i < numBots; i++) {
			allBots.add(new Bot(startingZoneBoundingBox.getCenterX(), startingZoneBoundingBox.getCenterY(), numBots, i, homeBase, BOUNDING_BOX));
		}

		//initialize the victims
		//only 2 for now, so we'll hard code them	
		allSurvivors = new ArrayList<Survivor>();

//		allSurvivors.add(new Survivor(FRAME_WIDTH/4.0, FRAME_HEIGHT/4.0, .5));
//		allSurvivors.add(new Survivor(FRAME_WIDTH/4.0, FRAME_HEIGHT*3.0/4.0, .5));

		debugShapesToDraw = new ArrayList<Shape>();

		setVisible(true);
	}

	private void setupFrame() {
		setSize(FRAME_WIDTH, FRAME_HEIGHT);
		setResizable(false);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
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

	private void fillInZones() {
		//first, get all unfilled zones
		Area filledAreas = new Area();
		for(Zone z : allZones) {
			filledAreas.add(new Area(z));
		}

		Area unfilledArea = new Area(BOUNDING_BOX);
		unfilledArea.subtract(filledAreas);

		List<Point2D> zoneVerticies = Utilities.getVerticies(unfilledArea);

		//the ZONE_COMPLEXITY constant basically defines how many extra verticies in Zones we should add
		//so add them
		int numLeftToAdd = ZONE_COMPLEXITY;
		while(numLeftToAdd > 0) {
			//make a random point
			Point2D.Double newPoint = new Point2D.Double(RAMOM_GENERATOR.nextInt(FRAME_WIDTH), RAMOM_GENERATOR.nextInt(FRAME_HEIGHT));
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
			Point2D p1 = zoneVerticies.remove(RAMOM_GENERATOR.nextInt(zoneVerticies.size()));
			Point2D p2 = zoneVerticies.remove(RAMOM_GENERATOR.nextInt(zoneVerticies.size()));
			Point2D p3 = zoneVerticies.remove(RAMOM_GENERATOR.nextInt(zoneVerticies.size()));

			int[] xPoints = {(int) p1.getX(), (int) p2.getX(), (int) p3.getX()};
			int[] yPoints = {(int) p1.getY(), (int) p2.getY(), (int) p3.getY()};

			//make a zones out of them
			Zone newZone;

			switch(RAMOM_GENERATOR.nextInt(5)) {
			case 0: newZone = new SafeZone(xPoints, yPoints, 3, allZones.size()); break; 
			case 1: newZone = new DangerZone(xPoints, yPoints, 3, allZones.size()); break;
			case 2: newZone = new SafeDebris(xPoints, yPoints, 3, allZones.size()); break;
			case 3: newZone = new DangerDebris(xPoints, yPoints, 3, allZones.size()); break;
			//			case 4: newZone = new Fire(xPoints, yPoints, 3, allZones.size()); break;
			default: newZone = new SafeZone(xPoints, yPoints, 3, allZones.size()); break;  
			}

			//make sure it doesn't intersect any existing zones
			if(Utilities.findAreaIntersectionsInList(newZone, allZones).size() > 0) {
				zoneVerticies.add(p1);
				zoneVerticies.add(p2);
				zoneVerticies.add(p3);
				continue;
			}

			//it checks out - add it
			allZones.add(newZone);
			//remove it's area from the unfilled area
			unfilledArea.subtract(new Area(newZone));

			//only put points back into the array if they are still in the unfilled area
			zoneVerticies.add(p1);

			zoneVerticies.add(p2);

			zoneVerticies.add(p3);
		}
	}


	public void go(int numTimestepsToRun) {
		//First, need to randomly distribute the bots a bit
		for(Bot b : allBots) {
			b.moveRandomly();
		}
		
		//then, start with timesteps
		for(currentTimestep = 0; currentTimestep < numTimestepsToRun; currentTimestep++) {
			System.out.println("On timestep " + currentTimestep);
			//do all the victims
			for(Survivor v : allSurvivors) {
				v.doOneTimestep();
			}
			System.out.println("Done with victims");
			//do all the bots
			for(Bot b : allBots) {
				b.doOneTimestep();
			}
			System.out.println("Done with bots");
			//repaint the scenario
			repaint();
			System.out.println("Done with repaint");
		}

	}

	public void paint(Graphics g) {		
		g = getGraphics();
		Graphics2D g2d = (Graphics2D) g;

		//		System.out.println("REPAINTING");

		//clear everything
		g2d.setColor(BACKGROUND_COLOR);
		g2d.fill(BOUNDING_BOX);

		//get a snapshot of the bots and victims
		allBotSnapshot = allBots.listIterator();
		allSurvivorSnapshot = allSurvivors.listIterator();

		//draw the zones
		g2d.setFont(ZONE_LABEL_FONT);
		for(Zone z : allZones) {
			g2d.setColor(z.getColor());
			g2d.fill(z);
			//			g2d.setColor(ZONE_LABEL_COLOR);
			//			g2d.drawString("" + z.getID(), (int)z.getCenterX(), (int)z.getCenterY());
			g2d.setColor(ZONE_OUTLINE_COLOR);
			g2d.draw(z);
			if(z instanceof Building) {
				g2d.draw(((Building) z).getFloorplan());
			}
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
		
		//draw all the bots and their radii and their labels
		g2d.setFont(BOT_LABEL_FONT);
		while(allBotSnapshot.hasNext()) {
			Bot curBot = allBotSnapshot.next();

			g2d.setColor(BOT_COLOR);
			g2d.fill(curBot);

			if(DRAW_BOT_RADII) {
				g2d.setColor(AUDIO_RANGE_COLOR);
				g2d.draw(curBot.getAuditbleArea());

				g2d.setColor(VISIBLE_RANGE_COLOR);
				g2d.draw(curBot.getVisibibleArea());

				g2d.setColor(BROADCAST_RANGE_COLOR);
				g2d.draw(curBot.getBroadcastArea());
			}

			g2d.setColor(BOT_LABEL_COLOR);
			g2d.drawString("" + curBot.getID(), (float) (curBot.getX()), (float) (curBot.getY() + curBot.getHeight()));

			g2d.setColor(BOT_MOVEMENT_VECTOR_COLOR);
			g2d.draw(curBot.getMovementVector().rescale(-5.0));
		}

		//draw all the victims
		g2d.setColor(VICTIM_COLOR);
		while(allSurvivorSnapshot.hasNext()) {
			Survivor curSur = allSurvivorSnapshot.next();

			g2d.fill(curSur);
		}

		//paint all the victim paths
		g2d.setColor(VICTIM_PATH_COLOR);
		g2d.setStroke(VICTIM_PATH_STROKE);
		//only basezones have VictimPaths
		for(Zone z : allZones) {
			//skip everything that isn't a BaseZone
			if(! (z instanceof BaseZone)) continue;

			BaseZone bz = (BaseZone) z;

			//draw all of is paths
			List<SurvivorPath> survivorPaths= bz.getBestSurvivorPaths();

			for(SurvivorPath sp : survivorPaths) {
				g2d.draw(sp);
			}

		}

		if(WORLD_DEBUG) {
			//draw the shapes in the debug arraylist
			g2d.setColor(Color.white);
			for(Shape s : debugShapesToDraw) {
				g2d.draw(s);
			}
		}


	}

	private double getAverageBotDistance() {
		//first, need to calculate it
		double curAvg = 0.0;

		ListIterator<Bot> botIt = allBots.listIterator();

		int i = 0;

		while(botIt.hasNext()) {
			Bot curBot = botIt.next();
			i++;

			double curDist = baseZone.getCenterLocation().distance(curBot.getCenterLocation());

			curAvg = curAvg + (curDist - curAvg)/i;
		}

		return curAvg;
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



	/**
	 * @param args
	 */
	public static void main(String[] args) {

		//make a new World
		World w = new World();
		w.go(500);
	}

}
