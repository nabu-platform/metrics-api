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

package be.nabu.libs.metrics.api;

import java.util.concurrent.TimeUnit;

public interface MetricInstance {
	/**
	 * Based on internal time capture, add a duration value
	 */
	public void duration(String id, long duration, TimeUnit timeUnit);
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
	/**
	 * Sets a certain value with a gauge that can return the actual value 
	 */
	public void set(String id, MetricGauge gauge);
}
