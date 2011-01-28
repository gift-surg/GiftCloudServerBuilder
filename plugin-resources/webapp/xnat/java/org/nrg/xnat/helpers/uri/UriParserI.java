package org.nrg.xnat.helpers.uri;

/**
 * A base uri parser that can be specialized to read different types of uri.
 * @author aditya
 *
 * @param <T> Results from parsing the uri are inserted into this type of object.
 */
interface UriParserI<T> {
	T readUri (String uri) throws java.util.MissingFormatArgumentException ;
}