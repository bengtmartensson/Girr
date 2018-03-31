/*
Copyright (C) 2018 Bengt Martensson.

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

/**
 * Exception for errors in Girr.
 */
public class GirrException extends Exception {

    /**
     * Exception constructor.
     * @param string
     */
    public GirrException(String string) {
        super(string);
    }

    /**
     * Exception constructor.
     * @param ex
     */
    public GirrException(Throwable ex) {
        super(ex);
    }

    /**
     * Constructor without arguments.
     */
    public GirrException() {
        super();
    }
}
