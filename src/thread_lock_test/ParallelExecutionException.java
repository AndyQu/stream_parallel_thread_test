package thread_lock_test;

import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.IntStream;
import java.util.stream.Stream;



/**
 * Runtime exceptions in parallel streams.<p>
 * 
 * This code will run through some different parallel streams but let the first
 * thread that is not the client's thread crash (RuntimeException).<p>
 * 
 * It is shown that..<p>
 * 
 * 1) All streams rethrow the exception, wrapped in another RuntimeException.<p>
 * 
 * 2) All streams but one abort the processing, letting the client's thread
 * return exceptionally while some threads still linger on processing elements
 * in the background.<p>
 * 
 * The one exception is Stream.generate() who almost complete all elements
 * before letting the client's thread return exceptionally.<p>
 * 
 * For details, see inline comments and this awesome Stackoverflow answer:
 * http://stackoverflow.com/a/39275425
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public class ParallelExecutionException {
	public static void main(String... ignored) {

		/*
		 * Elements seen after immediate return: 46 013 923 Elements seen after
		 * awaitQuiescence(): 404 659 272
		 */
		runStreamTest("IntStream", IntStream.range(0, 1_000_000_000).mapToObj(Integer::valueOf));

		/*
		 * Elements seen after immediate return: 999 999 873 Elements seen after
		 * awaitQuiescence(): 999 999 873
		 */

		runStreamTest("Stream.generate()", Stream.generate(() -> 1).limit(1_000_000_000));

		Object[] stuff = new Object[1_000_000];

		/*
		 * Elements seen after immediate return: 699 295 Elements seen after
		 * awaitQuiescence(): 812 501
		 */

		runStreamTest("Arrays.stream(T[])", Arrays.stream(stuff));

		/*
		 * Elements seen after immediate return: 652 968 Elements seen after
		 * awaitQuiescence(): 687 501
		 */

		runStreamTest("Collection.stream()", Arrays.asList(stuff).stream());
	}

	private static void runStreamTest(String streamName, Stream<?> stream) {
		final long clientId = Thread.currentThread().getId();

		AtomicBoolean throwEx = new AtomicBoolean();

		LongAdder elementCounter = new LongAdder();

		try {
			stream.parallel().forEach(i -> maybeCrashAndBurn(elementCounter, clientId, throwEx));

			System.out.printf("%s didn't throw anything.\n", streamName);
		} catch (RuntimeException e) {
			System.out.printf("Stack trace from %s.\n", streamName);
			e.printStackTrace(System.out);
		}

		System.out.printf("After immediate return, %s processed this many elements: %s\n", streamName,
				elementCounter.sum());

		ForkJoinPool.commonPool().awaitQuiescence(1, TimeUnit.DAYS);

		System.out.printf("After awaitQuiescence(), %s processed this many elements: %s\n", streamName,
				elementCounter.sum());
	}

	private static void maybeCrashAndBurn(LongAdder elementCounter, long clientId, AtomicBoolean throwEx) {
		elementCounter.increment();

		long myId = Thread.currentThread().getId();

		if (myId != clientId && throwEx.compareAndSet(false, true)) {
			System.out.println("Throwing RuntimeException from thread:" + myId);

			throw new RuntimeException("Crashing thread Id: " + myId);
		}
	}
}
