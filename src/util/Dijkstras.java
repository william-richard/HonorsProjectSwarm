package util;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.jgrapht.util.FibonacciHeap;
import org.jgrapht.util.FibonacciHeapNode;

import simulation.World;
import util.shapes.LineSegment;
import zones.Zone;

public class Dijkstras {

	/*TODO because this needs to visit each pixel, it sometimes ends up getting worse answers than it should
	 * need to think of a way to have Dijkstras' return the real shortest path - maybe consider every 1/10 of a pixel instead of every full pixel
	 */

	private HashMap<Integer, List<FibonacciHeapNode<DPixel>>> nodesByZone;
	private List<DPixel> pixels;
	private List<FibonacciHeapNode<DPixel>> nodes;
	int minX, maxX, minY, maxY;

	public Dijkstras(int _minX, int _maxX, int _minY, int _maxY) {
		System.out.println("Staring dijkstras constructor");
		minX = _minX;
		maxX = _maxX;
		minY = _minY;
		maxY = _maxY;

		//add all the pixels to the storage array list
		//want to put pixels along each line in all zones
		pixels = new ArrayList<DPixel>();
		nodes = new ArrayList<FibonacciHeapNode<DPixel>>();
		nodesByZone = new HashMap<Integer, List<FibonacciHeapNode<DPixel>>>();

		Zone curZone;
		List<LineSegment> curZoneSides;
		for(Integer zoneId : World.allZones.keySet()) {
			curZone = World.allZones.get(zoneId);

			curZoneSides = curZone.getSides();
			
			//for each side add all the points on it's side
			for(LineSegment curSide : curZoneSides) {
				Set<Point> curSidePoints = curSide.getIntegerPoints();

				for(Point p : curSidePoints) {
					DPixel newPix = new DPixel(p);
					//get the version already stored if it has been stored so we add to it's parents list
					if(pixels.contains(newPix)) {
						newPix = pixels.remove(pixels.indexOf(newPix));
					}
					newPix.addParentZone(zoneId);
					pixels.add(newPix);
				}
			}
		}

		System.out.println("finished dijkstras constructor");
	}

	/**
	 * Do Dijkstra's algorithm on the nodes, to find the shortest path from the base location to all other points
	 */
	public void dijkstras(Point source) {

		System.out.println("Staring dijkstras from "  + source);
		FibonacciHeap<DPixel> heap = new FibonacciHeap<DPixel>();
		nodes.clear();
		nodesByZone = new HashMap<Integer, List<FibonacciHeapNode<DPixel>>>();
		//reset the nodes
		//and add them to our Queue
		for(DPixel curPix : pixels) {
			curPix.setPrevious(null);
			FibonacciHeapNode<DPixel> newNode = new FibonacciHeapNode<DPixel>(curPix);
			heap.insert(newNode, Double.MAX_VALUE);
			nodes.add(newNode);
			for(Integer curZoneId : curPix.getParentZoneIds()) {
				//first, add a new list to the hash map if needed
				if(nodesByZone.get(curZoneId) == null) {
					nodesByZone.put(curZoneId, new ArrayList<FibonacciHeapNode<DPixel>>());
				}
				//then, add the new node to the approprate list in the hash
				nodesByZone.get(curZoneId).add(newNode);
			}
		}

		//add a pixel for the source, and set it to 0
		DPixel sourcePix = new DPixel(source);
		//add all the neighbors of the basezone to that pixel
		Zone sourceZone = World.findZone(source);
		List<Zone> sourceNeighbors = sourceZone.getNeighbors();
		for(Zone neighbor : sourceNeighbors) {
			sourcePix.addParentZone(new Integer(neighbor.getID()));
		}
		
		pixels.add(sourcePix);
		FibonacciHeapNode<DPixel> newNode = new FibonacciHeapNode<DPixel>(sourcePix);
		nodes.add(newNode);
		heap.insert(newNode, 0.0);

		System.out.println("Done with the initializations");

		FibonacciHeapNode<DPixel> nextNode;
		while(! heap.isEmpty()) {
			nextNode = heap.removeMin();
			if(nextNode.getKey() == Double.MAX_VALUE) {
				//no other nodes are reachable
				break;
			}

			//check out all the points on all the zones that this pixels touches
			DPixel nextPix = nextNode.getData();
			
			//doing it this way assumes that all zones are convex polygons
			//This is guaranteed by Voronoi according to wikipedia
			for(Integer zoneId : nextPix.getParentZoneIds()) {
				//take a look at all neighboring points of the current zone
				List<FibonacciHeapNode<DPixel>> zoneBorderNodes = nodesByZone.get(zoneId);
				
				for(FibonacciHeapNode<DPixel> curNode : zoneBorderNodes) {
					DPixel curPoint = curNode.getData();
					//calculate the weighted distance to this point from the 'next' point
					double weightedDistace = World.allZones.get(zoneId).getPathWeightPerPixel() * curPoint.getLocation().distance(nextPix.getLocation());
					double distThruNextPix = nextNode.getKey() + weightedDistace;
					if(distThruNextPix < curNode.getKey()) {
						heap.decreaseKey(curNode, distThruNextPix);
						curPoint.setPrevious(nextPix.getLocation());
					}
					
				}
			}			
		}

		System.out.println("Finished dijkstras's from " + source);		
	}	

	public double getDistanceTo(int x, int y) {
		return getDistanceTo(new Point(x,y));
	}
	
	public double getDistanceTo(Point2D p) {
		DPixel minPix = getClosestPixel(p);
		
		//figure out the distance from the minPix to p
		FibonacciHeapNode<DPixel> minNode = getNode(minPix);
		
		return minNode.getKey() + World.findZone(p).getPathWeightPerPixel() * minPix.getLocation().distance(p);
	}

	public DPixel getPixel(Point2D p) {
		return getPixel((int)p.getX(), (int)p.getY());
	}

	public DPixel getPixel(int x, int y) {
		//can do this because DPixel equals is defined by x & y
		DPixel curPix = new DPixel(x, y);
		if(pixels.contains(curPix)) {
			//we can just get it from the list
			return pixels.get(pixels.indexOf(curPix));
		} else {
			//we can't
			return null;
		}
	}

	public DPixel getClosestPixel(Point2D p) {
		//figure out what zone this point is in
		Zone pointZone = World.findZone(p);
		//do the key calculation from all points on the edge of this zone
		double minDist = Double.MAX_VALUE;
		DPixel minPix = null;
		for(FibonacciHeapNode<DPixel> curNode : nodesByZone.get(new Integer(pointZone.getID()))) {
			DPixel curPix = curNode.getData();
			
			double curDist = curNode.getKey() + pointZone.getPathWeightPerPixel() * curPix.getLocation().distance(p);
			if(curDist < minDist) {
				minDist = curDist;
				minPix = curPix;
			}
		}
		
		return minPix;
	}
	
	
	public FibonacciHeapNode<DPixel> getNode(int x, int y) {
		return getNode(getPixel(x,y));
	}
	
	public FibonacciHeapNode<DPixel> getNode(DPixel pix) {
		return nodes.get(pixels.indexOf(pix));
	}
}
