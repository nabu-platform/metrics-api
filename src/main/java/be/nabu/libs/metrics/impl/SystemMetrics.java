/*
* Copyright (C) 2015 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package be.nabu.libs.metrics.impl;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import be.nabu.libs.metrics.api.MetricGauge;
import be.nabu.libs.metrics.api.MetricInstance;
import be.nabu.libs.metrics.api.MetricProvider;

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
	
	public static final String FILE_DESCRIPTOR_USED = "fileDescriptorUsed";
	
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
	
	public static void record(MetricProvider provider) {
		recordFileMetrics(provider.getMetricInstance("file"));
		recordMemoryMetrics(provider.getMetricInstance("memory"));
		recordRuntimeMetrics(provider.getMetricInstance("runtime"));
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
	
	// https://docs.oracle.com/javase/8/docs/jre/api/management/extension/com/sun/management/UnixOperatingSystemMXBean.html has calls for file descriptors
	// because it is in the com.sun namespace, i don't want to include a reference directly
	private static void attemptRegisterFileDescriptorGauge(MetricInstance instance, final OperatingSystemMXBean operatingSystemMXBean) {
		try {
			final Method maxCount = operatingSystemMXBean.getClass().getMethod("getMaxFileDescriptorCount");
			final Method openCount = operatingSystemMXBean.getClass().getMethod("getOpenFileDescriptorCount");
			if (maxCount != null && openCount != null) {
				// not accessible by default?
				maxCount.setAccessible(true);
				openCount.setAccessible(true);
				// call at least once to make sure we can call it before we register a gauge
				long open = (Long) openCount.invoke(operatingSystemMXBean);
				long max = (Long) maxCount.invoke(operatingSystemMXBean);
				instance.set(FILE_DESCRIPTOR_USED, new MetricGauge() {
					@Override
					public long getValue() {
						try {
							long open = (Long) openCount.invoke(operatingSystemMXBean);
							long max = (Long) maxCount.invoke(operatingSystemMXBean);
							// an approximation in percentage of the load on the system
							return (long) (((1.0 * open) / max) * 100);
						}
						catch (Exception e) {
							// ignore, nothing we can do now...
							return 0;
						}
					}
				});
			}
		}
		catch (Exception e) {
			System.err.println("Can not register file descriptor gauge: " + e.getMessage());
		}
	}
	
	public static void recordRuntimeMetrics(MetricInstance instance) {
		final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
//		instance.set(METRICS_UPTIME, new MetricGauge() {
//			@Override
//			public long getValue() {
//				return runtimeMXBean.getUptime();
//			}
//		});
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
		attemptRegisterFileDescriptorGauge(instance, operatingSystemMXBean);
	}

	public static void recordMemoryMetrics(MetricInstance instance) {
		final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
		instance.set(METRICS_PENDING_FINALIZATION, new MetricGauge() {
			@Override
			public long getValue() {
				return memoryMXBean.getObjectPendingFinalizationCount();
			}
		});
//		final MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
//		instance.set(METRICS_HEAP_MAX, new MetricGauge() {
//			@Override
//			public long getValue() {
//				return heapMemoryUsage.getMax();
//			}
//		});
//		instance.set(METRICS_HEAP_INIT, new MetricGauge() {
//			@Override
//			public long getValue() {
//				return heapMemoryUsage.getInit();
//			}
//		});
		instance.set(METRICS_HEAP_USED, new MetricGauge() {
			@Override
			public long getValue() {
				MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
				return heapMemoryUsage.getUsed();
			}
		});
		
//		final MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();
//		instance.set(METRICS_NON_HEAP_MAX, new MetricGauge() {
//			@Override
//			public long getValue() {
//				return nonHeapMemoryUsage.getMax();
//			}
//		});
//		instance.set(METRICS_NON_HEAP_INIT, new MetricGauge() {
//			@Override
//			public long getValue() {
//				return nonHeapMemoryUsage.getInit();
//			}
//		});
		instance.set(METRICS_NON_HEAP_USED, new MetricGauge() {
			@Override
			public long getValue() {
				MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();
				return nonHeapMemoryUsage.getUsed();
			}
		});
		
		instance.set(METRICS_HEAP, new MetricGauge() {
			@Override
			public long getValue() {
				MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
				return (long) ((100.0 * heapMemoryUsage.getUsed()) / heapMemoryUsage.getMax());
			}
		});
		instance.set(METRICS_NON_HEAP, new MetricGauge() {
			@Override
			public long getValue() {
				MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();
				return (long) ((100.0 * nonHeapMemoryUsage.getUsed()) / nonHeapMemoryUsage.getMax());
			}
		});
		instance.set(METRICS_MEMORY, new MetricGauge() {
			@Override
			public long getValue() {
				MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();
				MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
				return (long) ((100.0 * (nonHeapMemoryUsage.getUsed() + heapMemoryUsage.getUsed())) / (nonHeapMemoryUsage.getMax() + heapMemoryUsage.getMax()));
			}
		});
	}
	
}
