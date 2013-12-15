package cmsc420.city;

import java.awt.geom.Point2D;
import java.util.Comparator;

public class Portal{
	private String name;
	private int x;
	private int y;
	private int z;
	
	public Portal(String name, int x, int y, int z) {
		this.name = name;
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public Portal(Portal p){
		this(p.getName(), p.getX(), p.getY(), p.getZ());
	}
	
	public String toString(){
		return String.format("P(%d,%d)",x,y);
	}
	
	public Point2D.Float toPoint2D() {
		return new Point2D.Float(x, y);
	}
	
	public String getName(){
		return name;
	}
	
	public int getX(){
		return x;
	}
	
	public int getY(){
		return y;
	}
	
	public int getZ(){
		return z;
	}

	
}
