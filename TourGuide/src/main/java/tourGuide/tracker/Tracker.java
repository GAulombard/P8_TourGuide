package tourGuide.tracker;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import lombok.SneakyThrows;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tourGuide.exception.UserNotFoundException;
import tourGuide.service.TourGuideService;
import tourGuide.model.User;

/**
 * The type Tracker.
 */
public class Tracker extends Thread {
	private Logger logger = LoggerFactory.getLogger(Tracker.class);

	private static final long trackingPollingInterval = TimeUnit.MINUTES.toSeconds(5); //represent a time duration, perform timing delay operation every "$duration"
	private final ExecutorService executorService = Executors.newSingleThreadExecutor();
	private final TourGuideService tourGuideService;
	private boolean stop = false;

	/**
	 * Instantiates a new Tracker.
	 *
	 * @param tourGuideService the tour guide service
	 * @param runTrackerAtStartup boolean used to disable Tracker at creation, this allows to make
	 * tests without tracker thread running.
	 */
	public Tracker(TourGuideService tourGuideService, boolean runTrackerAtStartup) {
		this.tourGuideService = tourGuideService;

		if(!runTrackerAtStartup) {
			logger.info("Tracker run at startup: DISABLED");
			this.stop = true;
			executorService.shutdown();
		} else {
			logger.info("Tracker run at startup: ENABLE");
			executorService.submit(this);
		}
	}

	/**
	 * Assures to shut down the Tracker thread
	 */
	public void stopTracking() {
		stop = true;
		executorService.shutdownNow();
	}
	
	@SneakyThrows
	@Override
	public void run() {

		StopWatch stopWatch = new StopWatch();

		while(true) {
			if(Thread.currentThread().isInterrupted() || stop) {
				logger.info("Tracker stopping");
				break;
			}
			
			List<User> users = tourGuideService.getAllUsers();
			logger.info("Begin Tracker. Tracking " + users.size() + " users.");
			stopWatch.start();


			tourGuideService.trackUserLocationMultiThread(users);

			stopWatch.stop();
			logger.info("Tracker Time Elapsed: " + TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()) + " seconds.");
			stopWatch.reset();

			try {
				logger.info("Tracker sleeping");
				TimeUnit.SECONDS.sleep(trackingPollingInterval);
			} catch (InterruptedException e) {
				break;
			}
		}
		
	}
}
