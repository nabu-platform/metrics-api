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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import be.nabu.libs.metrics.api.GroupLevelProvider;
import be.nabu.libs.metrics.api.MetricGauge;
import be.nabu.libs.metrics.api.MetricInstance;
import be.nabu.libs.metrics.api.MetricTimer;

/**
 * This grouper allows you to finetune the level of your metric grouping
 * For instance, a server could increment "connectionsAccepted:127.0.0.1:1234"
 * By default the depth is 1 so an increment will only be done on "connectionsAccepted" giving you an aggregate of all connections that have been accepted in general
 * If you update the depth to 2, you will still get the "connectionsAccepted" increment which contains the aggregate, but it will also increment "connectionsAccepted:127.0.0.1" which gives you a more detailed overview per client
 */
public class MetricGrouper implements MetricInstance {

	private MetricInstance parent;
	private GroupLevelProvider provider;
	
	public MetricGrouper(MetricInstance parent, GroupLevelProvider provider) {
		this.parent = parent;
		this.provider = provider;
	}

	public List<String> split(String id) {
		int maxDepth = 1;
		int currentDepth = 0;
		int index = -1;
		List<String> list = new ArrayList<String>();
		while (currentDepth < maxDepth) {
			int newIndex = id.indexOf(':', index + 1);
			String part = newIndex < 0 ? id : id.substring(0, newIndex);
			if (currentDepth == 0) {
				Integer level = provider.getLevel(part);
				// we ignore the id alltogether
				if (level != null && level == 0) {
					break;
				}
			}
			list.add(part);
			index = newIndex;
			currentDepth++;
		}
		return list;
	}
	
	@Override
	public void duration(String id, long duration, TimeUnit timeUnit) {
		for (String part : split(id)) {
			parent.duration(part, duration, timeUnit);
		}
	}

	@Override
	public MetricTimer start(String id) {
		List<MetricTimer> timers = new ArrayList<MetricTimer>();
		for (String part : split(id)) {
			timers.add(parent.start(part));
		}
		return new CombinedTimer(timers);
	}

	@Override
	public void increment(String id, long amount) {
		for (String part : split(id)) {
			parent.increment(part, amount);
		}
	}

	@Override
	public void log(String id, long value) {
		for (String part : split(id)) {
			parent.log(part, value);
		}
	}

	@Override
	public void set(String id, long value) {
		parent.set(id, value);
	}

	@Override
	public void set(String id, MetricGauge gauge) {
		parent.set(id, gauge);
	}
	
	public class CombinedTimer implements MetricTimer {
		private List<MetricTimer> timers;
		public CombinedTimer(List<MetricTimer> timers) {
			this.timers = timers;
		}
		@Override
		public long stop() {
			long value = 0;
			for (MetricTimer timer : timers) {
				value = timer.stop();
			}
			return value;
		}
		@Override
		public MetricInstance getMetrics() {
			return MetricGrouper.this;
		}
		@Override
		public TimeUnit getTimeUnit() {
			return timers.isEmpty() ? TimeUnit.MILLISECONDS : timers.get(0).getTimeUnit();
		}
	}

	public MetricInstance getParent() {
		return parent;
	}

	public GroupLevelProvider getGroupLevelProvider() {
		return provider;
	}
	
}
