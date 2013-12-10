package cmsc420.pmquadtree;

import cmsc420.city.City;
import cmsc420.city.Geometry;
import cmsc420.city.Road;
import cmsc420.pmquadtree.PMQuadtree.Black;

public class PM1Validator implements Validator{

	@Override
	public boolean valid(Black node) {
		City start, end, city;
		if(node.getNumPoints() <= 1){
			if(!node.containsCity()){
				return true;
			}
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
		}
		return false;
	}

}
