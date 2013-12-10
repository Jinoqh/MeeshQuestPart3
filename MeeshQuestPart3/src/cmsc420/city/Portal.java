package cmsc420.city;

import java.awt.geom.Point2D;

public class Portal extends City{
	
	public Portal(String name, int x, int y, int z) {
		super(name, x, y, z, 0, null);
	}
	
	@Override
	public int getType(){
		return POINT;
	}
	
	@Override
	public String toString(){
		return "P"+super.toString();
	}
}
