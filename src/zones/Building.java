package zones;

import java.awt.Color;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;

import simulation.Bot;
import simulation.Shout;
import simulation.Victim;

public class Building extends Zone {

	private static final long serialVersionUID = 1L;

	private static final Color BuildingColor = new Color(58,95,205);
	
	private Area floorplan;
	
	public Building(int centerX, int centerY, int width, int height, int _zoneID) {
		super(new int[0], new int[0], 0, _zoneID, BuildingColor);
		
		//convert from center to corner
		int cornerX = centerX - (int)(width / 2.0);
		int cornerY = centerY - (int)(height / 2.0);
		
		//add the points that define a box enclosing the building 
		addPoint(cornerX, cornerY);
		addPoint(cornerX + width, cornerY);
		addPoint(cornerX+width, cornerY + height);
		addPoint(cornerX, cornerY+height);
		
		
		//now, add the points that make up the floorplan
		floorplan = new Area();
		//walls are on average about 3 1/2 to 9 inches thick
		//we can't get that kind of resolution, so walls will be 1 pixel thick
		//doors are going to be at 1/4 and 3/4 of the width, on edge
		Polygon floorplanPart1 = new Polygon();
		floorplanPart1.addPoint(cornerX,							cornerY);
		floorplanPart1.addPoint((int)(cornerX + (width/4.0)),		cornerY);
		floorplanPart1.addPoint((int)(cornerX + (width/4.0)),	 	cornerY + 1);
		floorplanPart1.addPoint(cornerX + 1,						cornerY + 1);
		floorplanPart1.addPoint(cornerX + 1,						cornerY + height -1);
		floorplanPart1.addPoint((int)(cornerX + (width/2.0)),		cornerY + height - 1);
		floorplanPart1.addPoint((int)(cornerX + (width/2.0)),		cornerY + height);
		floorplanPart1.addPoint(cornerX, 							cornerY + height);
		
		Polygon floorplanPart2 = new Polygon();
		floorplanPart2.addPoint(cornerX + width, 					cornerY + height);
		floorplanPart2.addPoint((int)(cornerX + width * 3.0/4.0),	cornerY + height);
		floorplanPart2.addPoint((int)(cornerX + width * 3.0/4.0),	cornerY + height - 1);
		floorplanPart2.addPoint(cornerX + width - 1,				cornerY + height - 1);
		floorplanPart2.addPoint(cornerX + width - 1,				cornerY + 1);
		floorplanPart2.addPoint((int)(cornerX + (width/2.0)),		cornerY + 1);
		floorplanPart2.addPoint((int)(cornerX + (width/2.0)),		cornerY);
		floorplanPart2.addPoint(cornerX + width,					cornerY);
		
		floorplan.add(new Area(floorplanPart1));
		floorplan.add(new Area(floorplanPart2));
		
	}
	
	public Area getFloorplan() {
		return floorplan;
	}
	
	@Override
	public Shape getBroadcastRange(Point2D originator) {
		//make sure the originator is inside
		if(! this.contains(originator)) return null;
		
		//in this case, return a circle
		//know the center of the circle, and the radius - need to find the corner
		double broadcastRangeCornerX = originator.getX() - Bot.DEFAULT_INDOOR_BROADCAST_RADIUS;
		double broadcastRangeCornerY = originator.getY() - Bot.DEFAULT_INDOOR_BROADCAST_RADIUS;

		//now, make the broadcast range shape
		return new Ellipse2D.Double(broadcastRangeCornerX, broadcastRangeCornerY, Bot.DEFAULT_INDOOR_BROADCAST_RADIUS*2, Bot.DEFAULT_INDOOR_BROADCAST_RADIUS*2);
	}

	@Override
	public Shout getShout(Victim shouter) {
		//we don't know what to do if the victim is outside
		//so make sure they are inside
		if(! this.contains(shouter.getCenterLocation())) return null;
		
		//since we are inside, we don't want the shout to go too far outside 
	
		//so, we need to compute 2 circles
		//the first is one that is NOT hampered by walls
		//the other is one that IS hampered by walls
		Ellipse2D.Double shoutRadiusWithoutWall = new Ellipse2D.Double(shouter.getCenterX() - Shout.DEFAULT_SHOUT_RADIUS, shouter.getCenterY() - Shout.DEFAULT_SHOUT_RADIUS, Shout.DEFAULT_SHOUT_RADIUS*2, Shout.DEFAULT_SHOUT_RADIUS*2);
		Ellipse2D.Double shoutRadiusThroughWall = new Ellipse2D.Double(shouter.getCenterX() - Shout.DEFAULT_SHOUT_RADIUS_THROUGH_WALL, shouter.getCenterY() - Shout.DEFAULT_SHOUT_RADIUS_THROUGH_WALL, Shout.DEFAULT_SHOUT_RADIUS_THROUGH_WALL*2, Shout.DEFAULT_SHOUT_RADIUS_THROUGH_WALL*2);
		
		//now, we need to figure out with parts of these radii are inside the building and which parts are outside
		//we can do this using various Area methods
		//TODO: Make this better, where shouts can get out of doors and are not constrained by them
		Area indoorShoutRadius = new Area(shoutRadiusWithoutWall);
		indoorShoutRadius.intersect(new Area(this));
		
		Area outdoorShoutRadius = new Area(shoutRadiusThroughWall);
		outdoorShoutRadius.subtract(new Area(this));
		
		Area overallRadius = new Area();
		overallRadius.add(outdoorShoutRadius);
		overallRadius.add(indoorShoutRadius);
		
		//return the overall shout
		return new Shout(overallRadius, shouter);
	}

