package cmsc420.pmquadtree;

import cmsc420.city.City;
import cmsc420.city.Geometry;
import cmsc420.city.Road;
import cmsc420.pmquadtree.PMQuadtree.Black;

public class PM1Validator implements Validator{

	@Override
	public boolean valid(Black node) {
		int numPoints = node.getNumPoints();
		int numRoads = node.getGeometry().size() - numPoints;
		City start, end, city;
		
		if((node.containsCity() || node.containsRoad()) && node.portalExists()){
			return false;
		}
		
		if(numPoints <= 1){
			if(node.containsCity()){
				city = node.getCity();
				for(Geometry g : node.getGeometry()){
					if(g.isRoad()){
						start = ((Road) g).getStart();
						end = ((Road) g).getEnd();
						if(start.equals(city) || end.equals(city)){
							continue;
						}
						else{
							return false;
						}
					}
				}
				return true;
			} else {
				return numRoads <= 1;
			}
		}
		return false;
	}

}
