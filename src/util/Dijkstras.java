package util;

import java.awt.Point;
import java.util.ArrayList;

import org.jgrapht.util.FibonacciHeap;
import org.jgrapht.util.FibonacciHeapNode;

public class Dijkstras {
	
	private ArrayList<ArrayList<FibonacciHeapNode<DPixel>>> dNodes;
	int minX, maxX, minY, maxY;
	
	public Dijkstras(int _minX, int _maxX, int _minY, int _maxY) {
		System.out.println("Staring dijkstras constructor");
		minX = _minX;
		maxX = _maxX;
		minY = _minY;
		maxY = _maxY;
		
		//add all the nodes into storage the first time
		dNodes = new ArrayList<ArrayList<FibonacciHeapNode<DPixel>>>();
		for(int curX = 0; curX < maxX; curX++) {
			dNodes.add(new ArrayList<FibonacciHeapNode<DPixel>>());
			ArrayList<FibonacciHeapNode<DPixel>> curList = dNodes.get(curX);
			for(int curY = 0; curY < maxY; curY ++) {
				DPixel newPix = new DPixel(curX, curY);
				curList.add(new FibonacciHeapNode<DPixel>(newPix));
			}
		}
		System.out.println("finished dijkstras constructor");
	}

	/**
	 * Do Dijkstra's algorithm on the dNodes, to find the shortest path from the base location to all other points
	 */
	public void dijkstras(Point source) {
		System.out.println("Staring dijkstras from "  + source);
		FibonacciHeap<DPixel> heap = new FibonacciHeap<DPixel>();
		//reset the nodes
		//and add them to our Queue
		for(int curX = 0; curX < maxX; curX++) {
			for(int curY = 0; curY < maxY; curY++) {
				FibonacciHeapNode<DPixel> curNode = getNode(curX, curY);
				curNode.getData().setPrevious(null);
				heap.insert(curNode, Double.MAX_VALUE);
			}
		}
		
//		DijkstraHeap heap = new DijkstraHeap(dNodes);

		
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

			int nextNodeX = nextNode.getData().getX();
			int nextNodeY = nextNode.getData().getY();
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
						curNei.getData().setPrevious(nextNode.getData());
					}
				}
			}
		}
		
		System.out.println("Finished dijkstras's from " + source);
	}	
	
	public double getDistanceTo(int x, int y) {
		return getNode(x,y).getKey();
	}
	
	public DPixel getPixel(int x, int y) {
		return getNode(x, y).getData();
	}
	
	public FibonacciHeapNode<DPixel> getNode(int x, int y) {
		return dNodes.get(x).get(y);
	}
}
