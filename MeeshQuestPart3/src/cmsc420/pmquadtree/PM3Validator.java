package cmsc420.pmquadtree;


import cmsc420.pmquadtree.PMQuadtree.Black;

public class PM3Validator implements Validator {
	
	public boolean valid(final Black node) {
//		if(node.getNumPoints() <= 1){
//			System.out.println(node.geometry);
//			System.out.println();
//		}
		return (node.getNumPoints() <= 1);
	}
}
