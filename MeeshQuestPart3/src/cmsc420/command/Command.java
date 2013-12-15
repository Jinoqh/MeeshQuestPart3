package cmsc420.command;

import java.awt.Color;
import java.awt.geom.Arc2D;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Float;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import cmsc420.dijkstra.Dijkstranator;
import cmsc420.dijkstra.Path;
import cmsc420.drawing.CanvasPlus;
import cmsc420.geom.Circle2D;
import cmsc420.geom.Inclusive2DIntersectionVerifier;
import cmsc420.geom.Shape2DDistanceCalculator;
import cmsc420.city.City;
import cmsc420.city.CityLocationComparator;
import cmsc420.city.Geometry;
import cmsc420.city.Portal;
import cmsc420.city.Road;
import cmsc420.city.RoadAdjacencyList;
import cmsc420.pmquadtree.CityNotMappedThrowable;
import cmsc420.pmquadtree.InvalidPartitionThrowable;
import cmsc420.pmquadtree.OutOfBoundsThrowable;
import cmsc420.pmquadtree.PM1Quadtree;
import cmsc420.pmquadtree.PM3Quadtree;
import cmsc420.pmquadtree.PMQuadtree;
import cmsc420.pmquadtree.PortalIntersectsRoadThrowable;
import cmsc420.pmquadtree.PortalNotMappedThrowable;
import cmsc420.pmquadtree.RoadAlreadyExistsThrowable;
import cmsc420.pmquadtree.PMQuadtree.Black;
import cmsc420.pmquadtree.PMQuadtree.Gray;
import cmsc420.pmquadtree.PMQuadtree.Node;
import cmsc420.pmquadtree.RoadIntersectsAnotherRoadThrowable;
import cmsc420.pmquadtree.RoadNotMappedThrowable;
import cmsc420.sortedmap.GuardedAvlGTree;
import cmsc420.sortedmap.StringComparator;
import cmsc420.xml.XmlUtility;

/**
 * Processes each command in the MeeshQuest program. Takes in an XML command
 * node, processes the node, and outputs the results.
 */
public class Command {
	/** output DOM Document tree */
	protected Document results;

	/** root node of results document */
	protected Element resultsNode;

	/**
	 * stores created cities sorted by their names (used with listCities
	 * command)
	 */
	protected GuardedAvlGTree<String, City> citiesByName;

	/**
	 * stores created cities sorted by their locations (used with listCities
	 * command)
	 */
	protected final TreeSet<City> citiesByLocation = new TreeSet<City>(
			new CityLocationComparator());

	
	private final RoadAdjacencyList roads = new RoadAdjacencyList();

	/** stores mapped cities in a spatial data structure */
	protected PMQuadtree pmQuadtree;

	protected TreeMap<Integer, PMQuadtree> pmPortalQuadtree;
	protected final TreeMap<Integer, Portal> portalsByLevel = new TreeMap<Integer, Portal>();
	
	protected TreeMap<String,Portal> allPortals = new TreeMap<String,Portal>();
	/** order of the PM Quadtree */
	protected int pmOrder;

	/** spatial width of the PM Quadtree */
	protected int spatialWidth;

	/** spatial height of the PM Quadtree */
	protected int spatialHeight;

	/**
	 * Set the DOM Document tree to send the results of processed commands to.
	 * Creates the root results node.
	 * 
	 * @param results
	 *            DOM Document tree
	 */
	public void setResults(Document results) {
		this.results = results;
		resultsNode = results.createElement("results");
		results.appendChild(resultsNode);
	}

	/**
	 * Creates a command result element. Initializes the command name.
	 * 
	 * @param node
	 *            the command node to be processed
	 * @return the results node for the command
	 */
	private Element getCommandNode(final Element node) {
		final Element commandNode = results.createElement("command");
		commandNode.setAttribute("name", node.getNodeName());
		
		if (node.hasAttribute("id")) {
		    commandNode.setAttribute("id", node.getAttribute("id"));
		}
		return commandNode;
	}

	/**
	 * Processes an integer attribute for a command. Appends the parameter to
	 * the parameters node of the results. Should not throw a number format
	 * exception if the attribute has been defined to be an integer in the
	 * schema and the XML has been validated beforehand.
	 * 
	 * @param commandNode
	 *            node containing information about the command
	 * @param attributeName
	 *            integer attribute to be processed
	 * @param parametersNode
	 *            node to append parameter information to
	 * @return integer attribute value
	 */
	private int processIntegerAttribute(final Element commandNode,
			final String attributeName, final Element parametersNode) {
		final String value = commandNode.getAttribute(attributeName);

		if (parametersNode != null) {
			/* add the parameters to results */
			final Element attributeNode = results.createElement(attributeName);
			attributeNode.setAttribute("value", value);
			parametersNode.appendChild(attributeNode);
		}

		/* return the integer value */
		return Integer.parseInt(value);
	}

	/**
	 * Processes a string attribute for a command. Appends the parameter to the
	 * parameters node of the results.
	 * 
	 * @param commandNode
	 *            node containing information about the command
	 * @param attributeName
	 *            string attribute to be processed
	 * @param parametersNode
	 *            node to append parameter information to
	 * @return string attribute value
	 */
	private String processStringAttribute(final Element commandNode,
			final String attributeName, final Element parametersNode) {
		final String value = commandNode.getAttribute(attributeName);

		if (parametersNode != null) {
			/* add parameters to results */
			final Element attributeNode = results.createElement(attributeName);
			attributeNode.setAttribute("value", value);
			parametersNode.appendChild(attributeNode);
		}

		/* return the string value */
		return value;
	}

	/**
	 * Reports that the requested command could not be performed because of an
	 * error. Appends information about the error to the results.
	 * 
	 * @param type
	 *            type of error that occurred
	 * @param command
	 *            command node being processed
	 * @param parameters
	 *            parameters of command
	 */
	private void addErrorNode(final String type, final Element command,
			final Element parameters) {
		final Element error = results.createElement("error");
		error.setAttribute("type", type);
		error.appendChild(command);
		error.appendChild(parameters);
		resultsNode.appendChild(error);
	}

