import VF2Runner.VF2SubgraphIsomorphism;
import graphLoader.DBPediaLoader;
import infra.*;
import org.apache.jena.ext.com.google.common.collect.Sets;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.jetbrains.annotations.NotNull;
import org.jgrapht.Graph;
import org.jgrapht.GraphMapping;
import util.Config;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Map.Entry;

public class tgfdDiscovery {
	public static final int ENTITY_LIMIT = 5;
	public static final int MAX_NUMBER_OF_ATTR_PER_VERTEX = 2;
	public static final int NUM_OF_SINGLE_VERTEX_PATTERNS = 10;
	public static final int NUM_OF_CHILD_PATTERNS_LIMIT = 2;
	public static final int NUM_OF_SNAPSHOTS = 3;
	public static final double PATTERN_SUPPORT_THRESHOLD = 0.001;
	private static Integer NUM_OF_EDGES_IN_GRAPH;
	public static int NUM_OF_VERTICES_IN_GRAPH;
	public static int NUM_OF_FREQ_EDGES_TO_CONSIDER = 20;
	public static Map<String, Integer> uniqueVertexTypesHist; // freq nodes come from here
	public static Map<String, List<Map.Entry<String,Integer>>> vertexTypesAttributes; // freq attributes come from here
//	public static ArrayList<Entry<String, Integer>> attrSortedHist;
	public static Map<String, Integer> uniqueEdgesHist; // freq edges come from here
	public static GenerationTree genTree;
//	private static Map<String, Integer> attrHist;
	public static boolean isNaive = false;
	public static long fileSuffix;
	public static boolean isGraphExperiment;
	public static HashSet<String> lowPatternSupportEdges;

