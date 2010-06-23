import java.awt.Color;


public class DangerZone extends Zone {
	
	private static final long serialVersionUID = 1L;

	public DangerZone(int[] xPoints, int[] yPoints, int numPoints, int _zoneID) {
		super(xPoints, yPoints, numPoints, _zoneID, Color.red); //SET THE ZONE COLOR HERE
	}

}
