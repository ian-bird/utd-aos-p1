package utd.aos.p1;

import java.util.ArrayList;
import java.util.List;

import utd.aos.p1.map.MapProtocol;
import utd.aos.p1.map.NodeState;
import utd.aos.p1.puller.Rng;
import utd.aos.p1.pusher.Pusher;
import utd.aos.p1.pusher.SubscriberManager;
import utd.aos.p1.timer.LogicalTimer;

class Incrementable {
	private int i;

	public Incrementable(int i) {
		this.i = i;
	}

	public synchronized Incrementable incf() {
		i++;
		return this;
	}

	public synchronized int get() {
		return i;
	}
}

public class Main {
	public static void main(String[] args) {

		// read config
		// accept config / fork
		testMap();
	}

	public static void testMap() {
		List<Integer> actions = new ArrayList<Integer>();

		int nums[] = { 0, 1, 2, 3, 4, 5, 6, 7, 8 };
		Incrementable numDelivered = new Incrementable(0);
		int numSentToInbox = 0;
		int numSocketPushed = 0;
		Incrementable numReceived = new Incrementable(0);

		LogicalTimer timer = new LogicalTimer();
		Rng rand = new Rng(8);

		Pusher<Integer> input = new SubscriberManager<>();

		List<Pusher<Integer>> outputs = new ArrayList<>();
		outputs.add(new SubscriberManager<>());

		outputs.getFirst().registerCallback((i) -> {
			if (i != nums[numDelivered.get()]) {
				System.out.printf("%dth sent value was not expected: %d\n", numDelivered.get(), i);
				throw new RuntimeException();
			}
			System.out.printf("sent %d\n", i);
			numDelivered.incf();
		});

		MapProtocol<Integer> mp = new MapProtocol<>(input, outputs, timer, rand, NodeState.ACTIVE_READY);

		try {

			int numIterations = 0;
			while (true) {
				int nextAction = rand.pull().orElseThrow(() -> new RuntimeException()) % 3;
				actions.add(nextAction);

				switch (nextAction) {
					case 0:
						// advance time if possible.
						if (timer.emptyP())
							break;

						System.out.println("advancing time");
						timer.force();
						break;
					case 1:
						if (numSentToInbox >= nums.length)
							break;
						// push another value to send.
						System.out.printf("requested to send %d\n", nums[numSentToInbox]);
						mp.push(nums[numSentToInbox++]);
						break;
					case 2:
						// push another value into the virtual socket.
						input.push(numSocketPushed++);
						break;
				}

				mp.pull().map((n) -> {
					if (n != numReceived.get()) {
						System.out.printf("%dth received value was not expected: %d\n", numReceived.get(), n);
						throw new RuntimeException();
					}
					System.out.printf("received %d\n", n);
					numReceived.incf();
					return n;
				});

				if (numDelivered.get() == nums.length || numDelivered.get() == MapConfig.MAX_SENT)
					return;

				if (++numIterations > 100) {
					System.out.printf(
							"mp appears to have stalled. successfully delivered %d and received %d messages.\n",
							numDelivered.get(), numReceived.get());
					throw new RuntimeException();
				}
			}
		} catch (RuntimeException _re) {
			System.out.print("action sequence: [");

			for (int action : actions)
				System.out.printf("%d, ", action);

			System.out.print("]\n");
		}
	}
}
