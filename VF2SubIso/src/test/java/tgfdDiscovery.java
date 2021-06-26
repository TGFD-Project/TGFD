import VF2Runner.VF2SubgraphIsomorphism;
import graphLoader.DBPediaLoader;
import infra.*;
import org.apache.commons.cli.*;
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
	private static final int SIZE_OF_ACTIVE_ATTR_SET = 20;
	private static Integer NUM_OF_EDGES_IN_GRAPH;
	public static int NUM_OF_VERTICES_IN_GRAPH;
	public static int NUM_OF_FREQ_EDGES_TO_CONSIDER = 20;
	public static Map<String, Integer> uniqueVertexTypesHist; // freq nodes come from here
	//	public static Map<String, List<Map.Entry<String,Integer>>> vertexTypesAttributes; // freq attributes come from here
	public static Map<String, HashSet<String>> vertexTypesAttributes; // freq attributes come from here
	//	public static ArrayList<Entry<String, Integer>> attrSortedHist;
	public static Map<String, Integer> uniqueEdgesHist; // freq edges come from here
	public static GenerationTree genTree;
	//	private static Map<String, Integer> attrHist;
	public static boolean isNaive = false;
	public static Long fileSuffix = null;
	public static boolean isGraphExperiment;
	public static HashSet<String> lowPatternSupportEdges;
	private static HashSet<String> activeAttributesSet;
	private static ArrayList<DBPediaLoader> graphs;

	public static void printTGFDstoFile(String experimentName, int k, double theta, ArrayList<TGFD> tgfds) {
		tgfds.sort(new Comparator<TGFD>() {
			@Override
			public int compare(TGFD o1, TGFD o2) {
				return o2.getSupport().compareTo(o1.getSupport());
			}
		});
		try {
			String timeAndDateStamp = ZonedDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("uuuu.MM.dd.HH.mm.ss"));
//			PrintStream printStream = new PrintStream(experimentName + "-tgfds-" + (isNaive?"naive":"optimized") + "-" + k + "-" + String.format("%.1f", theta) + "-a"+MAX_NUMBER_OF_ATTR_PER_VERTEX+"-"+timeAndDateStamp+".txt");
			PrintStream printStream = new PrintStream(experimentName + "-tgfds-" + (isNaive ? "naive" : "optimized") + "-" + k + "-" + String.format("%.1f", theta) + "-a" + SIZE_OF_ACTIVE_ATTR_SET + "-" + timeAndDateStamp + ".txt");
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
			String timeAndDateStamp = ZonedDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("uuuu.MM.dd.HH.mm.ss"));
			PrintStream printStream = new PrintStream("k-experiments-runtimes-" + (isNaive ? "naive" : "optimized") + "-" + timeAndDateStamp + ".txt");
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
			String timeAndDateStamp = ZonedDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("uuuu.MM.dd.HH.mm.ss"));
			PrintStream printStream = new PrintStream("theta-experiments-runtimes-" + (isNaive ? "naive" : "optimized") + "-" + timeAndDateStamp + ".txt");
			for (Double kValue : runtimes.keySet()) {
				printStream.print("theta = " + kValue);
				printStream.println(", execution time = " + (runtimes.get(kValue)));
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static void printExperimentRuntimestoFile(String experimentName, TreeMap<String, Long> runtimes) {
		try {
			String timeAndDateStamp = ZonedDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("uuuu.MM.dd.HH.mm.ss"));
			PrintStream printStream = new PrintStream(experimentName + "-experiments-runtimes-" + (isNaive ? "naive" : "optimized") + "-" + timeAndDateStamp + ".txt");
			for (String key : runtimes.keySet()) {
				printStream.print(experimentName + " = " + key);
				printStream.println(", execution time = " + (runtimes.get(key)));
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static void createSmallerSizedGraphs(long[] sizes) {
		String timeAndDateStamp = ZonedDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("uuuu.MM.dd.HH.mm.ss"));
		PrintStream logStream = null;
		try {
			logStream = new PrintStream("graph-creation-log-" + timeAndDateStamp + ".txt");
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
						newModel.write(new PrintStream(newFileName), "N3");
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

		Options options = new Options();
		options.addOption("console", false, "print to console");
		options.addOption("naive", false, "run naive version of algorithm");
		options.addOption("G", false, "run experiment on graph sizes 400K-1.6M");
		options.addOption("g", true, "run experiment on a specific graph size");
		options.addOption("Theta", false, "run experiment using support thresholds 0.1 to 0.5");
		options.addOption("theta", true, "run experiment using a specific support threshold");
		options.addOption("K", false, "run experiment for k = 1 to 5");
		options.addOption("k", true, "run experiment for k iterations");

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		assert cmd != null;

		if (!cmd.hasOption("console")) {
			String timeAndDateStamp = ZonedDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("uuuu.MM.dd.HH.mm.ss"));
			PrintStream logStream = null;
			try {
				logStream = new PrintStream("tgfd-discovery-log-" + timeAndDateStamp + ".txt");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			System.setOut(logStream);
		}

		if (cmd.hasOption("naive")) {
			tgfdDiscovery.isNaive = true;
		}

		// |G| experiment
		if (cmd.hasOption("G")) {
			String experimentName = "G";
			tgfdDiscovery.isNaive = false;
//			tgfdDiscovery.isGraphExperiment = true;
			long[] sizes = {400000, 800000, 1200000, 1600000};
			TreeMap<String, Long> gRuntimes = new TreeMap<>();
			for (long size : sizes) {
				// Compute Statistics
				tgfdDiscovery.fileSuffix = size;
				histogram();
				printHistogram();
				System.gc();
				double theta = cmd.getOptionValue("theta") == null ? 0.1 : Double.parseDouble(cmd.getOptionValue("theta"));
				int k = cmd.getOptionValue("k") == null ? 2 : Integer.parseInt(cmd.getOptionValue("k"));
				System.out.println("Running experiment for |" + experimentName + "| = " + size);
				final long startTime = System.currentTimeMillis();
				discover(k, theta, experimentName + size + "-experiment");
				final long endTime = System.currentTimeMillis();
				final long runTime = endTime - startTime;
				System.out.println("Total execution time for |" + experimentName + "| = " + size + " : " + runTime);
				gRuntimes.put(Long.toString(size), runTime);
				System.gc();
			}
			printExperimentRuntimestoFile(experimentName, gRuntimes);
			System.out.println();
			System.out.println("Runtimes for varying |G|:");
			for (String size : gRuntimes.keySet()) {
				System.out.print("|G| = " + size);
				System.out.println(", execution time = " + (gRuntimes.get(size)));
			}
			System.out.println();
		}

		// theta-experiments
		// Compute Statistics
		if (cmd.hasOption("Theta")) {
			if (cmd.getOptionValue("g") != null) {
				tgfdDiscovery.fileSuffix = Long.parseLong(cmd.getOptionValue("g"));
			}
			histogram();
			printHistogram();
			System.gc();
			String experimentName = "theta";
			System.out.println("Varying " + experimentName);
			TreeMap<String, Long> thetaRuntimes = new TreeMap<>();
			for (double theta = 0.9; theta > 0.0; theta -= 0.1) {
//				double theta = 0.5;
				System.out.println("Running experiment for theta = " + String.format("%.1f", theta));
				final long startTime = System.currentTimeMillis();
				int k = cmd.getOptionValue("k") == null ? 1 : Integer.parseInt(cmd.getOptionValue("k"));
				discover(k, theta, experimentName + String.format("%.1f", theta) + "-experiment");
				final long endTime = System.currentTimeMillis();
				final long runTime = endTime - startTime;
				System.out.println("Total execution time for theta = " + String.format("%.1f", theta) + " : " + runTime);
				thetaRuntimes.put(String.format("%.1f", theta), runTime);
				System.gc();
			}
			printExperimentRuntimestoFile(experimentName, thetaRuntimes);
			System.out.println();
			System.out.println("Runtimes for varying " + experimentName + ":");
			for (String thetaValue : thetaRuntimes.keySet()) {
				System.out.println("theta = " + String.format("%.1f", thetaValue));
				System.out.println("Total execution time: " + (thetaRuntimes.get(thetaValue)));
			}
			System.out.println();
		}

		// k-experiments
		if (cmd.hasOption("K")) {
			if (cmd.getOptionValue("g") != null) {
				tgfdDiscovery.fileSuffix = Long.parseLong(cmd.getOptionValue("g"));
			}

			// Compute Statistics
			histogram();
			printHistogram();
			System.gc();

			System.out.println("Varying k");
			int k = cmd.getOptionValue("k") == null ? 5 : Integer.parseInt(cmd.getOptionValue("k"));
			double theta = cmd.getOptionValue("theta") == null ? 0.5 : Double.parseDouble(cmd.getOptionValue("theta"));
			System.out.println("Running experiment for k = " + k);
			discover(k, theta, "k" + k + "-experiment");
		}

		// Custom experiment
		if (!cmd.hasOption("K") && !cmd.hasOption("G") && !cmd.hasOption("Theta")) {
			if (cmd.getOptionValue("g") != null) {
				tgfdDiscovery.fileSuffix = Long.parseLong(cmd.getOptionValue("g"));
			}
			String fileSuffix = tgfdDiscovery.fileSuffix == null ? "" : Long.toString(tgfdDiscovery.fileSuffix);
			// Compute Statistics
			histogram();
			printHistogram();
			System.gc();
			TreeMap<String, Long> runtimes = new TreeMap<>();
			double theta = cmd.getOptionValue("theta") == null ? 0.5 : Double.parseDouble(cmd.getOptionValue("theta"));
			int k = cmd.getOptionValue("k") == null ? 1 : Integer.parseInt(cmd.getOptionValue("k"));
			System.out.println("Running experiment for |G| = " + fileSuffix + ", k = " + k + ", theta = " + String.format("%.1f", theta));
			final long startTime = System.currentTimeMillis();
			discover(k, theta, "G" + fileSuffix + "-k" + k + "-theta" + String.format("%.1f", theta) + "-experiment");
			final long endTime = System.currentTimeMillis();
			final long runTime = endTime - startTime;
			System.out.println("Total execution time for |G| = " + fileSuffix + ", k = " + k + ", theta = " + String.format("%.1f", theta) + " : " + runTime);
			runtimes.put("|G| = " + fileSuffix + ", k = " + k + ", theta = " + String.format("%.1f", theta), runTime);
			printExperimentRuntimestoFile("custom", runtimes);
		}
	}

	public static void computeNodeHistogram() {

		System.out.println("Computing Node Histogram");

		Map<String, Integer> vertexTypeHistogram = new HashMap<>();
//		Map<String, Map<String, Integer>> tempVertexAttrFreqMap = new HashMap<>();
		Map<String, Set<String>> tempVertexAttrFreqMap = new HashMap<>();
		Map<String, String> vertexNameToTypeMap = new HashMap<>();
		Model model = ModelFactory.createDefaultModel();

		for (int i = 5; i < 8; i++) {
			String fileSuffix = tgfdDiscovery.fileSuffix == null ? "" : "-" + tgfdDiscovery.fileSuffix;
			String fileName = "201" + i + "types" + fileSuffix + ".ttl";
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
//			tempVertexAttrFreqMap.putIfAbsent(vertexType, new HashMap<String, Integer>());
			tempVertexAttrFreqMap.putIfAbsent(vertexType, new HashSet<String>());
			vertexNameToTypeMap.put(vertexName, vertexType);
		}

		tgfdDiscovery.NUM_OF_VERTICES_IN_GRAPH = 0;
		for (Map.Entry<String, Integer> entry : vertexTypeHistogram.entrySet()) {
			tgfdDiscovery.NUM_OF_VERTICES_IN_GRAPH += entry.getValue();
		}
		System.out.println("Number of vertices in graph: " + tgfdDiscovery.NUM_OF_VERTICES_IN_GRAPH);

		tgfdDiscovery.uniqueVertexTypesHist = vertexTypeHistogram;

		computeAttrHistogram(vertexNameToTypeMap, tempVertexAttrFreqMap);
		computeEdgeHistogram(vertexNameToTypeMap);

	}

	//	public static void computeAttrHistogram(Map<String, String> nodesRecord, Map<String, Map<String, Integer>> tempVertexAttrFreqMap) {
	public static void computeAttrHistogram(Map<String, String> nodesRecord, Map<String, Set<String>> tempVertexAttrFreqMap) {
		System.out.println("Computing attributes histogram");

		Map<String, Integer> attrHistMap = new HashMap<>();
		Map<String, Set<String>> attrDistributionMap = new HashMap<>();

		Model model = ModelFactory.createDefaultModel();
		for (int i = 5; i < 8; i++) {
			String fileSuffix = tgfdDiscovery.fileSuffix == null ? "" : "-" + tgfdDiscovery.fileSuffix;
			String fileName = "201" + i + "literals" + fileSuffix + ".ttl";
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
			if (nodesRecord.get(vertexName) != null) {
				attrHistMap.merge(attrName, 1, Integer::sum);
				String vertexType = nodesRecord.get(vertexName);
				if (tempVertexAttrFreqMap.containsKey(vertexType)) {
//					tempVertexAttrFreqMap.get(vertexType).merge(attrName, 1, Integer::sum);
					tempVertexAttrFreqMap.get(vertexType).add(attrName);
				}
				if (!attrDistributionMap.containsKey(attrName)) {
					attrDistributionMap.put(attrName, new HashSet<>());
				} else {
					attrDistributionMap.get(attrName).add(vertexType);
				}
			}
		}

		ArrayList<Entry<String,Set<String>>> sortedAttrDistributionMap = new ArrayList<>(attrDistributionMap.entrySet());
		if (!tgfdDiscovery.isNaive) {
			sortedAttrDistributionMap.sort(new Comparator<Entry<String, Set<String>>>() {
				@Override
				public int compare(Entry<String, Set<String>> o1, Entry<String, Set<String>> o2) {
					return o2.getValue().size() - o1.getValue().size();
				}
			});
		}
		HashSet<String> mostFrequentAttributesSet = new HashSet<>();
		for (Entry<String, Set<String>> attrNameEntry : sortedAttrDistributionMap.subList(0, Math.min(SIZE_OF_ACTIVE_ATTR_SET, sortedAttrDistributionMap.size()))) {
			mostFrequentAttributesSet.add(attrNameEntry.getKey());
		}
		tgfdDiscovery.activeAttributesSet = mostFrequentAttributesSet;

//		tgfdDiscovery.attrHist = attrHistMap;
		ArrayList<Entry<String, Integer>> sortedHistogram = new ArrayList<>(attrHistMap.entrySet());
		if (!tgfdDiscovery.isNaive) {
			sortedHistogram.sort(new Comparator<Entry<String, Integer>>() {
				@Override
				public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
					return o2.getValue() - o1.getValue();
				}
			});
		}

//		HashSet<String> mostFrequentAttributesSet = new HashSet<>();
//		for (Map.Entry<String, Integer> attrNameEntry : sortedHistogram.subList(0, Math.min(SIZE_OF_ACTIVE_ATTR_SET, sortedHistogram.size()))) {
//			mostFrequentAttributesSet.add(attrNameEntry.getKey());
//		}
//		tgfdDiscovery.activeAttributesSet = mostFrequentAttributesSet;

		Map<String, HashSet<String>> vertexTypesAttributes = new HashMap<>();
		for (String vertexType : tempVertexAttrFreqMap.keySet()) {
			Set<String> attrNameSet = tempVertexAttrFreqMap.get(vertexType);
			vertexTypesAttributes.put(vertexType, new HashSet<>());
			for (String attrName : attrNameSet) {
				if (mostFrequentAttributesSet.contains(attrName)) {
					vertexTypesAttributes.get(vertexType).add(attrName);
				}
			}
//			ArrayList<Map.Entry<String,Integer>> sortedAttributes = new ArrayList<>(tempVertexAttrFreqMap.get(vertexType).entrySet());
//			if (!tgfdDiscovery.isNaive) {
//				sortedAttributes.sort(new Comparator<Entry<String, Integer>>() {
//					@Override
//					public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
//						return o2.getValue() - o1.getValue();
//					}
//				});
//			}
////			vertexTypesAttributes.put(vertexType, sortedAttributes.subList(0,Math.min(MAX_NUMBER_OF_ATTR_PER_VERTEX,sortedAttributes.size())));
//			vertexTypesAttributes.put(vertexType, sortedAttributes);
		}


		tgfdDiscovery.vertexTypesAttributes = vertexTypesAttributes;
//		tgfdDiscovery.attrSortedHist = sortedHistogram.subList(0, SIZE_OF_ACTIVE_ATTR_SET);
	}

	public static void computeEdgeHistogram(Map<String, String> nodesRecord) {
		System.out.println("Computing edges histogram");

		Map<String, Integer> edgesHist = new HashMap<>();

		Model model = ModelFactory.createDefaultModel();
		for (int i = 5; i < 8; i++) {
			String fileSuffix = tgfdDiscovery.fileSuffix == null ? "" : "-" + tgfdDiscovery.fileSuffix;
			String fileName = "201" + i + "objects" + fileSuffix + ".ttl";
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
		for (Map.Entry<String, Integer> entry : edgesHist.entrySet()) {
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
		return sortedVertexTypeHistogram.subList(0, size);
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
			if (1.0 * entry.getValue() / NUM_OF_EDGES_IN_GRAPH >= PATTERN_SUPPORT_THRESHOLD) {
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
			Set<String> attributes = vertexTypesAttributes.get(vertexType);
//			List<Entry<String, Integer>> attributes = vertexTypesAttributes.get(vertexType);
//			ArrayList<Entry<String, Integer>> attributes = (ArrayList<Entry<String, Integer>>) nodesHist.get(i).getValue().get("attributes");
			System.out.println(vertexType + "={count=" + entry.getValue() + ", attributes=" + attributes + "}");
		}
//		System.out.println();
//		System.out.println("Number of attribute types: " + attrSortedHist.size());
//		System.out.println("Attributes:");
//		for (int i=0; i < 10; i++) {
//			System.out.println(attrSortedHist.get(i));
//		}
		System.out.println();
		System.out.println("Size of active attributes set: " + activeAttributesSet.size());
		System.out.println("Attributes:");
		for (String attrName : activeAttributesSet) {
			System.out.println(attrName);
		}
		System.out.println();
		System.out.println("Number of edge types: " + uniqueEdgesHist.size());
		System.out.println("Frequent Edges:");
//		for (int i=0; i < 10; i++) {
		for (Entry<String, Integer> entry : sortedEdgeHistogram) {
			System.out.println("edge=\"" + entry.getKey() + "\", count=" + entry.getValue());
		}
		System.out.println();
	}

	public static Map<Set<ConstantLiteral>, ArrayList<Entry<ConstantLiteral, List<Integer>>>> findMatches2(ArrayList<DBPediaLoader> graphs, VF2PatternGraph pattern, List<ConstantLiteral> attributes) {
		String yVertexType = attributes.get(attributes.size()-1).getVertexType();
		String yAttrName = attributes.get(attributes.size()-1).getAttrName();
//		List<ConstantLiteral> xAttributes = attributes.subList(0,attributes.size()-1);
		Map<Set<ConstantLiteral>, Map<ConstantLiteral, List<Integer>>> entitiesWithRHSvalues = new HashMap<>();
		int t = 2015;
		for (DBPediaLoader graph : graphs) {
			System.out.println("---------- Attribute values in " + t + " ---------- ");
			int numOfMatches = 0;
			Iterator<GraphMapping<Vertex, RelationshipEdge>> results = new VF2SubgraphIsomorphism().execute(graph.getGraph(), pattern, false, true);
			if (results != null) {
				while (results.hasNext()) {
					Set<ConstantLiteral> entity = new HashSet<>();
					ConstantLiteral rhs = null;
					GraphMapping<Vertex, RelationshipEdge> match = results.next();
					for (Vertex patternVertex : pattern.getPattern().vertexSet()) {
						Vertex currentMatchedVertex = match.getVertexCorrespondence(patternVertex, false);
						if (currentMatchedVertex == null) continue;
						String patternVertexType = new ArrayList<>(patternVertex.getTypes()).get(0);
						for (ConstantLiteral attribute : attributes) {
							if (attribute.getVertexType().equals(patternVertexType)) {
								for (String attrName : patternVertex.getAllAttributesNames()) {
									if (attribute.getAttrName().equals(attrName)) {
										String xAttrValue = currentMatchedVertex.getAttributeValueByName(attrName);
										ConstantLiteral xLiteral = new ConstantLiteral(patternVertexType, attrName, xAttrValue);
										entity.add(xLiteral);
									}
								}
							}
						}
						if (patternVertexType.equals(yVertexType) && currentMatchedVertex.hasAttribute(yAttrName)) {
							String yAttrValue = currentMatchedVertex.getAttributeValueByName(yAttrName);
							rhs = new ConstantLiteral(patternVertexType, yAttrName, yAttrValue);
						}
					}
					if (entity.size() == 0 || rhs == null) continue;

					if (!entitiesWithRHSvalues.containsKey(entity)) {
						entitiesWithRHSvalues.put(entity, new HashMap<>());
					}
					if (!entitiesWithRHSvalues.get(entity).containsKey(rhs)) {
						entitiesWithRHSvalues.get(entity).put(rhs, new ArrayList<>());
					}
					entitiesWithRHSvalues.get(entity).get(rhs).add(t);
					numOfMatches++;
				}
			}
			System.out.println("Number of matches: " + numOfMatches);
			t++;
		}
		if (entitiesWithRHSvalues.size() == 0) return null;

		Comparator<Entry<ConstantLiteral, List<Integer>>> comparator = new Comparator<Entry<ConstantLiteral, List<Integer>>>() {
			@Override
			public int compare(Entry<ConstantLiteral, List<Integer>> o1, Entry<ConstantLiteral, List<Integer>> o2) {
				return o2.getValue().size() - o1.getValue().size();
			}
		};

		Map<Set<ConstantLiteral>, ArrayList<Map.Entry<ConstantLiteral, List<Integer>>>> entitiesWithSortedRHSvalues = new HashMap<>();
		for (Set<ConstantLiteral> entity : entitiesWithRHSvalues.keySet()) {
			Map<ConstantLiteral, List<Integer>> rhsMapOfEntity = entitiesWithRHSvalues.get(entity);
			ArrayList<Map.Entry<ConstantLiteral, List<Integer>>> sortedRhsMapOfEntity = new ArrayList<>(rhsMapOfEntity.entrySet());
			sortedRhsMapOfEntity.sort(comparator);
			entitiesWithSortedRHSvalues.put(entity, sortedRhsMapOfEntity);
		}

		return entitiesWithSortedRHSvalues;
	}

	public static Map<Set<ConstantLiteral>, ArrayList<Entry<ConstantLiteral, List<Integer>>>> findMatches(ArrayList<DBPediaLoader> graphs, VF2PatternGraph pattern, HashMap<String, HashSet<String>> xVertexToAttrNameMap, String yVertexType, String yAttrName) {
		Map<Set<ConstantLiteral>, Map<ConstantLiteral, List<Integer>>> entitiesWithRHSvalues = new HashMap<>();
		int t = 2015;
		for (DBPediaLoader graph : graphs) {
			System.out.println("---------- Attribute values in " + t + " ---------- ");
			int numOfMatches = 0;
			Iterator<GraphMapping<Vertex, RelationshipEdge>> results = new VF2SubgraphIsomorphism().execute(graph.getGraph(), pattern, false, true);
			if (results == null) continue;
			while (results.hasNext()) {
				Set<ConstantLiteral> entity = new HashSet<>();
				ConstantLiteral rhs = null;
				GraphMapping<Vertex, RelationshipEdge> match = results.next();
				for (Vertex patternVertex : pattern.getPattern().vertexSet()) {
					Vertex currentMatchedVertex = match.getVertexCorrespondence(patternVertex, false);
					if (currentMatchedVertex == null) continue;
					String patternVertexType = new ArrayList<>(patternVertex.getTypes()).get(0);
					if (xVertexToAttrNameMap.containsKey(patternVertexType) || yVertexType.equals(patternVertexType)) {
						for (String attrName : patternVertex.getAllAttributesNames()) {
							if (xVertexToAttrNameMap.get(patternVertexType).contains(attrName)) {
								String xAttrValue = currentMatchedVertex.getAttributeValueByName(attrName);
								ConstantLiteral xLiteral = new ConstantLiteral(patternVertexType, attrName, xAttrValue);
								entity.add(xLiteral);
							}
							if (yAttrName.equals(attrName)) {
								String yAttrValue = currentMatchedVertex.getAttributeValueByName(attrName);
								rhs = new ConstantLiteral(patternVertexType, attrName, yAttrValue);
							}
						}
					}
				}
				if (entity.size() == 0 || rhs == null) continue;

				if (!entitiesWithRHSvalues.containsKey(entity)) {
					entitiesWithRHSvalues.put(entity, new HashMap<>());
				}
				if (!entitiesWithRHSvalues.get(entity).containsKey(rhs)) {
					entitiesWithRHSvalues.get(entity).put(rhs, new ArrayList<>());
				}
				entitiesWithRHSvalues.get(entity).get(rhs).add(t);
				numOfMatches++;
			}
			System.out.println("Number of matches: " + numOfMatches);
			t++;
		}
		if (entitiesWithRHSvalues.size() == 0) return null;

		Comparator<Entry<ConstantLiteral, List<Integer>>> comparator = new Comparator<Entry<ConstantLiteral, List<Integer>>>() {
			@Override
			public int compare(Entry<ConstantLiteral, List<Integer>> o1, Entry<ConstantLiteral, List<Integer>> o2) {
				return o2.getValue().size() - o1.getValue().size();
			}
		};

		Map<Set<ConstantLiteral>, ArrayList<Map.Entry<ConstantLiteral, List<Integer>>>> entitiesWithSortedRHSvalues = new HashMap<>();
		for (Set<ConstantLiteral> entity : entitiesWithRHSvalues.keySet()) {
			Map<ConstantLiteral, List<Integer>> rhsMapOfEntity = entitiesWithRHSvalues.get(entity);
			ArrayList<Map.Entry<ConstantLiteral, List<Integer>>> sortedRhsMapOfEntity = new ArrayList<>(rhsMapOfEntity.entrySet());
			sortedRhsMapOfEntity.sort(comparator);
			entitiesWithSortedRHSvalues.put(entity, sortedRhsMapOfEntity);
		}

		return entitiesWithSortedRHSvalues;
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
//				while (numOfSelfMatchingEntitiesInSnapshot <= ENTITY_LIMIT && results.hasNext()) { // TO-DO: Remove this hard-coded limit?
				while (results.hasNext()) {
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
//				System.out.println(vertexType + ":" + attr + ":" + sortedMap.get(vertexType).get(attr).subList(0,Math.min(numOfValues,ENTITY_LIMIT)));
				System.out.println(vertexType + ":" + attr + ":" + sortedMap.get(vertexType).get(attr));
			}
		}
		return sortedMap;
	}

	public static void generateDummyTGFDsForDeltaDiscovery(int i, ArrayList<TGFD> dummyTGFDs) {
		ArrayList<GenTreeNode> nodes = genTree.getLevel(i);
		for (GenTreeNode node : nodes) {
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

	public static HashSet<ConstantLiteral> getActiveAttributesInPattern(Set<Vertex> vertexSet) {
		HashMap<String, HashSet<String>> patternVerticesAttributes = new HashMap<>();
		for (Vertex vertex : vertexSet) {
			for (String vertexType : vertex.getTypes()) {
				patternVerticesAttributes.put(vertexType, new HashSet<>());
				Set<String> attrNameSet = vertexTypesAttributes.get(vertexType);
				for (String attrName : attrNameSet) {
					patternVerticesAttributes.get(vertexType).add(attrName);
				}
			}
		}
		HashSet<ConstantLiteral> literals = new HashSet<>();
		for (String vertexType : patternVerticesAttributes.keySet()) {
			for (String attrName : patternVerticesAttributes.get(vertexType)) {
				ConstantLiteral literal = new ConstantLiteral(vertexType, attrName, null);
				literals.add(literal);
			}
		}
		return literals;
	}

	public static boolean isPathVisited(ArrayList<ConstantLiteral> path, ArrayList<ArrayList<ConstantLiteral>> visitedPaths) {
		for (ArrayList<ConstantLiteral> visitedPath : visitedPaths) {
			if (visitedPath.size() == path.size()
					&& visitedPath.subList(0,visitedPath.size()-1).containsAll(path.subList(0, path.size()-1))
					&& visitedPath.get(visitedPath.size()-1).equals(path.get(path.size()-1))) {
				System.out.println("This dependency was already visited.");
				return true;
			}
		}
		return false;
	}

	public static boolean isZeroEntityPath(ArrayList<ConstantLiteral> path, GenTreeNode patternTreeNode) {
		ArrayList<ArrayList<ConstantLiteral>> zeroEntityPaths = new ArrayList<>();
		// Get all zero-entity paths in pattern tree
		GenTreeNode currPatternTreeNode = patternTreeNode;
		zeroEntityPaths.addAll(currPatternTreeNode.getZeroEntityDependencies());
		while (currPatternTreeNode.parentNode() != null) {
			currPatternTreeNode = currPatternTreeNode.parentNode();
			zeroEntityPaths.addAll(currPatternTreeNode.getZeroEntityDependencies());
		}
		for (ArrayList<ConstantLiteral> zeroEntityPath : zeroEntityPaths) {
			if (path.get(path.size()-1).equals(zeroEntityPath.get(zeroEntityPath.size()-1)) && path.subList(0, path.size()-1).containsAll(zeroEntityPath.subList(0,zeroEntityPath.size()-1))) {
				System.out.println("Candidate dependency is a superset of zero-entity dependency: " + zeroEntityPath);
				return true;
			}
		}
		return false;
	}

	public static boolean isMinimalPath(ArrayList<ConstantLiteral> path, GenTreeNode patternTreeNode) {
		ArrayList<ArrayList<ConstantLiteral>> minimalPaths = new ArrayList<>();
		// Get all minimal paths in pattern tree
		GenTreeNode currPatternTreeNode = patternTreeNode;
		minimalPaths.addAll(currPatternTreeNode.getMinimalDependencies());
		while (currPatternTreeNode.parentNode() != null) {
			currPatternTreeNode = currPatternTreeNode.parentNode();
			minimalPaths.addAll(currPatternTreeNode.getMinimalDependencies());
		}
		for (ArrayList<ConstantLiteral> minimalPath : minimalPaths) {
			if (path.get(path.size()-1).equals(minimalPath.get(minimalPath.size()-1)) && path.subList(0, path.size()-1).containsAll(minimalPath.subList(0,minimalPath.size()-1))) {
				System.out.println("Candidate dependency is a superset of minimal dependency: " + minimalPath);
				return true;
			}
		}
		return false;
	}

	public static ArrayList<TGFD> deltaDiscovery2(ArrayList<DBPediaLoader> graphs, double theta, GenTreeNode patternNode, LiteralTreeNode literalTreeNode, ArrayList<ConstantLiteral> literalPath) {
		ArrayList<TGFD> tgfds = new ArrayList<>();

		// Add dependency attributes to pattern
		VF2PatternGraph patternForDependency = patternNode.getPattern().copy();
		Set<ConstantLiteral> attributesSetForDependency = new HashSet<>(literalPath);
		for (Vertex v : patternForDependency.getPattern().vertexSet()) {
			String vType = new ArrayList<>(v.getTypes()).get(0);
			for (ConstantLiteral attribute : attributesSetForDependency) {
				if (vType.equals(attribute.getVertexType())) {
					v.addAttribute(new Attribute(attribute.getAttrName()));
				}
			}
		}

		System.out.println("Pattern: " + patternForDependency);
		System.out.println("Dependency: " + "\n\tY=" + literalPath.get(literalPath.size()-1) + ",\n\tX=" + literalPath.subList(0,literalPath.size()-1) + "\n\t}");

		System.out.println("Performing Entity Discovery");

		// Discover entities
		Map<Set<ConstantLiteral>, ArrayList<Entry<ConstantLiteral, List<Integer>>>> entities = findMatches2(graphs, patternForDependency, literalPath.subList(0,literalPath.size()-1));
		if (entities == null) {
			System.out.println("Mark as Pruned. No matches found during entity discovery.");
			literalTreeNode.setIsPruned();
			patternNode.addZeroEntityDependency(literalPath);
			return tgfds;
		}

		System.out.println("Discovering constant TGFDs");

		// Find Constant TGFDs
		ArrayList<Pair> constantXdeltas = new ArrayList<>();
		ArrayList<TreeSet<Pair>> satisfyingAttrValues = new ArrayList<>();
		ArrayList<TGFD> constantTGFDs = discoverConstantTGFDs(theta, patternNode, literalPath.get(literalPath.size()-1), entities, constantXdeltas, satisfyingAttrValues);
		// TO-DO: Try discover general TGFD even if no constant TGFD candidate met support threshold
		System.out.println("Constant TGFDs discovered: " + constantTGFDs.size());
		tgfds.addAll(constantTGFDs);

		System.out.println("Discovering general TGFDs");

		// Find general TGFDs
		ArrayList<TGFD> generalTGFD = discoverGeneralTGFD(theta, patternForDependency, literalPath, entities.size(), constantXdeltas, satisfyingAttrValues);
		if (generalTGFD.size() > 0) {
			System.out.println("Marking literal node as pruned. Discovered general TGFDs for this dependency.");
			literalTreeNode.setIsPruned();
			patternNode.addMinimalDependency(literalPath);
		}
		tgfds.addAll(generalTGFD);

		return tgfds;
	}

	private static ArrayList<TGFD> discoverGeneralTGFD(double theta, VF2PatternGraph patternForDependency, ArrayList<ConstantLiteral> literalPath, int entitiesSize, ArrayList<Pair> constantXdeltas, ArrayList<TreeSet<Pair>> satisfyingAttrValues) {

		ArrayList<TGFD> tgfds = new ArrayList<>();

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
		int currMax = graphs.size() - 1;
		ArrayList<TreeSet<Pair>> currSatisfyingAttrValues = new ArrayList<>();
		for (int index = 0; index < constantXdeltas.size(); index++) {
			Pair deltaPair = constantXdeltas.get(index);
			if (Math.max(currMin, deltaPair.min()) <= Math.min(currMax, deltaPair.max())) {
				currMin = Math.max(currMin, deltaPair.min());
				currMax = Math.min(currMax, deltaPair.max());
				currSatisfyingAttrValues.add(satisfyingAttrValues.get(index)); // By axiom 4
			} else {
				intersections.putIfAbsent(new Pair(currMin, currMax), currSatisfyingAttrValues);
				currMin = 0;
				currMax = graphs.size() - 1;
				if (Math.max(currMin, deltaPair.min()) <= Math.min(currMax, deltaPair.max())) {
					currMin = Math.max(currMin, deltaPair.min());
					currMax = Math.min(currMax, deltaPair.max());
					currSatisfyingAttrValues.add(satisfyingAttrValues.get(index));
				}
			}
		}
		intersections.putIfAbsent(new Pair(currMin, currMax), currSatisfyingAttrValues);

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

			// Compute general support
			float numerator = 0;
			float denominator = 2 * entitiesSize * graphs.size();

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

			float support = numerator / denominator;
			if (support < theta) {
				System.out.println("Support for candidate general TGFD is below support threshold");
				continue;
			}

			System.out.println("TGFD Support = " + numerator + "/" + denominator);

			Delta delta = new Delta(Period.ofDays(generalMin * 183), Period.ofDays(generalMax * 183 + 1), Duration.ofDays(183));

			Dependency generalDependency = new Dependency();
			String yVertexType = literalPath.get(literalPath.size()-1).getVertexType();
			String yAttrName = literalPath.get(literalPath.size()-1).getAttrName();
			VariableLiteral y = new VariableLiteral(yVertexType, yAttrName, yVertexType, yAttrName);
			generalDependency.addLiteralToY(y);
			for (ConstantLiteral x : literalPath.subList(0, literalPath.size()-1)) {
				String xVertexType = x.getVertexType();
				String xAttrName = x.getAttrName();
				VariableLiteral varX = new VariableLiteral(xVertexType, xAttrName, xVertexType, xAttrName);
				generalDependency.addLiteralToX(varX);
			}

			TGFD tgfd = new TGFD(patternForDependency, delta, generalDependency, support, "");
			System.out.println("TGFD: " + tgfd);
			tgfds.add(tgfd);
		}
		return tgfds;
	}

	private static ArrayList<TGFD> discoverConstantTGFDs(double theta, GenTreeNode patternNode, ConstantLiteral yLiteral, Map<Set<ConstantLiteral>, ArrayList<Entry<ConstantLiteral, List<Integer>>>> entities, ArrayList<Pair> constantXdeltas, ArrayList<TreeSet<Pair>> satisfyingAttrValues) {
		ArrayList<TGFD> tgfds = new ArrayList<>();
		String yVertexType = yLiteral.getVertexType();
		String yAttrName = yLiteral.getAttrName();
		for (Entry<Set<ConstantLiteral>, ArrayList<Entry<ConstantLiteral, List<Integer>>>> entityEntry : entities.entrySet()) {
			VF2PatternGraph newPattern = patternNode.getPattern().copy();
			Dependency newDependency = new Dependency();
			for (ConstantLiteral xLiteral : entityEntry.getKey()) {
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

			System.out.println("Performing Constant TGFD discovery");
			System.out.println("Pattern: " + newPattern);
			System.out.println("Entity: " + newDependency);
			ArrayList<Entry<ConstantLiteral, List<Integer>>> attrValuesTimestampsSortedByFreq = entityEntry.getValue();

			for (Map.Entry<ConstantLiteral, List<Integer>> entry : attrValuesTimestampsSortedByFreq) {
				System.out.println(entry.getKey() + ":" + entry.getValue());
			}

			ArrayList<Pair> candidateDeltas = new ArrayList<>();
			if (attrValuesTimestampsSortedByFreq.size() == 1) {
				List<Integer> timestamps = attrValuesTimestampsSortedByFreq.get(0).getValue();
				int minDistance = graphs.size() - 1;
				int maxDistance = timestamps.get(timestamps.size() - 1) - timestamps.get(0);
				for (int index = 1; index < timestamps.size(); index++) {
					minDistance = Math.min(minDistance, timestamps.get(index) - timestamps.get(index - 1));
				}
				if (minDistance > maxDistance) {
					System.out.println("Not enough timestamped matches found for entity.");
					continue;
				}
				candidateDeltas.add(new Pair(minDistance, maxDistance));
			} else if (attrValuesTimestampsSortedByFreq.size() > 1) {
				int minExclusionDistance = graphs.size() - 1;
				int maxExclusionDistance = 0;
				ArrayList<Integer> distances = new ArrayList<>();
				int l1 = attrValuesTimestampsSortedByFreq.get(0).getValue().get(0);
				int u1 = attrValuesTimestampsSortedByFreq.get(0).getValue().get(attrValuesTimestampsSortedByFreq.get(0).getValue().size() - 1);
				for (int index = 1; index < attrValuesTimestampsSortedByFreq.size(); index++) {
					int l2 = attrValuesTimestampsSortedByFreq.get(index).getValue().get(0);
					int u2 = attrValuesTimestampsSortedByFreq.get(index).getValue().get(attrValuesTimestampsSortedByFreq.get(index).getValue().size() - 1);
					distances.add(Math.abs(u2 - l1));
					distances.add(Math.abs(u1 - l2));
				}
				for (int index = 0; index < distances.size(); index++) {
					minExclusionDistance = Math.min(minExclusionDistance, distances.get(index));
					maxExclusionDistance = Math.max(maxExclusionDistance, distances.get(index));
				}

				if (minExclusionDistance > 0) {
					Pair deltaPair = new Pair(0, minExclusionDistance - 1);
					candidateDeltas.add(deltaPair);
				}
				if (maxExclusionDistance < graphs.size() - 1) {
					Pair deltaPair = new Pair(maxExclusionDistance + 1, graphs.size() - 1);
					candidateDeltas.add(deltaPair);
				}
			}

			// Compute support
			Delta candidateTGFDdelta = null;
			double candidateTGFDsupport = 0.0;
			Pair mostSupportedDelta = null;
			TreeSet<Pair> mostSupportedSatisfyingPairs = null;
			for (Pair candidateDelta : candidateDeltas) {
				int minDistance = candidateDelta.min();
				int maxDistance = candidateDelta.max();
				if (minDistance <= maxDistance) {
					System.out.println("Calculating support for candidate delta ("+minDistance+","+maxDistance+")");
					float numer = 0;
					float denom = 2 * 1 * graphs.size();
					List<Integer> timestamps = attrValuesTimestampsSortedByFreq.get(0).getValue();
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
					double candidateSupport = numer / denom;

					if (candidateSupport > candidateTGFDsupport) {
						candidateTGFDsupport = candidateSupport;
						mostSupportedDelta = candidateDelta;
						mostSupportedSatisfyingPairs = satisfyingPairs;
					}
				}
			}
			System.out.println("Entity satisfying attributes:" + mostSupportedSatisfyingPairs);
			System.out.println("Entity delta = " + mostSupportedDelta);
			System.out.println("Entity support = " + candidateTGFDsupport);
			if (candidateTGFDsupport >= theta) {
				int minDistance = mostSupportedDelta.min();
				int maxDistance = mostSupportedDelta.max();
				candidateTGFDdelta = new Delta(Period.ofDays(minDistance * 183), Period.ofDays(maxDistance * 183 + 1), Duration.ofDays(183));
				satisfyingAttrValues.add(mostSupportedSatisfyingPairs);
				constantXdeltas.add(mostSupportedDelta);
				System.out.println(candidateTGFDdelta);
			} else {
				System.out.println("Could not satisfy TGFD support threshold for entity: " + entityEntry.getKey());
				continue;
			}

			//TO-DO: Only add tgfd to output set if its not a superset of a minimal TGFD
			TGFD entityTGFD = new TGFD(newPattern, candidateTGFDdelta, newDependency, candidateTGFDsupport, "");
			System.out.println("TGFD: " + entityTGFD);
			tgfds.add(entityTGFD);
		}
		return tgfds;
	}

	public static void loadDBpediaSnapshots() {
		graphs = new ArrayList<>();
		for (int year = 5; year < 8; year++) {
			String fileSuffix = tgfdDiscovery.fileSuffix == null ? "" : "-" + tgfdDiscovery.fileSuffix;
			String typeFileName = "201" + year + "types" + fileSuffix + ".ttl";
			String literalsFileName = "201" + year + "literals" + fileSuffix + ".ttl";
			String objectsFileName = "201" + year + "objects" + fileSuffix + ".ttl";
			DBPediaLoader dbpedia = new DBPediaLoader(new ArrayList<>(), new ArrayList<>(Collections.singletonList(typeFileName)), new ArrayList<>(Arrays.asList(literalsFileName, objectsFileName)));
			graphs.add(dbpedia);
		}
	}

	public static ArrayList<TGFD> hSpawn2(int i, double theta, GenTreeNode patternNode) {
		ArrayList<TGFD> tgfds = new ArrayList<>();

		System.out.println("Performing HSpawn for " + patternNode.getPattern());

		HashSet<ConstantLiteral> literals = getActiveAttributesInPattern(patternNode.getGraph().vertexSet());

		LiteralTree literalTree = new LiteralTree();
		for (int j = 0; j < literals.size(); j++) {

			System.out.println("HSpawn level " + j + "/" + literals.size());

			if (j == 0) {
				literalTree.addLevel();
				for (ConstantLiteral literal: literals) {
					literalTree.createNodeAtLevel(j, literal, null);
				}
			} else if (j <= i) {
				ArrayList<LiteralTreeNode> literalTreePreviousLevel = literalTree.getLevel(j - 1);
				if (literalTreePreviousLevel.size() == 0) {
					System.out.println("Previous level of literal tree is empty. Nothing to expand. End HSpawn");
					break;
				}
				literalTree.addLevel();
				ArrayList<ArrayList<ConstantLiteral>> visitedPaths = new ArrayList<>();
				ArrayList<TGFD> currentLevelTGFDs = new ArrayList<>();
				for (LiteralTreeNode previousLevelLiteral : literalTreePreviousLevel) {
					if (previousLevelLiteral.isPruned()) continue;
					ArrayList<ConstantLiteral> parentsPathToRoot = previousLevelLiteral.getPathToRoot();
					for (ConstantLiteral literal: literals) {
						// Check if leaf node exists in path already
						if (parentsPathToRoot.contains(literal)) continue;

						// Check if path to candidate leaf node is unique
						ArrayList<ConstantLiteral> newPath = new ArrayList<>();
						newPath.add(literal);
						newPath.addAll(previousLevelLiteral.getPathToRoot());
						if (isPathVisited(newPath, visitedPaths)) {
							System.out.println("Duplicate literal path: " + newPath);
							continue;
						}
						if (isZeroEntityPath(newPath, patternNode) || isMinimalPath(newPath, patternNode)) {
							System.out.println("Marking literal node as pruned. Skip dependency");
							continue;
						}
						System.out.println("Newly created unique literal path: " + newPath);

						// Add leaf node to tree
						LiteralTreeNode literalTreeNode = literalTree.createNodeAtLevel(j, literal, previousLevelLiteral);

						if (j != i) { // Ensures delta discovery only occurs at |LHS| = |Q|
							System.out.println("|LHS| != |Q|. Skip performing Delta Discovery HSpawn level " + j);
							continue;
						}
						visitedPaths.add(newPath);

						System.out.println("Performing Delta Discovery at HSpawn level " + j);
						ArrayList<TGFD> discoveredTGFDs = deltaDiscovery2(graphs, theta, patternNode, literalTreeNode, newPath);
						currentLevelTGFDs.addAll(discoveredTGFDs);
					}
				}
				if (currentLevelTGFDs.size() > 0) {
					System.out.println("TGFDs generated at HSpawn level " + j + ": " + currentLevelTGFDs.size());
					tgfds.addAll(currentLevelTGFDs);
				}
			} else {
				break;
			}
			System.out.println("Generated new literal tree nodes: "+ literalTree.getLevel(j).size());
		}
		System.out.println("For pattern " + patternNode.getPattern());
		System.out.println("HSpawn TGFD count: " + tgfds.size());
		return tgfds;
	}

	//	public static ArrayList<TGFD> deltaDiscovery(int k, double theta, ArrayList<DBPediaLoader> graphs) {
	public static ArrayList<TGFD> deltaDiscovery(int i, double theta) {

		// TO-DO: Is it possible to prune the graphs by passing them our generated patterns and dependencies as dummy TGFDs (with null Deltas) ???
		ArrayList<TGFD> dummyTGFDs = new ArrayList<>();
		if (Config.optimizedLoadingBasedOnTGFD) generateDummyTGFDsForDeltaDiscovery(i, dummyTGFDs);
		ArrayList<DBPediaLoader> graphs = new ArrayList<>();
		for (int year = 5; year < 8; year++) {
			String fileSuffix = tgfdDiscovery.fileSuffix == null ? "" : "-" + tgfdDiscovery.fileSuffix;
			String typeFileName = "201" + year + "types" + fileSuffix + ".ttl";
			String literalsFileName = "201" + year + "literals" + fileSuffix + ".ttl";
			String objectsFileName = "201" + year + "objects" + fileSuffix + ".ttl";
			DBPediaLoader dbpedia = new DBPediaLoader(dummyTGFDs, new ArrayList<>(Collections.singletonList(typeFileName)), new ArrayList<>(Arrays.asList(literalsFileName, objectsFileName)));
			graphs.add(dbpedia);
		}

		System.out.println("Performing Delta Discovery for Gen Tree Level " + i);

		ArrayList<TGFD> tgfds = new ArrayList<>();

//		for (int i = 0; i <= k; i++) {
		ArrayList<GenTreeNode> nodes = genTree.getLevel(i);
		for (GenTreeNode node : nodes) {

			int numOfTGFDsAddedForThisNode = 0;

			System.out.println("Discovering delta for: ");
			System.out.println(node.getPattern());
//			for (TreeSet<Dependency> dependencySet : node.getDependenciesSets()) {
//				for (Dependency dependency : dependencySet) {
//					System.out.println(dependency);
//				}
//			}
			ArrayList<TreeSet<Dependency>> dependenciesSets = node.getDependenciesSets();
			ArrayList<PatternDependencyPair> minimalSetOfTGFDs = new ArrayList<>();
			ArrayList<PatternDependencyPair> prunedSetOfTGFDs = new ArrayList<>();
			for (TreeSet<Dependency> dependencySet : dependenciesSets) {
				ArrayList<Dependency> prunedSetOfDependencies = new ArrayList<>();
				ArrayList<Dependency> minimalSetOfDependencies = new ArrayList<>();
				for (Dependency dependency : dependencySet) {
					// Prune low-suppport or non-minimal TGFDs
					if (node.parentNode() != null && !tgfdDiscovery.isNaive) {
						boolean isSubsetOfMinimalTGFD = isSubsetOfDependency(node.getPattern(), dependency, node.parentNode().getMinimalSetOfDependencies(), minimalSetOfTGFDs);
						boolean isSubsetOfPrunedTGFD = isSubsetOfDependency(node.getPattern(), dependency, node.parentNode().getPrunedSetOfDependencies(), prunedSetOfTGFDs);
						if (isSubsetOfMinimalTGFD || isSubsetOfPrunedTGFD) {
							System.out.println("Skip. Candidate dependency and pattern builds off " + (isSubsetOfPrunedTGFD ? "pruned" : "minimal") + " TGFD");
							continue;
						}
					}

					// Prune low-support or non-minimal dependencies
					if (!tgfdDiscovery.isNaive) {
						boolean hasAPrunedSubset = isSubsetOfDependency(dependency, prunedSetOfDependencies);
						boolean hasAMinimalSubset = isSubsetOfDependency(dependency, minimalSetOfDependencies);
						if (hasAPrunedSubset || hasAMinimalSubset) {
							System.out.println("The following dependency has a " + (hasAPrunedSubset ? "pruned" : "minimal") + " subset:" + dependency);
							System.out.println("Skipping this dependency");
							continue;
						}
					}

					// Discover entities
					int numOfTGFDsAddedForThisDependency = 0;

					// Get RHS attributes
					String yVertexType = ((ConstantLiteral) (dependency.getY().get(0))).getVertexType();
					String yAttrName = ((ConstantLiteral) (dependency.getY().get(0))).getAttrName();

					// Get LHS attributes
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

					// Add attributes to pattern
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
					Map<Set<ConstantLiteral>, ArrayList<Entry<ConstantLiteral, List<Integer>>>> entities = findMatches(graphs, patternCopy, xAttrNameMap, yVertexType, yAttrName);
					System.gc();

					if (entities == null) { // TO-DO: Only discard dependencies that build off this one. Don't discard ones that start in a different order.
						System.out.println("Could not find any matches during entity discovery for " + dependency);
						System.out.println("Do not expand this dependency. Pruned");
						prunedSetOfDependencies.add(dependency);
						prunedSetOfTGFDs.add(new PatternDependencyPair(patternCopy, dependency));
						continue;
					}

					// Discover Y values and corresponding deltas for each entity
					ArrayList<Pair> constantXdeltas = new ArrayList<>();
					ArrayList<TreeSet<Pair>> satisfyingAttrValues = new ArrayList<>();
					for (Entry<Set<ConstantLiteral>, ArrayList<Entry<ConstantLiteral, List<Integer>>>> entityEntry : entities.entrySet()) {
						VF2PatternGraph newPattern = node.getPattern().copy();
						Dependency newDependency = new Dependency();
						for (ConstantLiteral xLiteral : entityEntry.getKey()) {
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

						if (node.parentNode() != null && !tgfdDiscovery.isNaive) {
							boolean isSubsetOfPrunedTGFD = isSubsetOfDependency(node.getPattern(), newDependency, node.parentNode().getPrunedSetOfDependencies(), prunedSetOfTGFDs);

							if (isSubsetOfPrunedTGFD) {
								System.out.println("The following entity has a " + (isSubsetOfPrunedTGFD ? "pruned" : "minimal") + " subset from the previous iteration:" + newDependency);
								continue;
							}
						}

						// Prune entities that are supersets of entities with no matches
						if (!tgfdDiscovery.isNaive) {
							boolean hasAPrunedSubset = isSubsetOfDependency(newDependency, prunedSetOfDependencies);
							if (hasAPrunedSubset) {
								System.out.println("The following entity has a " + (hasAPrunedSubset ? "pruned" : "minimal") + " subset:" + newDependency);
								System.out.println("Skipping this entity");
								continue;
							}
						}

						System.out.println("Performing Y discovery on pattern: " + newPattern);
//						System.out.println("Entity: " + entity);
						System.out.println("Entity: " + entityEntry.getKey());
//						ArrayList<Map.Entry<String, ArrayList<Integer>>> attrValuesTimestampsSortedByFreq = findMatchesY(graphs, newPattern, yAttrName, yVertexType, theta);
						ArrayList<Entry<ConstantLiteral, List<Integer>>> attrValuesTimestampsSortedByFreq = entityEntry.getValue();

						for (Map.Entry<ConstantLiteral, List<Integer>> entry : attrValuesTimestampsSortedByFreq) {
							System.out.println(entry.getKey() + ":" + entry.getValue());
						}

						//TO-DO: Add skipped dependencies to pruned list?
						//TO-DO: Add skipped dependencies to pruned list of TGFDs?
						if (attrValuesTimestampsSortedByFreq.size() == 0) {
//							System.out.println("Skip. No matches found for entity " + entity);
							System.out.println("Skip. No matches found for entity " + entityEntry.getKey());
							System.out.println("Mark as pruned.");
							prunedSetOfDependencies.add(newDependency);
							prunedSetOfTGFDs.add(new PatternDependencyPair(patternCopy, newDependency));
							continue;
						}

						Delta delta = null;
						double support = 0.0;
						ArrayList<Pair> candidateDeltas = new ArrayList<>(2);
						if (attrValuesTimestampsSortedByFreq.size() == 1) {
							List<Integer> timestamps = attrValuesTimestampsSortedByFreq.get(0).getValue();
							int minDistance = graphs.size() - 1;
							int maxDistance = timestamps.get(timestamps.size() - 1) - timestamps.get(0);
							for (int index = 1; index < timestamps.size(); index++) {
								minDistance = Math.min(minDistance, timestamps.get(index) - timestamps.get(index - 1));
							}
							candidateDeltas.add(new Pair(minDistance, maxDistance));
						} else if (attrValuesTimestampsSortedByFreq.size() > 1) {
							int minExclusionDistance = graphs.size() - 1;
							int maxExclusionDistance = 0;
							ArrayList<Integer> distances = new ArrayList<>();
							int l1 = attrValuesTimestampsSortedByFreq.get(0).getValue().get(0);
							int u1 = attrValuesTimestampsSortedByFreq.get(0).getValue().get(attrValuesTimestampsSortedByFreq.get(0).getValue().size() - 1);
							for (int index = 1; index < attrValuesTimestampsSortedByFreq.size(); index++) {
								int l2 = attrValuesTimestampsSortedByFreq.get(index).getValue().get(0);
								int u2 = attrValuesTimestampsSortedByFreq.get(index).getValue().get(attrValuesTimestampsSortedByFreq.get(index).getValue().size() - 1);
								distances.add(Math.abs(u2 - l1));
								distances.add(Math.abs(u1 - l2));
							}
							for (int index = 0; index < distances.size(); index++) {
								minExclusionDistance = Math.min(minExclusionDistance, distances.get(index));
								maxExclusionDistance = Math.max(maxExclusionDistance, distances.get(index));
							}

							if (minExclusionDistance > 0) {
								Pair deltaPair = new Pair(0, minExclusionDistance - 1);
								candidateDeltas.add(deltaPair);
							}
							if (maxExclusionDistance < graphs.size() - 1) {
								Pair deltaPair = new Pair(maxExclusionDistance + 1, graphs.size() - 1);
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
//								String yValue = attrValuesTimestampsSortedByFreq.get(0).getKey();
								String yValue = attrValuesTimestampsSortedByFreq.get(0).getKey().getAttrValue();
								List<Integer> timestamps = attrValuesTimestampsSortedByFreq.get(0).getValue();
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

						//TO-DO: Add skipped constant dependencies to pruned list?
						//TO-DO: Create a separate list for general and constant TGFDs?
						if (delta == null) {
//							System.out.println("Could not find support for entity" + entity);
							System.out.println("Could not find support for entity" + entityEntry.getKey());
							System.out.println("Do not expand this entity. Pruned");
							prunedSetOfDependencies.add(newDependency);
							prunedSetOfTGFDs.add(new PatternDependencyPair(patternCopy, newDependency));
							continue;
						}

						System.out.println(delta);
						System.out.println("Support = " + support);

						//TO-DO: Add minimal constant dependencies to pruned list?
						TGFD tgfd = new TGFD(newPattern, delta, newDependency, support, "");
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
					int currMax = graphs.size() - 1;
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
					}
					intersections.putIfAbsent(new Pair(currMin, currMax), currSatisfyingAttrValues);

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
							System.out.println("Could not find support for general dependency: " + dependency);
							System.out.println("Do not expand this dependency. Pruned");
							prunedSetOfDependencies.add(dependency);
							prunedSetOfTGFDs.add(new PatternDependencyPair(patternCopy, dependency));
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
						for (String xVertexType : xAttrNameMap.keySet()) {
							for (String xAttrName : xAttrNameMap.get(xVertexType)) {
								VariableLiteral x = new VariableLiteral(xVertexType, xAttrName, xVertexType, xAttrName);
								generalDependency.addLiteralToX(x);
							}
						}

						TGFD tgfd = new TGFD(newPattern, delta, generalDependency, support, "");
						System.out.println("TGFD: " + tgfd);
						tgfds.add(tgfd);
						numOfTGFDsAddedForThisNode++;
						minimalSetOfTGFDs.add(new PatternDependencyPair(patternCopy, dependency));
						System.out.println("Discovered minimal TGFD");
						System.out.println("Do not expand this dependency");
						minimalSetOfDependencies.add(dependency);
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

//	private static void createCombinationOfEntities(ArrayList<ArrayList<ConstantLiteral>> sets) {
//		// if sets.size() > 1, take first set, take first element, add it to entity, recurse(entity, set.sublist(1,size))
//		ArrayList<ConstantLiteral> set = sets.get(0);
//		for (ConstantLiteral literal : set) {
//			List<ConstantLiteral> entity = Collections.singletonList(literal);
//			recurse(entity, sets.subList(1, sets.size()));
//		}
//		// if sets.size() == 1, iterate over single set, and add each element to entity, return list of entities
//	}

	private static ArrayList<ArrayList<ConstantLiteral>> createCombinationOfEntities(List<ConstantLiteral> entity, List<ArrayList<ConstantLiteral>> sets) {
		if (sets.size() == 1) {
			ArrayList<ArrayList<ConstantLiteral>> entities = new ArrayList<>();
			for (ConstantLiteral literal : sets.get(0)) {
				ArrayList<ConstantLiteral> finalEntity = new ArrayList<>();
				for (ConstantLiteral l : entity) {
					finalEntity.add(l);
				}
				finalEntity.add(literal);
				entities.add(finalEntity);
			}
			return entities;
		} else {
			ArrayList<ConstantLiteral> set = sets.get(0);
			ArrayList<ArrayList<ConstantLiteral>> entities = new ArrayList<>();
			for (ConstantLiteral setLiteral : set) {
				ArrayList<ConstantLiteral> supersetEntity = new ArrayList<>();
				for (ConstantLiteral entityLiteral : entity) {
					supersetEntity.add(entityLiteral);
				}
				supersetEntity.add(setLiteral);
				ArrayList<ArrayList<ConstantLiteral>> newEntities = createCombinationOfEntities(entity, sets.subList(1, sets.size()));
				entities.addAll(newEntities);
			}
			return entities;
		}
	}

	public static boolean isSubsetOfDependency(VF2PatternGraph pattern, Dependency dependency, ArrayList<PatternDependencyPair> setOfDependenciesFromParent, ArrayList<PatternDependencyPair> setOfDependenciesForCurrentNode) {
		for (PatternDependencyPair subsetDependency : setOfDependenciesFromParent) {
			if (dependency.getY().containsAll(subsetDependency.dependency().getY())) {
				if (dependency.getX().containsAll(subsetDependency.dependency().getX())) {
					System.out.println("Candidate Pattern " + pattern);
					System.out.println("is a subset of minimal TGFD with ");
					System.out.println("Pattern " + subsetDependency.pattern());
					System.out.println("and");
					System.out.println("Candidate Dependency " + dependency);
					System.out.println("is a subset of minimal TGFD with ");
					System.out.println("Dependency " + subsetDependency.dependency());
					setOfDependenciesForCurrentNode.add(subsetDependency);
					return true;
				}
			}
		}
		return false;
	}

	public static boolean isSubsetOfDependency(Dependency dependency, ArrayList<Dependency> setOfSubsetDependencies) {
		for (Dependency subsetDependency : setOfSubsetDependencies) {
			if (dependency.getY().containsAll(subsetDependency.getY())) {
				if (dependency.getX().containsAll(subsetDependency.getX())) {
					System.out.println("Candidate Dependency " + dependency);
					System.out.println("is a subset of");
					System.out.println("Dependency " + subsetDependency);
					return true;
				}
			}
		}
		return false;
	}

	public static ArrayList<Entry<String, ArrayList<Integer>>> findMatchesY(ArrayList<DBPediaLoader> graphs, VF2PatternGraph pattern, String yAttrName, String yVertexType, double theta) {
		TreeMap<String, ArrayList<Integer>> yValuesTimeStamps = new TreeMap<>();
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
			System.out.println(attrVal + ":" + yValuesTimeStamps.get(attrVal));
		}

		ArrayList<Map.Entry<String, ArrayList<Integer>>> majortyAttrValues = new ArrayList<>(yValuesTimeStamps.entrySet());
		majortyAttrValues.sort(new Comparator<Entry<String, ArrayList<Integer>>>() {
			@Override
			public int compare(Entry<String, ArrayList<Integer>> o1, Entry<String, ArrayList<Integer>> o2) {
				return o2.getValue().size() - o1.getValue().size();
			}
		});

		for (Map.Entry<String, ArrayList<Integer>> entry : majortyAttrValues) {
			System.out.println(entry.getKey() + ":" + entry.getValue());
		}

		return majortyAttrValues;

	}

	public static class PatternDependencyPair {
		private VF2PatternGraph pattern;
		private Dependency dependency;

		public PatternDependencyPair(VF2PatternGraph pattern, Dependency dependency) {
			this.pattern = pattern;
			this.dependency = dependency;
		}

		public VF2PatternGraph pattern() {
			return this.pattern;
		}

		public Dependency dependency() {
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
			return "(" + min +
					", " + max +
					')';
		}
	}

	public static void discover(int k, double theta, String experimentName) {
		// TO-DO: Is it possible to prune the graphs by passing them our histogram's frequent nodes and edges as dummy TGFDs ???
//		Config.optimizedLoadingBasedOnTGFD = !tgfdDiscovery.isNaive;

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
		loadDBpediaSnapshots();
		for (int i = 0; i <= k; i++) {
			ArrayList<TGFD> tgfds = vSpawn(i, theta);
//			vSpawn(i, theta);
//			if (MAX_NUMBER_OF_ATTR_PER_VERTEX == 1 && i < 1) continue; // ignore single-node patterns because they're expensive and uninteresting
//			ArrayList<TGFD> tgfds = deltaDiscovery(i, theta);

			printTGFDstoFile(experimentName, i, theta, tgfds);

			final long kEndTime = System.currentTimeMillis();
			final long kRunTime = kEndTime - kStartTime;
			if (experimentName.startsWith("k") && kExperimentResultsFile != null) {
				System.out.println("Total execution time for k = " + k + " : " + kRunTime);
				kExperimentResultsFile.print("k = " + i);
				kExperimentResultsFile.println(", execution time = " + kRunTime);
			}

			System.gc();
		}
	}

	public static ArrayList<TGFD> vSpawnInit(double theta) {
		ArrayList<TGFD> tgfds = new ArrayList<>();

		genTree = new GenerationTree();
		genTree.addLevel();

		List<Entry<String, Integer>> sortedNodeHistogram = getSortedNodeHistogram();

		System.out.println("VSpawn Level 0");
//		for (int i = 0; i < NUM_OF_SINGLE_VERTEX_PATTERNS; i++) {
		for (int i = 0; i < sortedNodeHistogram.size(); i++) {
			String vertexType = sortedNodeHistogram.get(i).getKey();

			if (vertexTypesAttributes.get(vertexType).size() < 2)
				continue; // TO-DO: Are we interested in TGFDs where LHS is empty?

			int numOfInstancesOfVertexType = sortedNodeHistogram.get(i).getValue();
			int numOfInstancesOfAllVertexTypes = tgfdDiscovery.NUM_OF_VERTICES_IN_GRAPH;

			double patternSupport = (double) numOfInstancesOfVertexType / (double) numOfInstancesOfAllVertexTypes;
//			System.out.println("Pattern Support: " + patternSupport);

			System.out.println(vertexType);
			VF2PatternGraph candidatePattern = new VF2PatternGraph();
			PatternVertex vertex = new PatternVertex(vertexType);
			candidatePattern.addVertex(vertex);
			System.out.println("VSpawnInit with single-node pattern: " + candidatePattern);

			if (tgfdDiscovery.isNaive) {
				if (patternSupport >= PATTERN_SUPPORT_THRESHOLD) {
					GenTreeNode patternNode = genTree.createNodeAtLevel(0, candidatePattern, patternSupport, null);
					ArrayList<TGFD> hSpawnTGFDs = hSpawn2(0, theta, patternNode);
					tgfds.addAll(hSpawnTGFDs);
//					ArrayList<TreeSet<Dependency>> dependenciesSets = hSpawn(candidatePattern, patternSupport);
//					genTree.createNodeAtLevel(0, candidatePattern, dependenciesSets, patternSupport);
				} else {
					System.out.println("Pattern support of " + patternSupport + " is below threshold.");
				}
			} else {
				GenTreeNode patternNode = genTree.createNodeAtLevel(0, candidatePattern, patternSupport, null);
				ArrayList<TGFD> hSpawnTGFDs = hSpawn2(0, theta, patternNode);
				tgfds.addAll(hSpawnTGFDs);
//				ArrayList<TreeSet<Dependency>> dependenciesSets = hSpawn(candidatePattern, patternSupport);
//				genTree.createNodeAtLevel(0, candidatePattern, dependenciesSets, patternSupport);
			}
			System.gc();
		}
		System.out.println("GenTree Level " + 0 + " size: " + genTree.getLevel(0).size());
		for (GenTreeNode node : genTree.getLevel(0)) {
			System.out.println("Pattern: " + node.getPattern());
			System.out.println("Pattern Support: " + node.getPatternSupport());
//			System.out.println("Dependency: " + node.getDependenciesSets());
		}

		System.out.println("Performing HSpawn");

		return tgfds;
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
	public static ArrayList<TGFD> vSpawn(int i, double theta) {

		if (i == 0) {
			ArrayList<TGFD> tgfds = new ArrayList<>();
			tgfds = vSpawnInit(theta);
			System.out.println("TGFD count for k=" + i + ": " + tgfds.size());
			return tgfds;
		}

		ArrayList<TGFD> tgfds = new ArrayList<>();

		List<Entry<String, Integer>> sortedUniqueEdgesHist = getSortedEdgeHistogram();

		System.out.println("Performing VSpawn");
//		for (int i = 1; i <= k; i++) {
		System.out.println("VSpawn Level " + i);
		System.gc();
		ArrayList<GenTreeNode> previousLevel = genTree.getLevel(i - 1);
		genTree.addLevel();
		for (GenTreeNode node : previousLevel) {
			System.out.println("Performing VSpawn on pattern: " + node.getPattern());

			node.deleteDependenciesSets();

			System.out.println("Level " + (i - 1) + " pattern: " + node.getPattern());
			if (node.isPruned() && !tgfdDiscovery.isNaive) {
				System.out.println("Marked as pruned. Skip.");
				continue;
			}

			for (Map.Entry<String, Integer> candidateEdge : sortedUniqueEdgesHist) {
				String candidateEdgeString = candidateEdge.getKey();
				System.out.println("Candidate edge:" + candidateEdgeString);
				if (tgfdDiscovery.isNaive && (1.0 * uniqueEdgesHist.get(candidateEdgeString) / NUM_OF_EDGES_IN_GRAPH) < PATTERN_SUPPORT_THRESHOLD) {
					System.out.println("Below pattern support threshold. Skip");
					continue;

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
						if (vertexTypesAttributes.get(targetVertexType).size() == 0) continue;
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

					for (RelationshipEdge tempE : newPattern.getPattern().edgeSet()) {
						String sourceType = (new ArrayList<>(tempE.getSource().getTypes())).get(0);
						String targetType = (new ArrayList<>(tempE.getTarget().getTypes())).get(0);
						String uniqueEdge = sourceType + " " + tempE.getLabel() + " " + targetType;
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
					System.out.println("Performing HSpawn");
//					ArrayList<TreeSet<Dependency>> literalTreeList = hSpawn(newPattern, patternSupport);
//					System.gc();
//					genTree.createNodeAtLevel(i, newPattern, literalTreeList, patternSupport, node);
					GenTreeNode patternNode = genTree.createNodeAtLevel(i, newPattern, patternSupport, node);
					ArrayList<TGFD> hSpawnTGFDs = hSpawn2(i, theta, patternNode);
					tgfds.addAll(hSpawnTGFDs);
				}
			}
		}
		System.out.println("GenTree Level " + i + " size: " + genTree.getLevel(i).size());
		for (GenTreeNode node : genTree.getLevel(i)) {
			System.out.println("Pattern: " + node.getPattern());
			System.out.println("Pattern Support: " + node.getPatternSupport());
		}
		System.out.println("TGFD count for k=" + i + ": " + tgfds.size());
		return tgfds;
//		}
	}

	public static ArrayList<TreeSet<Dependency>> hSpawn(VF2PatternGraph patternGraph, double patternSupport) {
		HashMap<String, HashSet<String>> patternVerticesAttributes = new HashMap<>();
		for (Vertex vertex : patternGraph.getPattern().vertexSet()) {
			for (String vertexType : vertex.getTypes()) {
				patternVerticesAttributes.put(vertexType, new HashSet<>());
				Set<String> attrNameSet = vertexTypesAttributes.get(vertexType);
				for (String attrName : attrNameSet) {
					patternVerticesAttributes.get(vertexType).add(attrName);
				}
			}
		}

		HashSet<ConstantLiteral> attrNamesSet = new HashSet<>();
		for (String vertexType : patternVerticesAttributes.keySet()) {
			for (String attrName : patternVerticesAttributes.get(vertexType)) {
				ConstantLiteral literal = new ConstantLiteral(vertexType, attrName, null);
				attrNamesSet.add(literal);
			}
		}

		// This creates a tree of size n! For e.g. if n = 15, tree size will be 1,307,674,368,000
		// TO-DO: Instead, what if we did DeltaDiscovery at each level of HSpawn(i,j) instead of after HSpawn?
//		LiteralTree literalTree = literalTreeGeneration(attrNamesSet);

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
						String vType = ((ConstantLiteral) l).getVertexType();
						String attrName = ((ConstantLiteral) l).getAttrName();
						o1Set.add(vType + "." + attrName);
					}
					HashSet<String> o2Set = new HashSet<>();
					for (Literal l : o2.getX()) {
						String vType = ((ConstantLiteral) l).getVertexType();
						String attrName = ((ConstantLiteral) l).getAttrName();
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
//		for (TreeSet<Dependency> set : dependenciesSets) {
//			numberOfDependencies += set.size();
//			for (Dependency dependency : set) {
//				System.out.println(dependency);
//			}
//		}
		System.out.println("Number of dependencies generated: " + numberOfDependencies);

		return dependenciesSets;

	}

	public static LiteralTree literalTreeGeneration(HashSet<ConstantLiteral> literals) {
		LiteralTree literalTree = new LiteralTree();
		for (int i = 0; i < literals.size(); i++) {
			literalTree.addLevel();
			if (i == 0) {
				for (ConstantLiteral literal: literals) {
					literalTree.createNodeAtLevel(i, literal, null);
				}
			} else {
				for (LiteralTreeNode previousLevelLiteral : literalTree.getLevel(i-1)) {
					for (ConstantLiteral literal: literals) {
						boolean existsInPath = false;
						LiteralTreeNode parent = previousLevelLiteral;
						while (parent != null) {
							if (parent.getLiteral().equals(literal)) {
								existsInPath = true;
								break;
							} else {
								parent = parent.getParent();
							}
						}
						if (existsInPath) continue;
						literalTree.createNodeAtLevel(i, literal, previousLevelLiteral);
					}
				}
			}
		}
		return literalTree;
	}
}

class LiteralTree {
	private ArrayList<ArrayList<LiteralTreeNode>> tree;
	public LiteralTree() {
		tree = new ArrayList<>();
	}

	public void addLevel() {
		tree.add(new ArrayList<>());
		System.out.println("LiteralTree levels: " + tree.size());
	}

	public LiteralTreeNode createNodeAtLevel(int level, ConstantLiteral literal, LiteralTreeNode parentNode) {
		LiteralTreeNode node = new LiteralTreeNode(literal, parentNode);
		tree.get(level).add(node);
		return node;
	}

	public ArrayList<LiteralTreeNode> getLevel(int i) {
		return tree.get(i);
	}
}

	class GenerationTree {
		private ArrayList<ArrayList<GenTreeNode>> tree;

		public GenerationTree() {
			tree = new ArrayList<>();
		}

		public void addLevel() {
			tree.add(new ArrayList<>());
			System.out.println("GenerationTree levels: " + tree.size());
		}

		public GenTreeNode createNodeAtLevel(int level, VF2PatternGraph pattern, double patternSupport, GenTreeNode parentNode) {
			GenTreeNode node = new GenTreeNode(pattern, patternSupport, parentNode);
			tree.get(level).add(node);
			return node;
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
		private ArrayList<tgfdDiscovery.PatternDependencyPair> prunedSetOfConstantTGFDs = new ArrayList<>();
		private ArrayList<ArrayList<ConstantLiteral>> zeroEntityDependencies = new ArrayList<ArrayList<ConstantLiteral>>();
		private ArrayList<ArrayList<ConstantLiteral>> minimalDependencies = new ArrayList<>();

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

		public GenTreeNode(VF2PatternGraph pattern, double patternSupport, GenTreeNode parentNode) {
			this.pattern = pattern;
			this.patternSupport = patternSupport;
			this.parentNode = parentNode;
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

		public void deleteDependenciesSets() {
			this.dependenciesSets = null;
		}

		public void setIsPruned() {
			this.isPruned = true;
		}

		public boolean isPruned() {
			return this.isPruned;
		}

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

		public void setPrunedSetOfConstantTGFDs(ArrayList<tgfdDiscovery.PatternDependencyPair> prunedSetOfConstantTGFDs) {
			this.prunedSetOfConstantTGFDs = prunedSetOfConstantTGFDs;
		}

		public ArrayList<tgfdDiscovery.PatternDependencyPair> getPrunedSetOfConstantTGFDs() {
			return this.prunedSetOfConstantTGFDs;
		}

		public void addZeroEntityDependency(ArrayList<ConstantLiteral> dependency) {
			this.zeroEntityDependencies.add(dependency);
		}

		public ArrayList<ArrayList<ConstantLiteral>> getZeroEntityDependencies() {
			return this.zeroEntityDependencies;
		}

		public void addMinimalDependency(ArrayList<ConstantLiteral> dependency) {
			this.minimalDependencies.add(dependency);
		}

		public ArrayList<ArrayList<ConstantLiteral>> getMinimalDependencies() {
			return this.minimalDependencies;
		}
	}

//	class LiteralTree {
//		private LiteralTreeNode root;
//
//		public LiteralTree(String vertexType, String attributeLabel, Map<String, HashSet<String>> patternVerticesAttributes) {
//			Map<String, HashSet<String>> copy = (Map<String, HashSet<String>>) patternVerticesAttributes.get(vertexType).clone();
//			copy.remove(attributeLabel);
//			this.root = new LiteralTreeNode(vertexType, new Attribute(attributeLabel), copy);
//		}
//
//		public LiteralTreeNode getRoot() {
//			return this.root;
//		}
//	}

	class LiteralTreeNode {
		private LiteralTreeNode parent = null;
//		String vertexType;
//		Attribute attribute;
		private ConstantLiteral literal;
		ArrayList<LiteralTreeNode> children;
		private boolean isPruned = false;

		public LiteralTreeNode(ConstantLiteral literal, LiteralTreeNode parent) {
			this.literal = literal;
			this.parent = parent;
		}

		// TO-DO: Recursion takes up too much memory. However, if we can find an efficient way to do it, it will save a lot of subset checks in Delta Discovery
		public LiteralTreeNode(ConstantLiteral literal, HashSet<ConstantLiteral> otherLiterals) {
			this.literal = literal;
			HashSet<ConstantLiteral> subset = new HashSet<>(otherLiterals);
			subset.remove(literal);
			for (ConstantLiteral otherLiteral : subset) {
				this.children.add(new LiteralTreeNode(otherLiteral, subset));
			}
		}

		public ArrayList<ConstantLiteral> getPathToRoot() {
			ArrayList<ConstantLiteral> literalPath = new ArrayList<>();
			literalPath.add(literal);
			LiteralTreeNode parentLiteralNode = parent;
			while (parentLiteralNode != null) {
				literalPath.add(parentLiteralNode.getLiteral());
				parentLiteralNode = parentLiteralNode.getParent();
			}
			return literalPath;
		}

		public ConstantLiteral getLiteral() {
			return this.literal;
		}

		public LiteralTreeNode getParent() {
			return this.parent;
		}

		public ArrayList<LiteralTreeNode> getChildren() {
			return this.children;
		}

		public void setIsPruned() {
			this.isPruned = true;
		}

		public boolean isPruned() {
			return this.isPruned;
		}

//		public String toString() {
//			StringBuilder buffer = new StringBuilder(50);
//			print(buffer, "", "");
//			return buffer.toString();
//		}

		// TO-DO: CITE THIS?
//		private void print(StringBuilder buffer, String prefix, String childrenPrefix) {
//			buffer.append(prefix);
////			buffer.append(vertexType).append(".").append(attribute.getAttrName());
//			buffer.append(nodeLiteral.getVertexType()).append(".").append(nodeLiteral.getAttrName());
//			buffer.append('\n');
//			for (Iterator<LiteralTreeNode> it = children.iterator(); it.hasNext(); ) {
//				LiteralTreeNode next = it.next();
//				if (it.hasNext()) {
//					next.print(buffer, childrenPrefix + "├── ", childrenPrefix + "│   ");
//				} else {
//					next.print(buffer, childrenPrefix + "└── ", childrenPrefix + "    ");
//				}
//			}
//		}
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