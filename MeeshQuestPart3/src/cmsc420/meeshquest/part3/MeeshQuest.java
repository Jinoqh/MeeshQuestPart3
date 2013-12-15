package cmsc420.meeshquest.part3;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import cmsc420.command.Command;
import cmsc420.drawing.CanvasPlus;
import cmsc420.xml.XmlUtility;


public class MeeshQuest {
    /* output DOM Document tree */
    private Document results;
    /* processes each command */
    private Command command;
    private File xmlInput;
    private File xmlOutput;

    public static void main(String[] args) {
    	final boolean debugMode = false;
    	final String inputFileName = "input.xml";
    	final String outputFileName = "output.xml";
        final MeeshQuest m = new MeeshQuest();
        if(debugMode){
        	m.processInput(inputFileName, outputFileName);
        }
        else {
        	m.processInput();
        }
    }

    private void processInput(String inputFileName, String outputFileName) {
    	try {
            xmlInput = new File(inputFileName);
            xmlOutput = new File(outputFileName);
            
            /* validate document */
            Document doc = XmlUtility.validateNoNamespace(xmlInput);
            
            /* create output */
            results = XmlUtility.getDocumentBuilder().newDocument();
            command = new Command();
            command.setResults(results);
            
            /* process commands element */
            Element commandNode = doc.getDocumentElement();
            processCommand(commandNode);

            /* process each command */
            final NodeList nl = commandNode.getChildNodes();
            for (int i = 0; i < nl.getLength(); i++) {
                if (nl.item(i).getNodeType() == Document.ELEMENT_NODE) {
                    /* need to check if Element (ignore comments) */
                    commandNode = (Element) nl.item(i);
                    processCommand(commandNode);
                }
            }
        } catch (SAXException e) {
            e.printStackTrace();
            addFatalError();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            addFatalError();
        } catch (IOException e) {
            e.printStackTrace();
            addFatalError();
        } catch (TransformerException e) {
            e.printStackTrace();
            addFatalError();
        } finally {
            try {
            	XmlUtility.write(results, xmlOutput);
                XmlUtility.print(results);
            } catch (TransformerException e) {
                System.exit(-1);
            } catch (FileNotFoundException e) {
				e.printStackTrace();
			}
        }
	}

	public void processInput() {
        try {
            /* validate document */
            Document doc = XmlUtility.validateNoNamespace(System.in);

            /* create output */
            results = XmlUtility.getDocumentBuilder().newDocument();
            command = new Command();
            command.setResults(results);
            /* process commands element */
            Element commandNode = doc.getDocumentElement();
            processCommand(commandNode);

            /* process each command */
            final NodeList nl = commandNode.getChildNodes();
            for (int i = 0; i < nl.getLength(); i++) {
                if (nl.item(i).getNodeType() == Document.ELEMENT_NODE) {
                    /* need to check if Element (ignore comments) */
                    commandNode = (Element) nl.item(i);
                    processCommand(commandNode);
                }
            }
        } catch (SAXException e) {
            e.printStackTrace();
            addFatalError();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            addFatalError();
        } catch (IOException e) {
            e.printStackTrace();
            addFatalError();
        } catch (TransformerException e) {
            e.printStackTrace();
            addFatalError();
        } finally {
            try {
                XmlUtility.print(results);
            } catch (TransformerException e) {
                System.exit(-1);
            }
        }
    }

    private void addFatalError() {
        try {
            results = XmlUtility.getDocumentBuilder().newDocument();
            final Element fatalError = results.createElement("fatalError");
            results.appendChild(fatalError);
        } catch (ParserConfigurationException e) {
            System.exit(-1);
        }
    }

    private void processCommand(final Element commandNode) throws IOException, ParserConfigurationException,
            TransformerException {
        final String name = commandNode.getNodeName();
        
        if (name.equals("commands")) {
            command.processCommands(commandNode);
        } else if (name.equals("createCity")) {
            command.processCreateCity(commandNode);
        } else if (name.equals("clearAll")) {
            command.processClearAll(commandNode);
        } else if (name.equals("listCities")) {
            command.processListCities(commandNode);
        } else if (name.equals("printAvlTree")) {
            command.processPrintAvlTree(commandNode);
        } else if (name.equals("mapRoad")) {
            command.processMapRoad(commandNode);
        } else if (name.equals("printPMQuadtree")) {
            command.processPrintPMQuadtree(commandNode);
        } else if (name.equals("saveMap")) {
            command.processSaveMap(commandNode);
        } else if (name.equals("rangeCities")) {
            command.processRangeCities(commandNode);
        } else if (name.equals("rangeRoads")) {
            command.processRangeRoads(commandNode);
        } else if (name.equals("nearestCity")) {
            command.processNearestCity(commandNode);
        } else if (name.equals("nearestRoad")) {
            command.processNearestRoad(commandNode);
        } else if (name.equals("nearestCityToRoad")) {
            command.processNearestCityToRoad(commandNode);
        } else if (name.equals("shortestPath")) {
            command.processShortestPath(commandNode);
        } else if (name.equals("mapPortal")){
        	command.processMapPortal(commandNode);
        } else if (name.equals("unmapPortal")){
        	command.processUnmapPortal(commandNode);
        } else if (name.equals("deleteCity")){
        	command.processDeleteCity(commandNode);
        } else if (name.equals("unmapRoad")){
        	command.processUnmapRoad(commandNode);
        } else if (name.equals("sweep")){
        	command.processSweep(commandNode);
        } else {
            /* problem with the Validator */
            System.exit(-1);
        }
    }
}