	public static void printTGFDstoFile(String experimentName, int k, double theta, ArrayList<TGFD> tgfds) {
		try {
			String timeAndDateStamp = ZonedDateTime.now( ZoneId.systemDefault() ).format( DateTimeFormatter.ofPattern( "uuuu.MM.dd.HH.mm.ss" ) );
			PrintStream printStream = new PrintStream(experimentName + "-" + (isNaive?"naive":"optimized") + "-" + k + "-" + String.format("%.1f", theta) + "-"+"-a"+MAX_NUMBER_OF_ATTR_PER_VERTEX+"-"+timeAndDateStamp+".txt");
			printStream.println("k = " + k);
			printStream.println("# of TGFDs generated = " + tgfds.size());
			for (TGFD tgfd : tgfds) {
				printStream.println(tgfd);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		System.gc();
	}

	public static void printKexperimentRuntimestoFile(TreeMap<Integer, Long> runtimes) {
		try {
			String timeAndDateStamp = ZonedDateTime.now( ZoneId.systemDefault() ).format( DateTimeFormatter.ofPattern( "uuuu.MM.dd.HH.mm.ss" ) );
			PrintStream printStream = new PrintStream("k-experiments-runtimes-"+ (isNaive?"naive":"optimized") + "-"+timeAndDateStamp+".txt");
			for (Integer kValue : runtimes.keySet()) {
				printStream.print("k = " + kValue);
				printStream.println(", execution time = " + (runtimes.get(kValue)));
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static void printThetaExperimentRuntimestoFile(TreeMap<Double, Long> runtimes) {
		try {
			String timeAndDateStamp = ZonedDateTime.now( ZoneId.systemDefault() ).format( DateTimeFormatter.ofPattern( "uuuu.MM.dd.HH.mm.ss" ) );
			PrintStream printStream = new PrintStream("theta-experiments-runtimes-"+ (isNaive?"naive":"optimized") + "-"+timeAndDateStamp+".txt");
			for (Double kValue : runtimes.keySet()) {
				printStream.print("theta = " + kValue);
				printStream.println(", execution time = " + (runtimes.get(kValue)));
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static void createSmallerSizedGraphs(long[] sizes) {
		String timeAndDateStamp = ZonedDateTime.now( ZoneId.systemDefault() ).format( DateTimeFormatter.ofPattern( "uuuu.MM.dd.HH.mm.ss" ) );
		PrintStream logStream = null;
		try {
			logStream = new PrintStream("graph-creation-log-"+timeAndDateStamp+".txt");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		System.setOut(logStream);
		String[] fileTypes = {"types", "literals", "objects"};
		for (long size : sizes) {
			for (int i = 5; i < 8; i++) {
				for (String fileType : fileTypes) {
					Model model = ModelFactory.createDefaultModel();
					String fileName = "201" + i + fileType + ".ttl";
					System.out.println("Processing " + fileName);
					Path input = Paths.get(fileName);
					model.read(input.toUri().toString());
					StmtIterator stmtIterator = model.listStatements();
					List<Statement> statements;
//					if (fileType.equals(fileTypes[0])) {
						statements = stmtIterator.toList().subList(0, Math.toIntExact(size));
//					} else {
//						statements = stmtIterator.toList().subList(0, Math.toIntExact(size*2));
//					}
					Model newModel = ModelFactory.createDefaultModel();
					newModel.add(statements);
					try {
						String newFileName = "201" + i + fileType + "-" + size + ".ttl";
						newModel.write(new PrintStream(newFileName),"N3");
						System.out.println("Wrote to " + newFileName);
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}
					System.gc();
				}
			}
		}
	}

//	public static void createAppropriateSizedGraphs(long[] sizes) {
//		String timeAndDateStamp = ZonedDateTime.now( ZoneId.systemDefault() ).format( DateTimeFormatter.ofPattern( "uuuu.MM.dd.HH.mm.ss" ) );
//		PrintStream logStream = null;
//		try {
//			logStream = new PrintStream("graph-creation-log-"+timeAndDateStamp+".txt");
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		}
//		System.setOut(logStream);
//		String[] fileTypes = {"types", "literals", "objects"};
//		for (long size : sizes) {
//			for (int i = 5; i < 8; i++) {
//				List<Resource> subjectsList = null;
//				for (String fileType : fileTypes) {
//					Model model = ModelFactory.createDefaultModel();
//					String fileName = "201" + i + fileType + ".ttl";
//					System.out.println("Processing " + fileName);
//					Path input = Paths.get(fileName);
//					model.read(input.toUri().toString());
//					ResIterator subjects = model.listSubjects();
//					if (fileType.equals("types")) {
//						subjectsList = subjects.toList().subList(0, Math.toIntExact(size));
//					}
//					StmtIterator statements = model.listStatements();
//					List<Statement> statementsList = statements.toList();
//					int removed = 0;
//					for (int index = 0; index < statementsList.size(); index++) {
//						Statement statement = statementsList.get(index);
//						if (!subjectsList.contains(statement.getSubject())) {
//							model.remove(statement);
//							removed++;
////							System.out.println("Removing " + statement);
//							System.out.println("Removed " + removed);
//						}
//					}
//					try {
//						String newFileName = "201" + i + fileType + "-" + size + ".ttl";
//						model.write(new PrintStream(newFileName));
//						System.out.println("Wrote to " + newFileName);
//					} catch (FileNotFoundException e) {
//						e.printStackTrace();
//					}
//					System.gc();
//				}
//			}
//		}
//	}

	public static void main(String[] args) {
		String timeAndDateStamp = ZonedDateTime.now( ZoneId.systemDefault() ).format( DateTimeFormatter.ofPattern( "uuuu.MM.dd.HH.mm.ss" ) );
		PrintStream logStream = null;
		try {
			logStream = new PrintStream("tgfd-discovery-log-"+timeAndDateStamp+".txt");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		System.setOut(logStream);

		// |G| experiment
		tgfdDiscovery.isNaive = false;
		tgfdDiscovery.isGraphExperiment = true;
		long[] sizes = {400000,800000,1200000,1600000};
		TreeMap<Long, Long> gRuntimes = new TreeMap<>();
		for (long size : sizes) {
			tgfdDiscovery.fileSuffix = size;
			// Compute Statistics
			histogram();
			printHistogram();
			System.gc();
			double theta = 0.1;
			int k = 2;
			System.out.println("Running experiment for |G| = " + size);
			final long startTime = System.currentTimeMillis();
			discover(k, theta, "G"+size+"-experiment");
			final long endTime = System.currentTimeMillis();
			final long runTime = endTime - startTime;
			System.out.println("Total execution time for |G| = " + size + " : " + runTime);
			gRuntimes.put(size, runTime);
			System.gc();
		}
		System.out.println();
		System.out.println("Runtimes for varying |G|:");
		for (long size : gRuntimes.keySet()) {
			System.out.print("|G| = " + size);
			System.out.println(", execution time = " + (gRuntimes.get(size)));
		}
		System.out.println();

		// theta-experiments
		// Compute Statistics
//		tgfdDiscovery.isNaive = false;
//		histogram();
//		printHistogram();
//		System.gc();
//		System.out.println("Varying theta");
//		TreeMap<Double, Long> thetaRuntimes = new TreeMap<>();
////		for (double theta = 0.5; theta > 0.0; theta-=0.1) {
//			double theta = 0.1;
//			System.out.println("Running experiment for theta = " + String.format("%.1f", theta));
//			final long startTime = System.currentTimeMillis();
//			discover(1, theta, "theta"+String.format("%.1f", theta)+"-experiment");
//			final long endTime = System.currentTimeMillis();
//			final long runTime = endTime - startTime;
//			System.out.println("Total execution time for theta = " + String.format("%.1f", theta) + " : " + runTime);
//			thetaRuntimes.put(theta, runTime);
//			System.gc();
////		}
//		printThetaExperimentRuntimestoFile(thetaRuntimes);
//		System.out.println();
//		System.out.println("Runtimes for varying theta:");
//		for (Double thetaValue : thetaRuntimes.keySet()) {
//			System.out.println("theta = " + String.format("%.1f", thetaValue));
//			System.out.println("Total execution time: " + (thetaRuntimes.get(thetaValue)));
//		}
//		System.out.println();

		// k-experiments
		// Compute Statistics
//		tgfdDiscovery.isNaive = false;
//		histogram();
//		printHistogram();
//		System.gc();
//		System.out.println("Varying k");
//		int k = 5;
//		System.out.println("Running experiment for k = "+k);
//		discover(k, 0.1, "k"+k+"-experiment");

	}

	public static String getLastPathSegment(String uriString) {
		String nodeURIString = uriString.toLowerCase();
		URI nodeURI = null;

		try {
			nodeURI = new URI(nodeURIString);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		assert nodeURI != null;
		String[] nodeURISegments = nodeURI.getPath().split("/");
		return nodeURISegments[nodeURISegments.length-1];
	}

	public static boolean isValidURI(String uriString) {
		try {
			new URI(uriString);
		} catch (URISyntaxException | NullPointerException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public static void computeNodeHistogram() {

		System.out.println("Computing Node Histogram");

		Map<String, Integer> vertexTypeHistogram = new HashMap<>();
		Map<String, Map<String, Integer>> tempVertexAttrFreqMap = new HashMap<>();
		Map<String, String> vertexNameToTypeMap = new HashMap<>();
		Model model = ModelFactory.createDefaultModel();

		for (int i = 5; i < 8; i++) {
			String fileName = "201"+i+"types"+(tgfdDiscovery.isGraphExperiment ? "-"+tgfdDiscovery.fileSuffix : "")+".ttl";
            Path input = Paths.get(fileName);
            System.out.println("Reading " + fileName);
            model.read(input.toUri().toString());
        }
		StmtIterator typeTriples = model.listStatements();
		while (typeTriples.hasNext()) {
			Statement stmt = typeTriples.nextStatement();
			String vertexType = stmt.getObject().asResource().getLocalName().toLowerCase();
			String vertexName = stmt.getSubject().getURI().toLowerCase();
			if (vertexName.length() > 28) {
				vertexName = vertexName.substring(28);
			}
			vertexTypeHistogram.merge(vertexType, 1, Integer::sum);
			tempVertexAttrFreqMap.putIfAbsent(vertexType, new HashMap<String, Integer>());
			vertexNameToTypeMap.put(vertexName, vertexType);
		}

		tgfdDiscovery.NUM_OF_VERTICES_IN_GRAPH = 0;
		for (Map.Entry<String, Integer> entry: vertexTypeHistogram.entrySet()) {
			tgfdDiscovery.NUM_OF_VERTICES_IN_GRAPH += entry.getValue();
		}
		System.out.println("Number of vertices in graph: " + tgfdDiscovery.NUM_OF_VERTICES_IN_GRAPH);
//		ArrayList<Entry<String, Integer>> sortedHistogram = new ArrayList<>(vertexTypeHistogram.entrySet());
//		if (!tgfdDiscovery.isNaive) {
//			sortedHistogram.sort(new Comparator<Entry<String, Integer>>() {
//				@Override
//				public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
//					return o2.getValue() - o1.getValue();
//				}
//			});
//		}
//
//		tgfdDiscovery.uniqueNodesHist = sortedHistogram;
		tgfdDiscovery.uniqueVertexTypesHist = vertexTypeHistogram;

		computeAttrHistogram(vertexNameToTypeMap, tempVertexAttrFreqMap);
		computeEdgeHistogram(vertexNameToTypeMap);

	}

	public static void computeAttrHistogram(Map<String, String> nodesRecord, Map<String, Map<String, Integer>> tempVertexAttrFreqMap) {
		System.out.println("Computing attributes histogram");

//		Map<String, Integer> attrHistMap = new HashMap<>();

		Model model = ModelFactory.createDefaultModel();
		for (int i = 5; i < 8; i++) {
//            String fileName = "201" + i + "literals.ttl";
			String fileName = "201"+i+"literals"+(tgfdDiscovery.isGraphExperiment ? "-"+tgfdDiscovery.fileSuffix : "")+".ttl";
            Path input = Paths.get(fileName);
			System.out.println("Reading " + fileName);
            model.read(input.toUri().toString());
        }
		StmtIterator typeTriples = model.listStatements();
		while (typeTriples.hasNext()) {
			Statement stmt = typeTriples.nextStatement();
			String vertexName = stmt.getSubject().getURI().toLowerCase();
			if (vertexName.length() > 28) {
				vertexName = vertexName.substring(28);
			}
			String attrName = stmt.getPredicate().getLocalName().toLowerCase();
//				attrHistMap.merge(attrName, 1, Integer::sum);
			if (nodesRecord.get(vertexName) != null) {
				String vertexType = nodesRecord.get(vertexName);
//					if (vertexHistogramMap.get(vertexType) != null) {
//					((HashMap<String, Integer>) vertexHistogramMap.get(nodesRecord.get(vertexName)).get("attributes")).merge(attrName, 1, Integer::sum);
				if 	(tempVertexAttrFreqMap.containsKey(vertexType)) {
					tempVertexAttrFreqMap.get(vertexType).merge(attrName, 1, Integer::sum);
				}
//					}
			}
		}
//		}
//		tgfdDiscovery.attrHist = attrHistMap;

		vertexTypesAttributes = new HashMap<>();

//		for (Map.Entry<String, Map<String,Object>> vertexType : vertexHistogramMap.entrySet()) {
		for (String vertexType : tempVertexAttrFreqMap.keySet()) {
//			ArrayList<Map.Entry<String,Integer>> sortedAttributes = new ArrayList<>(((HashMap<String, Integer>) vertexType.getValue().get("attributes")).entrySet());
			ArrayList<Map.Entry<String,Integer>> sortedAttributes = new ArrayList<>(tempVertexAttrFreqMap.get(vertexType).entrySet());
//			sortedAttributes.sort(new Comparator<String>() {
//				@Override
//				public int compare(String o1, String o2) {
//					return attrHist.get(o2) - attrHist.get(o1);
//				}
//			});
			if (!tgfdDiscovery.isNaive) {
				sortedAttributes.sort(new Comparator<Entry<String, Integer>>() {
					@Override
					public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
						return o2.getValue() - o1.getValue();
					}
				});
			}
//			vertexHistogramMap.get(vertexType.getKey()).put("attributes", sortedAttributes);
//			System.out.println(vertexType+":"+sortedAttributes);
			vertexTypesAttributes.put(vertexType, sortedAttributes.subList(0,Math.min(MAX_NUMBER_OF_ATTR_PER_VERTEX,sortedAttributes.size())));
//			System.out.println(vertexType+":"+vertexTypesAttributes.get(vertexType));
		}

//		ArrayList<Entry<String, Integer> > sortedHistogram = new ArrayList<>(attrHistMap.entrySet());
//		sortedHistogram.sort(new HistogramSort());

//		tgfdDiscovery.attrSortedHist = sortedHistogram;
	}

	public static void computeEdgeHistogram(Map<String, String> nodesRecord) {
		System.out.println("Computing edges histogram");

		Map<String, Integer> edgesHist = new HashMap<>();

		Model model = ModelFactory.createDefaultModel();
		for (int i = 5; i < 8; i++) {
//            String fileName = "201" + i + "objects.ttl";
			String fileName = "201"+i+"objects"+(tgfdDiscovery.isGraphExperiment ? "-"+tgfdDiscovery.fileSuffix : "")+".ttl";
            Path input = Paths.get(fileName);
			System.out.println("Reading " + fileName);
            model.read(input.toUri().toString());
        }
        StmtIterator typeTriples = model.listStatements();
        while (typeTriples.hasNext()) {
            Statement stmt = typeTriples.nextStatement();
            String subjectName = stmt.getSubject().getURI().toLowerCase();
            if (subjectName.length() > 28) {
                subjectName = subjectName.substring(28);
            }
            String predicateName = stmt.getPredicate().getLocalName().toLowerCase();
            String objectName = stmt.getObject().toString().substring(stmt.getObject().toString().lastIndexOf("/") + 1).toLowerCase();
            if (nodesRecord.get(subjectName) != null && nodesRecord.get(objectName) != null) {
                String uniqueEdge = nodesRecord.get(subjectName) + " " + predicateName + " " + nodesRecord.get(objectName);
                edgesHist.merge(uniqueEdge, 1, Integer::sum);
            }
        }

//		ArrayList<Entry<String, Integer>> sortedEdgesHist = new ArrayList<>(edgesHist.entrySet());
//		if (!tgfdDiscovery.isNaive) {
//			sortedEdgesHist.sort(new Comparator<Entry<String, Integer>>() {
//				@Override
//				public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
//					return o2.getValue() - o1.getValue();
//				}
//			});
//		}

		tgfdDiscovery.NUM_OF_EDGES_IN_GRAPH = 0;
		for (Map.Entry<String, Integer> entry: edgesHist.entrySet()) {
			tgfdDiscovery.NUM_OF_EDGES_IN_GRAPH += entry.getValue();
		}
		System.out.println("Number of edges in graph: " + tgfdDiscovery.NUM_OF_EDGES_IN_GRAPH);

		tgfdDiscovery.uniqueEdgesHist = edgesHist;
	}

	public static List<Entry<String, Integer>> getSortedNodeHistogram() {
		ArrayList<Entry<String, Integer>> sortedVertexTypeHistogram = new ArrayList<>(uniqueVertexTypesHist.entrySet());
		sortedVertexTypeHistogram.sort(new Comparator<Entry<String, Integer>>() {
			@Override
			public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
				return o2.getValue() - o1.getValue();
			}
		});
		int size = 0;
		for (Entry<String, Integer> entry : sortedVertexTypeHistogram) {
			if (1.0 * entry.getValue() / NUM_OF_VERTICES_IN_GRAPH >= PATTERN_SUPPORT_THRESHOLD) {
				size++;
			} else {
				break;
			}
		}
		if (tgfdDiscovery.isNaive) {
//			return (new ArrayList<>(uniqueVertexTypesHist.entrySet())).subList(0, size);
			return (new ArrayList<>(uniqueVertexTypesHist.entrySet()));
		}
		return sortedVertexTypeHistogram.subList(0,size);
	}

	public static List<Entry<String, Integer>> getSortedEdgeHistogram() {
		ArrayList<Entry<String, Integer>> sortedEdgesHist = new ArrayList<>(uniqueEdgesHist.entrySet());
		sortedEdgesHist.sort(new Comparator<Entry<String, Integer>>() {
			@Override
			public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
				return o2.getValue() - o1.getValue();
			}
		});
		int size = 0;
		for (Entry<String, Integer> entry : sortedEdgesHist) {
			if (1.0* entry.getValue() / NUM_OF_EDGES_IN_GRAPH >= PATTERN_SUPPORT_THRESHOLD) {
				size++;
			} else {
				break;
			}
		}
		if (tgfdDiscovery.isNaive) {
//			return (new ArrayList<>(uniqueEdgesHist.entrySet())).subList(0, size);
			return (new ArrayList<>(uniqueEdgesHist.entrySet()));
		}
		return sortedEdgesHist.subList(0, size);
	}

	public static void histogram() {
		computeNodeHistogram();
	}

	public static void printHistogram() {
		List<Entry<String, Integer>> sortedHistogram = getSortedNodeHistogram();
		List<Entry<String, Integer>> sortedEdgeHistogram = getSortedEdgeHistogram();

		System.out.println("Number of node types: " + sortedHistogram.size());
		System.out.println("Frequent Nodes:");
//		for (int i=0; i < 10; i++) {
		for (Entry<String, Integer> entry : sortedHistogram) {
			String vertexType = entry.getKey();
			List<Entry<String, Integer>> attributes = vertexTypesAttributes.get(vertexType);
//			ArrayList<Entry<String, Integer>> attributes = (ArrayList<Entry<String, Integer>>) nodesHist.get(i).getValue().get("attributes");
			System.out.println(vertexType+"={count="+ entry.getValue() +", attributes="+attributes+"}");
		}
//		System.out.println();
//		System.out.println("Number of attribute types: " + attrSortedHist.size());
//		System.out.println("Attributes:");
//		for (int i=0; i < 10; i++) {
//			System.out.println(attrSortedHist.get(i));
//		}
		System.out.println();
		System.out.println("Number of edge types: " + uniqueEdgesHist.size());
		System.out.println("Frequent Edges:");
//		for (int i=0; i < 10; i++) {
		for (Entry<String, Integer> entry : sortedEdgeHistogram) {
			System.out.println("edge=\""+entry.getKey()+"\", count="+ entry.getValue());
		}
		System.out.println();
	}

	public static HashMap<String, HashMap<String, ArrayList<Entry<String, Integer>>>> findMatchesX(ArrayList<DBPediaLoader> graphs, VF2PatternGraph pattern, HashMap<String, HashSet<String>> xVertexToAttrNameMap) {
		HashMap<String, HashMap<String, HashMap<String, Integer>>> xVertexToAttrNameToAttrValueMap = new HashMap<>();
		int t = 2015;

		for (DBPediaLoader graph : graphs) {
			Iterator<GraphMapping<Vertex, RelationshipEdge>> results = new VF2SubgraphIsomorphism().execute(graph.getGraph(), pattern, false, true);
			System.out.println("---------- Attribute values in " + t + " ---------- ");
			if (results != null) {
				int size = 0;
				int numOfSelfMatchingEntitiesInSnapshot = 0;
				// looks for n entity pairs in each snapshot, where n is the ENTITY_LIMIT
				while (numOfSelfMatchingEntitiesInSnapshot <= ENTITY_LIMIT && results.hasNext()) {
					GraphMapping<Vertex, RelationshipEdge> match = results.next();
					for (Vertex patternVertex : pattern.getPattern().vertexSet()) {
						Vertex currentMatchedVertex = match.getVertexCorrespondence(patternVertex, false);
						if (currentMatchedVertex != null) {
							String patternVertexType = new ArrayList<>(patternVertex.getTypes()).get(0);
							if (xVertexToAttrNameMap.containsKey(patternVertexType)) {
								xVertexToAttrNameToAttrValueMap.putIfAbsent(patternVertexType, new HashMap<>());
								for (String xAttrName : patternVertex.getAllAttributesNames()) {
									if (xVertexToAttrNameMap.get(patternVertexType).contains(xAttrName)) {
										xVertexToAttrNameToAttrValueMap.get(patternVertexType).putIfAbsent(xAttrName, new HashMap<>());
										String xAttrValue = currentMatchedVertex.getAttributeValueByName(xAttrName);

										xVertexToAttrNameToAttrValueMap.get(patternVertexType).get(xAttrName).merge(xAttrValue, 1, Integer::sum);
										if (xVertexToAttrNameToAttrValueMap.get(patternVertexType).get(xAttrName).get(xAttrValue) == 2) {
											numOfSelfMatchingEntitiesInSnapshot++;
										}
									}
								}
							}
							size++;
						}
					}
				}
				System.out.println("Number of matches: " + size);
			} else {
				System.out.println("No matches");
			}
			t++;
		}

//		for (String vertexType : xVertexToAttrNameToAttrValueMap.keySet()) {
//			for (String attr : xVertexToAttrNameToAttrValueMap.get(vertexType).keySet()) {
//				System.out.println("Number of values found for " + vertexType + "." + attr + ":" + xVertexToAttrNameToAttrValueMap.get(vertexType).get(attr).size());
//				System.out.println(vertexType + ":" + attr + ":" + xVertexToAttrNameToAttrValueMap.get(vertexType).get(attr));
//			}
//		}

		HashMap<String, HashMap<String, ArrayList<Entry<String, Integer>>>> sortedMap = new HashMap<>();
		for (String vertexType : xVertexToAttrNameToAttrValueMap.keySet()) {
			sortedMap.putIfAbsent(vertexType, new HashMap<>());
			for (String attr : xVertexToAttrNameToAttrValueMap.get(vertexType).keySet()) {
				ArrayList<Entry<String, Integer>> attrValueFrequencies = new ArrayList<>(xVertexToAttrNameToAttrValueMap.get(vertexType).get(attr).entrySet());
//				System.out.println("Size of map attribute values freq hashmap: " + map.get(vertexType).get(attr).size());
//				System.out.println("Size of map entry set: " + map.get(vertexType).get(attr).entrySet().size());
//				System.out.println("Size of attrValueFrequencies: " + attrValueFrequencies.size());
//				System.out.println("attrValueFrequencies: " + attrValueFrequencies);
				attrValueFrequencies.sort(new Comparator<Entry<String, Integer>>() {
					@Override
					public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
						return o2.getValue().compareTo(o1.getValue());
					}
				});
				sortedMap.get(vertexType).putIfAbsent(attr, attrValueFrequencies);
			}
		}

		for (String vertexType : sortedMap.keySet()) {
			for (String attr : sortedMap.get(vertexType).keySet()) {
				int numOfValues = sortedMap.get(vertexType).get(attr).size();
				System.out.println("Number of values found for " + vertexType + "." + attr + ": " + numOfValues);
				System.out.println(vertexType + ":" + attr + ":" + sortedMap.get(vertexType).get(attr).subList(0,Math.min(numOfValues,ENTITY_LIMIT)));
			}
		}
		return sortedMap;
	}

	public static void generateDummyTGFDsForDeltaDsicovery(int i, ArrayList<TGFD> dummyTGFDs) {
		ArrayList<GenTreeNode> nodes = genTree.getLevel(i);
		for (GenTreeNode node: nodes) {
//			if (node.parentNode() != null) {
//				if (node.parentNode().isPruned()) {
////					System.out.println("Skipping node: " + node.getPattern());
////					System.out.println("because of pruned parent node: " + node.parentNode().getPattern());
//					node.setIsPruned(); // TO-DO Wouldn't it be better to check this in VSpawn instead?
//					continue;
//				}
//			}
			ArrayList<TreeSet<Dependency>> dependenciesSets = node.getDependenciesSets();
			for (TreeSet<Dependency> dependencySet : dependenciesSets) {
				for (Dependency dependency : dependencySet) {
					VF2PatternGraph pattern = node.getPattern().copy();
					String yVertexType = ((ConstantLiteral) (dependency.getY().get(0))).getVertexType();
					String yAttrName = ((ConstantLiteral) (dependency.getY().get(0))).getAttrName();

					HashMap<String, HashSet<String>> xAttrNameMap = new HashMap<>();
					for (Literal l : dependency.getX()) { // TO-DO: What if X is an empty set?
						ConstantLiteral literal = (ConstantLiteral) l;
						String vType = literal.getVertexType();
						String attrName = literal.getAttrName();
						if (xAttrNameMap.get(vType) == null) {
							xAttrNameMap.put(vType, new HashSet<>(Collections.singletonList(attrName)));
						} else {
							xAttrNameMap.get(vType).add(attrName);
						}
					}

					for (Vertex v : pattern.getPattern().vertexSet()) {
						String vType = new ArrayList<>(v.getTypes()).get(0);
						if (xAttrNameMap.containsKey(vType)) {
							for (String attrName : xAttrNameMap.get(vType)) {
								v.addAttribute(new Attribute(attrName));
							}
						}
						if (v.getTypes().contains(yVertexType)) {
							v.addAttribute(new Attribute(yAttrName));
						}
					}
					TGFD dummyTGFD = new TGFD();
					dummyTGFD.setPattern(pattern);
					dummyTGFD.setDependency(dependency);
					dummyTGFD.setDelta(new Delta(Period.ofDays(0), Period.ofDays(Integer.MAX_VALUE), Duration.ofDays(1)));
					dummyTGFDs.add(dummyTGFD);
				}
			}
		}
	}

//	public static ArrayList<TGFD> deltaDiscovery(int k, double theta, ArrayList<DBPediaLoader> graphs) {
	public static ArrayList<TGFD> deltaDiscovery(int i, double theta) {

		// TO-DO: Is it possible to prune the graphs by passing them our generated patterns and dependencies as dummy TGFDs (with null Deltas) ???
		ArrayList<TGFD> dummyTGFDs = new ArrayList<>();
		if (Config.optimizedLoadingBasedOnTGFD) generateDummyTGFDsForDeltaDsicovery(i, dummyTGFDs);
		ArrayList<DBPediaLoader> graphs = new ArrayList<>();
		for (int year = 5; year < 8; year++) {
			String typeFileName = "201"+year+"types"+(tgfdDiscovery.isGraphExperiment ? "-"+tgfdDiscovery.fileSuffix : "")+".ttl";
			String literalsFileName = "201"+year+"literals"+(tgfdDiscovery.isGraphExperiment ? "-"+tgfdDiscovery.fileSuffix : "")+".ttl";
			String objectsFileName = "201"+year+"objects"+(tgfdDiscovery.isGraphExperiment ? "-"+tgfdDiscovery.fileSuffix : "")+".ttl";
			DBPediaLoader dbpedia = new DBPediaLoader(dummyTGFDs, new ArrayList<>(Collections.singletonList(typeFileName)), new ArrayList<>(Arrays.asList(literalsFileName, objectsFileName)));
			graphs.add(dbpedia);
		}

		System.out.println("Performing Delta Discovery for Gen Tree Level " + i);

		ArrayList<TGFD> tgfds = new ArrayList<>();

//		for (int i = 0; i <= k; i++) {
		ArrayList<GenTreeNode> nodes = genTree.getLevel(i);
		for (GenTreeNode node: nodes) {

			int numOfTGFDsAddedForThisNode = 0;

			System.out.println("Discovering delta for: ");
			System.out.println(node.getPattern());
			for (TreeSet<Dependency> dependencySet : node.getDependenciesSets()) {
				for (Dependency dependency : dependencySet) {
					System.out.println(dependency);
				}
			}
			ArrayList<TreeSet<Dependency>> dependenciesSets = node.getDependenciesSets();
			ArrayList<PatternDependencyPair> minimalSetOfTGFDs = new ArrayList<>();
			ArrayList<PatternDependencyPair> prunedSetOfTGFDs = new ArrayList<>();
			for (TreeSet<Dependency> dependencySet : dependenciesSets) {
				ArrayList<Dependency> prunedSetOfDependencies = new ArrayList<>();
				for (Dependency dependency: dependencySet) {
					// Prune non-minimal TGFDs
					if (node.parentNode() != null && !tgfdDiscovery.isNaive) {
						boolean isSubsetOfMinimalTGFD = isSubsetOfTGFD(node.getPattern(), dependency, node.parentNode().getMinimalSetOfDependencies(), minimalSetOfTGFDs);
						boolean isSubsetOfPrunedTGFD = isSubsetOfTGFD(node.getPattern(), dependency, node.parentNode().getPrunedSetOfDependencies(), prunedSetOfTGFDs);

						if (isSubsetOfMinimalTGFD) {
							System.out.println("Skip. Candidate dependency and pattern builds off minimal TGFD");
							continue;
						}
						if (isSubsetOfPrunedTGFD) {
							System.out.println("Skip. Candidate dependency and pattern builds off pruned TGFD");
							continue;
						}
					}

					// Prune low-support or non-minimal dependencies
					if (dependency.getX().size() != 1 && !tgfdDiscovery.isNaive) {
						boolean hasAPrunedSubset = false;
						for (Dependency prunedDependency : prunedSetOfDependencies) {
							if (dependency.getX().containsAll(prunedDependency.getX())) {
								hasAPrunedSubset = true;
								break;
							}
						}
						if (hasAPrunedSubset) {
							System.out.println("The following dependency has a pruned subset:" + dependency);
							System.out.println("Skipping this dependency");
							continue;
						}
					}

					// Discover entities
					int numOfTGFDsAddedForThisDependency = 0;

					String yVertexType = ((ConstantLiteral) (dependency.getY().get(0))).getVertexType();
					String yAttrName = ((ConstantLiteral) (dependency.getY().get(0))).getAttrName();

					HashMap<String, HashSet<String>> xAttrNameMap = new HashMap<>();
					for (Literal l : dependency.getX()) { // TO-DO: What if X is an empty set?
						ConstantLiteral literal = (ConstantLiteral) l;
						String vType = literal.getVertexType();
						String attrName = literal.getAttrName();
						if (xAttrNameMap.get(vType) == null) {
							xAttrNameMap.put(vType, new HashSet<>(Collections.singletonList(attrName)));
						} else {
							xAttrNameMap.get(vType).add(attrName);
						}
					}

					VF2PatternGraph patternCopy = node.getPattern().copy();
					for (Vertex v : patternCopy.getPattern().vertexSet()) {
						String vType = new ArrayList<>(v.getTypes()).get(0);
						if (xAttrNameMap.containsKey(vType)) {
							for (String attrName : xAttrNameMap.get(vType)) {
								v.addAttribute(new Attribute(attrName));
							}
						}
						if (v.getTypes().contains(yVertexType)) {
							v.addAttribute(new Attribute(yAttrName));
						}
					}

					System.out.println();
					System.out.println("Performing X discovery");
					System.out.println("Pattern: " + patternCopy);
					System.out.println("Dependency: " + dependency);
					HashMap<String, HashMap<String, ArrayList<Entry<String, Integer>>>> xAttrValueMap = findMatchesX(graphs, patternCopy, xAttrNameMap);
					System.gc();

					// Create a list of entities by using various combination of X attr values
					ArrayList<ArrayList<ConstantLiteral>> entities = new ArrayList<>();
					for (String vertexType : xAttrValueMap.keySet()) {
						HashMap<String, ArrayList<Entry<String, Integer>>> vertexTypeMap = xAttrValueMap.get(vertexType);
						for (String attrName : vertexTypeMap.keySet()) {
							int entityListSize = 0;
							ArrayList<Map.Entry<String, Integer>> attrValueFrequencies = vertexTypeMap.get(attrName);
							int subListSize = Math.min(attrValueFrequencies.size(), ENTITY_LIMIT);
							for (Entry<String, Integer> attrValueFreq : attrValueFrequencies.subList(0, subListSize)) {
								if (entities.size() - 1 < entityListSize) {
									entities.add(new ArrayList<>());
								}
								ConstantLiteral constantLiteral = new ConstantLiteral(vertexType, attrName, attrValueFreq.getKey());
								entities.get(entityListSize).add(constantLiteral);
								entityListSize++;
							}
						}
					}

					for (ArrayList<ConstantLiteral> entity : entities) {
						System.out.println(entity);
					}

					// Discover Y values and corresponding deltas for each entity
					ArrayList<Pair> constantXdeltas = new ArrayList<>();
					ArrayList<TreeSet<Pair>> satisfyingAttrValues = new ArrayList<>();
					for (ArrayList<ConstantLiteral> entity : entities) {
						VF2PatternGraph newPattern = node.getPattern().copy();
						Dependency newDependency = new Dependency();
						for (ConstantLiteral xLiteral : entity) {
							for (Vertex v : newPattern.getPattern().vertexSet()) {
								if (v.getTypes().contains(yVertexType)) {
									v.addAttribute(new Attribute(yAttrName));
									VariableLiteral newY = new VariableLiteral(yVertexType, yAttrName, yVertexType, yAttrName);
									newDependency.addLiteralToY(newY);
								}
								String vType = new ArrayList<>(v.getTypes()).get(0);
								if (xLiteral.getVertexType().equalsIgnoreCase(vType)) {
									v.addAttribute(new Attribute(xLiteral.getAttrName(), xLiteral.getAttrValue()));
									newDependency.addLiteralToX(new ConstantLiteral(vType, xLiteral.getAttrName(), xLiteral.getAttrValue()));
								}
							}
						}
						System.out.println("Performing Y discovery on pattern: " + newPattern);
						System.out.println("Entity: " + entity);
						ArrayList<Map.Entry<String, ArrayList<Integer>>> attrValuesTimestampsSortedByFreq = findMatchesY(graphs, newPattern, yAttrName, yVertexType, theta);
						//TO-DO: Add skipped dependencies to pruned list?
						if (attrValuesTimestampsSortedByFreq == null) {
							System.out.println("Skip. No matches found for entity " + entity);
							continue;
						} //TO-DO: Add skipped dependencies to pruned list?
						else if (attrValuesTimestampsSortedByFreq.size() == 0) {
							System.out.println("Skip. No matches found for entity " + entity);
							continue;
						}

						Delta delta = null;
						double support = 0.0;
						ArrayList<Pair> candidateDeltas = new ArrayList<>(2);
						if (attrValuesTimestampsSortedByFreq.size() == 1) {
							ArrayList<Integer> timestamps = attrValuesTimestampsSortedByFreq.get(0).getValue();
							int minDistance = graphs.size() - 1;
							int maxDistance = timestamps.get(timestamps.size() - 1) - timestamps.get(0);
							for (int index = 1; index < timestamps.size(); index++) {
								minDistance = Math.min(minDistance, timestamps.get(index) - timestamps.get(index - 1));
							}
							candidateDeltas.add(new Pair(minDistance, maxDistance));
						}
						else if (attrValuesTimestampsSortedByFreq.size() > 1) {
							int minExclusionDistance = graphs.size() - 1;
							int maxExclusionDistance = 0;
							ArrayList<Integer> distances = new ArrayList<>();
							int l1 = attrValuesTimestampsSortedByFreq.get(0).getValue().get(0);
							int u1 = attrValuesTimestampsSortedByFreq.get(0).getValue().get(attrValuesTimestampsSortedByFreq.get(0).getValue().size()-1);
							for (int index = 1; index < attrValuesTimestampsSortedByFreq.size(); index++) {
								int l2 = attrValuesTimestampsSortedByFreq.get(index).getValue().get(0);
								int u2 = attrValuesTimestampsSortedByFreq.get(index).getValue().get(attrValuesTimestampsSortedByFreq.get(index).getValue().size()-1);
								distances.add(Math.abs(u2-l1));
								distances.add(Math.abs(u1-l2));
							}
							for (int index = 0; index < distances.size(); index++) {
								minExclusionDistance = Math.min(minExclusionDistance, distances.get(index));
								maxExclusionDistance = Math.max(maxExclusionDistance, distances.get(index));
							}

							if (minExclusionDistance > 0) {
								Pair deltaPair = new Pair(0, minExclusionDistance-1);
								candidateDeltas.add(deltaPair);
							}
							if (maxExclusionDistance < graphs.size()-1) {
								Pair deltaPair = new Pair(maxExclusionDistance+1, graphs.size()-1);
								candidateDeltas.add(deltaPair);
							}
						}
						// Compute support
						for (Pair candidateDelta : candidateDeltas) {
							int minDistance = candidateDelta.min();
							int maxDistance = candidateDelta.max();
							if (minDistance <= maxDistance) {
								float numer = 0;
								float denom = 2 * 1 * graphs.size();
								String yValue = attrValuesTimestampsSortedByFreq.get(0).getKey();
								ArrayList<Integer> timestamps = attrValuesTimestampsSortedByFreq.get(0).getValue();
								TreeSet<Pair> satisfyingPairs = new TreeSet<Pair>();
								for (int index = 0; index < timestamps.size() - 1; index++) {
									for (int j = index + 1; j < timestamps.size(); j++) {
										if (timestamps.get(j) - timestamps.get(index) >= minDistance && timestamps.get(j) - timestamps.get(index) <= maxDistance) {
											satisfyingPairs.add(new Pair(timestamps.get(index), timestamps.get(j)));
										}
									}
								}

								System.out.println("Satisfying pairs: " + satisfyingPairs);

								numer = satisfyingPairs.size();
								System.out.println("Support = " + numer + "/" + denom);
								double candidateSupport = numer / denom;

								if (candidateSupport > support) {
									support = candidateSupport;
								}
								if (support >= theta) {
									delta = new Delta(Period.ofDays(minDistance * 183), Period.ofDays(maxDistance * 183 + 1), Duration.ofDays(183));
									satisfyingAttrValues.add(satisfyingPairs);
									constantXdeltas.add(new Pair(minDistance, maxDistance));
									break;
								}
							}
						}

                        //TO-DO: Add skipped dependencies to pruned list?
						if (delta == null) {
							continue;
						}

						System.out.println(delta);
						System.out.println("Support = " + support);

						TGFD tgfd = new TGFD(newPattern, delta, newDependency, "");
						System.out.println("TGFD: " + tgfd);
						tgfds.add(tgfd);
						numOfTGFDsAddedForThisDependency++;
						numOfTGFDsAddedForThisNode++;
					}

					if (numOfTGFDsAddedForThisDependency == 0) { // TO-DO: Only discard dependencies that build off this one. Don't discard ones that start in a different order.
						System.out.println("Could not find support for " + dependency);
						System.out.println("Do not expand this dependency. Pruned");
						prunedSetOfDependencies.add(dependency);
						prunedSetOfTGFDs.add(new PatternDependencyPair(patternCopy, dependency));
						continue;
					}

					System.gc();

					System.out.println("Size of constantXdeltas: " + constantXdeltas.size());
					for (Pair deltaPair : constantXdeltas) {
						System.out.println("constant delta: " + deltaPair);
					}

					System.out.println("Size of satisfyingAttrValues: " + satisfyingAttrValues.size());
					for (Set<Pair> satisfyingPairs : satisfyingAttrValues) {
						System.out.println("satisfyingAttrValues entry: " + satisfyingPairs);
					}

					// Find intersection delta
					HashMap<Pair, ArrayList<TreeSet<Pair>>> intersections = new HashMap<>();
					int currMin = 0;
					int currMax = graphs.size() - 1 ;
					ArrayList<TreeSet<Pair>> currSatisfyingAttrValues = new ArrayList<>();
					for (int index = 0; index < constantXdeltas.size(); index++) {
						Pair deltaPair = constantXdeltas.get(index);
						if (Math.max(currMin, deltaPair.min()) <= Math.min(currMax, deltaPair.max())) {
							currMin = Math.max(currMin, deltaPair.min());
							currMax = Math.min(currMax, deltaPair.max());
							currSatisfyingAttrValues.add(satisfyingAttrValues.get(index));
						} else {
							intersections.putIfAbsent(new Pair(currMin, currMax), currSatisfyingAttrValues);
							currSatisfyingAttrValues = new ArrayList<>();
							currMin = deltaPair.min();
							currMax = deltaPair.max();
						}
					} intersections.putIfAbsent(new Pair(currMin, currMax), currSatisfyingAttrValues);

					ArrayList<Entry<Pair, ArrayList<TreeSet<Pair>>>> sortedIntersections = new ArrayList<>(intersections.entrySet());
					sortedIntersections.sort(new Comparator<Entry<Pair, ArrayList<TreeSet<Pair>>>>() {
						@Override
						public int compare(Entry<Pair, ArrayList<TreeSet<Pair>>> o1, Entry<Pair, ArrayList<TreeSet<Pair>>> o2) {
							return o2.getValue().size() - o1.getValue().size();
						}
					});

					System.out.println("Candidate deltas for general TGFD:");
					for (Entry<Pair, ArrayList<TreeSet<Pair>>> intersection : sortedIntersections) {
						System.out.println(intersection.getKey());
					}

					for (Entry<Pair, ArrayList<TreeSet<Pair>>> intersection : sortedIntersections) {
						int generalMin = intersection.getKey().min();
						int generalMax = intersection.getKey().max();
						System.out.println("General min: " + generalMin);
						System.out.println("General max: " + generalMax);

//							// Compute general support
						float numerator = 0;
						float denominator = 2 * entities.size() * graphs.size();

						int numberOfSatisfyingPairs = 0;
						for (TreeSet<Pair> timestamps : intersection.getValue()) {
							TreeSet<Pair> satisfyingPairs = new TreeSet<Pair>();
							for (Pair timestamp : timestamps) {
								if (timestamp.max() - timestamp.min() >= generalMin && timestamp.max() - timestamp.min() <= generalMax) {
									satisfyingPairs.add(new Pair(timestamp.min(), timestamp.max()));
								}
							}
							numberOfSatisfyingPairs += satisfyingPairs.size();
						}

						System.out.println("Number of satisfying pairs: " + numberOfSatisfyingPairs);

						numerator = numberOfSatisfyingPairs;
						System.out.println("Support = " + numerator + "/" + denominator);

						float support = numerator / denominator;
						if (support < theta) {
							continue;
						}
						Delta delta = new Delta(Period.ofDays(generalMin * 183), Period.ofDays(generalMax * 183 + 1), Duration.ofDays(183));
						VF2PatternGraph newPattern = node.getPattern().copy();
						for (Vertex v : newPattern.getPattern().vertexSet()) {
							String vType = new ArrayList<>(v.getTypes()).get(0);
							if (xAttrNameMap.containsKey(vType)) {
								for (String attrName : xAttrNameMap.get(vType)) {
									v.addAttribute(new Attribute(attrName));
								}
							}
							if (v.getTypes().contains(yVertexType)) {
								v.addAttribute(new Attribute(yAttrName));
							}
						}
						Dependency generalDependency = new Dependency();
						VariableLiteral y = new VariableLiteral(yVertexType, yAttrName, yVertexType, yAttrName);
						generalDependency.addLiteralToY(y);
						for (String xVertexType : xAttrValueMap.keySet()) {
							for (String xAttrName : xAttrNameMap.get(xVertexType)) {
								VariableLiteral x = new VariableLiteral(xVertexType, xAttrName, xVertexType, xAttrName);
								generalDependency.addLiteralToX(x);
							}
						}
						TGFD tgfd = new TGFD(newPattern, delta, generalDependency, "");
						System.out.println("TGFD: " + tgfd);
						tgfds.add(tgfd);
						numOfTGFDsAddedForThisNode++;
						minimalSetOfTGFDs.add(new PatternDependencyPair(patternCopy, dependency));
						System.out.println("Discovered minimal TGFD");
						System.out.println("Do not expand this dependency");
						prunedSetOfDependencies.add(dependency);
						break;
					}
				}
			}
			if (numOfTGFDsAddedForThisNode == 0) {
				System.out.println("Couldn't find any TGFDS for node : " + node.getPattern());
				System.out.println("Marking as pruned node.");
				node.setIsPruned();
				continue;
			}
			node.setMinimalSetOfDependencies(minimalSetOfTGFDs);
			node.setPrunedSetOfDependencies(prunedSetOfTGFDs);
		}
//		}

		System.out.println("Number of TGFDs generated for k = " + i + ": " + tgfds.size());
		for (TGFD tgfd : tgfds) {
			System.out.println(tgfd);
		}
		return tgfds;
	}

	public static boolean isSubsetOfTGFD(VF2PatternGraph pattern, Dependency dependency, ArrayList<PatternDependencyPair> setOfDependenciesFromParent, ArrayList<PatternDependencyPair> setOfDependenciesForCurrentNode) {
		for (PatternDependencyPair minimalPatternDependency : setOfDependenciesFromParent) {
			if (dependency.getY().containsAll(minimalPatternDependency.dependency().getY())) {
				if (dependency.getX().containsAll(minimalPatternDependency.dependency().getX())) {
					System.out.println("Candidate Pattern " + pattern);
					System.out.println("is a subset of minimal TGFD with ");
					System.out.println("Pattern " + minimalPatternDependency.pattern());
					System.out.println("and");
					System.out.println("Candidate Dependency " + dependency);
					System.out.println("is a subset of minimal TGFD with ");
					System.out.println("Dependency " + minimalPatternDependency.dependency());
					setOfDependenciesForCurrentNode.add(minimalPatternDependency);
					return true;
				}
			}
		}
		return false;
	}

	public static ArrayList<Entry<String, ArrayList<Integer>>> findMatchesY(ArrayList<DBPediaLoader> graphs, VF2PatternGraph pattern, String yAttrName, String yVertexType, double theta) {
		TreeMap<String,ArrayList<Integer>> yValuesTimeStamps = new TreeMap<>();
		int t = 2015;
		for (DBPediaLoader graph : graphs) {
			ArrayList<String> yAttrValues = new ArrayList<>();
			Iterator<GraphMapping<Vertex, RelationshipEdge>> results = new VF2SubgraphIsomorphism().execute(graph.getGraph(), pattern, false, true);

			System.out.println("---------- Attribute values in " + t + " ---------- ");
			if (results == null) {
				System.out.println("No matches");
				continue;
			}
			int size = 0;
			while (results.hasNext()) {
				GraphMapping<Vertex, RelationshipEdge> mappings = results.next();
				for (Vertex patternVertex : pattern.getPattern().vertexSet()) {
					Vertex currentMatchedVertex = mappings.getVertexCorrespondence(patternVertex, false);
					String patternVertexType = new ArrayList<>(patternVertex.getTypes()).get(0);
					if (currentMatchedVertex != null && patternVertexType.equalsIgnoreCase(yVertexType)) {
						for (String patternAttrName : patternVertex.getAllAttributesNames()) {
							if (patternAttrName.equalsIgnoreCase(yAttrName)) {
								String matchedYattrValue = currentMatchedVertex.getAttributeValueByName(patternAttrName);
								yAttrValues.add(matchedYattrValue);
								yValuesTimeStamps.putIfAbsent(matchedYattrValue, new ArrayList<>());
							}
						}
						size++;
					}
				}
			}
			System.out.println("Number of matches: " + size);
//			} else {
//				System.out.println("No matches");
//				continue;
//			}
			for (String value : yAttrValues) {
				yValuesTimeStamps.get(value).add(t);
			}
			t++;
		}

		for (String attrVal : yValuesTimeStamps.keySet()) {
			System.out.println(attrVal+":"+yValuesTimeStamps.get(attrVal));
		}

		ArrayList<Map.Entry<String,ArrayList<Integer>>> majortyAttrValues = new ArrayList<>(yValuesTimeStamps.entrySet());
		majortyAttrValues.sort(new Comparator<Entry<String, ArrayList<Integer>>>() {
			@Override
			public int compare(Entry<String, ArrayList<Integer>> o1, Entry<String, ArrayList<Integer>> o2) {
				return o2.getValue().size() - o1.getValue().size();
			}
		});

		for (Map.Entry<String,ArrayList<Integer>> entry : majortyAttrValues) {
			System.out.println(entry.getKey()+":"+entry.getValue());
		}

		return majortyAttrValues;

//		Delta delta = null;
//		double support = 0.0;
//		for (Map.Entry<String,ArrayList<Integer>> attrValueEntry : majortyAttrValues){
//			int minDistance = graphs.size() - 1;
//			int maxDistance = 0;
//
//			ArrayList<Integer> timestamps = attrValueEntry.getValue();
//			maxDistance = timestamps.get(timestamps.size() - 1) - timestamps.get(0);
//			for (int i = 1; i < timestamps.size(); i++) {
//				if ((timestamps.get(1) - timestamps.get(0)) < minDistance) {
//					minDistance = (timestamps.get(1) - timestamps.get(0));
//				}
//			}
//			// Compute support
//			float numer = 0;
//			float denom = 2 * 1 * graphs.size();
//			Set<Pair> satisfyingPairs = new TreeSet<Pair>();
//			for (int i = 0; i < timestamps.size() - 1; i++) {
//				for (int j = i+1; j < timestamps.size(); j++) {
//					satisfyingPairs.add(new Pair(timestamps.get(i), timestamps.get(j)));
//				}
//			}
//			System.out.println("Satisfying pairs: " + satisfyingPairs);
//
//			numer = satisfyingPairs.size();
//			System.out.println("Support = " + numer + "/" + denom);
//			double candidateSupport = numer / denom;
//
//			if (candidateSupport > support) {
//				support = candidateSupport;
//				delta = new Delta(Period.ofDays(minDistance * 183), Period.ofDays(maxDistance * 183 + 1), Duration.ofDays(183));
//			}
//			if (support >= theta) {
//				totalNumberOfSatisfyingPairs += satisfyingPairs.size();
//				break;
//			}
//		}
//
//		System.out.println(delta);
//		System.out.println("Support = " + support);

//		return delta;

	}

	public static class PatternDependencyPair {
		private VF2PatternGraph pattern;
		private Dependency dependency;
		public PatternDependencyPair(VF2PatternGraph pattern, Dependency dependency) {
			this.pattern = pattern;
			this.dependency = dependency;
		}
		public VF2PatternGraph pattern(){
			return this.pattern;
		}
		public Dependency dependency(){
			return this.dependency;
		}
	}

	public static class Pair implements Comparable<Pair> {
		private Integer min;
		private Integer max;
		public Pair(int min, int max) {
			this.min = min;
			this.max = max;
		}

		public Integer min() {
			return min;
		}

		public Integer max() {
			return max;
		}

		@Override
		public int compareTo(@NotNull tgfdDiscovery.Pair o) {
			if (this.min.equals(o.min)) {
				return this.max.compareTo(o.max);
			} else {
				return this.min.compareTo(o.min);
			}
		}

		@Override
		public String toString() {
			return "("+ min +
					", " + max +
					')';
		}
	}

//	public static class Match {
//		Integer time;
//		String attrValue;
//		public Match(int time, String attrValue) {
//			this.time = time;
//			this.attrValue = attrValue;
//		}
//
//		@Override
//		public String toString() {
//			return "(" +
//					"t_" + time +
//					", " + attrValue + '\'' +
//					')';
//		}
//	}

	public static ArrayList<TGFD> generateDummyTGFDsForOptimizedLoading() {

		List<Map.Entry<String,Integer>> sortedVertexTypeHistogram = getSortedNodeHistogram();
		List<Map.Entry<String,Integer>> sortedUniqueEdgesHistogram = getSortedEdgeHistogram();

		ArrayList<TGFD> dummyTGFDs = new ArrayList<>();
		if (Config.optimizedLoadingBasedOnTGFD) {
			System.out.println("Generating dummy TGFDs for optimized loading");

			for (int i = 0; i < NUM_OF_SINGLE_VERTEX_PATTERNS; i++) {
				String vertexLabel = sortedVertexTypeHistogram.get(i).getKey();
				VF2PatternGraph singleNodePattern = new VF2PatternGraph();
				PatternVertex vertex = new PatternVertex(vertexLabel);
				singleNodePattern.addVertex(vertex);

				if (MAX_NUMBER_OF_ATTR_PER_VERTEX > 1) {
					ArrayList<TreeSet<Dependency>> dependenciesSets = hSpawn(singleNodePattern, 0);
					for (TreeSet<Dependency> dependencySet : dependenciesSets) {
						for (Dependency dependency : dependencySet) {
							TGFD dummyTGFD = new TGFD();
							dummyTGFD.setPattern(singleNodePattern);
							dummyTGFD.setDependency(dependency);
							dummyTGFD.setDelta(new Delta(Period.ofDays(0), Period.ofDays(Integer.MAX_VALUE), Duration.ofDays(1)));
							dummyTGFDs.add(dummyTGFD);
						}
					}
				} else {
					TGFD dummyTGFD = new TGFD();
					dummyTGFD.setPattern(singleNodePattern);
					dummyTGFD.setDelta(new Delta(Period.ofDays(0), Period.ofDays(Integer.MAX_VALUE), Duration.ofDays(1)));
					dummyTGFDs.add(dummyTGFD);
				}
				for (Map.Entry<String, Integer> candidateEdge : sortedUniqueEdgesHistogram.subList(0, Math.min(uniqueEdgesHist.size(), NUM_OF_FREQ_EDGES_TO_CONSIDER))) {
					System.out.println("Candidate edge:" + candidateEdge.getKey());
					String type1 = candidateEdge.getKey().split(" ")[0];
					String type2 = candidateEdge.getKey().split(" ")[2];
					String edgeType = candidateEdge.getKey().split(" ")[1];

					for (Vertex v : singleNodePattern.getPattern().vertexSet()) {
						System.out.println("Looking for type: " + v.getTypes());
						if (v.getTypes().contains(type1) || v.getTypes().contains(type2)) { // TO-DO: FIX label conflict. What if an edge has same vertex type on both sides?
							// Create copy of k-1 pattern
							VF2PatternGraph newPattern = singleNodePattern.copy();
//							if (!isDuplicateEdge(newPattern, edgeType, type1, type2)) {
							if (v.getTypes().contains(type1)) {
								PatternVertex node2 = new PatternVertex(type2);
								newPattern.addVertex(node2);
								RelationshipEdge newEdge = new RelationshipEdge(edgeType);
//									System.out.println(newEdge);
//									PatternVertex node1 = ((PatternVertex) v).copy();
//									System.out.println(newPattern);
								PatternVertex node1 = null;
								for (Vertex nv : newPattern.getPattern().vertexSet()) {
									if (nv.getTypes().contains(type1)) {
										node1 = (PatternVertex) nv;
									}
								}
								newPattern.addEdge(node1, node2, newEdge);
							} else if (v.getTypes().contains(type2)) {
								PatternVertex node1 = new PatternVertex(type1);
								newPattern.addVertex(node1);
								RelationshipEdge newEdge = new RelationshipEdge(edgeType);
//									System.out.println(newEdge);
//									PatternVertex node2 = ((PatternVertex) v).copy();
//									System.out.println(newPattern);
								PatternVertex node2 = null;
								for (Vertex nv : newPattern.getPattern().vertexSet()) {
									if (nv.getTypes().contains(type2)) {
										node2 = (PatternVertex) nv;
									}
								}
								newPattern.addEdge(node1, node2, newEdge);
							}
//							} else {
//								System.out.println("Candidate edge already exists: " + candidateEdge.getKey());
//								continue;
//							}
							System.out.println("Created new pattern: " + newPattern);
							ArrayList<TreeSet<Dependency>> dependenciesSets = hSpawn(newPattern, 0);
							for (TreeSet<Dependency> dependencySet : dependenciesSets) {
								for (Dependency dependency : dependencySet) {
									TGFD dummyTGFD = new TGFD();
									dummyTGFD.setPattern(newPattern);
									dummyTGFD.setDependency(dependency);
									dummyTGFD.setDelta(new Delta(Period.ofDays(0), Period.ofDays(Integer.MAX_VALUE), Duration.ofDays(1)));
									dummyTGFDs.add(dummyTGFD);
								}
							}
						}
					}
				}
			}

//			for (Map.Entry<String, Integer> candidateEdge : edgesHist.subList(0, Math.min(edgesHist.size(), NUM_OF_FREQ_EDGES_TO_CONSIDER))) {
//				String[] edgeString = candidateEdge.getKey().split(" ");
//				String type1 = edgeString[0];
//				String type2 = edgeString[2];
//				String edgeType = edgeString[1];
//				VF2PatternGraph singleEdgePattern = new VF2PatternGraph();
//				PatternVertex vertex1 = new PatternVertex(type1);
//				if (MAX_NUMBER_OF_ATTR_PER_VERTEX == 1) vertex1.addAttribute(new Attribute(vertexTypesAttributes.get(type1).get(0).getKey()));
//				PatternVertex vertex2 = new PatternVertex(type2);
//				if (MAX_NUMBER_OF_ATTR_PER_VERTEX == 1) vertex2.addAttribute(new Attribute(vertexTypesAttributes.get(type2).get(0).getKey()));
//				singleEdgePattern.addVertex(vertex1);
//				singleEdgePattern.addVertex(vertex2);
//				RelationshipEdge edge = new RelationshipEdge(edgeType);
//				singleEdgePattern.addEdge(vertex1, vertex2, edge);
//				ArrayList<TreeSet<Dependency>> dependenciesSets = hSpawn(singleEdgePattern, 0);
//				for (TreeSet<Dependency> dependencySet : dependenciesSets) {
//					for (Dependency dependency : dependencySet) {
//						TGFD dummyTGFD = new TGFD();
//						dummyTGFD.setPattern(singleEdgePattern);
//						dummyTGFD.setDependency(dependency);
//						dummyTGFD.setDelta(new Delta(Period.ofDays(0), Period.ofDays(Integer.MAX_VALUE), Duration.ofDays(1)));
//						dummyTGFDs.add(dummyTGFD);
//					}
//				}
//			}
		}
		return dummyTGFDs;
	}

	public static void discover(int k, double theta, String experimentName) {
		// TO-DO: Is it possible to prune the graphs by passing them our histogram's frequent nodes and edges as dummy TGFDs ???
		Config.optimizedLoadingBasedOnTGFD = !tgfdDiscovery.isNaive;
		tgfdDiscovery.lowPatternSupportEdges = new HashSet<>();

		PrintStream kExperimentResultsFile = null;
        if (experimentName.startsWith("k")) {
            try {
                String timeAndDateStamp = ZonedDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("uuuu.MM.dd.HH.mm.ss"));
                kExperimentResultsFile = new PrintStream("k-experiments-runtimes-" + (isNaive ? "naive" : "optimized") + "-" + timeAndDateStamp + ".txt");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
		final long kStartTime = System.currentTimeMillis();

		for (int i = 0; i <= k; i++) {
			vSpawn(i);
			if (MAX_NUMBER_OF_ATTR_PER_VERTEX == 1 && i < 1) continue; // ignore single-node patterns because they're expensive and uninteresting
			ArrayList<TGFD> tgfds = deltaDiscovery(i, theta);

			final long kEndTime = System.currentTimeMillis();
			final long kRunTime = kEndTime - kStartTime;
			System.out.println("Total execution time for k = " + k + " : " + kRunTime);
			if (experimentName.startsWith("k") && kExperimentResultsFile != null) {
				kExperimentResultsFile.print("k = " + i);
				kExperimentResultsFile.println(", execution time = " + kRunTime);
			}
			printTGFDstoFile(experimentName, i, theta, tgfds);

			System.gc();
		}
	}

	public static void vSpawnInit() {
		genTree = new GenerationTree();
		genTree.addLevel();

		List<Entry<String, Integer>> sortedHistogram = getSortedNodeHistogram();

		System.out.println("VSpawn Level 0");
//		for (int i = 0; i < NUM_OF_SINGLE_VERTEX_PATTERNS; i++) {
		for (int i = 0; i < sortedHistogram.size(); i++) {
			String vertexLabel = sortedHistogram.get(i).getKey();

			int numOfInstancesOfVertexType = sortedHistogram.get(i).getValue();
			double numerator = 2 * numOfInstancesOfVertexType * NUM_OF_SNAPSHOTS;
			int numOfInstancesOfAllVertexTypes = tgfdDiscovery.NUM_OF_VERTICES_IN_GRAPH;
			double denominator = 2 * numOfInstancesOfAllVertexTypes * NUM_OF_SNAPSHOTS;

			double patternSupport = numerator / denominator;

			System.out.println(vertexLabel);
			VF2PatternGraph candidatePattern = new VF2PatternGraph();
			System.out.println(candidatePattern);
			PatternVertex vertex = new PatternVertex(vertexLabel);
			candidatePattern.addVertex(vertex);
			System.out.println("Pattern: " + candidatePattern);

			if (tgfdDiscovery.isNaive) {
				if (patternSupport >= PATTERN_SUPPORT_THRESHOLD) {
					System.out.println("Pattern Support: " + patternSupport);
					System.out.println("Performing HSpawn");
					ArrayList<TreeSet<Dependency>> dependenciesSets = hSpawn(candidatePattern, patternSupport);
					genTree.createNodeAtLevel(0, candidatePattern, dependenciesSets, patternSupport);
	//				genTree.createNodeAtLevel(0, candidatePattern, new ArrayList<>(), patternSupport);
				}
				else {
	//				genTree.createNodeAtLevel(0, candidatePattern, new ArrayList<>(), patternSupport, true);
					System.out.println("Pattern support of "+patternSupport+" is below threshold.");
	//				if (!tgfdDiscovery.isNaive) {
	//					System.out.println("Reached last frequent vertexType that satisfies threshold. ");
	//					System.out.println("Skip remaining vertexTypes.");
	//					break;
	//				}
				}
			} else {
				System.out.println("Pattern Support: " + patternSupport);
				System.out.println("Performing HSpawn");
				ArrayList<TreeSet<Dependency>> dependenciesSets = hSpawn(candidatePattern, patternSupport);
				genTree.createNodeAtLevel(0, candidatePattern, dependenciesSets, patternSupport);
			}
			System.gc();
		}
		System.out.println("GenTree Level " + 0 + " size: " + genTree.getLevel(0).size());
		for (GenTreeNode node : genTree.getLevel(0)) {
			System.out.println("Pattern: " + node.getPattern());
			System.out.println("Pattern Support: " + node.getPatternSupport());
			System.out.println("Dependency: " + node.getDependenciesSets());
		}
	}

	public static boolean isDuplicateEdge(VF2PatternGraph pattern, String edgeType, String type1, String type2) {
		for (RelationshipEdge edge : pattern.getPattern().edgeSet()) {
			if (edge.getLabel().equalsIgnoreCase(edgeType)) {
				if (edge.getSource().getTypes().contains(type1) && edge.getTarget().getTypes().contains(type2)) {
					return true;
				}
			}
		}
		return false;
	}

//	public static void vSpawn(int k, ArrayList<DBPediaLoader> graphs) {
	public static void vSpawn(int i) {

		if (i == 0) {
			vSpawnInit();
			return;
		}

		List<Entry<String, Integer>> sortedUniqueEdgesHist = getSortedEdgeHistogram();

		System.out.println("Performing VSpawn");
//		for (int i = 1; i <= k; i++) {
		System.out.println("VSpawn Level " + i);
		System.gc();
		ArrayList<GenTreeNode> previousLevel =  genTree.getLevel(i-1);
		genTree.addLevel();
		for (GenTreeNode node : previousLevel) {
			System.out.println("Level "+ (i-1) +" pattern: " + node.getPattern());
			if (node.isPruned() && !tgfdDiscovery.isNaive) {
				System.out.println("Marked as pruned. Skip.");
				continue;
			}
//			int newPatternsFormed = 0;
//			if (tgfdDiscovery.isNaive) {
//				NUM_OF_FREQ_EDGES_TO_CONSIDER = edgesHist.size();
//			}
//			for (Map.Entry<String, Integer> candidateEdge : edgesHist.subList(0, Math.min(edgesHist.size(), NUM_OF_FREQ_EDGES_TO_CONSIDER))) {
			for (Map.Entry<String, Integer> candidateEdge : sortedUniqueEdgesHist) {
				String candidateEdgeString = candidateEdge.getKey();
				System.out.println("Candidate edge:" + candidateEdgeString);
				if (tgfdDiscovery.isNaive && (1.0*uniqueEdgesHist.get(candidateEdgeString)/NUM_OF_EDGES_IN_GRAPH) < PATTERN_SUPPORT_THRESHOLD) {
					System.out.println("Below pattern support threshold. Skip");
					continue;
//					if (!tgfdDiscovery.isNaive) {
//						System.out.println("Reach last frequent edge in sorted histogram.");
//						System.out.println("Skipping remaining edges in histogram.");
//						break;
//					}
				}
				String sourceVertexType = candidateEdgeString.split(" ")[0];
				String targetVertexType = candidateEdgeString.split(" ")[2];
				String edgeType = candidateEdgeString.split(" ")[1];

				// TO-DO: FIX label conflict. What if an edge has same vertex type on both sides?
				for (Vertex v : node.getGraph().vertexSet()) {
					System.out.println("Looking for type: " + v.getTypes());
					if (!v.getTypes().contains(sourceVertexType)) {
						continue;
					}
//					if (v.getTypes().contains(sourceVertexType) || v.getTypes().contains(targetVertexType)) {
						// Create copy of k-1 pattern
						VF2PatternGraph newPattern = node.getPattern().copy();
						if (!isDuplicateEdge(newPattern, edgeType, sourceVertexType, targetVertexType)) {
//							if (v.getTypes().contains(sourceVertexType)) {
								PatternVertex node2 = new PatternVertex(targetVertexType);
								newPattern.addVertex(node2);
								RelationshipEdge newEdge = new RelationshipEdge(edgeType);
								PatternVertex node1 = null;
								for (Vertex vertex : newPattern.getPattern().vertexSet()) {
									if (vertex.getTypes().contains(sourceVertexType)) {
										node1 = (PatternVertex) vertex;
									}
								}
								newPattern.addEdge(node1, node2, newEdge);
//							}
//							else if (v.getTypes().contains(targetVertexType)) {
//								PatternVertex node1 = new PatternVertex(sourceVertexType);
//								newPattern.addVertex(node1);
//								RelationshipEdge newEdge = new RelationshipEdge(edgeType);
//								PatternVertex node2 = null;
//								for (Vertex vertex : newPattern.getPattern().vertexSet()) {
//									if (vertex.getTypes().contains(targetVertexType)) {
//										node2 = (PatternVertex) vertex;
//									}
//								}
//								newPattern.addEdge(node1, node2, newEdge);
//							}
						} else {
							System.out.println("Candidate edge: " + candidateEdge.getKey());
							System.out.println("already exists in pattern");
							continue;
						}
						System.out.println("Created new pattern: " + newPattern);

//						boolean isIsomorph = false;
//						for (GenTreeNode otherNode: genTree.getLevel(i)) {
//							Comparator<RelationshipEdge> myEdgeComparator = (o1, o2) -> {
//								if (o1.getLabel().equals("*") || o2.getLabel().equals("*"))
//									return 0;
//								else if (o1.getLabel().equals(o2.getLabel()))
//									return 0;
//								else
//									return 1;
//							};
//
//							Comparator<Vertex> myVertexComparator = (v1, v2) -> {
//								if (v1.isMapped(v2))
//									return 0;
//								else
//									return 1;
//							};
//							VF2AbstractIsomorphismInspector<Vertex, RelationshipEdge> inspector = new VF2SubgraphIsomorphismInspector<Vertex, RelationshipEdge>(
//									otherNode.getGraph(), newPattern.getPattern(),
//									myVertexComparator, myEdgeComparator, false);
//							if (inspector.isomorphismExists()) { // TO-DO: Verify... Is this ever true?
//								isIsomorph = true;
//							}
//						}
//						if (isIsomorph) {
//							System.out.println("New pattern is an isomorph of an existing pattern. Skip.");
//							continue;
//						}
						double numerator = Double.MAX_VALUE;
						double denominator = NUM_OF_EDGES_IN_GRAPH;
//						for (Vertex tempV: newPattern.getPattern().vertexSet()) {
//							String vertexType = (new ArrayList<>(tempV.getTypes())).get(0));
//							numerator = Math.min(numerator, uniqueVertexTypesHist.get(vertexType);
//
//						}
						for (RelationshipEdge tempE: newPattern.getPattern().edgeSet()) {
							String sourceType = (new ArrayList<>(tempE.getSource().getTypes())).get(0);
							String targetType = (new ArrayList<>(tempE.getTarget().getTypes())).get(0);
							String uniqueEdge = sourceType+" "+tempE.getLabel()+" "+targetType;
							numerator = Math.min(numerator, uniqueEdgesHist.get(uniqueEdge));
						}
						assert numerator <= denominator;
						double patternSupport = numerator / denominator;
//						if (i == 1) {
//							for (DBPediaLoader graph : graphs) {
//								for (RelationshipEdge e : graph.getGraph().getGraph().edgeSet()) {
//									if (e.getLabel().equalsIgnoreCase(edgeType)) {
//										// match!
//										patternSupport += 1;
//									}
//								}
//							}
//						}
////							else {
//                                ConfigParser.printDetailedMatchingResults=false;
////								System.gc();
//								for (DBPediaLoader graph : graphs) {
//									VF2SubgraphIsomorphism VF2 = new VF2SubgraphIsomorphism();
//									Iterator<GraphMapping<Vertex, RelationshipEdge>> results = VF2.execute(graph.getGraph(), newPattern, theta, false);
//									if (results != null) {
//										patternSupport += Iterators.size(results);
//									}
//								}
//								System.gc();
//							}
						System.out.println("Pattern Support: " + patternSupport);
//						if (patternSupport >= PATTERN_SUPPORT_THRESHOLD) {
							System.out.println("Performing HSpawn");
							ArrayList<TreeSet<Dependency>> literalTreeList = hSpawn(newPattern, patternSupport);
							System.gc();
							genTree.createNodeAtLevel(i, newPattern, literalTreeList, patternSupport, node);
//							newPatternsFormed++;
//						} else {
//							System.out.println("New pattern does not satisfy support threshold.");
//						}
//						if (newPatternsFormed == NUM_OF_CHILD_PATTERNS_LIMIT) {
//							break;
//						}
//					}
				}
//				if (newPatternsFormed == NUM_OF_CHILD_PATTERNS_LIMIT) {
//					break;
//				}
			}
		}
		System.out.println("GenTree Level " + i + " size: " + genTree.getLevel(i).size());
		for (GenTreeNode node : genTree.getLevel(i)) {
			System.out.println("Pattern: " + node.getPattern());
			System.out.println("Pattern Support: " + node.getPatternSupport());
		}
//		}
	}

	public static ArrayList<TreeSet<Dependency>> hSpawn(VF2PatternGraph patternGraph, double patternSupport) {
		HashMap<String, HashSet<String>> patternVerticesAttributes = new HashMap<>();
		for (Vertex vertex : patternGraph.getPattern().vertexSet()) {
//			int numOfAttrInVertex = 0;
			for (String vertexType : vertex.getTypes()) {
				patternVerticesAttributes.put(vertexType, new HashSet<>());
				for (Map.Entry<String,Integer> attrNameRecord : vertexTypesAttributes.get(vertexType)) {
//					if (attrHist.get(attr) >= patternSupport) { // TO-DO: Is this requirement too strict?
						patternVerticesAttributes.get(vertexType).add(attrNameRecord.getKey());
//						numOfAttrInVertex++;
//					}
//					if (numOfAttrInVertex >= MAX_NUMBER_OF_ATTR_PER_VERTEX) { break; }
					// TO-DO change vertexTypesAttributes to have a sorted list of attributes instead of Hashset
				}
			}
		}

		HashSet<ConstantLiteral> attrNamesSet = new HashSet<>();
		for (String vertexType : patternVerticesAttributes.keySet()) {
			for (String attrName : patternVerticesAttributes.get(vertexType)) {
//				attrNamesSet.add(vertexType+"."+attrName);
				ConstantLiteral literal = new ConstantLiteral(vertexType, attrName, null);
				attrNamesSet.add(literal);
			}
		}

		HashMap<ConstantLiteral, Set<Set<ConstantLiteral>>> dependenciesMap = new HashMap<>();
		for (ConstantLiteral l : attrNamesSet) {
			HashSet<ConstantLiteral> exclusionSet = (new HashSet<ConstantLiteral>(attrNamesSet));
			exclusionSet.remove(l);
			Set<Set<ConstantLiteral>> powerSet = Sets.powerSet(exclusionSet);
			dependenciesMap.putIfAbsent(l, powerSet);
		}

		Comparator<Dependency> dependencyComparator = new Comparator<Dependency>() {
			@Override
			public int compare(Dependency o1, Dependency o2) {
				if (o1.getX().size() == o2.getX().size()) {
					HashSet<String> o1Set = new HashSet<>();
					for (Literal l : o1.getX()) {
						String vType = ((ConstantLiteral)l).getVertexType();
						String attrName = ((ConstantLiteral)l).getAttrName();
						o1Set.add(vType + "." + attrName);
					}
					HashSet<String> o2Set = new HashSet<>();
					for (Literal l : o2.getX()) {
						String vType = ((ConstantLiteral)l).getVertexType();
						String attrName = ((ConstantLiteral)l).getAttrName();
						o2Set.add(vType + "." + attrName);
					}
					if (o1Set.equals(o2Set)) {
						return 0;
					} else {
						return -1;
					}
				} else {
					return o1.getX().size() - o2.getX().size();
				}
			}
		};

//		ArrayList<TreeSet<Dependency>> dependenciesSets = new ArrayList<>();
//		for (ConstantLiteral yLiteral : dependenciesMap.keySet()) {
//
//			Set<Set<ConstantLiteral>> powerSet = dependenciesMap.get(yLiteral);
//			ArrayList<ConstantLiteral> singleXLiterals = new ArrayList<>();
//			for (Set<ConstantLiteral> set : powerSet) {
//				if (set.size() == 1) {
//					singleXLiterals.addAll(set);
//				}
//			}
//			for (ConstantLiteral singleXLiteral : singleXLiterals) {
//				TreeSet<Dependency> dependencySet = new TreeSet<>(dependencyComparator);
//				for (Set<ConstantLiteral> set : powerSet) {
//					if (set.contains(singleXLiteral)) {
//						Dependency dependency = new Dependency();
//						dependency.addLiteralToY(yLiteral);
//						for (ConstantLiteral xLiteral : set) {
//							dependency.addLiteralToX(xLiteral);
//						}
//						dependencySet.add(dependency);
//					}
//				}
//				dependenciesSets.add(dependencySet);
//			}
//		}

		ArrayList<TreeSet<Dependency>> dependenciesSets = new ArrayList<>();
		for (ConstantLiteral yLiteral : dependenciesMap.keySet()) {
			TreeSet<Dependency> dependencySet = new TreeSet<>(dependencyComparator);
//			System.out.print("Y="+yLiteral);
			Set<Set<ConstantLiteral>> powerSet = dependenciesMap.get(yLiteral);
			for (Set<ConstantLiteral> set : powerSet) {
				if (set.size() > 0) { // TO-DO: Are we interested in emptySet -> Y dependensies???
					Dependency dependency = new Dependency();
					dependency.addLiteralToY(yLiteral);
					for (ConstantLiteral xLiteral : set) {
						dependency.addLiteralToX(xLiteral);
					}
//					System.out.println("X="+set);
					dependencySet.add(dependency);
				}
			}
			dependenciesSets.add(dependencySet);
		}

		int numberOfDependencies = 0;
		for (TreeSet<Dependency> set : dependenciesSets) {
			numberOfDependencies += set.size();
			for (Dependency dependency: set) {
				System.out.println(dependency);
			}
		}
		System.out.println("Number of dependencies generated: " + numberOfDependencies);

		return dependenciesSets;

//		System.out.println("Performing HSpawn of " + patternGraph);
//		ArrayList<LiteralTreeNode> literalTreeList = new ArrayList<>();
//		for (Map.Entry<String, HashSet<String>> vertexAttributes : patternVerticesAttributes.entrySet()) {
//			for (String attributeType : vertexAttributes.getValue()) {
//				LiteralTreeNode literalTree = new LiteralTreeNode(null, vertexAttributes.getKey(), new Attribute(attributeType), patternVerticesAttributes);
//				literalTreeList.add(literalTree);
////				System.out.println(literalTree);
//			}
//		}
//
//		return literalTreeList;
	}

//	public static List<List<LiteralTreeNode>> findPaths(LiteralTreeNode root) {
//		List<List<LiteralTreeNode>> retLists = new ArrayList<>();
//
//		if(root.getChildren().size() == 0) {
//			List<LiteralTreeNode> leafList = new LinkedList<>();
//			leafList.add(root);
//			retLists.add(leafList);
//		} else {
//			for (LiteralTreeNode node : root.getChildren()) {
//				List<List<LiteralTreeNode>> nodeLists = findPaths(node);
//				for (List<LiteralTreeNode> nodeList : nodeLists) {
//					nodeList.add(0, root);
//					retLists.add(nodeList);
//				}
//			}
//		}
//
//		return retLists;
//	}
}

class GenerationTree {
	private ArrayList<ArrayList<GenTreeNode>> tree;

	public GenerationTree() {
		tree = new ArrayList<>();
	}

	public void addLevel() {
		tree.add(new ArrayList<>());
		System.out.println("Tree size: " + tree.size());
	}

	public void createNodeAtLevel(int level, VF2PatternGraph pattern, ArrayList<TreeSet<Dependency>> dependenciesSets, double support) {
		GenTreeNode node = new GenTreeNode(pattern, dependenciesSets, support);
		tree.get(level).add(node);
	}

	public ArrayList<GenTreeNode> getLevel(int i) {
		return tree.get(i);
	}

	public void createNodeAtLevel(int level, VF2PatternGraph pattern, ArrayList<TreeSet<Dependency>> dependenciesSets, double patternSupport, GenTreeNode parentNode) {
		GenTreeNode node = new GenTreeNode(pattern, dependenciesSets, patternSupport, parentNode);
		tree.get(level).add(node);
	}

	public void createNodeAtLevel(int level, VF2PatternGraph pattern, ArrayList<TreeSet<Dependency>> dependenciesSets, double patternSupport, boolean isPruned) {
		GenTreeNode node = new GenTreeNode(pattern, dependenciesSets, patternSupport, isPruned);
		tree.get(level).add(node);
	}
}

class GenTreeNode {
	private VF2PatternGraph pattern;
	private ArrayList<TreeSet<Dependency>> dependenciesSets;
	private double patternSupport;
	private GenTreeNode parentNode;
	private boolean isPruned = false;
	private ArrayList<tgfdDiscovery.PatternDependencyPair> minimalSetOfDependencies = new ArrayList<>();
	private ArrayList<tgfdDiscovery.PatternDependencyPair> prunedSetOfTGFDs = new ArrayList<>();

	public GenTreeNode(VF2PatternGraph pattern, ArrayList<TreeSet<Dependency>> dependenciesSets, double patternSupport) {
		this.pattern = pattern;
		this.dependenciesSets = dependenciesSets;
		this.patternSupport = patternSupport;
	}

	public GenTreeNode(VF2PatternGraph pattern, ArrayList<TreeSet<Dependency>> dependenciesSets, double patternSupport, GenTreeNode parentNode) {
		this.pattern = pattern;
		this.dependenciesSets = dependenciesSets;
		this.patternSupport = patternSupport;
		this.parentNode = parentNode;
	}

	public GenTreeNode(VF2PatternGraph pattern, ArrayList<TreeSet<Dependency>> dependenciesSets, double patternSupport, boolean isPruned) {
		this.pattern = pattern;
		this.dependenciesSets = dependenciesSets;
		this.patternSupport = patternSupport;
		this.isPruned = isPruned;
	}

	public Graph<Vertex, RelationshipEdge> getGraph() {
		return pattern.getPattern();
	}
	public VF2PatternGraph getPattern() {
		return pattern;
	}

	public double getPatternSupport() {
		return patternSupport;
	}

	public GenTreeNode parentNode() {
		return this.parentNode;
	}

	public ArrayList<TreeSet<Dependency>> getDependenciesSets() {
		return dependenciesSets;
	}

	public void setIsPruned() {
		this.isPruned = true;
	}

	public boolean isPruned() {
		return this.isPruned;
	}

//	public ArrayList<TreeSet<Dependency>> getDependencies() {
//		ArrayList<TreeSet<Dependency>> dependencies = new ArrayList<>();
//		for (LiteralTreeNode literalTree : literalTrees) {
//			dependencies.add(new TreeSet<>(new Comparator<Dependency>() {
//				@Override
//				public int compare(Dependency o1, Dependency o2) {
//					if (o1.getX().size() == o2.getX().size()) {
//						HashSet<String> o1Set = new HashSet<>();
//						for (Literal l : o1.getX()) {
//							String vType = ((ConstantLiteral)l).getVertexType();
//							String attrName = ((ConstantLiteral)l).getAttrName();
//							o1Set.add(vType + "." + attrName);
//						}
//						HashSet<String> o2Set = new HashSet<>();
//						for (Literal l : o2.getX()) {
//							String vType = ((ConstantLiteral)l).getVertexType();
//							String attrName = ((ConstantLiteral)l).getAttrName();
//							o2Set.add(vType + "." + attrName);
//						}
//						if (o1Set.equals(o2Set)) {
//							return 0;
//						} else {
//							return -1;
//						}
//					} else {
//						return o1.getX().size() - o2.getX().size();
//					}
//				}
//			}));
//			List<List<LiteralTreeNode>> paths = tgfdDiscovery.findPaths(literalTree);
//			for (List<LiteralTreeNode> path : paths) {
//				LiteralTreeNode yLiteral = path.get(0);
//				int i = 2;
//				while (i <= path.size()) {
//					Dependency dependency = new Dependency();
//					dependency.addLiteralToY(new ConstantLiteral(yLiteral.getVertexType(), yLiteral.getAttribute().getAttrName(), yLiteral.getAttribute().getAttrValue()));
//					for (LiteralTreeNode xLiteral : path.subList(1, i)) {
//						dependency.addLiteralToX(new ConstantLiteral(xLiteral.getVertexType(), xLiteral.getAttribute().getAttrName(), xLiteral.getAttribute().getAttrValue()));
//					}
//					dependencies.get(dependencies.size()-1).add(dependency);
//					i++;
//				}
//			}
//		}
//		for (TreeSet<Dependency> dependencySet : dependencies) {
//			for (Dependency dependency : dependencySet) {
//				System.out.println(dependency);
//			}
//		}
//		return dependencies;
//	}

	@Override
	public String toString() {
		return "GenTreeNode{" +
				"pattern=" + pattern +
				",\n dependenciesSets=" + dependenciesSets +
				",\n support=" + patternSupport +
				'}';
	}

	public void setMinimalSetOfDependencies(ArrayList<tgfdDiscovery.PatternDependencyPair> minimalSetOfDependencies) {
		this.minimalSetOfDependencies = minimalSetOfDependencies;
	}

	public ArrayList<tgfdDiscovery.PatternDependencyPair> getMinimalSetOfDependencies() {
		return this.minimalSetOfDependencies;
	}

	public void setPrunedSetOfDependencies(ArrayList<tgfdDiscovery.PatternDependencyPair> prunedSetOfTGFDs) {
		this.prunedSetOfTGFDs = prunedSetOfTGFDs;
	}

	public ArrayList<tgfdDiscovery.PatternDependencyPair> getPrunedSetOfDependencies() {
		return this.prunedSetOfTGFDs;
	}
}

//class LiteralTree {
//	private LiteralTreeNode root;
//	public LiteralTree(String vertexType, String attributeLabel, Map<String, HashSet<String>> patternVerticesAttributes) {
//		Map<String, HashSet<String>> copy = (Map<String, HashSet<String>>) patternVerticesAttributes.get(vertexType).clone();
//		copy.remove(attributeLabel);
//		this.root = new LiteralTreeNode(vertexType, new Attribute(attributeLabel), copy);
//	}
//
//	public LiteralTreeNode getRoot() {
//	    return this.root;
//    }
//}

class LiteralTreeNode {
	LiteralTreeNode parent;
	String vertexType;
	Attribute attribute;
	ArrayList<LiteralTreeNode> children;

	public LiteralTreeNode(LiteralTreeNode parent, String vertexType, Attribute attribute, HashMap<String, HashSet<String>> attributeTypes) {
		this.vertexType = vertexType;
		this.attribute = attribute;
		this.children = new ArrayList<>();
		HashMap<String, HashSet<String>> clone = new HashMap<>();
		for (String vt : attributeTypes.keySet()) {
			clone.put(vt, new HashSet<>());
			for (String at: attributeTypes.get(vt)) {
				clone.get(vt).add(at);
			}
		}
		clone.get(vertexType).remove(attribute.getAttrName());
		this.populateTree(clone);
	}

	public void populateTree(HashMap<String, HashSet<String>> attributeTypes) {
		for (String vertexType : attributeTypes.keySet()) {
			for (String attributeType: attributeTypes.get(vertexType)) {
				HashMap<String, HashSet<String>> clone = new HashMap<>();
				for (String vt : attributeTypes.keySet()) {
					clone.put(vt, new HashSet<>());
					for (String at: attributeTypes.get(vt)) {
						clone.get(vt).add(at);
					}
				}
				clone.get(vertexType).remove(attributeType);
				this.addChild(vertexType, new Attribute(attributeType), clone);
			}
		}
	}

	public void traverseTree() {

	}

	private void addChild(String vertexType, Attribute attribute, HashMap<String, HashSet<String>> attributeTypes) {
		this.children.add(new LiteralTreeNode(this, vertexType, attribute, attributeTypes));
	}

	public Attribute getAttribute() {
		return this.attribute;
	}

	public String getVertexType() {
		return this.vertexType;
	}

	public ArrayList<LiteralTreeNode> getChildren() {
		return this.children;
	}

	public String toString() {
		StringBuilder buffer = new StringBuilder(50);
		print(buffer, "", "");
		return buffer.toString();
	}

	private void print(StringBuilder buffer, String prefix, String childrenPrefix) {
		buffer.append(prefix);
		buffer.append(vertexType).append(".").append(attribute.getAttrName());
		buffer.append('\n');
		for (Iterator<LiteralTreeNode> it = children.iterator(); it.hasNext();) {
			LiteralTreeNode next = it.next();
			if (it.hasNext()) {
				next.print(buffer, childrenPrefix + "├── ", childrenPrefix + "│   ");
			} else {
				next.print(buffer, childrenPrefix + "└── ", childrenPrefix + "    ");
			}
		}
	}
}

//class NodeHistogramSort implements Comparator<Entry<String, Map<String, Object>>> {
//
//	@Override
//	public int compare(Entry<String, Map<String, Object>> o1, Entry<String, Map<String, Object>> o2) {
//		return (int) o2.getValue().get("count") - (int) o1.getValue().get("count");
//	}
//
//}

//class HistogramSort implements Comparator<Entry<String, Integer>> {
//
//	@Override
//	public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
//		return o2.getValue() - o1.getValue();
//	}
//
//}