	/**
	 * Reports that a command was successfully performed. Appends the report to
	 * the results.
	 * 
	 * @param command
	 *            command not being processed
	 * @param parameters
	 *            parameters used by the command
	 * @param output
	 *            any details to be reported about the command processed
	 */
	private Element addSuccessNode(final Element command,
			final Element parameters, final Element output) {
		final Element success = results.createElement("success");
		success.appendChild(command);
		success.appendChild(parameters);
		success.appendChild(output);
		resultsNode.appendChild(success);
		return success;
	}

	/**
	 * Processes the commands node (root of all commands). Gets the spatial
	 * width and height of the map and send the data to the appropriate data
	 * structures.
	 * 
	 * @param node
	 *            commands node to be processed
	 */
	public void processCommands(final Element node) {
		spatialWidth = Integer.parseInt(node.getAttribute("spatialWidth"));
		spatialHeight = Integer.parseInt(node.getAttribute("spatialHeight"));
		pmOrder = Integer.parseInt(node.getAttribute("pmOrder"));
		pmQuadtree = null;
		
		if (pmOrder == 3) {
			pmQuadtree = new PM3Quadtree(spatialWidth, spatialHeight);
		} else if (pmOrder == 1) {
			pmQuadtree = new PM1Quadtree(spatialWidth, spatialHeight);
		}
		
		pmPortalQuadtree = new TreeMap<Integer, PMQuadtree>();
		allPortals = new TreeMap<String, Portal>();
        citiesByName = new GuardedAvlGTree<String, City>(new StringComparator(),
                Integer.parseInt(node.getAttribute("g")));
	}

	/**
	 * Processes a createCity command. Creates a city in the dictionary (Note:
	 * does not map the city). An error occurs if a city with that name or
	 * location is already in the dictionary.
	 * 
	 * @param node
	 *            createCity node to be processed
	 */
	public void processCreateCity(final Element node) {
		final Element commandNode = getCommandNode(node);
		final Element parametersNode = results.createElement("parameters");

		final String name = processStringAttribute(node, "name", parametersNode);
		final int x = processIntegerAttribute(node, "x", parametersNode);
		final int y = processIntegerAttribute(node, "y", parametersNode);
		final int z = processIntegerAttribute(node, "z", parametersNode);

		final int radius = processIntegerAttribute(node, "radius",
				parametersNode);
		final String color = processStringAttribute(node, "color",
				parametersNode);

		/* create the city */
		final City city = new City(name, x, y, z, radius, color);

		if (citiesByName.containsKey(name) || allPortals.containsKey(name)) {
			addErrorNode("duplicateCityName", commandNode, parametersNode);
		} else if (citiesByLocation.contains(city) || containsPortalCoordinate(city.toPoint2D(), city.getZ())) {
			addErrorNode("duplicateCityCoordinates", commandNode,
					parametersNode);
		} else {
			final Element outputNode = results.createElement("output");

			/* add city to dictionary */
			citiesByName.put(name, city);
			citiesByLocation.add(city);

			/* add success node to results */
			addSuccessNode(commandNode, parametersNode, outputNode);
		}
	}

	private boolean containsPortalName(String name){
		for(Portal portal : portalsByLevel.values()){
			if(portal.getName().equals(name)){
				return true;
			}
		}
		return false;
	}
	
	private boolean containsPortalCoordinate(Point2D.Float pt, int z){
		for(Portal portal : allPortals.values()){
			if(pt.equals(portal.toPoint2D()) && z == portal.getZ()){
				return true;
			}
		}
		return false;
	}
	
	private boolean containsCityCoordinate (Point2D.Float pt, int z){
		for(City city : citiesByLocation){
			if(city.toPoint2D().equals(pt) && city.getZ() == z)
				return true;
		}
		return false;
	}
	/**
	 * Clears all the data structures do there are not cities or roads in
	 * existence in the dictionary or on the map.
	 * 
	 * @param node
	 *            clearAll node to be processed
	 */
	public void processClearAll(final Element node) {
		final Element commandNode = getCommandNode(node);
		final Element parametersNode = results.createElement("parameters");
		final Element outputNode = results.createElement("output");

		/* clear data structures */
		citiesByName.clear();
		citiesByLocation.clear();
		pmQuadtree.clear();
		pmPortalQuadtree.clear();
		roads.clear();
		addSuccessNode(commandNode, parametersNode, outputNode);
		/* clear canvas */
		// canvas.clear();
		/* add a rectangle to show where the bounds of the map are located */
		// canvas.addRectangle(0, 0, spatialWidth, spatialHeight, Color.BLACK,
		// false);
		/* add success node to results */
	}

	/**
	 * Lists all the cities, either by name or by location.
	 * 
	 * @param node
	 *            listCities node to be processed
	 */
	public void processListCities(final Element node) {
		final Element commandNode = getCommandNode(node);
		final Element parametersNode = results.createElement("parameters");
		final String sortBy = processStringAttribute(node, "sortBy",
				parametersNode);

		if (citiesByName.isEmpty()) {
			addErrorNode("noCitiesToList", commandNode, parametersNode);
		} else {
			final Element outputNode = results.createElement("output");
			final Element cityListNode = results.createElement("cityList");

			Collection<City> cityCollection = null;
			if (sortBy.equals("name")) {
				List<City> cities = new ArrayList<City>(citiesByLocation.size());
				for (City c : citiesByLocation)
					cities.add(c);
				Collections.sort(cities, new Comparator<City>() {

					// @Override
					public int compare(City arg0, City arg1) {
						return arg0.getName().compareTo(arg1.getName());
					}
				});
				cityCollection = cities;
			} else if (sortBy.equals("coordinate")) {
				cityCollection = citiesByLocation;
			} else {
				/* XML validator failed */
				System.exit(-1);
			}

			for (City c : cityCollection) {
				addCityNode(cityListNode, c);
			}
			outputNode.appendChild(cityListNode);

			/* add success node to results */
			addSuccessNode(commandNode, parametersNode, outputNode);
		}
	}

