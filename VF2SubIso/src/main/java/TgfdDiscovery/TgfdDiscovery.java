package TgfdDiscovery;

import IncrementalRunner.IncUpdates;
import IncrementalRunner.IncrementalChange;
import VF2Runner.VF2SubgraphIsomorphism;
import changeExploration.Change;
import changeExploration.ChangeLoader;
import graphLoader.DBPediaLoader;
import infra.*;
import org.apache.commons.cli.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.jetbrains.annotations.NotNull;
import org.jgrapht.GraphMapping;
import org.jgrapht.alg.isomorphism.VF2AbstractIsomorphismInspector;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

public class TgfdDiscovery {
	private final int numOfSnapshots;
	public static final double DEFAULT_PATTERN_SUPPORT_THRESHOLD = 0.001;
	public static final int DEFAULT_GAMMA = 20;
	public static final int DEFAULT_K = 3;
	public static final double DEFAULT_THETA = 0.7;
	private Integer NUM_OF_EDGES_IN_GRAPH;
	public int NUM_OF_VERTICES_IN_GRAPH;
	public Map<String, Integer> uniqueVertexTypesHist; // freq nodes come from here
	public Map<String, HashSet<String>> vertexTypesAttributes; // freq attributes come from here
	public Map<String, Integer> uniqueEdgesHist; // freq edges come from here
	public PatternTree patternTree;
	public boolean isNaive;
	public Long graphSize = null;
	private final boolean interestingTGFDs;
	private final int k;
	private final double theta;
	private final int gamma;
	private final double patternSupportThreshold;
	private HashSet<String> activeAttributesSet;
	private int previousLevelNodeIndex = 0;
	private int candidateEdgeIndex = 0;
	private int currentVSpawnLevel = 1;
	private final ArrayList<ArrayList<TGFD>> tgfds;
	private final long startTime;
	private ArrayList<Long> kRuntimes = new ArrayList<>();
	private String timeAndDateStamp = null;
	private boolean isKExperiment = false;
	private boolean useChangeFile;
	private ArrayList<Model> models = new ArrayList<>();
	private HashMap<String, org.json.simple.JSONArray> changeFilesMap = new HashMap<>();

	public TgfdDiscovery(int numOfSnapshots) {
		this.startTime = System.currentTimeMillis();
		this.k = DEFAULT_K;
		this.theta = DEFAULT_THETA;
		this.gamma = DEFAULT_GAMMA;
		this.numOfSnapshots = numOfSnapshots;
		this.patternSupportThreshold = DEFAULT_PATTERN_SUPPORT_THRESHOLD;
		this.isNaive = false;
		this.interestingTGFDs = true;
		this.useChangeFile = false;

		System.out.println("Running experiment for |G|="+this.graphSize+", k="+this.k+", theta="+this.theta+", gamma"+this.gamma+", patternSupport="+this.patternSupportThreshold+", interesting="+this.interestingTGFDs+", optimized="+!this.isNaive);

		this.tgfds = new ArrayList<>();
		for (int vSpawnLevel = 0; vSpawnLevel <= k; vSpawnLevel++) {
			tgfds.add(new ArrayList<>());
		}
	}

	public TgfdDiscovery(int k, double theta, int gamma, Long graphSize, double patternSupport, int numOfSnapshots, boolean isNaive, boolean interestingTGFDsOnly, boolean useChangeFile) {
		this.startTime = System.currentTimeMillis();
		this.k = k;
		this.theta = theta;
		this.gamma = gamma;
		this.graphSize = graphSize;
		this.numOfSnapshots = numOfSnapshots;
		this.patternSupportThreshold = patternSupport;
		this.isNaive = isNaive;
		this.interestingTGFDs = interestingTGFDsOnly;
		this.useChangeFile = useChangeFile;

		System.out.println("Running experiment for |G|="+this.graphSize+", k="+this.k+", theta="+this.theta+", gamma"+this.gamma+", patternSupport="+this.patternSupportThreshold+", interesting="+this.interestingTGFDs+", optimized="+!this.isNaive);

		this.tgfds = new ArrayList<>();
		for (int vSpawnLevel = 0; vSpawnLevel <= k; vSpawnLevel++) {
			tgfds.add(new ArrayList<>());
		}
	}

	public void initialize() {
		vSpawnInit();
		this.kRuntimes.add(System.currentTimeMillis() - this.startTime);
		this.patternTree.addLevel();
	}

	@Override
	public String toString() {
		return (this.isNaive ? "naive" : "optimized") +
				(this.graphSize == null ? "" : "-G"+this.graphSize) +
				(this.interestingTGFDs ? "-interesting" : "") +
				"-k" + this.currentVSpawnLevel +
				"-theta" + String.format("%.1f", this.theta) +
				"-a" + this.gamma +
				"-pTheta" + this.patternSupportThreshold +
				(this.useChangeFile ? "-changefile" : "") +
				(this.timeAndDateStamp == null ? "" : ("-"+this.timeAndDateStamp));
	}

