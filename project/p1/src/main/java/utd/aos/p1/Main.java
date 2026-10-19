package utd.aos.p1;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

import utd.aos.p1.config.MapConfig;
import utd.aos.p1.map.MapProtocol;
import utd.aos.p1.map.NodeState;
import utd.aos.p1.puller.Rng;
import utd.aos.p1.pusher.Listener;
import utd.aos.p1.pusher.Pusher;
import utd.aos.p1.pusher.Sender;
import utd.aos.p1.timer.SystemTimer;
import utd.aos.p1.utils.FileUtil;

public class Main {
	public static void main(String[] args) throws FileNotFoundException, IOException, InterruptedException {
		MapConfig.loadConfig(FileUtil.slurp("config.txt"));

		var myName = InetAddress.getLocalHost().getHostName().split("\\.")[0];
		System.out.println(myName);
		var myNodeNum = MapConfig.NODE_AND_PORT_BY_HOST.get(myName).getKey();

		// set up my input channel
		var port = MapConfig.NODE_AND_PORT_BY_HOST.get(myName).getValue();
		var incoming = new Listener<>(port, Integer::parseInt);

		// set up my output channels
		var outgoing = new ArrayList<Pusher<Integer>>();
		for (var neighborNode : MapConfig.NEIGHBORS.get(myNodeNum)) {

			var retries = 0;
			while (true) {
				try {
					var p = MapConfig.ADDRESSES_BY_NODE_NUM.get(neighborNode);
					var s = new Sender<Integer>(InetAddress.getByName(p.getKey().concat(".utdallas.edu")),
							p.getValue(), (i) -> i.toString());
					outgoing.add(s);
					break;
				} catch (IOException ie) {
					if (retries++ > 10) {
						System.out.printf("failed to acquire socket for node %d\n", neighborNode);
						throw ie;
					}
					Thread.sleep(200);
				}
			}
		}

		// set up the protocol
		var proto = new MapProtocol<Integer>(incoming, outgoing, new SystemTimer(),
				new Rng((int) System.currentTimeMillis()), myNodeNum == 0 ? NodeState.ACTIVE_SLEEP : NodeState.PASSIVE);

		// register a callback to log when we receive a final message
		proto.registerCallback((i) -> {
			if (i % 100 + 1 == MapConfig.MAX_NUMBER)
				System.out.printf("node %d completed.\n", i / 10000);
		});

		// saturate the queue of stuff to send out
		for (int i = 0; i < MapConfig.MAX_NUMBER; i++)
			proto.push(myNodeNum * 10000 + i);

		// wait until we're killed externally
		Thread.sleep(5_000);
	}

}