	@Override
	public Shape getAudibleRange(Point2D originator) {
		//make sure the passed point is inside our bounds
		if(! this.contains(originator)) return null;
		
		//as before with shouts, we can't hear as well outside if we are inside
		//so we need to consider 2 radii
		//and take from one or the other based on where the walls are.
		//TODO: Need to make this better as well, just like the Shouts need to be improved
		double withoutWallCornerX = originator.getX() - Bot.DEFAULT_AUDITORY_RADIUS;
		double withoutWallCornerY = originator.getY() - Bot.DEFAULT_AUDITORY_RADIUS;
		Ellipse2D.Double audibleRadiusWithoutWall = new Ellipse2D.Double(withoutWallCornerX, withoutWallCornerY, Bot.DEFAULT_AUDITORY_RADIUS*2, Bot.DEFAULT_AUDITORY_RADIUS*2);
		
		double throughWallCornerX = originator.getX() - Bot.DEFAULT_AUDITORY_RADIS_THROUGH_WALL;
		double throughWallCornerY = originator.getY() - Bot.DEFAULT_AUDITORY_RADIS_THROUGH_WALL;
		Ellipse2D.Double audibleRadiusThroughWall = new Ellipse2D.Double(throughWallCornerX, throughWallCornerY, Bot.DEFAULT_AUDITORY_RADIS_THROUGH_WALL*2, Bot.DEFAULT_AUDITORY_RADIS_THROUGH_WALL*2);
		
		Area indoorAudibleRadius = new Area(audibleRadiusThroughWall);
		indoorAudibleRadius.intersect(new Area(this));
		
		Area outdoorAudibleRadius = new Area(audibleRadiusWithoutWall);
		outdoorAudibleRadius.subtract(new Area(this));
		
		Area overallRadius = new Area();
		overallRadius.add(outdoorAudibleRadius);
		overallRadius.add(indoorAudibleRadius);
		
		//return the overall shout
		return overallRadius;
	}

	@Override
	public Shape getVisibilityRange(Point2D originator) {
		//make sure the originator is inside the building
		if(! this.contains(originator)) return null;
		
		//we don't have xray vision, yet
		//so we need to cut off the visibility range at the wall
		//make the un-encombered visibility range
		Ellipse2D.Double unblockedRange = new Ellipse2D.Double(originator.getX() - Bot.DEFALUT_VISIBILITY_RADIUS, originator.getY() - Bot.DEFALUT_VISIBILITY_RADIUS, Bot.DEFALUT_VISIBILITY_RADIUS*2, Bot.DEFALUT_VISIBILITY_RADIUS*2);
		
		//now, need to cut it off at the walls
		Area cutOffRange = new Area(unblockedRange);
		cutOffRange.intersect(new Area(this));
		
		return cutOffRange;
	}

	@Override
	public Shape getFoundRange(Point2D originator) {
		//make sure the originator is inside the building
		if(! this.contains(originator)) return null;
		
		//again, we can't find people who are outside
		//so we need to return a cut off circle
		Ellipse2D.Double unblockedRange = new Ellipse2D.Double(originator.getX() - Bot.DEFALUT_VISIBILITY_RADIUS, originator.getY() - Bot.DEFALUT_VISIBILITY_RADIUS, Bot.DEFALUT_VISIBILITY_RADIUS*2, Bot.DEFALUT_VISIBILITY_RADIUS*2);
		
		//now, need to cut it off at the walls
		Area cutOffRange = new Area(unblockedRange);
		cutOffRange.intersect(new Area(this));
		
		return cutOffRange;
	}

	@Override
	public double getBotMaxVelocity() {
		return Bot.DEFAULT_MAX_VELOCITY;
	}

	@Override
	public boolean isObstacle() {
		return true;
	}


}
