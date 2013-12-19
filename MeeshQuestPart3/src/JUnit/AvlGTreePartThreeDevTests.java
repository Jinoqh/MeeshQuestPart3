package JUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.Test;

import cmsc420.sortedmap.AvlGTree;

//import cmsc420.sortedmap.AvlGTree.AvlEntry;

public class AvlGTreePartThreeDevTests {
	
	
	
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
//		AvlGTree<Integer, Integer> avl = new AvlGTree<Integer, Integer>(2);
//		for (int i = 0; i < 100; i++) {
//			avl.put(i, i);
//		}
//		for (int i = 0; i < 100; i++) {
//			if (i % 10 == 0) {
//				avl.remove(i);
//			}
//		}
//		for (int i = 0; i < 100; i++) {
//			if (i % 10 == 0) {
//				if (avl.getNode(i) == null) fail("" + i); // sentinel entry
//			}
//			else {
//				if (avl.getEntry(i) == null) fail("" + i); // non-sentinel entry
//			}
//		}
//		avl.sweep(); // call sweep
//		for (int i = 0; i < 100; i++) {
//			if (i % 10 == 0) {
//				if (avl.getEntry(i) != null) fail("" + i); // should be gone now..
//			}
//			else {
//				if (avl.getEntry(i) == null) fail("" + i); // non-sentinel entry
//			}
//		}
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
	
	@Test
	public void testTreeMapEntrySet() {
//		TreeMap<Integer, Integer> tm = new TreeMap<Integer, Integer>();
//		AvlGTree<Integer, Integer> avl = new AvlGTree<Integer, Integer>(2);
//		for (int i = 0; i < 1000; i++) {
//			tm.put(i, i);
//			avl.put(i,  i);
//		}
//		
//		Set<Map.Entry<Integer, Integer>> tmSet = tm.entrySet();
//		Set<Map.Entry<Integer, Integer>> avlSet = avl.entrySet();
//		
//		if (!(avlSet.equals(tmSet)) || !(tmSet.equals(avlSet))) {
//			System.out.println("this was in part two..");
//			fail();
//		}
//		
//		AvlEntry<Integer, Integer> temp;
//		for (int i = 1000; i > -1; i--) {
//			temp = new AvlEntry<Integer, Integer>(i, i);
//			tmSet.remove(temp);
//			avlSet.remove(temp);
//			if (!(tmSet.equals(avlSet))) {
//				findMissingEntries(avlSet, tmSet);
//				System.out.println("\n\nindex value at failure: " + i);
//			}
//			if (!(avlSet.equals(tmSet))) {
//				findMissingEntries(avlSet, tmSet);
//				System.out.println("\n\nindex value at failure: " + i);
//			}
//			assertTrue(tmSet.equals(avlSet));
//			assertTrue(avlSet.equals(tmSet));
//		}
	}
	
	@Test
	public void testAscendingDeletes() {
		AvlGTree<Integer, Integer> avl = new AvlGTree<Integer, Integer>(2);
		TreeMap<Integer, Integer> tm = new TreeMap<Integer, Integer>();
		
		for (int i = 0; i < 100; i++) {
			avl.put(i, i);
			tm.put(i, i);
		}
		
		SortedMap<Integer, Integer> subAvl = null;
		SortedMap<Integer, Integer> subTm = null;
		Set<Map.Entry<Integer, Integer>> avlSet = null;
		Set<Map.Entry<Integer, Integer>> tmSet = null;
		
		for (int j = 0; j < 100; j++) {
			avl.remove(j);
			tm.remove(j);
			assertEquals(avl.size(), tm.size());
			for (int i = -1; i < 101; i++) {
				subAvl = avl.headMap(i);
				subTm = tm.headMap(i);
				assertTrue(subAvl.equals(subTm));
				assertTrue(subTm.equals(subAvl));
				assertTrue(subAvl.entrySet().equals(subTm.entrySet()));
				assertTrue(subTm.entrySet().equals(subAvl.entrySet()));
				subAvl = avl.tailMap(i);
				subTm = tm.tailMap(i);
				assertTrue(subAvl.equals(subTm));
				assertTrue(subTm.equals(subAvl));
				assertTrue(subAvl.entrySet().equals(subTm.entrySet()));
				assertTrue(subTm.entrySet().equals(subAvl.entrySet()));
				int temp = Math.max(i, 100 - i);
				subAvl = avl.subMap(100 - temp, temp); // enforce fromKey < toKey
				subTm = tm.subMap(100 - temp, temp);
				assertTrue(subAvl.equals(subTm));
				assertTrue(subTm.equals(subAvl));
				assertTrue(subAvl.entrySet().equals(subTm.entrySet()));
				assertTrue(subTm.entrySet().equals(subAvl.entrySet()));
				avlSet = avl.entrySet();
				tmSet = tm.entrySet();
				assertTrue(avlSet.equals(tmSet));
				assertTrue(tmSet.equals(avlSet));
			}
		}
	}
	
