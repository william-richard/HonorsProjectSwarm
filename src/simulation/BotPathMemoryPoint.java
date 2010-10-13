package simulation;

import java.awt.geom.Point2D;

import util.Vector;

public class BotPathMemoryPoint extends Point2D {

	double x;
	double y;
	Vector fromDirection;
	Vector toDirection;
	int fromZoneAssessment;
	int toZoneAssessment;
	
	public BotPathMemoryPoint(double _x, double _y, Vector _fromDirection, Vector _toDirection, int _fromAssessment, int _toAssesment) {
		this.setLocation(_x, _y);
		fromDirection = _fromDirection;
		toDirection = _toDirection;
		fromZoneAssessment = _fromAssessment;
		toZoneAssessment = _toAssesment;
	}
	
	public BotPathMemoryPoint(Point2D transitionPoint, Vector _fromDirection, Vector _toDirection, int _fromAssessment, int _toAssessment) {
		this(transitionPoint.getX(), transitionPoint.getY(), _fromDirection, _toDirection, _fromAssessment, _toAssessment);
	}
	
	@Override
	public double getX() {
		return x;
	}

	@Override
	public double getY() {
		return y;
	}

	@Override
	public void setLocation(double x, double y) {
		this.x = x;
		this.y = y;
	}

}
