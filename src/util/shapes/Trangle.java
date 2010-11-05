package util.shapes;

import java.awt.Point;
import java.awt.Polygon;

public class Trangle extends Polygon {
	
	private static final long serialVersionUID = -3336242588726421233L;

	public Trangle() {
		super();
	}
	
	public Trangle(Point p1, Point p2, Point p3) {
		super(new int[] {p1.x, p2.x, p3.x}, new int[] {p1.y, p2.y, p3.y}, 3);
	}
	
	@Override
	public void addPoint(int x, int y) {
		if(this.npoints >= 3) {
			throw new IndexOutOfBoundsException("Triangles cannot have more than 3 points");
		}
	}

}