	/**
	 * Creates a city node containing information about a city. Appends the city
	 * node to the passed in node.
	 * 
	 * @param node
	 *            node which the city node will be appended to
	 * @param cityNodeName
	 *            name of city node
	 * @param city
	 *            city which the city node will describe
	 */
	private void addCityNode(final Element node, final String cityNodeName,
			final City city) {
		final Element cityNode = results.createElement(cityNodeName);
		cityNode.setAttribute("name", city.getName());
		cityNode.setAttribute("x", Integer.toString((int) city.getX()));
		cityNode.setAttribute("y", Integer.toString((int) city.getY()));
		cityNode.setAttribute("z", Integer.toString(((int) city.getZ())));
		cityNode.setAttribute("radius",
				Integer.toString((int) city.getRadius()));
		cityNode.setAttribute("color", city.getColor());
		node.appendChild(cityNode);
	}

	private void addCityNode(final Element node, final City city) {
		addCityNode(node, "city", city);
	}

	private void addRoadNode(final Element node, final Road road) {
		addRoadNode(node, "road", road);
	}

	private void addRoadNode(final Element node, final String roadNodeName,
			final Road road) {
		final Element roadNode = results.createElement(roadNodeName);
		roadNode.setAttribute("start", road.getStart().getName());
		roadNode.setAttribute("end", road.getEnd().getName());
		node.appendChild(roadNode);
	}

	public void processMapRoad(Element node) {
		final Element commandNode = getCommandNode(node);
		final Element parametersNode = results.createElement("parameters");

		final String start = processStringAttribute(node, "start",
				parametersNode);
		final String end = processStringAttribute(node, "end", parametersNode);
		
		final Element outputNode = results.createElement("output");
		
		if (!citiesByName.containsKey(start)) {
			addErrorNode("startPointDoesNotExist", commandNode, parametersNode);
		} else if (!citiesByName.containsKey(end)) {
			addErrorNode("endPointDoesNotExist", commandNode, parametersNode);
		} else if (start.equals(end)) {
			addErrorNode("startEqualsEnd", commandNode, parametersNode);
		} else if (citiesByName.get(start).getZ() != citiesByName.get(end).getZ()) {
			addErrorNode("roadNotOnOneLevel", commandNode, parametersNode);
		}
		else {
			try {
				final Integer z = citiesByName.get(start).getZ();
				// add to spatial structure
				if(!pmPortalQuadtree.containsKey(z)){
					pmPortalQuadtree.put(z, pmOrder == 1 ? new PM1Quadtree(spatialWidth, spatialHeight) : pmOrder == 3 ? new PM3Quadtree(spatialWidth, spatialHeight) : null );
				}
				
//				pmQuadtree.addRoad(new Road((City) citiesByName.get(start),(City) citiesByName.get(end)));
				pmPortalQuadtree.get(z).addRoad(new Road((City) citiesByName.get(start),(City) citiesByName.get(end)));
				
				if (Inclusive2DIntersectionVerifier.intersects(citiesByName.get(start).toPoint2D(), new Rectangle2D.Float(0, 0,spatialWidth, spatialHeight))
						&& Inclusive2DIntersectionVerifier.intersects(citiesByName.get(end).toPoint2D(),new Rectangle2D.Float(0, 0, spatialWidth,spatialHeight))) {
					// add to adjacency list
					roads.addRoad((City) citiesByName.get(start),(City) citiesByName.get(end));
				}
				// create roadCreated element
				final Element roadCreatedNode = results.createElement("roadCreated");
				roadCreatedNode.setAttribute("start", start);
				roadCreatedNode.setAttribute("end", end);
				outputNode.appendChild(roadCreatedNode);
				// add success node to results
				addSuccessNode(commandNode, parametersNode, outputNode);
			} catch (RoadAlreadyExistsThrowable e) {
				addErrorNode("roadAlreadyMapped", commandNode, parametersNode);
			} catch (OutOfBoundsThrowable e) {
				addErrorNode("roadOutOfBounds", commandNode, parametersNode);
			} catch (RoadIntersectsAnotherRoadThrowable e){
				addErrorNode("roadIntersectsAnotherRoad", commandNode, parametersNode);
			} catch (InvalidPartitionThrowable e){
				addErrorNode("roadViolatesPMRules", commandNode, parametersNode);
			} 
		}
	}
	
	public void processMapPortal(Element node) {
		final Element commandNode = getCommandNode(node);
		final Element parametersNode = results.createElement("parameters");

		final String name = processStringAttribute(node, "name", parametersNode);
		final int x = processIntegerAttribute(node, "x", parametersNode);
		final int y = processIntegerAttribute(node, "y", parametersNode);
		final int z = processIntegerAttribute(node, "z", parametersNode);
		final Element outputNode = results.createElement("output");
		
		/* create the city */
		final Portal portal = new Portal(name, x, y, z);
	
		if(!pmPortalQuadtree.containsKey(z)){
			pmPortalQuadtree.put(z, pmOrder == 1 ? new PM1Quadtree(spatialWidth, spatialHeight) : pmOrder == 3 ? new PM3Quadtree(spatialWidth, spatialHeight) : null );
		}
		
		if (pmPortalQuadtree.get(z).containsPortal()){
			addErrorNode("redundantPortal", commandNode, parametersNode);
		}  else if (citiesByName.containsKey(portal.getName()) || allPortals.containsKey(portal.getName())){
			addErrorNode("duplicatePortalName", commandNode, parametersNode);
		}  else if (containsPortalCoordinate(portal.toPoint2D(), portal.getZ()) || containsCityCoordinate(portal.toPoint2D(), portal.getZ())){
			addErrorNode("duplicatePortalCoordinates", commandNode, parametersNode);
		} else {
			try {
			
				pmPortalQuadtree.get(z).addPortal(portal);
				portalsByLevel.put(portal.getZ(), portal);
				allPortals.put(name,portal);
				
				// add success node to results
				addSuccessNode(commandNode, parametersNode, outputNode);
			} catch (OutOfBoundsThrowable e){
				addErrorNode("portalOutOfBounds", commandNode, parametersNode);
			} catch (PortalIntersectsRoadThrowable e) {
				addErrorNode("portalIntersectsRoad", commandNode, parametersNode);
			} catch (InvalidPartitionThrowable e) {
				addErrorNode("portalViolatesPMRules", commandNode, parametersNode);
			}
		}
	}

