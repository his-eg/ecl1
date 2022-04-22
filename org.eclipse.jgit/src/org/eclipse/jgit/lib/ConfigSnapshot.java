/*
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2009, Constantine Plotnikov <constantine.plotnikov@gmail.com>
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008-2012, Google Inc.
 * Copyright (C) 2009, JetBrains s.r.o.
 * Copyright (C) 2007-2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2006-2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Thad Hughes <thadh@thad.corp.google.com> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.lib;

import static org.eclipse.jgit.util.StringUtils.compareIgnoreCase;
import static org.eclipse.jgit.util.StringUtils.compareWithCase;
import static org.eclipse.jgit.util.StringUtils.toLowerCase;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jgit.util.StringUtils;

class ConfigSnapshot {
	final List<ConfigLine> entryList;
	final Map<Object, Object> cache;
	final ConfigSnapshot baseState;
	volatile List<ConfigLine> sorted;
	volatile SectionNames names;

	ConfigSnapshot(List<ConfigLine> entries, ConfigSnapshot base) {
		entryList = entries;
		cache = new ConcurrentHashMap<>(16, 0.75f, 1);
		baseState = base;
	}

	Set<String> getSections() {
		return names().sections;
	}

	Set<String> getSubsections(String section) {
		Map<String, Set<String>> m = names().subsections;
		Set<String> r = m.get(section);
		if (r == null)
			r = m.get(toLowerCase(section));
		if (r == null)
			return Collections.emptySet();
		return Collections.unmodifiableSet(r);
	}

	Set<String> getNames(String section, String subsection) {
		return getNames(section, subsection, false);
	}

	Set<String> getNames(String section, String subsection, boolean recursive) {
		Map<String, String> m = getNamesInternal(section, subsection, recursive);
		return new CaseFoldingSet(m);
	}

	private Map<String, String> getNamesInternal(String section,
			String subsection, boolean recursive) {
		List<ConfigLine> s = sorted();
		int idx = find(s, section, subsection, ""); //$NON-NLS-1$
		if (idx < 0)
			idx = -(idx + 1);

		Map<String, String> m = new LinkedHashMap<>();
		while (idx < s.size()) {
			ConfigLine e = s.get(idx++);
			if (!e.match(section, subsection))
				break;
			if (e.name == null)
				continue;
			String l = toLowerCase(e.name);
			if (!m.containsKey(l))
				m.put(l, e.name);
		}
		if (recursive && baseState != null)
			m.putAll(baseState.getNamesInternal(section, subsection, recursive));
		return m;
	}

	String[] get(String section, String subsection, String name) {
		List<ConfigLine> s = sorted();
		int idx = find(s, section, subsection, name);
		if (idx < 0)
			return null;
		int end = end(s, idx, section, subsection, name);
		String[] r = new String[end - idx];
		for (int i = 0; idx < end;)
			r[i++] = s.get(idx++).value;
		return r;
	}

	private int find(List<ConfigLine> s, String s1, String s2, String name) {
		int low = 0;
		int high = s.size();
		while (low < high) {
			int mid = (low + high) >>> 1;
			ConfigLine e = s.get(mid);
			int cmp = compare2(
					s1, s2, name,
					e.section, e.subsection, e.name);
			if (cmp < 0)
				high = mid;
			else if (cmp == 0)
				return first(s, mid, s1, s2, name);
			else
				low = mid + 1;
		}
		return -(low + 1);
	}

	private int first(List<ConfigLine> s, int i, String s1, String s2, String n) {
		while (0 < i) {
			if (s.get(i - 1).match(s1, s2, n))
				i--;
			else
				return i;
		}
		return i;
	}

	private int end(List<ConfigLine> s, int i, String s1, String s2, String n) {
		while (i < s.size()) {
			if (s.get(i).match(s1, s2, n))
				i++;
			else
				return i;
		}
		return i;
	}

	private List<ConfigLine> sorted() {
		List<ConfigLine> r = sorted;
		if (r == null)
			sorted = r = sort(entryList);
		return r;
	}

	private static List<ConfigLine> sort(List<ConfigLine> in) {
		List<ConfigLine> sorted = new ArrayList<>(in.size());
		for (ConfigLine line : in) {
			if (line.section != null && line.name != null)
				sorted.add(line);
		}
		Collections.sort(sorted, new LineComparator());
		return sorted;
	}

	private static int compare2(
			String aSection, String aSubsection, String aName,
			String bSection, String bSubsection, String bName) {
		int c = compareIgnoreCase(aSection, bSection);
		if (c != 0)
			return c;

		if (aSubsection == null && bSubsection != null)
			return -1;
		if (aSubsection != null && bSubsection == null)
			return 1;
		if (aSubsection != null) {
			c = compareWithCase(aSubsection, bSubsection);
			if (c != 0)
				return c;
		}

		return compareIgnoreCase(aName, bName);
	}

	private static class LineComparator implements Comparator<ConfigLine> {
		@Override
		public int compare(ConfigLine a, ConfigLine b) {
			return compare2(
					a.section, a.subsection, a.name,
					b.section, b.subsection, b.name);
		}
	}

	private SectionNames names() {
		SectionNames n = names;
		if (n == null)
			names = n = new SectionNames(this);
		return n;
	}

	private static class SectionNames {
		final CaseFoldingSet sections;
		final Map<String, Set<String>> subsections;

		SectionNames(ConfigSnapshot cfg) {
			Map<String, String> sec = new LinkedHashMap<>();
			Map<String, Set<String>> sub = new HashMap<>();
			while (cfg != null) {
				for (ConfigLine e : cfg.entryList) {
					if (e.section == null)
						continue;

					String l1 = toLowerCase(e.section);
					if (!sec.containsKey(l1))
						sec.put(l1, e.section);

					if (e.subsection == null)
						continue;

					Set<String> m = sub.get(l1);
					if (m == null) {
						m = new LinkedHashSet<>();
						sub.put(l1, m);
					}
					m.add(e.subsection);
				}
				cfg = cfg.baseState;
			}

			sections = new CaseFoldingSet(sec);
			subsections = sub;
		}
	}

	private static class CaseFoldingSet extends AbstractSet<String> {
		private final Map<String, String> names;

		CaseFoldingSet(Map<String, String> names) {
			this.names = names;
		}

		@Override
		public boolean contains(Object needle) {
			if (needle instanceof String) {
				String n = (String) needle;
				return names.containsKey(n)
						|| names.containsKey(StringUtils.toLowerCase(n));
			}
			return false;
		}

		@Override
		public Iterator<String> iterator() {
			final Iterator<String> i = names.values().iterator();
			return new Iterator<>() {

				@Override
				public boolean hasNext() {
					return i.hasNext();
				}

				@Override
				public String next() {
					return i.next();
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}

		@Override
		public int size() {
			return names.size();
		}
	}
}
