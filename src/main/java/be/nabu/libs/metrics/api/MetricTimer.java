package be.nabu.libs.metrics.api;

public interface MetricTimer {
	public long stop();
	public MetricInstance getMetrics();
}