	public void processPrintAvlTree(Element node) {
        final Element commandNode = getCommandNode(node);
        final Element parametersNode = results.createElement("parameters");
        final Element outputNode = results.createElement("output");

        if (citiesByName.isEmpty()) {
            addErrorNode("emptyTree", commandNode, parametersNode);
        } else {
            outputNode.appendChild(citiesByName.createXml(outputNode));
            addSuccessNode(commandNode, parametersNode, outputNode);
        }
	}

	public void processShortestPath(final Element node) throws IOException,
			ParserConfigurationException, TransformerException {
		final Element commandNode = getCommandNode(node);
		final Element parametersNode = results.createElement("parameters");

		final String start = processStringAttribute(node, "start",
				parametersNode);
		final String end = processStringAttribute(node, "end", parametersNode);

		String saveMapName = "";
		if (!node.getAttribute("saveMap").equals("")) {
			saveMapName = processStringAttribute(node, "saveMap",
					parametersNode);
		}

		String saveHTMLName = "";
		if (!node.getAttribute("saveHTML").equals("")) {
			saveHTMLName = processStringAttribute(node, "saveHTML",
					parametersNode);
		}

		if (!pmQuadtree.containsCity(start)) {
			addErrorNode("nonExistentStart", commandNode, parametersNode);
		} else if (!pmQuadtree.containsCity(end)) {
			addErrorNode("nonExistentEnd", commandNode, parametersNode);
		} else {
			final DecimalFormat decimalFormat = new DecimalFormat("#0.000");

			final Dijkstranator dijkstranator = new Dijkstranator(roads);

			final City startCity = (City) citiesByName.get(start);
			final City endCity = (City) citiesByName.get(end);

			final Path path = dijkstranator.getShortestPath(startCity, endCity);

			if (path == null) {
				addErrorNode("noPathExists", commandNode, parametersNode);
			} else {
				final Element outputNode = results.createElement("output");

				final Element pathNode = results.createElement("path");
				pathNode.setAttribute("length",
						decimalFormat.format(path.getDistance()));
				pathNode.setAttribute("hops", Integer.toString(path.getHops()));

				final LinkedList<City> cityList = path.getCityList();

				/* if required, save the map to an image */
				if (!saveMapName.equals("")) {
					saveShortestPathMap(saveMapName, cityList);
				}
				if (!saveHTMLName.equals("")) {
					saveShortestPathMap(saveHTMLName, cityList);
				}

				if (cityList.size() > 1) {
					/* add the first road */
					City city1 = cityList.remove();
					City city2 = cityList.remove();
					Element roadNode = results.createElement("road");
					roadNode.setAttribute("start", city1.getName());
					roadNode.setAttribute("end", city2.getName());
					pathNode.appendChild(roadNode);

					while (!cityList.isEmpty()) {
						City city3 = cityList.remove();

						/* process the angle */
						Arc2D.Float arc = new Arc2D.Float();
						arc.setArcByTangent(city1.toPoint2D(),
								city2.toPoint2D(), city3.toPoint2D(), 1);

						/* print out the direction */
						double angle = arc.getAngleExtent();
						final String direction;
						while (angle < 0) {
							angle += 360;
						}
						while (angle > 360) {
							angle -= 360;
						}
						if (angle > 180 && angle <= 180 + 135) {
							direction = "left";
						} else if (angle > 45 && angle <= 180 ) {
							direction = "right";
						} else {
							direction = "straight";
						}
						Element directionNode = results
								.createElement(direction);
						pathNode.appendChild(directionNode);

						/* print out the next road */
						roadNode = results.createElement("road");
						roadNode.setAttribute("start", city2.getName());
						roadNode.setAttribute("end", city3.getName());
						pathNode.appendChild(roadNode);

						/* increment city references */
						city1 = city2;
						city2 = city3;
					}
				}
				outputNode.appendChild(pathNode);
				Element successNode = addSuccessNode(commandNode,
						parametersNode, outputNode);

				if (!saveHTMLName.equals("")) {
					/* save shortest path to HTML */
					Document shortestPathDoc = XmlUtility.getDocumentBuilder()
							.newDocument();
					org.w3c.dom.Node spNode = shortestPathDoc.importNode(
							successNode, true);
					shortestPathDoc.appendChild(spNode);
					XmlUtility.transform(shortestPathDoc, new File(
							"shortestPath.xsl"), new File(saveHTMLName
							+ ".html"));
				}
			}
		}
	}

	private void saveShortestPathMap(final String mapName,
			final List<City> cityList) throws IOException {
		final CanvasPlus map = new CanvasPlus();
		/* initialize map */
		map.setFrameSize(spatialWidth, spatialHeight);
		/* add a rectangle to show where the bounds of the map are located */
		map.addRectangle(0, 0, spatialWidth, spatialHeight, Color.BLACK, false);

		final Iterator<City> it = cityList.iterator();
		City city1 = it.next();

		/* map green starting point */
		map.addPoint(city1.getName(), city1.getX(), city1.getY(), Color.GREEN);

		if (it.hasNext()) {
			City city2 = it.next();
			/* map blue road */
			map.addLine(city1.getX(), city1.getY(), city2.getX(), city2.getY(),
					Color.BLUE);

			while (it.hasNext()) {
				/* increment cities */
				city1 = city2;
				city2 = it.next();

				/* map point */
				map.addPoint(city1.getName(), city1.getX(), city1.getY(),
						Color.BLUE);

				/* map blue road */
				map.addLine(city1.getX(), city1.getY(), city2.getX(),
						city2.getY(), Color.BLUE);
			}

			/* map red end point */
			map.addPoint(city2.getName(), city2.getX(), city2.getY(), Color.RED);

		}

		/* save map to image file */
		map.save(mapName);

		map.dispose();
	}