	@Test
	public void testDescendingDeletes() {
		AvlGTree<Integer, Integer> avl = new AvlGTree<Integer, Integer>(2);
		TreeMap<Integer, Integer> tm = new TreeMap<Integer, Integer>();
		
		for (int i = 0; i < 100; i++) {
			avl.put(i, i);
			tm.put(i, i);
		}
		
		SortedMap<Integer, Integer> subAvl = null;
		SortedMap<Integer, Integer> subTm = null;
		Set<Map.Entry<Integer, Integer>> avlSet = null;
		Set<Map.Entry<Integer, Integer>> tmSet = null;
		
		for (int j = 100; j >= 0; j--) {
			avl.remove(j);
			tm.remove(j);
			assertEquals(avl.size(), tm.size());
			for (int i = -1; i < 101; i++) {
				subAvl = avl.headMap(i);
				subTm = tm.headMap(i);
				assertTrue(subAvl.equals(subTm));
				assertTrue(subTm.equals(subAvl));
				assertTrue(subAvl.entrySet().equals(subTm.entrySet()));
				assertTrue(subTm.entrySet().equals(subAvl.entrySet()));
				subAvl = avl.tailMap(i);
				subTm = tm.tailMap(i);
				assertTrue(subAvl.equals(subTm));
				assertTrue(subTm.equals(subAvl));
				assertTrue(subAvl.entrySet().equals(subTm.entrySet()));
				assertTrue(subTm.entrySet().equals(subAvl.entrySet()));
				int temp = Math.max(i, 100 - i);
				subAvl = avl.subMap(100 - temp, temp);
				subTm = tm.subMap(100 - temp, temp);
				assertTrue(subAvl.equals(subTm));
				assertTrue(subTm.equals(subAvl));
				assertTrue(subAvl.entrySet().equals(subTm.entrySet()));
				assertTrue(subTm.entrySet().equals(subAvl.entrySet()));
				avlSet = avl.entrySet();
				tmSet = avl.entrySet();
				assertTrue(avlSet.equals(tmSet));
				assertTrue(tmSet.equals(avlSet));
			}
		}
	}
	
	private void findMissingEntries(Set<Map.Entry<Integer, Integer>> setOne, 
			Set<Map.Entry<Integer, Integer>> setTwo) {
		// gather this information for future use, if needed..
		LinkedList<Map.Entry<Integer, Integer>> entryOne = new LinkedList<Map.Entry<Integer, Integer>>();
		LinkedList<Map.Entry<Integer, Integer>> entryTwo = new LinkedList<Map.Entry<Integer, Integer>>();
		
		// primary storage for the results
		StringBuilder one = new StringBuilder();
		StringBuilder two = new StringBuilder();
		
		// find all the keys in entryOne but not in entryTwo
		for (Map.Entry<Integer, Integer> me : setOne) {
			if (!(setTwo.contains(me))) {
				entryOne.add(me);
				if (!(one.toString().equals(""))) { one.append(", "); }
				one.append("<" + me.getKey().toString() + ", " + me.getValue().toString() + ">");
			}
		}
		
		// find all the keys in entryTwo but not in entryOne
		for (Map.Entry<Integer, Integer> me : setTwo) {
			if (!(setOne.contains(me))) {
				entryTwo.add(me);
				if (!(two.toString().equals(""))) { two.append(", "); }
				two.append("<" + me.getKey().toString() + ", " + me.getValue().toString() + ">");
			}
		}
		
		// print out the results
		System.out.println("CALL TO: findMissingEntreis(" + setOne.getClass() + ", " + setTwo.getClass() + ")");
		if (entryOne.isEmpty() && entryTwo.isEmpty()) {
			System.out.println("CLEAR\n"); // skip extra line to separate calls
		} 
		else {
			System.out.println("Entries unique to setOne:\n" + one.toString());
			System.out.println("Entries unique to setTwo:\n" + two.toString());
		}
	}
}
