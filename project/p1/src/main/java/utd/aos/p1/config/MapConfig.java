package utd.aos.p1.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import utd.aos.p1.utils.Pair;

public class MapConfig {
	public static int NUM_NODES = 1;
	public static int MAX_PER_ACTIVE = 1;
	public static int MIN_PER_ACTIVE = 1;
	public static int MIN_SEND_DELAY = 1;
	public static int SNAPSHOT_DELAY = 1;
	public static int MAX_NUMBER = 5;
	public static List<Pair<String, Integer>> ADDRESSES;
	public static List<List<Integer>> NEIGHBORS;

	public static void loadConfig(String fileContents) {
		List<String> commentedLines = split(fileContents, "\n");	
		List<String> lines = commentedLines.stream().map((l)->l.split("#")[0]).filter((l)->!l.isEmpty()).collect(Collectors.toList());
		List<Integer> nums = split(lines.getFirst(), " ").stream().map(Integer::parseInt).collect(Collectors.toList());

		NUM_NODES = nums.get(0);
		MAX_PER_ACTIVE = nums.get(1);
		MIN_PER_ACTIVE = nums.get(2);
		MIN_SEND_DELAY = nums.get(3);
		SNAPSHOT_DELAY = nums.get(4);
		MAX_NUMBER = nums.get(5);

		ADDRESSES = new ArrayList<>(Collections.nCopies(NUM_NODES, null));
		for(String l: lines.subList(1, NUM_NODES + 1)) {
			List<String> data = split(l, " ");
			ADDRESSES.set(Integer.parseInt(data.get(0)), new Pair<>(data.get(1), Integer.parseInt(data.get(2))));
		}

		NEIGHBORS = lines.subList(1 + NUM_NODES, 1 + 2 * NUM_NODES).stream()
				.map((l) -> split(l, " ").stream().map(Integer::parseInt).collect(Collectors.toList()))
				.collect(Collectors.toList());
	}

	private static List<String> split(String s, String on) {
		return Arrays.asList(s.split(on)).stream().filter((l) -> !l.isEmpty())
				.collect(Collectors.toList());
	}
}