	/**
	 * Processes a saveMap command. Saves the graphical map to a given file.
	 * 
	 * @param node
	 *            saveMap command to be processed
	 * @throws IOException
	 *             problem accessing the image file
	 */
	public void processSaveMap(final Element node) throws IOException {
		final Element commandNode = getCommandNode(node);
		final Element parametersNode = results.createElement("parameters");
		final int z = processIntegerAttribute(node, "z", parametersNode);
		final String name = processStringAttribute(node, "name", parametersNode);

		final Element outputNode = results.createElement("output");

		CanvasPlus canvas = drawPMQuadtree(z);

		/* save canvas to '(name).png' */
		canvas.save(name);

//		canvas.dispose();

		/* add success node to results */
		addSuccessNode(commandNode, parametersNode, outputNode);
	}

	private CanvasPlus drawPMQuadtree(int z) {
		final CanvasPlus canvas = new CanvasPlus("MeeshQuest");

		/* initialize canvas */
		canvas.setFrameSize(spatialWidth, spatialHeight);

		/* add a rectangle to show where the bounds of the map are located */
		canvas.addRectangle(0, 0, spatialWidth, spatialHeight, Color.BLACK,
				false);

		/* draw PM Quadtree */
//		drawPMQuadtreeHelper(pmQuadtree.getRoot(), canvas);
		drawPMQuadtreeHelper(pmPortalQuadtree.get(z).getRoot(), canvas);

		return canvas;
	}

	private void drawPMQuadtreeHelper(Node node, CanvasPlus canvas) {
		if (node.getType() == Node.BLACK) {
			Black blackNode = (Black) node;
			
			if(blackNode.portalExists()){
				final Portal p = blackNode.getPortal();
				canvas.addPoint(p.getName(), p.getX(), p.getY(), Color.RED);
			}
			for (Geometry g : blackNode.getGeometry()) {
				if (g.isCity()) {
					City city = (City) g;
					canvas.addPoint(city.getName(), city.getX(), city.getY(), Color.BLACK);
				} else {
					Road road = (Road) g;
					canvas.addLine(road.getStart().getX(), road.getStart().getY(), road.getEnd().getX(),road.getEnd().getY(), Color.BLACK);
				}
			}
		} else if (node.getType() == Node.GRAY) {
			Gray grayNode = (Gray) node;
			canvas.addCross(grayNode.getCenterX(), grayNode.getCenterY(),
					grayNode.getHalfWidth(), Color.GRAY);
			for (int i = 0; i < 4; i++) {
				drawPMQuadtreeHelper(grayNode.getChild(i), canvas);
			}
		}
	}

	/**
	 * Prints out the structure of the PM Quadtree in an XML format.
	 * 
	 * @param node
	 *            printPMQuadtree command to be processed
	 */

	public void processPrintPMQuadtree(final Element node) {
		final Element commandNode = getCommandNode(node);
		final Element parametersNode = results.createElement("parameters");
		final Element outputNode = results.createElement("output");
		final int z = processIntegerAttribute(node, "z", parametersNode);
		
		if(!pmPortalQuadtree.containsKey(z)){
			pmPortalQuadtree.put(z, pmOrder == 1 ? new PM1Quadtree(spatialWidth, spatialHeight) : pmOrder == 3 ? new PM3Quadtree(spatialWidth, spatialHeight) : null );
		}
		
		if (pmPortalQuadtree.get(z).isEmpty()){
			/* empty PR Quadtree */
			addErrorNode("levelIsEmpty", commandNode, parametersNode);
		} else {
			/* print PR Quadtree */
			final Element quadtreeNode = results.createElement("quadtree");
			quadtreeNode.setAttribute("order", Integer.toString(pmOrder));
			printPMQuadtreeHelper(pmPortalQuadtree.get(z).getRoot(), quadtreeNode);

			outputNode.appendChild(quadtreeNode);

			/* add success node to results */
			addSuccessNode(commandNode, parametersNode, outputNode);
		}
	}

	/**
	 * Traverses each node of the PR Quadtree.
	 * 
	 * @param currentNode
	 *            PR Quadtree node being printed
	 * @param xmlNode
	 *            XML node representing the current PR Quadtree node
	 */

	private void printPMQuadtreeHelper(final Node currentNode,
			final Element xmlNode) {
		if (currentNode.getType() == Node.WHITE) {
			Element white = results.createElement("white");
			xmlNode.appendChild(white);
		} else if (currentNode.getType() == Node.BLACK) {
			Black currentLeaf = (Black) currentNode;
			Element blackNode = results.createElement("black");
			blackNode.setAttribute("cardinality",
					Integer.toString(currentLeaf.getGeometry().size() + (currentLeaf.portalExists() ? 1 : 0)));
			
			if(currentLeaf.portalExists()){
				Portal p = currentLeaf.getPortal();
				Element portal = results.createElement("portal");
				portal.setAttribute("name", p.getName());
				portal.setAttribute("x", Integer.toString(p.getX()));
				portal.setAttribute("y", Integer.toString(p.getY()));
				portal.setAttribute("z", Integer.toString(p.getZ()));
				blackNode.appendChild(portal);
			}
			
			for (Geometry g : currentLeaf.getGeometry()) {
				if (g.isCity()) {
					City c = (City) g;
					Element city = results.createElement("city");
					city.setAttribute("name", c.getName());
					city.setAttribute("x", Integer.toString((int) c.getX()));
					city.setAttribute("y", Integer.toString((int) c.getY()));
					city.setAttribute("z", Integer.toString(c.getZ()));
					city.setAttribute("radius", Integer.toString((int) c.getRadius()));
					city.setAttribute("color", c.getColor());
					blackNode.appendChild(city);
				} else {
					City c1 = ((Road) g).getStart();
					City c2 = ((Road) g).getEnd();
					Element road = results.createElement("road");
					road.setAttribute("start", c1.getName());
					road.setAttribute("end", c2.getName());
					blackNode.appendChild(road);
				}
			}
			xmlNode.appendChild(blackNode);
		} else {
			final Gray currentInternal = (Gray) currentNode;
			final Element gray = results.createElement("gray");
			gray.setAttribute("x",
					Integer.toString((int) currentInternal.getCenterX()));
			gray.setAttribute("y",
					Integer.toString((int) currentInternal.getCenterY()));
			for (int i = 0; i < 4; i++) {
				printPMQuadtreeHelper(currentInternal.getChild(i), gray);
			}
			xmlNode.appendChild(gray);
		}
	}

