package cmsc420.pmquadtree;

import cmsc420.city.Geometry;
import cmsc420.city.Road;
import cmsc420.pmquadtree.PMQuadtree.Black;

public class PM1Validator implements Validator{

	@Override
	public boolean valid(Black node) {
		if(node.getNumPoints() <= 1){
			for(Geometry g : node.getGeometry()){
				if(g.isCity())
					continue;
				if(!((Road) g).getStart().equals(node.getCity()) && !((Road) g).getEnd().equals(node.getCity())){
					return false;
				}
			}
			return true;
		}
		return false;
	}

}
