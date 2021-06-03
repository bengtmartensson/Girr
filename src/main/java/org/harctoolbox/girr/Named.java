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

    /**
     * Generate a Map&lt;String, T&gt; map containing one element, the argument.
     * @param <T> type extending Named
     * @param thing
     * @return Map&lt;String, T&gt;
     */
    public static <T extends Named> Map<String, T> toMap(T thing) {
        Map<String, T> map = new LinkedHashMap<>(1);
        map.put(thing.getName(), thing);
        return map;
    }

    /**
     * Generate a Map&lt;String, T&gt; map containing one element, the argument.
     *
     * @param <T>
     * @param thing
     * @return Map&lt;String, T&gt;
     */
    public static <T extends Named> List<T> toList(T thing) {
        List<T> list = new ArrayList<>(1);
        list.add(thing);
        return list;
    }

    /**
     * Populate the map as first argument with the elements contained in the Collection in the second argument.
     * @param <T>
     * @param map
     * @param collection
     */
    public static <T extends Named> void populateMap(Map<String, T> map, Collection<T> collection) {
        map.clear();
        collection.forEach(thing -> {
            map.put(thing.getName(), thing);
        });
    }

    /**
     * Create a Map&lt;String, T&gt; and populate with the elements of the second argument.
     * @param <T>
     * @param collection Collection of Ts.
     * @return
     */
    public static <T extends Named> Map<String, T> toMap(Collection<T> collection) {
        Map<String, T> map = new LinkedHashMap<>(collection.size());
        populateMap(map, collection);
        return map;
    }

    /**
     * Return the name of the object.
     * @return
     */
    public String getName();

    /**
     * Class containing a Comparator implementing standard alphabetical, case sensitive order.
     */
    public static class CompareNameCaseSensitive implements Comparator<Named>, Serializable {
        @Override
        public int compare(Named o1, Named o2) {
            return o1.getName().compareTo(o2.getName());
        }
    }

    /**
     * Class containing a Comparator implementing standard alphabetical, case insensitive order.
     */
    public static class CompareNameCaseInsensitive implements Comparator<Named>, Serializable {
        @Override
        public int compare(Named o1, Named o2) {
            return o1.getName().compareToIgnoreCase(o2.getName());
        }
    }
}