	/**
	 * Finds the mapped cities within the range of a given point.
	 * 
	 * @param node
	 *            rangeCities command to be processed
	 * @throws IOException
	 */
	public void processRangeCities(final Element node) throws IOException {
		final Element commandNode = getCommandNode(node);
		final Element parametersNode = results.createElement("parameters");
		final Element outputNode = results.createElement("output");

		final int x = processIntegerAttribute(node, "x", parametersNode);
		final int y = processIntegerAttribute(node, "y", parametersNode);
		final int z = processIntegerAttribute(node, "z", parametersNode);
		final int radius = processIntegerAttribute(node, "radius",
				parametersNode);

		String pathFile = "";
		if (!node.getAttribute("saveMap").equals("")) {
			pathFile = processStringAttribute(node, "saveMap", parametersNode);
		}

		if (radius == 0) {
			addErrorNode("noCitiesExistInRange", commandNode, parametersNode);
		} else {
			final TreeSet<Geometry> citiesInRange = new TreeSet<Geometry>();
			rangeHelper(new Circle2D.Double(x, y, radius),
					pmQuadtree.getRoot(), citiesInRange, false, true);

			/* print out cities within range */
			if (citiesInRange.isEmpty()) {
				addErrorNode("noCitiesExistInRange", commandNode,
						parametersNode);
			} else {
				/* get city list */
				final Element cityListNode = results.createElement("cityList");
				for (Geometry g : citiesInRange) {
					addCityNode(cityListNode, (City) g);
				}
				outputNode.appendChild(cityListNode);

				/* add success node to results */
				addSuccessNode(commandNode, parametersNode, outputNode);

				if (pathFile.compareTo("") != 0) {
					/* save canvas to file with range circle */
					CanvasPlus canvas = drawPMQuadtree(z);
					canvas.addCircle(x, y, radius, Color.BLUE, false);
					canvas.save(pathFile);
					canvas.dispose();
				}
			}
		}
	}

	public void processRangeRoads(final Element node) throws IOException {
		final Element commandNode = getCommandNode(node);
		final Element parametersNode = results.createElement("parameters");
		final Element outputNode = results.createElement("output");

		final int x = processIntegerAttribute(node, "x", parametersNode);
		final int y = processIntegerAttribute(node, "y", parametersNode);
		final int z = processIntegerAttribute(node, "z", parametersNode);
		final int radius = processIntegerAttribute(node, "radius",
				parametersNode);

		String pathFile = "";
		if (!node.getAttribute("saveMap").equals("")) {
			pathFile = processStringAttribute(node, "saveMap", parametersNode);
		}

		if (radius == 0) {
			addErrorNode("noRoadsExistInRange", commandNode, parametersNode);
		} else {
			final TreeSet<Geometry> roadsInRange = new TreeSet<Geometry>();
			rangeHelper(new Circle2D.Double(x, y, radius),
					pmQuadtree.getRoot(), roadsInRange, true, false);

			/* print out cities within range */
			if (roadsInRange.isEmpty()) {
				addErrorNode("noRoadsExistInRange", commandNode, parametersNode);
			} else {
				/* get road list */
				final Element roadListNode = results.createElement("roadList");
				for (Geometry g : roadsInRange) {
					addRoadNode(roadListNode, (Road) g);
				}
				outputNode.appendChild(roadListNode);

				/* add success node to results */
				addSuccessNode(commandNode, parametersNode, outputNode);

				if (pathFile.compareTo("") != 0) {
					/* save canvas to file with range circle */
					CanvasPlus canvas = drawPMQuadtree(z);
					canvas.addCircle(x, y, radius, Color.BLUE, false);
					canvas.save(pathFile);
					canvas.dispose();
				}
			}
		}
	}

	/**
	 * Helper function for both rangeCities and rangeRoads
	 * 
	 * @param range
	 *            defines the range as a circle
	 * @param node
	 *            is the node in the pmQuadtree being processed
	 * @param gInRange
	 *            stores the results
	 * @param includeRoads
	 *            specifies if the range search should include roads
	 * @param includeCities
	 *            specifies if the range search should include cities
	 */
	private void rangeHelper(final Circle2D.Double range, final Node node,
			final TreeSet<Geometry> gInRange, final boolean includeRoads,
			final boolean includeCities) {
		if (node.getType() == Node.BLACK) {
			final Black leaf = (Black) node;
			for (Geometry g : leaf.getGeometry()) {
				if (includeCities
						&& g.isCity()
						&& !gInRange.contains(g)
						&& Inclusive2DIntersectionVerifier.intersects(
								((City) g).toPoint2D(), range)) {
					gInRange.add(g);
				}
				if (includeRoads
						&& g.isRoad()
						&& !gInRange.contains(g)
						&& (((Road) g).toLine2D().ptSegDist(range.getCenter()) <= range
								.getRadius())) {
					gInRange.add(g);
				}
			}
		} else if (node.getType() == Node.GRAY) {
			final Gray internal = (Gray) node;
			for (int i = 0; i < 4; i++) {
				if (Inclusive2DIntersectionVerifier.intersects(
						internal.getChildRegion(i), range)) {
					rangeHelper(range, internal.getChild(i), gInRange,
							includeRoads, includeCities);
				}
			}
		}
	}

