package utd.aos.p1.config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import utd.aos.p1.utils.Pair;

/*
The map config contains information about the system loaded in from a config file at startup.
 */
public class MapConfig {
	public static int NUM_NODES = 1;
	public static int MIN_PER_ACTIVE = 1;
	public static int MAX_PER_ACTIVE = 1;
	public static int MIN_SEND_DELAY = 1;
	public static int SNAPSHOT_DELAY = 1;
	public static int MAX_NUMBER = 5;
	public static HashMap<String, Pair<Integer, Integer>> NODE_AND_PORT_BY_HOST;
	public static HashMap<Integer, Pair<String, Integer>> ADDRESSES_BY_NODE_NUM;
	public static List<List<Integer>> NEIGHBORS;

	// set up the configuration via a file formatted according to the project 1
	// specifications
	public static void loadConfig(String fileContents) {
		List<String> commentedLines = split(fileContents, "\n");
		List<String> lines = commentedLines.stream().map((l) -> l.split("#")[0]).filter((l) -> !l.isEmpty()).toList();
		List<Integer> nums = split(lines.getFirst(), " ").stream().map(Integer::parseInt).toList();

		NUM_NODES = nums.get(0);
		MIN_PER_ACTIVE = nums.get(1);
		MAX_PER_ACTIVE = nums.get(2);
		MIN_SEND_DELAY = nums.get(3);
		SNAPSHOT_DELAY = nums.get(4);
		MAX_NUMBER = nums.get(5);

		NODE_AND_PORT_BY_HOST = new HashMap<>();
		ADDRESSES_BY_NODE_NUM = new HashMap<>();

		for (String l : lines.subList(1, NUM_NODES + 1)) {
			List<String> data = split(l, " ");
			NODE_AND_PORT_BY_HOST.put(data.get(1),
					new Pair<>(Integer.parseInt(data.get(0)), Integer.parseInt(data.get(2))));
			ADDRESSES_BY_NODE_NUM.put(Integer.parseInt(data.get(0)),
					new Pair<>(data.get(1), Integer.parseInt(data.get(2))));
		}

		NEIGHBORS = lines.subList(1 + NUM_NODES, 1 + 2 * NUM_NODES).stream()
				.map((l) -> split(l, " ").stream().map(Integer::parseInt).toList()).toList();
	}

	// utility function
	private static List<String> split(String s, String on) {
		return Arrays.asList(s.split(on)).stream().filter((l) -> !l.isEmpty()).toList();
	}
}
