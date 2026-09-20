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

	public Incrementable incf() {
		i++;
		return this;
	}

	public int get() {
		return i;
	}
}

public class Main {
	public static void main() {

		// read config
		// accept config / fork
		testMap();
	}

	public static void testMap() {
		List<Integer> actions = new ArrayList<Integer>();
		try {

			LogicalTimer timer = new LogicalTimer();
			Rng rand = new Rng(0);

			Pusher<Integer> input = new SubscriberManager<>();

			int nums[] = { 1, 2, 3, 4, 5 };
			Incrementable numDelivered = new Incrementable(0);
			int numSentToInbox = 0;
			int numSocketPushed = 0;
			Incrementable numReceived = new Incrementable(0);

			List<Pusher<Integer>> outputs = new ArrayList<>();
			outputs.add(new SubscriberManager<>());

			outputs.getFirst().registerCallback((i) -> {
				if (i != nums[numDelivered.incf().get()]) {
					System.out.printf("%dth sent value was not expected: %d\n", numReceived.get() - 1, i);
					throw new RuntimeException();
				}
			});

			MapProtocol<Integer> mp = new MapProtocol<>(input, outputs, timer, rand, NodeState.ACTIVE_SLEEP);

			int numIterations = 0;
			while (true) {
				int nextAction = rand.pull().orElseThrow(() -> new RuntimeException()) % 3;
				actions.add(nextAction);

				switch (nextAction) {
					case 0:
						// advance time if possible.
						if (timer.emptyP())
							break;
						timer.force();
						break;
					case 1:
						if (numSentToInbox > nums.length)
							break;
						// push another value to send.
						mp.push(nums[numSentToInbox++]);
						break;
					case 2:
						if (numSocketPushed > nums.length)
							break;
						// push another value into the virtual socket.
						input.push(nums[numSocketPushed++]);
						break;
				}

				mp.pull().map((n) -> {
					if (n != nums[numReceived.incf().get()]) {
						System.out.printf("%dth received value was not expected: %d\n", numReceived.get() - 1, n);
						throw new RuntimeException();
					}
					return n;
				});

				if (numReceived.get() == nums.length && numDelivered.get() == nums.length)
					return;

				if (++numIterations > 100) {
					System.out.printf(
							"mp appears to have stalled. successfully delivered %n and received %n messages.\n",
							numDelivered, numReceived);
					throw new RuntimeException();
				}
			}
		} catch (RuntimeException _re) {
			System.out.print("action sequence: [");
			actions.stream().peek((n) -> System.out.printf("%d, ", n));
			System.out.print("]\n");
		}
	}
}