	public void processNearestCity(Element node) {
		final Element commandNode = getCommandNode(node);
		final Element parametersNode = results.createElement("parameters");
		final Element outputNode = results.createElement("output");

		/* extract attribute values from command */
		final int x = processIntegerAttribute(node, "x", parametersNode);
		final int y = processIntegerAttribute(node, "y", parametersNode);

		final Point2D.Float point = new Point2D.Float(x, y);

		if (pmQuadtree.getNumCities() == 0) {
			addErrorNode("cityNotFound", commandNode, parametersNode);
		} else {
			addCityNode(outputNode, nearestCityHelper(point));
			addSuccessNode(commandNode, parametersNode, outputNode);
		}
	}

	private City nearestCityHelper(Point2D.Float point) {
		Node n = pmQuadtree.getRoot();
		PriorityQueue<NearestSearchRegion> nearCities = new PriorityQueue<NearestSearchRegion>();

		if (n.getType() == Node.BLACK) {
			Black b = (Black) n;
			
			if (b.getCity() != null) {
				return b.getCity();
			}
		}

		while (n.getType() == Node.GRAY) {
			Gray g = (Gray) n;
			Node kid;
			
			for (int i = 0; i < 4; i++) {
				kid = g.getChild(i);
				
				if (kid.getType() == Node.BLACK) {
					Black b = (Black) kid;
					City c = b.getCity();
					
					if (c != null) {
						double dist = point.distance(c.toPoint2D());
						nearCities.add(new NearestSearchRegion(kid, dist, c));
					}
				} else if (kid.getType() == Node.GRAY) {
					double dist = Shape2DDistanceCalculator.distance(point,
							g.getChildRegion(i));
					nearCities.add(new NearestSearchRegion(kid, dist, null));
				}
			}
			
			try {
				n = nearCities.remove().node;
			} catch (Exception ex) {
				throw new IllegalStateException();
			}
		}
		return ((Black) n).getCity();
	}

	// // nearest road ////
	public void processNearestRoad(final Element node) throws IOException {
		final Element commandNode = getCommandNode(node);
		final Element parametersNode = results.createElement("parameters");
		final Element outputNode = results.createElement("output");

		/* extract values from command */
		final int x = processIntegerAttribute(node, "x", parametersNode);
		final int y = processIntegerAttribute(node, "y", parametersNode);

		if (pmQuadtree.getNumRoads() <= 0) {
			addErrorNode("roadNotFound", commandNode, parametersNode);
		} else {
			final Point2D.Float pt = new Point2D.Float(x, y);
			Road road = nearestRoadHelper(pt);
			addRoadNode(outputNode, road);
			addSuccessNode(commandNode, parametersNode, outputNode);
		}
	}

	private Road nearestRoadHelper(Point2D.Float point) {
		Node n = pmQuadtree.getRoot();
		PriorityQueue<NearestSearchRegion> nearRoads = new PriorityQueue<NearestSearchRegion>();
		NearestSearchRegion region = null;
		
		if (n.getType() == Node.BLACK) {
			List<Geometry> gList = ((Black) n).getGeometry();
			double minDist = Double.MAX_VALUE;
			Road road = null;
			for (Geometry geom : gList) {
				if (geom.isRoad()) {
					double d = ((Road) geom).toLine2D().ptSegDist(point);
					if (d < minDist) {
						minDist = d;
						road = (Road) geom;
					}
				}
			}
			return road;
		}

		while (n.getType() == Node.GRAY) {
			Gray g = (Gray) n;
			Node kid;
			for (int i = 0; i < 4; i++) {
				kid = g.getChild(i);
				if (kid.getType() == Node.BLACK) {
					Black b = (Black) kid;
					List<Geometry> gList = b.getGeometry();
					double minDist = Double.MAX_VALUE;
					Road road = null;
					for (Geometry geom : gList) {
						if (geom.isRoad()) {
							double d = ((Road) geom).toLine2D()
									.ptSegDist(point);
							if (d < minDist) {
								minDist = d;
								road = (Road) geom;
							}
						}
					}
					if (road == null) {
						continue;
					}
					nearRoads.add(new NearestSearchRegion(kid, minDist, road));
				} else if (kid.getType() == Node.GRAY) {
					double dist = Shape2DDistanceCalculator.distance(point,
							g.getChildRegion(i));
					nearRoads.add(new NearestSearchRegion(kid, dist, null));
				}
			}
			try {
				region = nearRoads.remove();
				n = region.node;
			} catch (Exception ex) {
				// should be impossible to reach here
				throw new IllegalStateException();
			}
		}
		assert region.node.getType() == Node.BLACK;
		return (Road) region.g;
	}

	public void processNearestCityToRoad(final Element node) throws IOException {
		final Element commandNode = getCommandNode(node);
		final Element parametersNode = results.createElement("parameters");
		final Element outputNode = results.createElement("output");

		final String start = processStringAttribute(node, "start",
				parametersNode);
		final String end = processStringAttribute(node, "end", parametersNode);

		final City startCity = (City) citiesByName.get(start);
		final City endCity = (City) citiesByName.get(end);
		if (startCity == null || endCity == null) {
			addErrorNode("roadIsNotMapped", commandNode, parametersNode);
			return;
		}
		final Road road = new Road(startCity, endCity);

		if (pmQuadtree.containsRoad(road)) {
			City nc = nearestCityToRoadHelper(road);
			if (nc == null) {
				addErrorNode("noOtherCitiesMapped", commandNode, parametersNode);
			} else {
				addCityNode(outputNode, nc);
				addSuccessNode(commandNode, parametersNode, outputNode);
			}
		} else {
			addErrorNode("roadIsNotMapped", commandNode, parametersNode);
		}
	}

