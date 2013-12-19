package JUnit;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cmsc420.city.City;
import cmsc420.city.Geometry;
import cmsc420.city.Portal;
import cmsc420.city.Road;
import cmsc420.pmquadtree.InvalidPartitionThrowable;
import cmsc420.pmquadtree.OutOfBoundsThrowable;
import cmsc420.pmquadtree.PM1Quadtree;
import cmsc420.pmquadtree.PMQuadtree;
import cmsc420.pmquadtree.RoadAlreadyExistsThrowable;
import cmsc420.pmquadtree.RoadIntersectsAnotherRoadThrowable;
import cmsc420.sortedmap.AvlGTree;
import cmsc420.sortedmap.GuardedAvlGTree;
import junit.framework.TestCase;

public class Test0 extends TestCase{
	PMQuadtree quadtree;
	final int spatialWidth = 16;
	final int spatialHeight = 16;
	City a, b, c, d;
	Road ab, ac, ad, bc, bd, cd;
	
    @Before
    public void setUp() {
    	quadtree = new PM1Quadtree(spatialWidth, spatialHeight);
		a = new City("A", 0,15,10,"Black");
		b = new City("B", 15,15,10,"Black");
		c = new City("C", 0,0,10,"Black");
		d = new City("D", 15,0,10,"Black");
		
		bc = new Road(b,c);
		ad = new Road(a,d);
    }
 
    @Test
    public void testPM1() {
//    	try {
//			quadtree.addRoad(bc);
//			quadtree.addRoad(ad);
//		} catch (RoadAlreadyExistsThrowable | OutOfBoundsThrowable | InvalidPartitionThrowable | RoadIntersectsAnotherRoadThrowable e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
    }
    
    @Test
    public void testGeometry(){
//    	Geometry g =  new Portal("A", 0,15,10);
//    	Geometry g2 = new Portal("B", 0,12, 3);
//    	Geometry g3 = g2;
//    	System.out.println(g.equals(g2));
//    	System.out.println(g2.equals(g3));
    	
    }
    
    
    @Test
    public void testGuarded(){
    	GuardedAvlGTree<String, Integer> testTree = new GuardedAvlGTree<String, Integer>(String.CASE_INSENSITIVE_ORDER, 2);
    	testTree.put("a", 3423421);
    	testTree.put("b", 333);
    	testTree.put("c", 1);
//    	testTree.put("d", 2);
//    	testTree.put("e", 1);
    	testTree.remove("b");
    	testTree.put("b", 1);
    	testTree.remove("b");
    	testTree.put("b", 122);
//    	testTree.put("f", 2);
//    	testTree.put("g", 1);
//    	testTree.put("h", 2);
//    	testTree.put("i", 1);
//    	testTree.put("j", 2);
//    	testTree.remove("a");
//    	testTree.put("a", 1);
//    	testTree.put("b", 1);
//    	testTree.remove("j");
//    	testTree.put("j", 2);
//    	testTree.remove("a");
//    	testTree.remove("b");
//    	testTree.remove("j");
//    	testTree.put("a", 1);
    	System.out.println(testTree.entrySet());
    	testTree.sweep();
    }
    
    @Test
	public void testTreeMapIterator() {
		TreeMap<Integer, Integer> tm = new TreeMap<Integer, Integer>();
		tm.put(3, 3);
		tm.put(2, 2);
		tm.put(4, 4);
		tm.put(1, 1);
		tm.put(5, 5);
		Set<Integer> s = tm.keySet();
		tm.put(1, 1);
		Iterator<Integer> it = s.iterator();
		assertTrue(it.next().equals(1));
		assertTrue(it.next().equals(2));
		assertTrue(it.next().equals(3));
		assertTrue(it.next().equals(4));
		assertTrue(it.next().equals(5));
		try {
			it.next();
		} catch (NoSuchElementException nsee) {
			return;
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
		fail();
	}
    
    @Test
	public void testAvlGTreeIterator() {
		AvlGTree<Integer, Integer> avl = new AvlGTree<Integer, Integer>(2);
		avl.put(3, 3);
		avl.put(2, 2);
		avl.put(4, 4);
//		avl.put(1, 1);
		avl.put(5, 5);
		Set<Integer> s = avl.keySet();
		avl.put(1, 1);
		avl.remove(3);
		avl.remove(5);
		Iterator<Integer> it = s.iterator();
		assertTrue(it.next().equals(1));
		assertTrue(it.next().equals(2));
//		assertTrue(it.next().equals(3)); // comment for delete
		assertTrue(it.next().equals(4));
//		assertTrue(it.next().equals(5)); // comment for delete
		try {
			it.next();
		} catch (NoSuchElementException nsee) {
			return; 
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
		fail();
	}
    
    
 // NOTE: getEntry(key) returns the Entry of the specified key regardless of it
 	// being a sentinel or not.
 	@Test
 	public void testAvlGTreeSweep() {
 		AvlGTree<Integer, Integer> avl = new AvlGTree<Integer, Integer>(2);
 		for (int i = 0; i < 100; i++) {
 			avl.put(i, i);
 		}
 		
 		for (int i = 0; i < 100; i++) {
 			if (i % 10 == 0) {
 				avl.remove(i);
 			}
 		}
 		System.out.println(avl.size());
 		for (int i = 0; i < 100; i++) {
 			if (i % 10 == 0) {
 				if (avl.getNode(i) == null) fail("" + i); // sentinel entry
 			}
 			else {
 				if (avl.getNode(i) == null) fail("" + i); // non-sentinel entry
 			}
 		}
 		
 		avl.sweep(); // call sweep
 		for (int i = 0; i < 100; i++) {
 			if (i % 10 == 0) {
 				if (avl.getNode(i) != null) fail("" + i); // should be gone now..
 			}
 			else {
 				if (avl.getNode(i) == null) fail("" + i); // non-sentinel entry
 			}
 		}
 	}
 	
 	@Test
	public void testAvlGTreeHeadTail() {
		AvlGTree<Integer, Integer> avl = new AvlGTree<Integer, Integer>(2);
		for (int i = 0; i < 100; i++) {
			avl.put(i, i);
		}
		SortedMap<Integer, Integer> sub = avl.subMap(20, 80);
		for (int i = 0; i < 100; i++) {
			if (i < 20) {
				if (sub.containsKey(i)) fail();
			}
			else if (i >= 80) {
				if (sub.containsKey(i)) fail();
			}
			else {
				if (!(sub.containsKey(i))) fail();
			}
		}
	}
 	
 	public void testIsEmpty(){
 		GuardedAvlGTree<String, Integer> avl = new GuardedAvlGTree<String, Integer>(String.CASE_INSENSITIVE_ORDER,2);
		avl.put("3", 3);
		avl.put("2", 2);
		avl.put("4", 4);
		avl.remove("3");
		avl.remove("2");
		avl.remove("4");
		avl.put("3", 5);
		avl.put("5", 4);
		avl.put("4", 2);
		avl.remove("3");
		avl.remove("5");
		avl.remove("4");
		avl.sweep();
		avl.isEmpty();
 	}
 	
 	public void testContainsKey(){
 		GuardedAvlGTree<String, Integer> avl = new GuardedAvlGTree<String, Integer>(String.CASE_INSENSITIVE_ORDER,2);
		avl.put("3", 3);
		avl.put("2", 2);
		avl.put("4", 4);
		avl.remove("3");
		avl.containsKey("3");
		avl.put("3", 3);
		avl.containsKey("3");
		avl.sweep();
		avl.containsKey("3");
 	}
}
