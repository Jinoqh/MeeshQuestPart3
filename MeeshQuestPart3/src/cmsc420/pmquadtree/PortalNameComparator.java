package cmsc420.pmquadtree;

import java.util.Comparator;

import cmsc420.city.Portal;

public class PortalNameComparator implements Comparator<Portal> {

	@Override
	public int compare(Portal p1, Portal p2) {
		return p1.compareTo(p2);
	}

}
