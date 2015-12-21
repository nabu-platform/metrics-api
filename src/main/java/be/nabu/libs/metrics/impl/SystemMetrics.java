package be.nabu.libs.metrics.impl;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;

import be.nabu.libs.metrics.api.MetricGauge;
import be.nabu.libs.metrics.api.MetricInstance;

public class SystemMetrics {
	
	public static final String METRICS_HEAP_MAX = "heapMax";
	public static final String METRICS_HEAP_INIT = "heapInit";
	public static final String METRICS_HEAP_USED = "heapUsed";
	
	public static final String METRICS_NON_HEAP_MAX = "nonHeapMax";
	public static final String METRICS_NON_HEAP_INIT = "nonHeapInit";
	public static final String METRICS_NON_HEAP_USED = "nonHeapUsed";
	
	public static final String METRICS_PENDING_FINALIZATION = "pendingFinalization";
	
	public static final String METRICS_UPTIME = "uptime";
	public static final String METRICS_CURRENT_THREAD_COUNT = "currentThreadCount";
	public static final String METRICS_PEAK_THREAD_COUNT = "peakThreadCount";
	
	public static final String METRICS_LOAD = "load";
	
	public static void record(MetricInstance instance) {
		recordRuntimeMetrics(instance);
		recordMemoryMetrics(instance);
	}

	public static void recordRuntimeMetrics(MetricInstance instance) {
		final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
		instance.set(METRICS_UPTIME, new MetricGauge() {
			@Override
			public long getValue() {
				return runtimeMXBean.getUptime();
			}
		});
		final OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
		instance.set(METRICS_LOAD, new MetricGauge() {
			@Override
			public long getValue() {
				// an approximation in percentage of the load on the system
				return (long) ((operatingSystemMXBean.getSystemLoadAverage() / operatingSystemMXBean.getAvailableProcessors()) * 100);
			}
		});
		final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
		instance.set(METRICS_CURRENT_THREAD_COUNT, new MetricGauge() {
			@Override
			public long getValue() {
				return threadMXBean.getThreadCount();
			}
		});
		instance.set(METRICS_PEAK_THREAD_COUNT, new MetricGauge() {
			@Override
			public long getValue() {
				return threadMXBean.getPeakThreadCount();
			}
		});
	}

	public static void recordMemoryMetrics(MetricInstance instance) {
		final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
		instance.set(METRICS_PENDING_FINALIZATION, new MetricGauge() {
			@Override
			public long getValue() {
				return memoryMXBean.getObjectPendingFinalizationCount();
			}
		});
		final MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
		instance.set(METRICS_HEAP_MAX, new MetricGauge() {
			@Override
			public long getValue() {
				return heapMemoryUsage.getMax();
			}
		});
		instance.set(METRICS_HEAP_INIT, new MetricGauge() {
			@Override
			public long getValue() {
				return heapMemoryUsage.getInit();
			}
		});
		instance.set(METRICS_HEAP_USED, new MetricGauge() {
			@Override
			public long getValue() {
				return heapMemoryUsage.getUsed();
			}
		});
		
		final MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();
		instance.set(METRICS_NON_HEAP_MAX, new MetricGauge() {
			@Override
			public long getValue() {
				return nonHeapMemoryUsage.getMax();
			}
		});
		instance.set(METRICS_NON_HEAP_INIT, new MetricGauge() {
			@Override
			public long getValue() {
				return nonHeapMemoryUsage.getInit();
			}
		});
		instance.set(METRICS_NON_HEAP_USED, new MetricGauge() {
			@Override
			public long getValue() {
				return nonHeapMemoryUsage.getUsed();
			}
		});
	}
	
}
