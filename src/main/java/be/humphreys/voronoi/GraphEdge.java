

package main.java.be.humphreys.voronoi;

public class GraphEdge {
    public double x1, y1, x2, y2;

    public int site1;
    public int site2;
        
    @Override
    public String toString() {
    	return "(" + x1 + ", " + y1 + ") (" + x2 + ", " + y2 + ")\t" + site1 + " " + site2;
    }

}