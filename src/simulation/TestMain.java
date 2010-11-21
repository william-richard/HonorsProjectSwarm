package simulation;

import java.awt.geom.Point2D;

import util.Vector;

public class TestMain {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Point2D startPoint = new Point2D.Double(0.0, 0.0);
		Vector zero = Vector.getHorizontalUnitVector(startPoint);
		Vector ninty = zero.rotateDegrees(62);
		System.out.println(Math.toDegrees(zero.getAngleBetween(ninty)));
		System.out.println(Math.toDegrees(ninty.getAngleBetween(zero)));

	}

}
