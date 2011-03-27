package util;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import org.jgrapht.util.FibonacciHeap;
import org.jgrapht.util.FibonacciHeapNode;

public class Dijkstras {
	
	/*TODO because this needs to visit each pixel, it sometimes ends up getting worse answers than it should
	 * need to think of a way to have Dijkstras' return the real shortest path - maybe consider every 1/10 of a pixel instead of every full pixel
	 */
	
	private ArrayList<ArrayList<DPixel>> pixels;
	private ArrayList<ArrayList<FibonacciHeapNode<DPixel>>> nodes;
	int minX, maxX, minY, maxY;
	
	public Dijkstras(int _minX, int _maxX, int _minY, int _maxY) {
		System.out.println("Staring dijkstras constructor");
		minX = _minX;
		maxX = _maxX;
		minY = _minY;
		maxY = _maxY;
		
		//add all the pixels to the storage array list
		pixels = new ArrayList<ArrayList<DPixel>>(maxX);
		nodes = new ArrayList<ArrayList<FibonacciHeapNode<DPixel>>>();
		for(int curX = 0; curX < maxX; curX++) {
			pixels.add(new ArrayList<DPixel>(maxY));
			ArrayList<DPixel> curList = pixels.get(curX);
			for(int curY = 0; curY < maxY; curY ++) {
				DPixel newPix = new DPixel(curX, curY);
				curList.add(newPix);
			}
		}
		System.out.println("finished dijkstras constructor");
	}

	public Dijkstras(Dijkstras other) {
		minX = other.minX;
		maxX = other.maxX;
		minY = other.minY;
		maxY = other.maxY;

		pixels = new ArrayList<ArrayList<DPixel>>(maxX);
		for(int x = 0; x < maxX; x++) {
			pixels.add(new ArrayList<DPixel>(maxY));
			for(int y = 0; y < maxY; y++) {
				DPixel newPix = new DPixel(other.getPixel(x, y));
				pixels.get(x).add(newPix);
			}
		}
	}
	
	
	/**
	 * Do Dijkstra's algorithm on the nodes, to find the shortest path from the base location to all other points
	 */
	public void dijkstras(Point source) {
		
		System.out.println("Staring dijkstras from "  + source);
		FibonacciHeap<DPixel> heap = new FibonacciHeap<DPixel>();
		nodes = new ArrayList<ArrayList<FibonacciHeapNode<DPixel>>>(maxX);
		//reset the nodes
		//and add them to our Queue
		for(int curX = 0; curX < maxX; curX++) {
			nodes.add(new ArrayList<FibonacciHeapNode<DPixel>>(maxY));
			for(int curY = 0; curY < maxY; curY++) {
				DPixel curPix = getPixel(curX, curY);
				curPix.setPrevious(null);
				FibonacciHeapNode<DPixel> newNode = new FibonacciHeapNode<DPixel>(curPix);
				heap.insert(newNode, Double.MAX_VALUE);
				nodes.get(curX).add(newNode);
			}
		}
		
//		DijkstraHeap heap = new DijkstraHeap(nodes);

		
		//set the source distance to 0
		heap.decreaseKey(getNode(source.x, source.y), 0.0);

		System.out.println("Done with the initializations");
				
		FibonacciHeapNode<DPixel> nextNode;
		while(! heap.isEmpty()) {
			nextNode = heap.removeMin();
			if(nextNode.getKey() == Double.MAX_VALUE) {
				//no other nodes are reachable
				break;
			}

			int nextNodeX = (int)nextNode.getData().getX();
			int nextNodeY = (int)nextNode.getData().getY();
			double distThruNextNode = nextNode.getKey() + nextNode.getData().getWeight();
			for(int curNeiX = nextNodeX - 1; curNeiX <= nextNodeX + 1; curNeiX++) {
				for(int curNeiY = nextNodeY - 1; curNeiY <= nextNodeY + 1; curNeiY++) {
					//don't examine ourselves
					if(curNeiX == nextNodeX && curNeiY == nextNodeY) {
						continue;
					}
					//don't examine pixels that are out of bounds
					if(curNeiX < minX || curNeiX >= maxX || curNeiY < minY || curNeiY >= maxY) {
						continue;
					}
					FibonacciHeapNode<DPixel> curNei = getNode(curNeiX, curNeiY);
					
					if(distThruNextNode < curNei.getKey()) {
						heap.decreaseKey(curNei, distThruNextNode);
						curNei.getData().setPrevious(nextNode.getData().getLocation());
					}
				}
			}
		}
		
		System.out.println("Finished dijkstras's from " + source);		
	}	
	
	public double getDistanceTo(int x, int y) {
		return getNode(x,y).getKey();
	}
	
	public DPixel getPixel(Point2D p) {
		return getPixel((int)p.getX(), (int)p.getY());
	}
	
	public DPixel getPixel(int x, int y) {
		return pixels.get(x).get(y);
	}
	
	public FibonacciHeapNode<DPixel> getNode(int x, int y) {
		return nodes.get(x).get(y);
	}
}
