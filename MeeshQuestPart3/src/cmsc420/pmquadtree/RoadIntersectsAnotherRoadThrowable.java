package cmsc420.pmquadtree;

public class RoadIntersectsAnotherRoadThrowable extends Exception {
	private static final long serialVersionUID = 1L;
	
	public RoadIntersectsAnotherRoadThrowable() {
    }

    public RoadIntersectsAnotherRoadThrowable(String msg) {
    	super(msg);
    } 
}
