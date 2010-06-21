import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Random;


public class World extends Rectangle2D.Double {

	/***************************************************************************
	 * CONSTANTS
	 **************************************************************************/
	public static final Random RAMOM_GENERATOR = new Random();

	private static final long serialVersionUID = 1L;

	/** VARIABLES */
	List<Zone> Zones; //The zones in the world - should be non-overlapping
	List<Bot> Bots; //List of the Bots, so we can do stuff with them
	List<Victim> Victims; //The Victims
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