	public void printTgfdsToFile(String experimentName, ArrayList<TGFD> tgfds) {
		tgfds.sort(new Comparator<TGFD>() {
			@Override
			public int compare(TGFD o1, TGFD o2) {
				return o2.getSupport().compareTo(o1.getSupport());
			}
		});
		System.out.println("Printing TGFDs to file for k = " + this.currentVSpawnLevel);
		try {
			PrintStream printStream = new PrintStream(experimentName + "-tgfds-" + this + ".txt");
			printStream.println("k = " + this.currentVSpawnLevel);
			printStream.println("# of TGFDs generated = " + tgfds.size());
			for (TGFD tgfd : tgfds) {
				printStream.println(tgfd);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		System.gc();
	}

	public void printExperimentRuntimestoFile(String experimentName, ArrayList<Long> runtimes) {
		try {
			PrintStream printStream = new PrintStream(experimentName + "-experiments-runtimes-" + this + ".txt");
			for (int i  = 0; i < runtimes.size(); i++) {
				printStream.print("k = " + i);
				printStream.println(", execution time = " + runtimes.get(i));
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {

		Options options = new Options();
		options.addOption("console", false, "print to console");
		options.addOption("naive", false, "run naive version of algorithm");
		options.addOption("interesting", false, "run algorithm and only consider interesting TGFDs");
		options.addOption("g", true, "run experiment on a specific graph size");
		options.addOption("k", true, "run experiment for k iterations");
		options.addOption("a", true, "run experiment for specified active attribute set size");
		options.addOption("theta", true, "run experiment using a specific support threshold");
		options.addOption("K", false, "run experiment for k = 1 to 5");
		options.addOption("p", true, "run experiment using specific pattern support threshold");
		options.addOption("changefile", false, "run experiment using changefiles instead of snapshots");

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		assert cmd != null;

		String timeAndDateStamp = ZonedDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("uuuu.MM.dd.HH.mm.ss"));
		if (!cmd.hasOption("console")) {
			PrintStream logStream = null;
			try {
				logStream = new PrintStream("tgfd-discovery-log-" + timeAndDateStamp + ".txt");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			System.setOut(logStream);
		}

		boolean isNaive = cmd.hasOption("naive");
		boolean interestingTGFDs = cmd.hasOption("interesting");
		boolean useChangeFile = cmd.hasOption("changefile");

		Long graphSize = null;
		if (cmd.getOptionValue("g") != null) {
			graphSize = Long.parseLong(cmd.getOptionValue("g"));
		}
		int gamma = cmd.getOptionValue("a") == null ? DEFAULT_GAMMA : Integer.parseInt(cmd.getOptionValue("a"));
		double theta = cmd.getOptionValue("theta") == null ? DEFAULT_THETA : Double.parseDouble(cmd.getOptionValue("theta"));
		int k = cmd.getOptionValue("k") == null ? DEFAULT_K : Integer.parseInt(cmd.getOptionValue("k"));
		double patternSupportThreshold = cmd.getOptionValue("p") == null ? DEFAULT_PATTERN_SUPPORT_THRESHOLD : Double.parseDouble(cmd.getOptionValue("p"));

		TgfdDiscovery tgfdDiscovery = new TgfdDiscovery(k, theta, gamma, graphSize, patternSupportThreshold, 3, isNaive, interestingTGFDs, useChangeFile);
		tgfdDiscovery.histogram();

		ArrayList<DBPediaLoader> graphs = null;
		if (!tgfdDiscovery.useChangeFile) {
			graphs = tgfdDiscovery.loadDBpediaSnapshots(graphSize);
		} else {
			tgfdDiscovery.loadModels(graphSize);
		}

		tgfdDiscovery.setExperimentDateAndTimeStamp(timeAndDateStamp);
		tgfdDiscovery.initialize();
		if (cmd.hasOption("K")) tgfdDiscovery.markAsKexperiment();
		while (tgfdDiscovery.currentVSpawnLevel <= tgfdDiscovery.k) {

			System.out.println("VSpawn level " + tgfdDiscovery.currentVSpawnLevel);
			System.out.println("Previous level node index " + tgfdDiscovery.previousLevelNodeIndex);
			System.out.println("Candidate edge index " + tgfdDiscovery.candidateEdgeIndex);

			PatternTreeNode patternTreeNode = null;
			while (patternTreeNode == null && tgfdDiscovery.currentVSpawnLevel <= tgfdDiscovery.k) {
				patternTreeNode = tgfdDiscovery.vSpawn();
			}
			if (tgfdDiscovery.currentVSpawnLevel > tgfdDiscovery.k) break;
			ArrayList<ArrayList<HashSet<ConstantLiteral>>> matches = new ArrayList<>();
			for (int timestamp = 0; timestamp < tgfdDiscovery.numOfSnapshots; timestamp++) {
				matches.add(new ArrayList<>());
			}
			double realPatternSupport;
			if (graphs != null) { // TO-DO: Investigate - why is there a slight discrepancy between the # of matches found via snapshot vs. changefile?
				final long startTime = System.currentTimeMillis();
				// TO-DO: For full-sized dbpedia, can we store the models and create an optimized graph for every search?
				realPatternSupport = tgfdDiscovery.getMatchesForPattern(graphs, patternTreeNode, matches); // this can be called repeatedly on many graphs
				printWithTime("getMatchesForPattern", (System.currentTimeMillis() - startTime));
			}
			else {
				final long startTime = System.currentTimeMillis();
				realPatternSupport = tgfdDiscovery.getMatchesForPattern2(patternTreeNode, matches);
				printWithTime("getMatchesForPattern2", (System.currentTimeMillis() - startTime));
			}
//			return;
			if (realPatternSupport < tgfdDiscovery.patternSupportThreshold) {
				System.out.println("Mark as pruned. Real pattern support too low for pattern " + patternTreeNode.getPattern());
				patternTreeNode.setIsPruned();
				continue;
			}
			final long startTime = System.currentTimeMillis();
			ArrayList<TGFD> tgfds = tgfdDiscovery.hSpawn(patternTreeNode, matches);
			printWithTime("hSpawn", (System.currentTimeMillis() - startTime));
			tgfdDiscovery.tgfds.get(tgfdDiscovery.currentVSpawnLevel).addAll(tgfds);
		}
	}

	private void markAsKexperiment() {
		this.isKExperiment = true;
	}

	private void setExperimentDateAndTimeStamp(String timeAndDateStamp) {
		this.timeAndDateStamp = timeAndDateStamp;
	}

	public void computeVertexHistogram() {

		System.out.println("Computing Node Histogram");

		Map<String, Integer> vertexTypeHistogram = new HashMap<>();
//		Map<String, Map<String, Integer>> tempVertexAttrFreqMap = new HashMap<>();
		Map<String, Set<String>> tempVertexAttrFreqMap = new HashMap<>();
		Map<String, String> vertexNameToTypeMap = new HashMap<>();
		Model model = ModelFactory.createDefaultModel();

		for (int i = 5; i < 8; i++) {
			String fileSuffix = this.graphSize == null ? "" : "-" + this.graphSize;
			String fileName = "201" + i + "types" + fileSuffix + ".ttl";
			Path input = Paths.get(fileName);
			System.out.println("Reading " + fileName);
			model.read(input.toUri().toString());
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
				vertexNameToTypeMap.putIfAbsent(vertexName, vertexType); // Every vertex in DBpedia only has one type?
			}
		}

		this.NUM_OF_VERTICES_IN_GRAPH = 0;
		for (Map.Entry<String, Integer> entry : vertexTypeHistogram.entrySet()) {
			this.NUM_OF_VERTICES_IN_GRAPH += entry.getValue();
		}
		System.out.println("Number of vertices in graph: " + this.NUM_OF_VERTICES_IN_GRAPH);

		this.uniqueVertexTypesHist = vertexTypeHistogram;

		computeAttrHistogram(vertexNameToTypeMap, tempVertexAttrFreqMap);
		computeEdgeHistogram(vertexNameToTypeMap);

	}

	//	public static void computeAttrHistogram(Map<String, String> nodesRecord, Map<String, Map<String, Integer>> tempVertexAttrFreqMap) {
	public void computeAttrHistogram(Map<String, String> nodesRecord, Map<String, Set<String>> tempVertexAttrFreqMap) {
		System.out.println("Computing attributes histogram");

		Map<String, Integer> attrHistMap = new HashMap<>();
		Map<String, Set<String>> attrDistributionMap = new HashMap<>();

		Model model = ModelFactory.createDefaultModel();
		for (int i = 5; i < 8; i++) {
			String fileSuffix = this.graphSize == null ? "" : "-" + this.graphSize;
			String fileName = "201" + i + "literals" + fileSuffix + ".ttl";
			Path input = Paths.get(fileName);
			System.out.println("Reading " + fileName);
			model.read(input.toUri().toString());
		}
		StmtIterator typeTriples = model.listStatements();
		int numOfAttributes = 0;
		while (typeTriples.hasNext()) {
			numOfAttributes++;
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
					tempVertexAttrFreqMap.get(vertexType).add(attrName);
				}
				if (!attrDistributionMap.containsKey(attrName)) {
					attrDistributionMap.put(attrName, new HashSet<>());
				} else {
					attrDistributionMap.get(attrName).add(vertexType);
				}
			}
		}
		System.out.println("Number of attributes in graph: " + numOfAttributes);

		ArrayList<Entry<String,Set<String>>> sortedAttrDistributionMap = new ArrayList<>(attrDistributionMap.entrySet());
		if (!this.isNaive) {
			sortedAttrDistributionMap.sort(new Comparator<Entry<String, Set<String>>>() {
				@Override
				public int compare(Entry<String, Set<String>> o1, Entry<String, Set<String>> o2) {
					return o2.getValue().size() - o1.getValue().size();
				}
			});
		}
		HashSet<String> mostDistributedAttributesSet = new HashSet<>();
		for (Entry<String, Set<String>> attrNameEntry : sortedAttrDistributionMap.subList(0, Math.min(this.gamma, sortedAttrDistributionMap.size()))) {
			mostDistributedAttributesSet.add(attrNameEntry.getKey());
		}
		this.activeAttributesSet = mostDistributedAttributesSet;

//		tgfdDiscovery.attrHist = attrHistMap;
		ArrayList<Entry<String, Integer>> sortedHistogram = new ArrayList<>(attrHistMap.entrySet());
		if (!this.isNaive) {
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
				if (mostDistributedAttributesSet.contains(attrName)) { // Filters non-frequent attributes
					vertexTypesAttributes.get(vertexType).add(attrName);
				}
			}
		}


		this.vertexTypesAttributes = vertexTypesAttributes;
	}

	public void computeEdgeHistogram(Map<String, String> nodesRecord) {
		System.out.println("Computing edges histogram");

		Map<String, Integer> edgesHist = new HashMap<>();

		Model model = ModelFactory.createDefaultModel();
		for (int i = 5; i < 8; i++) {
			String fileSuffix = this.graphSize == null ? "" : "-" + this.graphSize;
			String fileName = "201" + i + "objects" + fileSuffix + ".ttl";
			Path input = Paths.get(fileName);
			System.out.println("Reading " + fileName);
			model.read(input.toUri().toString());

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
		}

		this.NUM_OF_EDGES_IN_GRAPH = 0;
		for (Map.Entry<String, Integer> entry : edgesHist.entrySet()) {
			this.NUM_OF_EDGES_IN_GRAPH += entry.getValue();
		}
		System.out.println("Number of edges in graph: " + this.NUM_OF_EDGES_IN_GRAPH);

		this.uniqueEdgesHist = edgesHist;
	}

	public List<Entry<String, Integer>> getSortedFrequentVetexTypesHistogram() {
		ArrayList<Entry<String, Integer>> sortedVertexTypeHistogram = new ArrayList<>(this.uniqueVertexTypesHist.entrySet());
		sortedVertexTypeHistogram.sort(new Comparator<Entry<String, Integer>>() {
			@Override
			public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
				return o2.getValue() - o1.getValue();
			}
		});
		int size = 0;
		for (Entry<String, Integer> entry : sortedVertexTypeHistogram) {
			if (1.0 * entry.getValue() / NUM_OF_VERTICES_IN_GRAPH >= this.patternSupportThreshold) {
				size++;
			} else {
				break;
			}
		}
		if (this.isNaive) {
//			return (new ArrayList<>(uniqueVertexTypesHist.entrySet())).subList(0, size);
			return (new ArrayList<>(this.uniqueVertexTypesHist.entrySet()));
		}
		return sortedVertexTypeHistogram.subList(0, size);
	}

