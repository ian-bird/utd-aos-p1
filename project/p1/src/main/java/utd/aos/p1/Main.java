package utd.aos.p1;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

import utd.aos.p1.config.MapConfig;
import utd.aos.p1.huang.HaltDetector;
import utd.aos.p1.huang.Rational;
import utd.aos.p1.map.MapProtocol;
import utd.aos.p1.map.NodeState;
import utd.aos.p1.puller.Rng;
import utd.aos.p1.pusher.Listener;
import utd.aos.p1.pusher.Pusher;
import utd.aos.p1.pusher.Sender;
import utd.aos.p1.snapshot.Acknowledge;
import utd.aos.p1.snapshot.Message;
import utd.aos.p1.snapshot.Snapshot;
import utd.aos.p1.timer.SystemTimer;
import utd.aos.p1.utils.FileUtil;
import utd.aos.p1.utils.Pair;

public class Main {
	private static Message<Pair<Rational, Integer>> parseMessage(String s) {
		try(var bais = new ByteArrayInputStream(Base64.getDecoder().decode(s));
			var ois = new ObjectInputStream(bais)) {
			

			@SuppressWarnings("unchecked")
			var result = (Message<Pair<Rational, Integer>>) ois.readObject();

			return result;
		} catch (Exception _ex) {
			System.out.println("failed to deserialize object.\n");
			return new Acknowledge<>(0, 0, 0);
		}
	}

	public static void main(String[] args) throws FileNotFoundException, IOException, InterruptedException {
		MapConfig.loadConfig(FileUtil.slurp("config.txt"));

		var myName = InetAddress.getLocalHost().getHostName().split("\\.")[0];
		System.out.println(myName);

		var maybePair = MapConfig.NODE_AND_PORT_BY_HOST.get(myName);
		if (maybePair == null)
			System.exit(1);
		var myNodeNum = maybePair.getKey();

		// set up my input channel
		var port = MapConfig.NODE_AND_PORT_BY_HOST.get(myName).getValue();

		var incoming = new Listener<>(port, Main::parseMessage);

		// set up my output channels
		var outgoing = new ArrayList<Pusher<Message<Pair<Rational, Integer>>>>();
		for (var neighborNode : MapConfig.NEIGHBORS.get(myNodeNum)) {

			var retries = 0;
			while (true) {
				try {
					var p = MapConfig.ADDRESSES_BY_NODE_NUM.get(neighborNode);
					var s = new Sender<Message<Pair<Rational, Integer>>>(
							InetAddress.getByName(p.getKey().concat(".utdallas.edu")),
							p.getValue(), (o) -> {
								String result;
								try {
									var baos = new ByteArrayOutputStream();
									var oos = new ObjectOutputStream(baos);
									oos.writeObject(o);
									result = Base64.getEncoder().encodeToString(baos.toByteArray());
								} catch (Exception _ex) {
									System.out.println("failed to serialize object.\n");
									return "";
								}
								return result;
							});
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

		// configure snapshots and halt detection
		var snapshot = new Snapshot<>(incoming, outgoing, MapConfig.NEIGHBORS.get(myNodeNum), myNodeNum,
				MapConfig.NUM_NODES);

		var hd = new HaltDetector<>(snapshot.outputs, snapshot.input, myNodeNum == 0, () -> {
			System.out.println("process completed.\n");
			System.exit(0);
		}, -1);

		// set up the protocol
		var proto = new MapProtocol<Integer>(hd.in, hd.outs, new SystemTimer(),
				new Rng((int) System.currentTimeMillis()), myNodeNum == 0 ? NodeState.ACTIVE_SLEEP : NodeState.PASSIVE);

		// snapshots and halt detection are defined in mutual recursion with proto, so
		// finish their config
		snapshot.clientState = proto::getState;

		hd.setSleepSource(proto.onSleep);
		hd.clientState = proto::getState;

		// register a callback to log when we receive a final message
		proto.registerCallback((i) -> {
			System.out.printf("received %d from %d\n", i % 100, i / 10000);
			if (i % 100 + 1 == MapConfig.MAX_NUMBER)
				System.out.printf("node %d completed.\n", i / 10000);
		});

		// saturate the queue of stuff to send out
		for (int i = 0; i < MapConfig.MAX_NUMBER * 2; i++)
			proto.push(myNodeNum * 10000 + i);

		CompletableFuture.runAsync(() -> {
			if(myNodeNum != 0)
				return;

			try (var writer = new BufferedWriter(new FileWriter("/home/011/i/ir/irb170030/src/utd-aos-p1/snapshots.txt"))) {
			    writer.write("---snapshots---\n");
				while (true) {
					Thread.sleep(MapConfig.SNAPSHOT_DELAY);
					snapshot.capture((response) -> {
						try {
							writer.write(response.states.toString());
							writer.flush();
							System.out.println("wrote to file");
						} catch (IOException _ie) {
						}
					});
				}
			} catch (Exception _e) {
			}
		});

		Thread.sleep(10_000);
	}

}
