package be.nabu.libs.metrics.api;

public interface MetricInstance {
	/**
	 * Start a new timer
	 */
	public MetricTimer start(String id);
	/**
	 * Increment (or decrement) a value with a given amount
	 */
	public void increment(String id, long amount);
	/**
	 * Log a certain value that is part of a series of values
	 */
	public void log(String id, long value);
	/**
	 * Set a certain value that is not part of a series, it is simply a single state
	 */
	public void set(String id, long value);
}
