package be.nabu.libs.metrics.api;

public interface MetricProvider {
	public MetricInstance newMetricInstance(String id);
}
