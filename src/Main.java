import java.util.ArrayList;
import java.util.List;

import pusher.SubscriberManager;

public class Main {
	public static void main() {

		// read config
		// accept config / fork
		test();
	}

	public static void test() {
		List<Int> actions = new ArrayList<Int>();
		try {

			Timer timer = new LogicalTimer();
			Rng rand = new Rng(0);

			Pusher<Int> input = new SubscriberManager<>();

			int nums[] = { 1, 2, 3, 4, 5 };
			int numDelivered = 0;

			List<Pusher<Int>> outputs = new ArrayList<>(new SubscriberManager<>());
			outputs[0].registerCallback((i) -> {
				if (i != nums[numDelivered++]) {
					System.out.printf("%dth sent value was not expected: %d\n", numReceived - 1, n);
					throw new RuntimeException();
				}
			});

			MapProtocol mp = new MapProtocol(input, outputs, timer, rand, ACTIVE_SLEEP);

			int numSentToInbox = 0;
			int numSocketPushed = 0;
			int numReceived = 0;

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
						if (numSentToInbox > n.length)
							break;
						// push another value to send.
						mp.push(nums[numSentToInbox++]);
						break;
					case 2:
						if (numSocketPushed > n.length)
							break;
						// push another value into the virtual socket.
						input.push(nums[numSocketPushed++]);
						break;
				}

				mp.pull().map((n) -> {
					if (n != nums[numReceived++]) {
						System.out.printf("%dth received value was not expected: %d\n", numReceived - 1, n);
						throw new RuntimeException();
					}
				});

				if (numReceived == nums.length && numDelivered == nums.length)
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
			system.out.print("]\n");
		}
	}
}