	public List<Entry<String, Integer>> getSortedFrequentEdgeHistogram() {
		ArrayList<Entry<String, Integer>> sortedEdgesHist = new ArrayList<>(this.uniqueEdgesHist.entrySet());
		sortedEdgesHist.sort(new Comparator<Entry<String, Integer>>() {
			@Override
			public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
				return o2.getValue() - o1.getValue();
			}
		});
		int size = 0;
		for (Entry<String, Integer> entry : sortedEdgesHist) {
			if (1.0 * entry.getValue() / NUM_OF_EDGES_IN_GRAPH >= this.patternSupportThreshold) {
				size++;
			} else {
				break;
			}
		}
		if (this.isNaive) {
//			return (new ArrayList<>(uniqueEdgesHist.entrySet())).subList(0, size);
			return (new ArrayList<>(uniqueEdgesHist.entrySet()));
		}
		return sortedEdgesHist.subList(0, size);
	}

	public void histogram() {
		computeVertexHistogram();
		printHistogram();
	}

	public void printHistogram() {
		List<Entry<String, Integer>> sortedHistogram = getSortedFrequentVetexTypesHistogram();
		List<Entry<String, Integer>> sortedEdgeHistogram = getSortedFrequentEdgeHistogram();

		System.out.println("Number of vertex types: " + sortedHistogram.size());
		System.out.println("Frequent Vertices:");
		for (Entry<String, Integer> entry : sortedHistogram) {
			String vertexType = entry.getKey();
			Set<String> attributes = this.vertexTypesAttributes.get(vertexType);
			System.out.println(vertexType + "={count=" + entry.getValue() + ", support=" + (1.0 * entry.getValue() / NUM_OF_VERTICES_IN_GRAPH) + ", attributes=" + attributes + "}");
		}

		System.out.println();
		System.out.println("Size of active attributes set: " + this.activeAttributesSet.size());
		System.out.println("Attributes:");
		for (String attrName : this.activeAttributesSet) {
			System.out.println(attrName);
		}
		System.out.println();
		System.out.println("Number of edge types: " + sortedEdgeHistogram.size());
		System.out.println("Frequent Edges:");
		for (Entry<String, Integer> entry : sortedEdgeHistogram) {
			System.out.println("edge=\"" + entry.getKey() + "\", count=" + entry.getValue() + ", support=" +(1.0 * entry.getValue() / NUM_OF_EDGES_IN_GRAPH));
		}
		System.out.println();
	}

	public static Map<Set<ConstantLiteral>, ArrayList<Entry<ConstantLiteral, List<Integer>>>> findEntities(List<ConstantLiteral> attributes, ArrayList<ArrayList<HashSet<ConstantLiteral>>> matchesPerTimestamps) {
		String yVertexType = attributes.get(attributes.size()-1).getVertexType();
		String yAttrName = attributes.get(attributes.size()-1).getAttrName();
		List<ConstantLiteral> xAttributes = attributes.subList(0,attributes.size()-1);
		Map<Set<ConstantLiteral>, Map<ConstantLiteral, List<Integer>>> entitiesWithRHSvalues = new HashMap<>();
		int t = 2015;
		for (ArrayList<HashSet<ConstantLiteral>> matchesInOneTimeStamp : matchesPerTimestamps) {
			System.out.println("---------- Attribute values in " + t + " ---------- ");
			int numOfMatches = 0;
			if (matchesInOneTimeStamp.size() > 0) {
				for(HashSet<ConstantLiteral> match : matchesInOneTimeStamp) {
					if (match.size() < attributes.size()) continue;
					Set<ConstantLiteral> entity = new HashSet<>();
					ConstantLiteral rhs = null;
					for (ConstantLiteral literalInMatch : match) {
						if (literalInMatch.getVertexType().equals(yVertexType) && literalInMatch.getAttrName().equals(yAttrName)) {
							rhs = new ConstantLiteral(literalInMatch.getVertexType(), literalInMatch.getAttrName(), literalInMatch.getAttrValue());
							continue;
						}
						for (ConstantLiteral attibute : xAttributes) {
							if (literalInMatch.getVertexType().equals(attibute.getVertexType()) && literalInMatch.getAttrName().equals(attibute.getAttrName())) {
								entity.add(new ConstantLiteral(literalInMatch.getVertexType(), literalInMatch.getAttrName(), literalInMatch.getAttrValue()));
							}
						}
					}
					if (entity.size() < xAttributes.size() || rhs == null) continue;

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

	public HashSet<ConstantLiteral> getActiveAttributesInPattern(Set<Vertex> vertexSet, boolean considerURI) {
		HashMap<String, HashSet<String>> patternVerticesAttributes = new HashMap<>();
		for (Vertex vertex : vertexSet) {
			for (String vertexType : vertex.getTypes()) {
				patternVerticesAttributes.put(vertexType, new HashSet<>());
				Set<String> attrNameSet = this.vertexTypesAttributes.get(vertexType);
				for (String attrName : attrNameSet) {
					patternVerticesAttributes.get(vertexType).add(attrName);
				}
			}
		}
		HashSet<ConstantLiteral> literals = new HashSet<>();
		for (String vertexType : patternVerticesAttributes.keySet()) {
			if (considerURI) literals.add(new ConstantLiteral(vertexType,"uri",null));
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
				System.out.println("This literal path was already visited.");
				return true;
			}
		}
		return false;
	}

	public static boolean isSupersetPath(ArrayList<ConstantLiteral> path, ArrayList<ArrayList<ConstantLiteral>> prunedPaths) {
		for (ArrayList<ConstantLiteral> prunedPath : prunedPaths) {
			if (path.get(path.size()-1).equals(prunedPath.get(prunedPath.size()-1)) && path.subList(0, path.size()-1).containsAll(prunedPath.subList(0,prunedPath.size()-1))) {
				System.out.println("Candidate path " + path + " is a superset of pruned path " + prunedPath);
				return true;
			}
		}
		return false;
	}

	public ArrayList<TGFD> deltaDiscovery(double theta, PatternTreeNode patternNode, LiteralTreeNode literalTreeNode, ArrayList<ConstantLiteral> literalPath, ArrayList<ArrayList<HashSet<ConstantLiteral>>> matchesPerTimestamps) {
		ArrayList<TGFD> tgfds = new ArrayList<>();

		// Add dependency attributes to pattern
		// TO-DO: Fix - when multiple vertices in a pattern have the same type, attribute values get overwritten
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
		Map<Set<ConstantLiteral>, ArrayList<Entry<ConstantLiteral, List<Integer>>>> entities = findEntities(literalPath, matchesPerTimestamps);
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
		ArrayList<TGFD> generalTGFD = discoverGeneralTGFD(theta, patternNode, patternNode.getPatternSupport(), literalPath, entities.size(), constantXdeltas, satisfyingAttrValues);
		if (generalTGFD.size() > 0) {
			System.out.println("Marking literal node as pruned. Discovered general TGFDs for this dependency.");
			literalTreeNode.setIsPruned();
			patternNode.addMinimalDependency(literalPath);
		}
		tgfds.addAll(generalTGFD);

		return tgfds;
	}

	private ArrayList<TGFD> discoverGeneralTGFD(double theta, PatternTreeNode patternTreeNode, double patternSupport, ArrayList<ConstantLiteral> literalPath, int entitiesSize, ArrayList<Pair> constantXdeltas, ArrayList<TreeSet<Pair>> satisfyingAttrValues) {

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
		int currMax = this.numOfSnapshots - 1;
		// TO-DO: Verify if TreeSet<Pair> is being sorted correctly
		ArrayList<TreeSet<Pair>> currSatisfyingAttrValues = new ArrayList<>();
		for (int index = 0; index < constantXdeltas.size(); index++) {
			Pair deltaPair = constantXdeltas.get(index);
			if (Math.max(currMin, deltaPair.min()) <= Math.min(currMax, deltaPair.max())) {
				currMin = Math.max(currMin, deltaPair.min());
				currMax = Math.min(currMax, deltaPair.max());
				currSatisfyingAttrValues.add(satisfyingAttrValues.get(index)); // By axiom 4
			} else {
				intersections.putIfAbsent(new Pair(currMin, currMax), currSatisfyingAttrValues);
				currSatisfyingAttrValues = new ArrayList<>();
				currMin = 0;
				currMax = this.numOfSnapshots - 1;
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

		System.out.println("Evaluating candidate deltas for general TGFD...");
		for (Entry<Pair, ArrayList<TreeSet<Pair>>> intersection : sortedIntersections) {
			int generalMin = intersection.getKey().min();
			int generalMax = intersection.getKey().max();
			System.out.println("Calculating support for candidate general TGFD candidate delta: " + intersection.getKey());

			// Compute general support
			float numerator = 0;
			float denominator = 2 * entitiesSize * this.numOfSnapshots;

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
			System.out.println("Candidate general TGFD support: " + support);
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

			System.out.println("Creating new general TGFD...");
			TGFD tgfd = new TGFD(patternTreeNode.getPattern(), delta, generalDependency, support, patternSupport, "");
			System.out.println("TGFD: " + tgfd);
			tgfds.add(tgfd);
		}
		return tgfds;
	}

	private ArrayList<TGFD> discoverConstantTGFDs(double theta, PatternTreeNode patternNode, ConstantLiteral yLiteral, Map<Set<ConstantLiteral>, ArrayList<Entry<ConstantLiteral, List<Integer>>>> entities, ArrayList<Pair> constantXdeltas, ArrayList<TreeSet<Pair>> satisfyingAttrValues) {
		ArrayList<TGFD> tgfds = new ArrayList<>();
		String yVertexType = yLiteral.getVertexType();
		String yAttrName = yLiteral.getAttrName();
		for (Entry<Set<ConstantLiteral>, ArrayList<Entry<ConstantLiteral, List<Integer>>>> entityEntry : entities.entrySet()) {
			VF2PatternGraph newPattern = patternNode.getPattern().copy();
			Dependency newDependency = new Dependency();
			ArrayList<ConstantLiteral> constantPath = new ArrayList<>();
			for (Vertex v : newPattern.getPattern().vertexSet()) {
				String vType = new ArrayList<>(v.getTypes()).get(0);
				if (vType.equalsIgnoreCase(yVertexType)) { // TO-DO: What if our pattern has duplicate vertex types?
					v.addAttribute(new Attribute(yAttrName));
					if (newDependency.getY().size() == 0) {
						VariableLiteral newY = new VariableLiteral(yVertexType, yAttrName, yVertexType, yAttrName);
						newDependency.addLiteralToY(newY);
					}
				}
				for (ConstantLiteral xLiteral : entityEntry.getKey()) {
					if (xLiteral.getVertexType().equalsIgnoreCase(vType)) {
						v.addAttribute(new Attribute(xLiteral.getAttrName(), xLiteral.getAttrValue()));
						ConstantLiteral newXLiteral = new ConstantLiteral(vType, xLiteral.getAttrName(), xLiteral.getAttrValue());
						newDependency.addLiteralToX(newXLiteral);
						constantPath.add(newXLiteral);
					}
				}
			}
			constantPath.add(new ConstantLiteral(yVertexType, yAttrName, null));

			System.out.println("Performing Constant TGFD discovery");
			System.out.println("Pattern: " + newPattern);
			System.out.println("Entity: " + newDependency);

			System.out.println("Candidate RHS values for entity...");
			ArrayList<Entry<ConstantLiteral, List<Integer>>> attrValuesTimestampsSortedByFreq = entityEntry.getValue();
			for (Map.Entry<ConstantLiteral, List<Integer>> entry : attrValuesTimestampsSortedByFreq) {
				System.out.println(entry.getKey() + ":" + entry.getValue());
			}

			ArrayList<Pair> candidateDeltas = new ArrayList<>();
			if (attrValuesTimestampsSortedByFreq.size() == 1) {
				List<Integer> timestamps = attrValuesTimestampsSortedByFreq.get(0).getValue();
				int minDistance = this.numOfSnapshots - 1;
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
				int minExclusionDistance = this.numOfSnapshots - 1;
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
				if (maxExclusionDistance < this.numOfSnapshots - 1) {
					Pair deltaPair = new Pair(maxExclusionDistance + 1, this.numOfSnapshots - 1);
					candidateDeltas.add(deltaPair);
				}
			}
			if (candidateDeltas.size() == 0) {
				System.out.println("Could not come up with any deltas for entity: " + entityEntry.getKey());
				continue;
			}

			// Compute TGFD support
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
					float denom = 2 * 1 * this.numOfSnapshots;
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
			if (mostSupportedDelta == null) {
				System.out.println("Could not come up with mostSupportedDelta for entity: " + entityEntry.getKey());
				continue;
			}
			System.out.println("Entity satisfying attributes:" + mostSupportedSatisfyingPairs);
			System.out.println("Entity delta = " + mostSupportedDelta);
			System.out.println("Entity support = " + candidateTGFDsupport);
			satisfyingAttrValues.add(mostSupportedSatisfyingPairs);
			constantXdeltas.add(mostSupportedDelta);
			if (candidateTGFDsupport < theta) {
				System.out.println("Could not satisfy TGFD support threshold for entity: " + entityEntry.getKey());
				continue;
			}
			int minDistance = mostSupportedDelta.min();
			int maxDistance = mostSupportedDelta.max();
			candidateTGFDdelta = new Delta(Period.ofDays(minDistance * 183), Period.ofDays(maxDistance * 183 + 1), Duration.ofDays(183));
			System.out.println("Constant TGFD delta: "+candidateTGFDdelta);

			if (isSupersetPath(constantPath, patternNode.getAllMinimalDependenciesOnThisPath())) { // Ensures we don't expand constant TGFDs from previous iterations
				System.out.println("Candidate constant TGFD " + constantPath + " is a superset of an existing minimal constant TGFD");
				continue;
			}
			System.out.println("Creating new constant TGFD...");
			TGFD entityTGFD = new TGFD(newPattern, candidateTGFDdelta, newDependency, candidateTGFDsupport, patternNode.getPatternSupport(), "");
			System.out.println("TGFD: " + entityTGFD);
			tgfds.add(entityTGFD);
			patternNode.addMinimalDependency(constantPath);
		}
		constantXdeltas.sort(new Comparator<Pair>() {
			@Override
			public int compare(Pair o1, Pair o2) {
				return o1.compareTo(o2);
			}
		});
		return tgfds;
	}

	public ArrayList<TGFD> getDummyTGFDs() {
		ArrayList<TGFD> dummyTGFDs = new ArrayList<>();
//		for (Map.Entry<String,Integer> frequentVertexTypeEntry : getSortedFrequentVetexTypesHistogram()) {
//			String frequentVertexType = frequentVertexTypeEntry.getKey();
//			VF2PatternGraph patternGraph = new VF2PatternGraph();
//			PatternVertex patternVertex = new PatternVertex(frequentVertexType);
//			patternGraph.addVertex(patternVertex);
////			HashSet<ConstantLiteral> activeAttributes = getActiveAttributesInPattern(patternGraph.getPattern().vertexSet());
////			for (ConstantLiteral activeAttribute: activeAttributes) {
//				TGFD dummyTGFD = new TGFD();
//				dummyTGFD.setName(frequentVertexType);
//				dummyTGFD.setPattern(patternGraph);
////				Dependency dependency = new Dependency();
////				dependency.addLiteralToY(activeAttribute);
//				dummyTGFDs.add(dummyTGFD);
////			}
//		}
		for (Map.Entry<String,Integer> frequentEdgeEntry : getSortedFrequentEdgeHistogram()) {
			String frequentEdge = frequentEdgeEntry.getKey();
			String[] info = frequentEdge.split(" ");
			String sourceVertexType = info[0];
			String edgeType = info[1];
			String targetVertexType = info[2];
			VF2PatternGraph patternGraph = new VF2PatternGraph();
			PatternVertex sourceVertex = new PatternVertex(sourceVertexType);
			patternGraph.addVertex(sourceVertex);
			PatternVertex targetVertex = new PatternVertex(targetVertexType);
			patternGraph.addVertex(targetVertex);
			RelationshipEdge edge = new RelationshipEdge(edgeType);
			patternGraph.addEdge(sourceVertex, targetVertex, edge);
//			HashSet<ConstantLiteral> activeAttributes = getActiveAttributesInPattern(patternGraph.getPattern().vertexSet());
//			for (ConstantLiteral activeAttribute: activeAttributes) {
				TGFD dummyTGFD = new TGFD();
				dummyTGFD.setName(frequentEdge);
				dummyTGFD.setPattern(patternGraph);
//				Dependency dependency = new Dependency();
//				dependency.addLiteralToY(activeAttribute);
				dummyTGFDs.add(dummyTGFD);
//			}
		}
		return dummyTGFDs;
	}


	private void loadModels(Long graphSize) {
		String fileSuffix = graphSize == null ? "" : "-" + graphSize;
		for (String pathString : Arrays.asList("2015types" + fileSuffix + ".ttl", "2015literals" + fileSuffix + ".ttl", "2015objects" + fileSuffix + ".ttl")) {
			System.out.println("Reading " + pathString);
			Model model = ModelFactory.createDefaultModel();
			Path input = Paths.get(pathString);
			model.read(input.toUri().toString());
			this.models.add(model);
		}
	}

	public ArrayList<DBPediaLoader> loadDBpediaSnapshots(Long graphSize) {
		ArrayList<TGFD> dummyTGFDs = new ArrayList<>();
		System.out.println("Number of dummy TGFDs: " + dummyTGFDs.size());
		ArrayList<DBPediaLoader> graphs = new ArrayList<>();
		String fileSuffix = graphSize == null ? "" : "-" + graphSize;
		for (int year = 5; year < 8; year++) {
			String typeFileName = "201" + year + "types" + fileSuffix + ".ttl";
			String literalsFileName = "201" + year + "literals" + fileSuffix + ".ttl";
			String objectsFileName = "201" + year + "objects" + fileSuffix + ".ttl";
			DBPediaLoader dbpedia = new DBPediaLoader(dummyTGFDs, new ArrayList<>(Collections.singletonList(typeFileName)), new ArrayList<>(Arrays.asList(literalsFileName, objectsFileName)));
			graphs.add(dbpedia);
		}
		return graphs;
	}

	public ArrayList<TGFD> hSpawn(PatternTreeNode patternTreeNode, ArrayList<ArrayList<HashSet<ConstantLiteral>>> matchesPerTimestamps) {
		ArrayList<TGFD> tgfds = new ArrayList<>();

		System.out.println("Performing HSpawn for " + patternTreeNode.getPattern());

		HashSet<ConstantLiteral> literals = getActiveAttributesInPattern(patternTreeNode.getGraph().vertexSet(), false);

		LiteralTree literalTree = new LiteralTree();
		for (int j = 0; j < literals.size(); j++) {

			System.out.println("HSpawn level " + j + "/" + literals.size());

			if (j == 0) {
				literalTree.addLevel();
				for (ConstantLiteral literal: literals) {
					literalTree.createNodeAtLevel(j, literal, null);
				}
			} else if ((j + 1) <= patternTreeNode.getGraph().vertexSet().size()) { // Ensures # of literals in dependency equals number of vertices in graph
				ArrayList<LiteralTreeNode> literalTreePreviousLevel = literalTree.getLevel(j - 1);
				if (literalTreePreviousLevel.size() == 0) {
					System.out.println("Previous level of literal tree is empty. Nothing to expand. End HSpawn");
					break;
				}
				literalTree.addLevel();
				ArrayList<ArrayList<ConstantLiteral>> visitedPaths = new ArrayList<>();
				ArrayList<TGFD> currentLevelTGFDs = new ArrayList<>();
				for (LiteralTreeNode previousLevelLiteral : literalTreePreviousLevel) {
					System.out.println("Creating literal tree node " + literalTree.getLevel(j).size() + "/" + (literalTreePreviousLevel.size() * (literalTreePreviousLevel.size()-1)));
					if (previousLevelLiteral.isPruned()) continue;
					ArrayList<ConstantLiteral> parentsPathToRoot = previousLevelLiteral.getPathToRoot();
					for (ConstantLiteral literal: literals) {
						if (this.interestingTGFDs) { // Ensures all vertices are involved in dependency
							if (isUsedVertexType(literal.getVertexType(), parentsPathToRoot)) continue;
						} else { // Check if leaf node exists in path already
							if (parentsPathToRoot.contains(literal)) continue;
						}

						// Check if path to candidate leaf node is unique
						ArrayList<ConstantLiteral> newPath = new ArrayList<>();
						newPath.add(literal);
						newPath.addAll(previousLevelLiteral.getPathToRoot());
						System.out.println("New candidate literal path: " + newPath);
						if (isPathVisited(newPath, visitedPaths)) { // TO-DO: Is this relevant anymore?
							System.out.println("Skip. Duplicate literal path.");
							continue;
						}
						if (isSupersetPath(newPath, patternTreeNode.getAllZeroEntityDependenciesOnThisPath())) { // Ensures we don't re-explore dependencies whose subsets have no entities
							System.out.println("Skip. Candidate literal path is a superset of zero-entity dependency.");
							continue;
						}
						if (isSupersetPath(newPath, patternTreeNode.getAllMinimalDependenciesOnThisPath())) { // Ensures we don't re-explore dependencies whose subsets have already have a general dependency
							System.out.println("Skip. Candidate literal path is a superset of minimal dependency.");
							continue;
						}
						System.out.println("Newly created unique literal path: " + newPath);

						// Add leaf node to tree
						LiteralTreeNode literalTreeNode = literalTree.createNodeAtLevel(j, literal, previousLevelLiteral);

						// Ensures delta discovery only occurs when # of literals in dependency equals number of vertices in graph
						if ((j + 1) != patternTreeNode.getGraph().vertexSet().size()) {
							System.out.println("|LHS|+|RHS| != |Q|. Skip performing Delta Discovery HSpawn level " + j);
							continue;
						}
						visitedPaths.add(newPath);

						System.out.println("Performing Delta Discovery at HSpawn level " + j);
						ArrayList<TGFD> discoveredTGFDs = deltaDiscovery(theta, patternTreeNode, literalTreeNode, newPath, matchesPerTimestamps);
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
		System.out.println("For pattern " + patternTreeNode.getPattern());
		System.out.println("HSpawn TGFD count: " + tgfds.size());
		return tgfds;
	}

	private static boolean isUsedVertexType(String vertexType, ArrayList<ConstantLiteral> parentsPathToRoot) {
		for (ConstantLiteral literal : parentsPathToRoot) {
			if (literal.getVertexType().equals(vertexType)) {
				return true;
			}
		}
		return false;
	}

	public static class Pair implements Comparable<Pair> {
		private final Integer min;
		private final Integer max;

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
		public int compareTo(@NotNull TgfdDiscovery.Pair o) {
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

//	public void discover(int k, double theta, String experimentName) {
//		// TO-DO: Is it possible to prune the graphs by passing them our histogram's frequent nodes and edges as dummy TGFDs ???
////		Config.optimizedLoadingBasedOnTGFD = !tgfdDiscovery.isNaive;
//
//		PrintStream kExperimentResultsFile = null;
//		if (experimentName.startsWith("k")) {
//			try {
//				String timeAndDateStamp = ZonedDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("uuuu.MM.dd.HH.mm.ss"));
//				kExperimentResultsFile = new PrintStream("k-experiments-runtimes-" + (isNaive ? "naive" : "optimized") + (interestingTGFDs ? "-interesting" : "") + "-" + timeAndDateStamp + ".txt");
//			} catch (FileNotFoundException e) {
//				e.printStackTrace();
//			}
//		}
//		final long kStartTime = System.currentTimeMillis();
//		loadDBpediaSnapshots();
//		for (int i = 0; i <= k; i++) {
//			ArrayList<TGFD> tgfds = vSpawn(i);
//
//			printTgfdsToFile(experimentName, i, theta, tgfds);
//
//			final long kEndTime = System.currentTimeMillis();
//			final long kRunTime = kEndTime - kStartTime;
//			if (experimentName.startsWith("k") && kExperimentResultsFile != null) {
//				System.out.println("Total execution time for k = " + k + " : " + kRunTime);
//				kExperimentResultsFile.print("k = " + i);
//				kExperimentResultsFile.println(", execution time = " + kRunTime);
//			}
//
//			System.gc();
//		}
//	}

	public void vSpawnInit() {

		this.patternTree = new PatternTree();
		this.patternTree.addLevel();

		List<Entry<String, Integer>> sortedNodeHistogram = getSortedFrequentVetexTypesHistogram();

		System.out.println("VSpawn Level 0");
		for (int i = 0; i < sortedNodeHistogram.size(); i++) {
			String vertexType = sortedNodeHistogram.get(i).getKey();

			if (this.vertexTypesAttributes.get(vertexType).size() < 2)
				continue; // TO-DO: Are we interested in TGFDs where LHS is empty?

			int numOfInstancesOfVertexType = sortedNodeHistogram.get(i).getValue();
			int numOfInstancesOfAllVertexTypes = this.NUM_OF_VERTICES_IN_GRAPH;

			double patternSupport = (double) numOfInstancesOfVertexType / (double) numOfInstancesOfAllVertexTypes;
			System.out.println("Estimate Pattern Support: " + patternSupport);

			System.out.println(vertexType);
			VF2PatternGraph candidatePattern = new VF2PatternGraph();
			PatternVertex vertex = new PatternVertex(vertexType);
			candidatePattern.addVertex(vertex);
			System.out.println("VSpawnInit with single-node pattern " + (i+1) + "/" + sortedNodeHistogram.size() + ": " + candidatePattern);

			if (this.isNaive) {
				if (patternSupport >= this.patternSupportThreshold) {
					this.patternTree.createNodeAtLevel(0, candidatePattern, patternSupport, null);
				} else {
					System.out.println("Pattern support of " + patternSupport + " is below threshold.");
				}
			} else {
				this.patternTree.createNodeAtLevel(0, candidatePattern, patternSupport, null);
			}
			System.gc();
		}
		System.out.println("GenTree Level " + 0 + " size: " + this.patternTree.getLevel(0).size());
		for (PatternTreeNode node : this.patternTree.getLevel(0)) {
			System.out.println("Pattern: " + node.getPattern());
			System.out.println("Pattern Support: " + node.getPatternSupport());
//			System.out.println("Dependency: " + node.getDependenciesSets());
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

	public PatternTreeNode vSpawn() {

		List<Entry<String, Integer>> sortedUniqueEdgesHist = getSortedFrequentEdgeHistogram();

		if (this.candidateEdgeIndex > sortedUniqueEdgesHist.size()-1) {
			this.candidateEdgeIndex = 0;
			this.previousLevelNodeIndex++;
		}

		if (this.previousLevelNodeIndex >= this.patternTree.getLevel(this.currentVSpawnLevel -1).size()) {
			this.kRuntimes.add(System.currentTimeMillis() - this.startTime);
			String experimentName = "api-test";
			this.printTgfdsToFile(experimentName, this.tgfds.get(this.currentVSpawnLevel));
			if (this.isKExperiment) this.printExperimentRuntimestoFile(experimentName, this.kRuntimes);
			this.currentVSpawnLevel++;
			if (this.currentVSpawnLevel > this.k) {
				return null;
			}
			this.patternTree.addLevel();
			this.previousLevelNodeIndex = 0;
			this.candidateEdgeIndex = 0;
		}

		System.out.println("Performing VSpawn");
		System.out.println("VSpawn Level " + this.currentVSpawnLevel);
		System.gc();

		ArrayList<PatternTreeNode> previousLevel = this.patternTree.getLevel(this.currentVSpawnLevel - 1);

		PatternTreeNode previousLevelNode = previousLevel.get(this.previousLevelNodeIndex);
		System.out.println("Processing previous level node " + this.previousLevelNodeIndex + "/" + previousLevel.size());
		System.out.println("Performing VSpawn on pattern: " + previousLevelNode.getPattern());

		System.out.println("Level " + (this.currentVSpawnLevel - 1) + " pattern: " + previousLevelNode.getPattern());
		if (previousLevelNode.isPruned() && !this.isNaive) {
			System.out.println("Marked as pruned. Skip.");
			this.previousLevelNodeIndex++;
			return null;
		}

		System.out.println("Processing candidate edge " + this.candidateEdgeIndex + "/" + sortedUniqueEdgesHist.size());
		Map.Entry<String, Integer> candidateEdge = sortedUniqueEdgesHist.get(this.candidateEdgeIndex);
		String candidateEdgeString = candidateEdge.getKey();
		System.out.println("Candidate edge:" + candidateEdgeString);

		// For naive version only - checks if candidate edge is frequent enough
		if (this.isNaive && (1.0 * uniqueEdgesHist.get(candidateEdgeString) / NUM_OF_EDGES_IN_GRAPH) < this.patternSupportThreshold) {
			System.out.println("Candidate edge is below pattern support threshold. Skip");
			this.candidateEdgeIndex++;
			return null;
		}

		String sourceVertexType = candidateEdgeString.split(" ")[0];
		String targetVertexType = candidateEdgeString.split(" ")[2];

		if (this.vertexTypesAttributes.get(targetVertexType).size() == 0) {
			System.out.println("Target vertex in candidate edge does not contain active attributes");
			this.candidateEdgeIndex++;
			return null;
		}

		// TO-DO: We should add support for duplicate vertex types in the future
		if (sourceVertexType.equals(targetVertexType)) {
			System.out.println("Candidate edge contains duplicate vertex types. Skip.");
			this.candidateEdgeIndex++;
			return null;
		}
		String edgeType = candidateEdgeString.split(" ")[1];

		// Check if candidate edge already exists in pattern
		if (isDuplicateEdge(previousLevelNode.getPattern(), edgeType, sourceVertexType, targetVertexType)) {
			System.out.println("Candidate edge: " + candidateEdge.getKey());
			System.out.println("already exists in pattern");
			this.candidateEdgeIndex++;
			return null;
		}

		// Checks if candidate edge extends pattern
		PatternVertex sourceVertex = isDuplicateVertex(previousLevelNode.getPattern(), sourceVertexType);
		PatternVertex targetVertex = isDuplicateVertex(previousLevelNode.getPattern(), targetVertexType);
		if (sourceVertex == null && targetVertex == null) {
			System.out.println("Candidate edge: " + candidateEdge.getKey());
			System.out.println("does not extend from pattern");
			this.candidateEdgeIndex++;
			return null;
		}

		PatternTreeNode patternTreeNode = null;
		// TO-DO: FIX label conflict. What if an edge has same vertex type on both sides?
		for (Vertex v : previousLevelNode.getGraph().vertexSet()) {
			System.out.println("Looking to add candidate edge to vertex: " + v.getTypes());

			if (v.isMarked()) {
				System.out.println("Skip vertex. Already added candidate edge to vertex: " + v.getTypes());
				continue;
			}
			if (!v.getTypes().contains(sourceVertexType) && !v.getTypes().contains(targetVertexType)) {
				System.out.println("Skip vertex. Candidate edge does not connect to vertex: " + v.getTypes());
				v.setMarked(true);
				continue;
			}

			// Create unmarked copy of k-1 pattern
			VF2PatternGraph newPattern = previousLevelNode.getPattern().copy();
			if (targetVertex == null) {
				targetVertex = new PatternVertex(targetVertexType);
				newPattern.addVertex(targetVertex);
			} else {
				for (Vertex vertex : newPattern.getPattern().vertexSet()) {
					if (vertex.getTypes().contains(targetVertexType)) {
						targetVertex.setMarked(true);
						targetVertex = (PatternVertex) vertex;
						break;
					}
				}
			}
			RelationshipEdge newEdge = new RelationshipEdge(edgeType);
			if (sourceVertex == null) {
				sourceVertex = new PatternVertex(sourceVertexType);
				newPattern.addVertex(sourceVertex);
			} else {
				for (Vertex vertex : newPattern.getPattern().vertexSet()) {
					if (vertex.getTypes().contains(sourceVertexType)) {
						sourceVertex.setMarked(true);
						sourceVertex = (PatternVertex) vertex;
						break;
					}
				}
			}
			newPattern.addEdge(sourceVertex, targetVertex, newEdge);

			System.out.println("Created new pattern: " + newPattern);

			double numerator = Double.MAX_VALUE;
			double denominator = this.NUM_OF_EDGES_IN_GRAPH;

			for (RelationshipEdge tempE : newPattern.getPattern().edgeSet()) {
				String sourceType = (new ArrayList<>(tempE.getSource().getTypes())).get(0);
				String targetType = (new ArrayList<>(tempE.getTarget().getTypes())).get(0);
				String uniqueEdge = sourceType + " " + tempE.getLabel() + " " + targetType;
				numerator = Math.min(numerator, uniqueEdgesHist.get(uniqueEdge));
			}
			assert numerator <= denominator;
			double estimatedPatternSupport = numerator / denominator;
			System.out.println("Estimate Pattern Support: " + estimatedPatternSupport);

			// TO-DO: Debug - Why does this work with strings but not subgraph isomorphism???
			if (isIsomorphPattern(newPattern, this.patternTree)) {
				v.setMarked(true);
				System.out.println("Skip. Candidate pattern is an isomorph of existing pattern");
				continue;
			}

			if (isSupergraphPattern(newPattern, this.patternTree)) {
				v.setMarked(true);
				System.out.println("Skip. Candidate pattern is a supergraph of pruned pattern");
				continue;
			}
			newPattern.setDiameter(this.currentVSpawnLevel);
			patternTreeNode = this.patternTree.createNodeAtLevel(this.currentVSpawnLevel, newPattern, estimatedPatternSupport, previousLevelNode, candidateEdgeString);
			System.out.println("Marking vertex " + v.getTypes() + "as expanded.");
			break;
		}
		if (patternTreeNode == null) {
			for (Vertex v : previousLevelNode.getGraph().vertexSet()) {
				System.out.println("Unmarking all vertices in current pattern for the next candidate edge");
				v.setMarked(false);
			}
			this.candidateEdgeIndex++;
		}
		return patternTreeNode;
	}

	private boolean isIsomorphPattern(VF2PatternGraph newPattern, PatternTree patternTree) {
	    System.out.println("Checking if new pattern " + newPattern + "\nis isomorphic");
	    ArrayList<String> newPatternEdges = new ArrayList<>();
        newPattern.getPattern().edgeSet().forEach((edge) -> {newPatternEdges.add(edge.toString());});
		for (PatternTreeNode otherPattern: patternTree.getLevel(this.currentVSpawnLevel)) {
//			VF2AbstractIsomorphismInspector<Vertex, RelationshipEdge> results = new VF2SubgraphIsomorphism().execute2(newPattern.getPattern(), otherPattern.getPattern(), false);
//			if (results.isomorphismExists()) {
            ArrayList<String> otherPatternEdges = new ArrayList<>();
            otherPattern.getGraph().edgeSet().forEach((edge) -> {otherPatternEdges.add(edge.toString());});
//            if (newPattern.toString().equals(otherPattern.getPattern().toString())) {
            if (newPatternEdges.containsAll(otherPatternEdges)) {
				System.out.println("Candidate pattern: " + newPattern);
				System.out.println("is an isomorph of current VSpawn level pattern: " + otherPattern.getPattern());
				return true;
			}
		}
		return false;
	}

	private boolean isSupergraphPattern(VF2PatternGraph newPattern, PatternTree patternTree) {
        ArrayList<String> newPatternEdges = new ArrayList<>();
        newPattern.getPattern().edgeSet().forEach((edge) -> {newPatternEdges.add(edge.toString());});
		int i = this.currentVSpawnLevel;
		while (i > 0) {
			for (PatternTreeNode otherPattern : patternTree.getLevel(i)) {
				if (otherPattern.isPruned()) {
//					VF2AbstractIsomorphismInspector<Vertex, RelationshipEdge> results = new VF2SubgraphIsomorphism().execute2(newPattern.getPattern(), otherPattern.getPattern(), false);
//			        if (results.isomorphismExists()) {
//                    if (newPattern.toString().equals(otherPattern.getPattern().toString())) {
                    ArrayList<String> otherPatternEdges = new ArrayList<>();
                    otherPattern.getGraph().edgeSet().forEach((edge) -> {otherPatternEdges.add(edge.toString());});
                    if (newPatternEdges.containsAll(otherPatternEdges)) {
                        System.out.println("Candidate pattern: " + newPattern);
						System.out.println("is a superset of pruned subgraph pattern: " + otherPattern.getPattern());
						return true;
					}
				}
			}
			i--;
		}
		return false;
	}

	private PatternVertex isDuplicateVertex(VF2PatternGraph newPattern, String vertexType) {
		for (Vertex v: newPattern.getPattern().vertexSet()) {
			if (v.getTypes().contains(vertexType)) {
				return (PatternVertex) v;
			}
		}
		return null;
	}

	public double getMatchesForPattern(ArrayList<DBPediaLoader> graphs, PatternTreeNode patternTreeNode, ArrayList<ArrayList<HashSet<ConstantLiteral>>> matchesPerTimestamps) {
		// TO-DO: Potential speed up for single-edge/single-node patterns. Iterate through all edges/nodes in graph.
		for (int year = 0; year < this.numOfSnapshots; year++) {
			long searchStartTime = System.currentTimeMillis();
			VF2AbstractIsomorphismInspector<Vertex, RelationshipEdge> results = new VF2SubgraphIsomorphism().execute2(graphs.get(year).getGraph(), patternTreeNode.getPattern(), false);
			ArrayList<HashSet<ConstantLiteral>> matches = new ArrayList<>();
			if (results.isomorphismExists()) {
				extractMatches(results.getMappings(), matches, patternTreeNode);
			}
			matchesPerTimestamps.get(year).addAll(matches);
			printWithTime("Search Cost", (System.currentTimeMillis() - searchStartTime));
		}
		// TO-DO: Should we implement pattern support here to weed out patterns with few matches in later iterations?
		// Is there an ideal pattern support threshold after which very few TGFDs are discovered?
		// How much does the real pattern differ from the estimate?
		double numberOfMatchesFound = 0;
		for (ArrayList<HashSet<ConstantLiteral>> matchesInOneTimestamp : matchesPerTimestamps) {
			numberOfMatchesFound += matchesInOneTimestamp.size();
		}

		double realPatternSupport = numberOfMatchesFound / this.NUM_OF_EDGES_IN_GRAPH;
		System.out.println("Real Pattern Support: "+numberOfMatchesFound+" / "+this.NUM_OF_EDGES_IN_GRAPH+" = " + realPatternSupport);
		return numberOfMatchesFound;
	}

	private void extractMatch(GraphMapping<Vertex, RelationshipEdge> result, PatternTreeNode patternTreeNode, HashSet<ConstantLiteral> match) {
		for (Vertex v : patternTreeNode.getGraph().vertexSet()) {
			Vertex currentMatchedVertex = result.getVertexCorrespondence(v, false);
			if (currentMatchedVertex == null) continue;
			String patternVertexType = new ArrayList<>(currentMatchedVertex.getTypes()).get(0);
			for (ConstantLiteral activeAttribute : getActiveAttributesInPattern(patternTreeNode.getGraph().vertexSet(),true)) {
				if (!activeAttribute.getVertexType().equals(patternVertexType)) continue;
				for (String matchedAttrName : currentMatchedVertex.getAllAttributesNames()) {
					if (!activeAttribute.getAttrName().equals(matchedAttrName)) continue;
					String matchedAttrValue = currentMatchedVertex.getAttributeValueByName(matchedAttrName);
					ConstantLiteral xLiteral = new ConstantLiteral(patternVertexType, matchedAttrName, matchedAttrValue);
					match.add(xLiteral);
				}
			}
		}
	}

	private void extractMatches(Iterator<GraphMapping<Vertex, RelationshipEdge>> iterator, ArrayList<HashSet<ConstantLiteral>> matches, PatternTreeNode patternTreeNode) {
		int numOfMatches = 0;
		while (iterator.hasNext()) {
			numOfMatches++;
			GraphMapping<Vertex, RelationshipEdge> result = iterator.next();
			HashSet<ConstantLiteral> match = new HashSet<>();
			extractMatch(result, patternTreeNode, match);
			if (match.size() == 0) continue;
			matches.add(match);
		}
		System.out.println("Number of matches found: " + numOfMatches);
		System.out.println("Number of matches found that contain active attributes: " + matches.size());
		matches.sort(new Comparator<HashSet<ConstantLiteral>>() {
			@Override
			public int compare(HashSet<ConstantLiteral> o1, HashSet<ConstantLiteral> o2) {
				return o1.size() - o2.size();
			}
		});
	}

	public double getMatchesForPattern2(PatternTreeNode patternTreeNode, ArrayList<ArrayList<HashSet<ConstantLiteral>>> matchesPerTimestamps) {

		TGFD dummyTgfd = new TGFD();
		dummyTgfd.setName(patternTreeNode.getEdgeString());
		dummyTgfd.setPattern(patternTreeNode.getPattern());

		System.out.println("-----------Snapshot (1)-----------");
		long startTime=System.currentTimeMillis();
		List<TGFD> tgfds = Collections.singletonList(dummyTgfd);
		int numberOfMatchesFound = 0;
//		LocalDate currentSnapshotDate = LocalDate.parse("2015-10-01");
		DBPediaLoader dbpedia = new DBPediaLoader(tgfds, this.models.get(0), this.models.subList(1, this.models.size()));

		printWithTime("Load graph (1)", System.currentTimeMillis()-startTime);

//		HashMap<String, MatchCollection> matchCollectionHashMap = new HashMap<>();
//		for (TGFD tgfd : tgfds) {
//			matchCollectionHashMap.put(tgfd.getName(), new MatchCollection(tgfd.getPattern(), tgfd.getDependency(), Duration.ofDays(183)));
//		}

		// Now, we need to find the matches for each snapshot.
		// Finding the matches...

		for (TGFD tgfd : tgfds) {
//			VF2SubgraphIsomorphism VF2 = new VF2SubgraphIsomorphism();
			System.out.println("\n###########" + tgfd.getName() + "###########");
//			Iterator<GraphMapping<Vertex, RelationshipEdge>> results = VF2.execute(dbpedia.getGraph(), tgfd.getPattern(), false);

			//Retrieving and storing the matches of each timestamp.
//			System.out.println("Retrieving the matches");
//			startTime=System.currentTimeMillis();
//			matchCollectionHashMap.get(tgfd.getName()).addMatches(currentSnapshotDate, results);
//			printWithTime("Match retrieval", System.currentTimeMillis()-startTime);
			final long searchStartTime = System.currentTimeMillis();
			VF2AbstractIsomorphismInspector<Vertex, RelationshipEdge> results = new VF2SubgraphIsomorphism().execute2(dbpedia.getGraph(), patternTreeNode.getPattern(), false);
			ArrayList<HashSet<ConstantLiteral>> matches = new ArrayList<>();
			if (results.isomorphismExists()) {
				extractMatches(results.getMappings(), matches, patternTreeNode);
			}
			numberOfMatchesFound += matches.size();
			matchesPerTimestamps.get(0).addAll(matches);
			printWithTime("Search Cost", (System.currentTimeMillis() - searchStartTime));
		}

		//Load the change files
//		List<String> paths = Arrays.asList("changes_t1_t2_film_starring_person_"+this.graphSize+"_full_test.json", "changes_t2_t3_film_starring_person_"+this.graphSize+"_full_test.json");
//		List<LocalDate> snapshotDates = Arrays.asList(LocalDate.parse("2016-04-01"), LocalDate.parse("2016-10-01"));
//		for (int i = 0; i < paths.size(); i++) {
		for (int i = 0; i < 2; i++) {
			System.out.println("-----------Snapshot (" + (i+2) + ")-----------");

//			currentSnapshotDate = snapshotDates.get(i);
//			ChangeLoader changeLoader = new ChangeLoader(paths.get(i));
//			List<Change> changes = changeLoader.getAllChanges();
			List<HashMap<Integer,HashSet<Change>>> changes = new ArrayList<>();
			List<Change> allChangesAsList=new ArrayList<>();
			for (String edgeString : patternTreeNode.getAllEdgeStrings()) {
				String path = "changes_t"+(i+1)+"_t"+(i+2)+"_"+edgeString.replace(" ", "_")+"_"+this.graphSize+"_full_test.json";
				if (!this.changeFilesMap.containsKey(path)) {
					JSONParser parser = new JSONParser();
					Object json;
					org.json.simple.JSONArray jsonArray = new JSONArray();
					try {
						json = parser.parse(new FileReader(path));
						jsonArray = (org.json.simple.JSONArray) json;
					} catch (Exception e) {
						e.printStackTrace();
					}
					System.out.println("Storing " + path + " in memory");
					this.changeFilesMap.put(path, jsonArray);
				}
				startTime = System.currentTimeMillis();
				ChangeLoader changeLoader = new ChangeLoader(this.changeFilesMap.get(path));
				HashMap<Integer,HashSet<Change>> newChanges = changeLoader.getAllGroupedChanges();
				printWithTime("Load changes (" + path + ")", System.currentTimeMillis()-startTime);
				System.out.println("Total number of changes in changefile: " + newChanges.size());
				changes.add(newChanges);
				allChangesAsList.addAll(changeLoader.getAllChanges());
			}

//			printWithTime("Load changes ("+paths.get(i) + ")", System.currentTimeMillis()-startTime);
			System.out.println("Total number of changes: " + changes.size());

			// Now, we need to find the matches for each snapshot.
			// Finding the matches...

			startTime=System.currentTimeMillis();
			System.out.println("Updating the graph");
			IncUpdates incUpdatesOnDBpedia = new IncUpdates(dbpedia.getGraph(), tgfds);
			incUpdatesOnDBpedia.AddNewVertices(allChangesAsList);

//			HashMap<String, ArrayList<String>> newMatchesSignaturesByTGFD = new HashMap<>();
//			HashMap<String, ArrayList<String>> removedMatchesSignaturesByTGFD = new HashMap<>();
			HashMap<String, TGFD> tgfdsByName = new HashMap<>();
			for (TGFD tgfd : tgfds) {
//				newMatchesSignaturesByTGFD.put(tgfd.getName(), new ArrayList<>());
//				removedMatchesSignaturesByTGFD.put(tgfd.getName(), new ArrayList<>());
				tgfdsByName.put(tgfd.getName(), tgfd);
			}
			ArrayList<HashSet<ConstantLiteral>> newMatches = new ArrayList<>();
			ArrayList<HashSet<ConstantLiteral>> removedMatches = new ArrayList<>();
			int numOfNewMatchesFoundInSnapshot = 0;
			for (HashMap<Integer,HashSet<Change>> changesByFile:changes) {
				for (int changeID : changesByFile.keySet()) {

					//System.out.print("\n" + change.getId() + " --> ");
					HashMap<String, IncrementalChange> incrementalChangeHashMap = incUpdatesOnDBpedia.updateGraphByGroupOfChanges(changesByFile.get(changeID), tgfdsByName);
					if (incrementalChangeHashMap == null)
						continue;
					for (String tgfdName : incrementalChangeHashMap.keySet()) {
//					newMatchesSignaturesByTGFD.get(tgfdName).addAll(incrementalChangeHashMap.get(tgfdName).getNewMatches().keySet());
//					removedMatchesSignaturesByTGFD.get(tgfdName).addAll(incrementalChangeHashMap.get(tgfdName).getRemovedMatchesSignatures());
//					matchCollectionHashMap.get(tgfdName).addMatches(currentSnapshotDate, incrementalChangeHashMap.get(tgfdName).getNewMatches());
						for (GraphMapping<Vertex, RelationshipEdge> mapping : incrementalChangeHashMap.get(tgfdName).getNewMatches().values()) {
							numOfNewMatchesFoundInSnapshot++;
							HashSet<ConstantLiteral> match = new HashSet<>();
							extractMatch(mapping, patternTreeNode, match);
							if (match.size() == 0) continue;
							newMatches.add(match);
						}

						for (GraphMapping<Vertex, RelationshipEdge> mapping : incrementalChangeHashMap.get(tgfdName).getRemovedMatches().values()) {
							HashSet<ConstantLiteral> match = new HashSet<>();
							extractMatch(mapping, patternTreeNode, match);
							if (match.size() == 0) continue;
							removedMatches.add(match);
						}
					}
				}
			}
			System.out.println("Number of new matches found: " + numOfNewMatchesFoundInSnapshot);
			System.out.println("Number of new matches found that contain active attributes: " + newMatches.size());
			System.out.println("Number of removed matched: " + removedMatches.size());

			matchesPerTimestamps.get(i+1).addAll(newMatches);

			int numOfOldMatchesFoundInSnapshot = 0;
			for (HashSet<ConstantLiteral> previousMatch : matchesPerTimestamps.get(i)) {
//				if(removedMatches.contains(previousMatch) || newMatches.contains(previousMatch)) { /*previousMatch not in newMatches && previousMatch not in removedMatches*/
//					continue;
//				}
				boolean skip = false;
				for (HashSet<ConstantLiteral> removedMatch : removedMatches) {
//					if (removedMatch.equals(previousMatch)) {
					if (equalsLiteral(removedMatch, previousMatch)) {
						skip = true;
					}
				}
				if (skip) continue;
				for (HashSet<ConstantLiteral> newMatch : newMatches) {
//					if (newMatch.equals(previousMatch)) {
					if (equalsLiteral(newMatch, previousMatch)) {
						skip = true;
					}
				}
				if (skip) continue;
				matchesPerTimestamps.get(i+1).add(previousMatch);
				numOfOldMatchesFoundInSnapshot++;
			}
			System.out.println("Number of valid old matches that are not new or removed: " + numOfOldMatchesFoundInSnapshot);
			System.out.println("Total number of matches with active attributes found in this snapshot: " + matchesPerTimestamps.get(i+1).size());
			numberOfMatchesFound += matchesPerTimestamps.get(i+1).size();

			matchesPerTimestamps.get(i+1).sort(new Comparator<HashSet<ConstantLiteral>>() {
				@Override
				public int compare(HashSet<ConstantLiteral> o1, HashSet<ConstantLiteral> o2) {
					return o1.size() - o2.size();
				}
			});
//			for (TGFD tgfd : tgfds) {
//				matchCollectionHashMap.get(tgfd.getName()).addTimestamp(currentSnapshotDate,
//						newMatchesSignaturesByTGFD.get(tgfd.getName()), removedMatchesSignaturesByTGFD.get(tgfd.getName()));
//				System.out.println("New matches (" + tgfd.getName() + "): " +
//						newMatchesSignaturesByTGFD.get(tgfd.getName()).size() + " ** " + removedMatchesSignaturesByTGFD.get(tgfd.getName()).size());
//			}
			printWithTime("Update and retrieve matches", System.currentTimeMillis()-startTime);
			//myConsole.print("#new matches: " + newMatchesSignatures.size()  + " - #removed matches: " + removedMatchesSignatures.size());
		}

		System.out.println("-------------------------------------");
		System.out.println("Total number of matches found in all snapshots: " + numberOfMatchesFound);
		double realPatternSupport = 1.0 * numberOfMatchesFound / this.NUM_OF_EDGES_IN_GRAPH;
		System.out.println("Real Pattern Support: "+numberOfMatchesFound+" / "+this.NUM_OF_EDGES_IN_GRAPH+" = " + realPatternSupport);
		return numberOfMatchesFound;
	}

	private boolean equalsLiteral(HashSet<ConstantLiteral> match1, HashSet<ConstantLiteral> match2) {
		HashSet<String> uris1 = new HashSet<>();
		for (ConstantLiteral match1Attr : match1) {
			if (match1Attr.getAttrName().equals("uri")) {
				uris1.add(match1Attr.getAttrValue());
			}
		}
		HashSet<String> uris2 = new HashSet<>();
		for (ConstantLiteral match2Attr: match2) {
			if (match2Attr.getAttrName().equals("uri")) {
				uris2.add(match2Attr.getAttrValue());
			}
		}
		return uris1.equals(uris2);
	}

	private static void printWithTime(String message, long runTimeInMS)
	{
		System.out.println(message + " time: " + runTimeInMS + "(ms) ** " +
				TimeUnit.MILLISECONDS.toSeconds(runTimeInMS) + "(sec) ** " +
				TimeUnit.MILLISECONDS.toMinutes(runTimeInMS) +  "(min)");
	}
}

