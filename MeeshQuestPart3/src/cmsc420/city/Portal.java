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
	
	@Override
	public boolean equals(final Object obj) {
		if (obj == this)
			return true;
		if (obj != null && (obj.getClass().equals(this.getClass()))) {
			Portal p = (Portal) obj;
			return (pt.equals(p.pt) && z == p.z && name.equals(p.name));
		}
		return false;
	}
}
