package be.nabu.libs.metrics.impl;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import be.nabu.libs.metrics.api.MetricGauge;
import be.nabu.libs.metrics.api.MetricInstance;

public class SystemMetrics {
	
	public static final String METRICS_HEAP_MAX = "heapMax";
	public static final String METRICS_HEAP_INIT = "heapInit";
	public static final String METRICS_HEAP_USED = "heapUsed";
	
	public static final String METRICS_NON_HEAP_MAX = "nonHeapMax";
	public static final String METRICS_NON_HEAP_INIT = "nonHeapInit";
	public static final String METRICS_NON_HEAP_USED = "nonHeapUsed";
	
	public static final String METRICS_HEAP = "heap";
	public static final String METRICS_NON_HEAP = "nonHeap";
	public static final String METRICS_MEMORY = "memory";
	
	public static final String METRICS_PENDING_FINALIZATION = "pendingFinalization";
	
	public static final String METRICS_UPTIME = "uptime";
	public static final String METRICS_CURRENT_THREAD_COUNT = "currentThreadCount";
	public static final String METRICS_PEAK_THREAD_COUNT = "peakThreadCount";
	
	public static final String METRICS_LOAD = "load";
	
	public static final String METRICS_SPACE_USED = "spaceUsed";
	
	private static List<String> fileNamesToIgnore = Arrays.asList("udev", "tmpfs");
	
	public static List<FileStore> filestores() {
		List<FileStore> stores = new ArrayList<FileStore>();
		for (FileStore store : FileSystems.getDefault().getFileStores()) {
			try {
				// if, when asked, the usable space is already 0, we will not be watching it
				// especially on linux there are tons of of "fake" file stores like /snap packages, they are not necessarily marked as read only but they usually don't have any space left
				// the "tmpfs" is a linux thing which holds a memory-based file system, we don't particularly care about that
				if (!store.isReadOnly() && store.getUsableSpace() > 0 && fileNamesToIgnore.indexOf(store.name()) < 0) {
					stores.add(store);
				}
			}
			catch (IOException e) {
				// we ignore a store if we can't get the usable space
				System.err.println("GlueMetrics encountered an issue reading a filestore: " + e.getMessage());
			}
		}
		return stores;
	}
	
	public static void record(MetricInstance instance) {
		recordRuntimeMetrics(instance);
		recordMemoryMetrics(instance);
		recordFileMetrics(instance);
	}
	
	public static Map<String, FileStore> filestoreUsageMetrics() {
		Map<String, FileStore> filestores = new HashMap<String, FileStore>();
		for (FileStore store : filestores()) {
			filestores.put("file-" + store.name() + "-" + METRICS_SPACE_USED, store);
		}
		return filestores;
	}

	public static void recordFileMetrics(MetricInstance instance) {
		for (final FileStore store : filestores()) {
			instance.set("file-" + store.name() + "-" + METRICS_SPACE_USED, new MetricGauge() {
				@Override
				public long getValue() {
					try {
						return (long) (((1.0 * (store.getTotalSpace() - store.getUsableSpace())) / store.getTotalSpace()) * 100);
					}
					catch (IOException e) {
						System.err.println("Could not read file usage: " + e.getMessage());
						return 0;
					}
				}
			});
		}
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
		
		instance.set(METRICS_HEAP, new MetricGauge() {
			@Override
			public long getValue() {
				return (long) ((1.0 * heapMemoryUsage.getUsed()) / heapMemoryUsage.getMax());
			}
		});
		instance.set(METRICS_NON_HEAP, new MetricGauge() {
			@Override
			public long getValue() {
				return (long) ((1.0 * nonHeapMemoryUsage.getUsed()) / nonHeapMemoryUsage.getMax());
			}
		});
		instance.set(METRICS_MEMORY, new MetricGauge() {
			@Override
			public long getValue() {
				return (long) ((1.0 * (nonHeapMemoryUsage.getUsed() + heapMemoryUsage.getUsed())) / (nonHeapMemoryUsage.getMax() + heapMemoryUsage.getMax()));
			}
		});
	}
	
}
