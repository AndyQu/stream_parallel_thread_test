package thread_lock_test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.IntStream;

public class Main {
	public static void main(String[] args) {
		LongAdder elementCounter = new LongAdder();
		Map<Long, List<Integer>> stat = new HashMap<Long, List<Integer>>();
		try {
			IntStream.range(1, 100).parallel().forEach(i -> {
				long tid = Thread.currentThread().getId();
				List<Integer> v = stat.get(tid);
				if (v == null) {
					v = new ArrayList<Integer>();
					stat.put(new Long(tid), v);
				}
				v.add(i);
				elementCounter.increment();
				System.out.println(System.currentTimeMillis() + "," + Thread.currentThread().getId() + "," + i);
				if (i % 89 == 0) {
					String msg = System.currentTimeMillis() + "," + Thread.currentThread().getId() + ",Oh, you sucks:"
							+ i + "\n";
					System.out.println(msg);
					throw new RuntimeException(msg);
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
		ForkJoinPool.commonPool().awaitQuiescence(1, TimeUnit.DAYS);
		System.out.println(System.currentTimeMillis() + "," + "Process Numbers Total:" + elementCounter);
		for (Map.Entry<Long, List<Integer>> entry : stat.entrySet()) {
			System.out.println(entry.getKey() + ":" + entry.getValue());
		}
	}

}
