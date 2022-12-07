/*
 * Copyright (c) 2015 Jonas Kalderstam.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nononsenseapps.notepad.android.provider

import android.net.Uri

/**
 * Helper functions related to provider operations.
 */
const val URI_NOMATCH = -1
const val URI_ROOT = 101
const val URI_LIST = 102
const val URI_DETAILS = 103
const val URI_LIST_PREFIX = "list"
const val URI_DETAILS_PREFIX = "details"

/**
 * Returns a list uri given a base and relativePath
 * @param base such as content://my.provider.authority
 * *
 * @param relativePath /foo/bar
 * *
 * @return the list uri: content://my.provider.authority/list/foo/bar
 */
fun getListUri(base: Uri, relativePath: String): Uri {
    return Uri.withAppendedPath(Uri.withAppendedPath(base, "list"),
            relativePath)
}

/**
 * Returns a details uri given a base and relativePath
 * @param base such as content://my.provider.authority
 * *
 * @param relativePath /foo/bar
 * *
 * @return the details uri: content://my.provider.authority/details/foo/bar
 */
fun getDetailsUri(base: Uri, relativePath: String): Uri {
    return Uri.withAppendedPath(Uri.withAppendedPath(base, "details"),
            relativePath)
}

/**
 * Returns only the scheme and authority parts.

 * @param uri like content://my.provider.authority/details/foo/bar
 * *
 * @return uri with only scheme and authority: content://my.provider.authority
 */
fun getBase(uri: Uri): Uri {
    return Uri.parse(uri.scheme + "://" + uri.authority)
}

/**
 * Note that /ACTION will return "/".

 * @param uri like content://my.provider.authority/ACTION/foo/bar
 * *
 * @return relative path without action part like /foo/bar
 */
fun getRelativePath(uri: Uri): String {
    return getRelativePath(uri.path!!)
}

tailrec fun getRelativePath(path: String): String {
    val i = path.indexOf("/")
    if (i == 0) {
        return getRelativePath(path.substring(1))
    } else if (i < 0) {
        return "/"
    } else {
        return path.substring(i)
    }
}

/**
 * Split a path on "/", taking initial slashes into account.
 * Multiple slashes are interpreted as single slashes, just as on the file system.

 * @param path like /foo/bar/baz
 * *
 * @return array like [foo, bar, baz]
 */
tailrec fun split(fullPath: String): Array<String> {
    var path = fullPath.replace("/+".toRegex(), "/")

    if (path.startsWith("/")) {
        return split(path.substring(1))
    } else if (path.isEmpty()) {
        return arrayOf()
    }
    return path.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
}

/**

 * @param path like /foo/bar
 * *
 * @return first part of path like foo
 */
tailrec fun firstPart(path: String): String {
    val i = path.indexOf("/")
    if (i == 0) {
        return firstPart(path.substring(1))
    } else if (i > 0) {
        return path.substring(0, i)
    } else {
        // No slashes
        return path
    }
}

/**
 * If nothing remains, returns the empty string.

 * @param path like /foo/bar/baz
 * *
 * @return the bit after first like bar/baz (without starting slash)
 */
tailrec fun restPart(path: String): String {
    val i = path.indexOf("/")
    if (i == 0) {
        return restPart(path.substring(1))
    } else if (i > 0) {
        return path.substring(i + 1)
    } else {
        return ""
    }
}

fun matchUri(uri: Uri): Int {
    return matchPath(uri.path)
}

/**
 * Since UriMatcher isn't as good as it should be, this implements the matching I want.

 * @param path like /foo/bar/baz
 * *
 * @return type of the path
 */
tailrec fun matchPath(path: String?): Int {
    if (path == null || path.isEmpty()) {
        return URI_ROOT
    } else if (path.startsWith("/")) {
        return matchPath(path.substring(1))
    } else {
        val fp = firstPart(path).toLowerCase()
        when (fp) {
            URI_LIST_PREFIX -> return URI_LIST
            URI_DETAILS_PREFIX -> {
                // Details must specify an item
                if (restPart(path).isEmpty()) {
                    return URI_NOMATCH
                } else {
                    return URI_DETAILS
                }
            }
            else -> return URI_NOMATCH
        }
    }
}

/**
 * Join two pieces together, separated by a /
 * @param path1 like /foo
 * *
 * @param path2 like bar
 * *
 * @return /foo/bar
 */
fun join(path1: String, path2: String): String {
    if (path1.endsWith("/")) {
        if (path2.startsWith("/")) {
            return path1 + path2.substring(1)
        } else {
            return path1 + path2
        }
    } else {
        if (path2.startsWith("/")) {
            return path1 + path2
        } else {
            return path1 + "/" + path2
        }
    }
}

