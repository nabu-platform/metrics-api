package be.nabu.libs.metrics.api;

import java.util.concurrent.TimeUnit;

public interface MetricTimer {
	public long stop();
	public MetricInstance getMetrics();
	public TimeUnit getTimeUnit();
}