	private City nearestCityToRoadHelper(Road road) {
		Node n = pmQuadtree.getRoot();
		PriorityQueue<NearestSearchRegion> nearCities = new PriorityQueue<NearestSearchRegion>();

		while (n.getType() == Node.GRAY) {
			Gray g = (Gray) n;
			Node kid;
			for (int i = 0; i < 4; i++) {
				kid = g.getChild(i);
				if (kid.getType() == Node.BLACK) {
					City c = ((Black) kid).getCity();
					if (c != null && !road.contains(c)) {
						double dist = road.toLine2D().ptSegDist(c.toPoint2D());
						nearCities.add(new NearestSearchRegion(kid, dist, c));
					}
				} else if (kid.getType() == Node.GRAY) {
					double dist = Shape2DDistanceCalculator.distance(
							road.toLine2D(), g.getChildRegion(i));
					nearCities.add(new NearestSearchRegion(kid, dist, null));
				}
			}
			try {
				if (nearCities.isEmpty()) {
					// no other cities mapped
					return null;
				}
				n = nearCities.remove().node;
			} catch (Exception ex) {
				throw new IllegalStateException();
			}
		}
		return ((Black) n).getCity();
	}

	/**
	 * Helper class for nearest everything (city/road/etc)
	 */
	private class NearestSearchRegion implements
			Comparable<NearestSearchRegion> {
		private Node node;
		private double distance;
		private Geometry g;

		public NearestSearchRegion(Node node, double distance, Geometry g) {
			this.node = node;
			this.distance = distance;
			this.g = g;
		}

		public int compareTo(NearestSearchRegion o) {
			if (distance == o.distance) {
				if (node.getType() == Node.BLACK
						&& o.node.getType() == Node.BLACK) {
					return g.compareTo(o.g);
				} else if (node.getType() == Node.BLACK
						&& o.node.getType() == Node.GRAY) {
					return 1;
				} else if (node.getType() == Node.GRAY
						&& o.node.getType() == Node.BLACK) {
					return -1;
				} else {
					return ((Gray) node).hashCode()
							- ((Gray) o.node).hashCode();
				}
			}
			return (distance < o.distance) ? -1 : 1;
		}
	}

	public void processUnmapPortal(Element node) {
 		final Element commandNode = getCommandNode(node);
		final Element parametersNode = results.createElement("parameters");
		final String name = processStringAttribute(node, "name", parametersNode);
		
		if (!allPortals.containsKey(name)) {
			addErrorNode("portalDoesNotExist", commandNode, parametersNode);
		} else {
			try {
				final Element outputNode = results.createElement("output");
				Portal p = new Portal(allPortals.get(name));
				pmPortalQuadtree.get(p.getZ()).removePortal(p);
				allPortals.remove(name);
				addSuccessNode(commandNode, parametersNode, outputNode);
			} catch (Exception e){
				addErrorNode("portalDoesNotExist", commandNode, parametersNode);
			}
		}
	}
	
	public void processUnmapRoad(Element node) {
		final Element commandNode = getCommandNode(node);
		final Element parametersNode = results.createElement("parameters");

		final String start = processStringAttribute(node, "start", parametersNode);
		final String end = processStringAttribute(node, "end", parametersNode);

		if (!citiesByName.containsKey(start)) {
			addErrorNode("startPointDoesNotExist", commandNode, parametersNode);
		} else if (!citiesByName.containsKey(end)) {
			addErrorNode("endPointDoesNotExist", commandNode, parametersNode);
		} else if (start.equals(end)) {
			addErrorNode("startEqualsEnd", commandNode, parametersNode);
		} else {
			try {
				final Integer z = citiesByName.get(start).getZ();
				final Element outputNode = results.createElement("output");
				final Element roadDeletedNode = results.createElement("roadDeleted");
				
				Road r = new Road((City) citiesByName.get(start), (City) citiesByName.get(end));
				pmPortalQuadtree.get(z).removeRoad(r);
				
				roadDeletedNode.setAttribute("start", start);
				roadDeletedNode.setAttribute("end", end);
				
				outputNode.appendChild(roadDeletedNode);
				addSuccessNode(commandNode, parametersNode, outputNode);
				
			} catch (RoadNotMappedThrowable e){
				addErrorNode("roadNotMapped", commandNode, parametersNode);
			}
		}
	}

	public void processDeleteCity(Element node) {
		final Element commandNode = getCommandNode(node);
		final Element parametersNode = results.createElement("parameters");

		final String name = processStringAttribute(node, "name", parametersNode);
		final Element outputNode = results.createElement("output");
	
		
		if (!citiesByName.containsKey(name)){
			addErrorNode("CityDoesNotExist", commandNode, parametersNode);
		} else {
			City city = citiesByName.get(name);
			final int z = city.getZ();
			
			try {
				
				TreeSet<Road> removedRoads = pmPortalQuadtree.get(z).removeCity(city);
				citiesByName.remove(name);
				Element cityUnmappedNode = results.createElement("cityUnmapped");
				cityUnmappedNode.setAttribute("color", city.getColor());
				cityUnmappedNode.setAttribute("name", city.getName());
				cityUnmappedNode.setAttribute("radius", String.valueOf(city.getRadius()));
				cityUnmappedNode.setAttribute("x", String.valueOf(city.getX()));
				cityUnmappedNode.setAttribute("y", String.valueOf(city.getY()));
				cityUnmappedNode.setAttribute("z", String.valueOf(city.getZ()));
				outputNode.appendChild(cityUnmappedNode);
				
				for(Road r : removedRoads){
					Element roadUnmappedNode = results.createElement("roadUnmapped");
					roadUnmappedNode.setAttribute("end", r.getEnd().getName());
					roadUnmappedNode.setAttribute("start", r.getStart().getName());
					outputNode.appendChild(roadUnmappedNode);
				}
				
				addSuccessNode(commandNode, parametersNode, outputNode);
			} catch (CityNotMappedThrowable e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (RoadNotMappedThrowable e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void processSweep(Element commandNode) {
		citiesByName.sweep();
	}
}
