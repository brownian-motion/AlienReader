package com.brownian.alienreader.message

/**
 * Represents a "fullname" in the Reddit API,
 * which uniquely identifies any object on Reddit.
 *
 * Note that this may identify something which is not a [ReadableThing], like an image/video post.
 */
data class Fullname(val fullname: String) {
    override fun toString() = fullname
}

/**
 * Something that can be read by the Text-to-Speech engine.
 */
data class ReadableThing(val id: Fullname, val body: CharSequence)

/**
 * Identifies some "Listing", which is a stream of posts.
 * The actual Listings these identify are read in slices of a few posts at a time,
 * with [Fullname]s identifying what comes "before" and "after" that slice.
 */
sealed class ListingId {
    data class Subreddit(val subreddit: String)
    // TODO: define different kinds of listings that can be traversed through
}

data class ListingSlice(
    val listing: ListingId,
    val contents: List<ReadableThing>,
    val before: Fullname?,
    val after: Fullname?
)