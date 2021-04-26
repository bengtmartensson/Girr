/*
Copyright (C) 2021 Bengt Martensson.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License along with
this program. If not, see http://www.gnu.org/licenses/.
 */
package org.harctoolbox.girr;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This interface describes something that has a name, using the usual semantics.
 */
public interface Named extends Serializable {

    public static <T extends Named> Map<String, T> toMap(T thing) {
        Map<String, T> map = new LinkedHashMap<>(1);
        map.put(thing.getName(), thing);
        return map;
    }

    public static <T extends Named> List<T> toList(T thing) {
        List<T> list = new ArrayList<>(1);
        list.add(thing);
        return list;
    }

    public static <T extends Named> void populateMap(Map<String, T> map, Collection<T> collection) {
        map.clear();
        for (T thing : collection)
            map.put(thing.getName(), thing);
    }

    public static <T extends Named> Map<String, T> toMap(Collection<T> collection) {
        Map<String, T> map = new LinkedHashMap<>(collection.size());
        populateMap(map, collection);
        return map;
    }

    public String getName();

    public static class CompareNameCaseSensitive implements Comparator<Named>, Serializable {
        @Override
        public int compare(Named o1, Named o2) {
            return o1.getName().compareTo(o2.getName());
        }
    }

    public static class CompareNameCaseInsensitive implements Comparator<Named>, Serializable {
        @Override
        public int compare(Named o1, Named o2) {
            return o1.getName().compareToIgnoreCase(o2.getName());
        }
    }
}
