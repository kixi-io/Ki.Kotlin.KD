package io.kixi.kd

import io.kixi.Ki

/**
 * KD documents are composed of KD tags. Tags contain:
 *
 * 1. a name (if not present, the empty string "" is used)
 * 1. a namespace (optional)
 * 1. 0 or more values
 * 1. 0 or more attributes
 * 1. 0 or more children
 *
 * KD tag examples:
 * ```
 *     size 4
 *     secure true
 *     version 5.2.3-beta-3
 *     range 25..50
 *     oddValues 1 3 5 7
 *     valueAndAtt "manahoana" greeting=true
 *     myList [2 4 6]
 *     myMap [name="Isabella" programmer=true]
 *     kitchenSink 1 'a' true [1 2 3] [4 5 6] [pizza=true sardines=false] 3.0..<5.0
 * ```
 *
 * The KD utility class can be used to read KD code from a file, URL, string or reader.
 * ```
 *     var root = KD.read("""
 *          size 15
 *          optimized true
 *          version 5.0.2
 *     """)
 *
 *     var size = root.getChild("size").value as Int
 *     var optimized = root.getChild("optimized").value as Boolean
 *     var version = root.getChild("version").value as Version
 * ```
 *
 * In the example above, the tags in the string are read into a tag called "root".  It has
 * three children (tags) called "size", "optimized", and "version".  They each have one
 * value, no attributes, and no bodies.
 *
 * KD is often used for simple key-value mappings.  Because of this common case, Tag
 * provides convenience methods such as getValue and setValue which operate on the first
 * element in the values list.
 *
 * The example above used the simple format common in property files:
 * ```
 *     name value
 * ```
 *
 * The full KD tag format is:
 * ```
 *     namespace:name value_list attribute_list {
 *         children_tags
 *     }
 * ```
 *
 * ...where value_list is zero or more space separated KD literals and
 * attribute_list is zero or more space separated (namespace:)key=value pairs.
 * The name, namespace, and keys are KD identifiers.  Values are KD literals.
 * Namespace is optional for tag names and attributes.  Tag bodies are also
 * optional.  KD identifiers begin with a unicode letter, underscore, dollar
 * sign or emoji followed by zero or more Unicode letters, numbers, underscores,
 * dollar signs or emoji.
 *
 * KD also supports anonymous tags which have the name "" (the empty string).
 * An anonymous tag starts with a literal and is followed by zero or more
 * additional literals and zero or more attributes.
 *
 * For more information on the KD language see the
 * [KD Wiki Page](https://github.com/kixi-io/Ki.Docs/wiki/Ki-Data-(KD)).
 */
class Tag {

    var nsid: NSID

    var values = ArrayList<Any?>()
    var attributes = HashMap<NSID, Any?>()

    var children = ArrayList<Tag>()

    constructor(nsid: NSID) {
        this.nsid = nsid
    }

    constructor(name:String, namespace:String = ""): this(NSID(name, namespace)) {}

    fun isAnonymous(): Boolean = nsid == NSID.ANONYMOUS

    companion object {
        private val NEW_LINE = System.getProperty("line.separator")
    }
    /**
     * @param linePrefix A prefix to insert before every line.
     * @return A string representation of this tag using KD
     *
     * TODO: break up long lines using the backslash
     */
     fun toString(linePrefix: String): String {
        val builder = StringBuilder(linePrefix)
        var skipValueSpace = false

        if (isAnonymous()) {
            skipValueSpace = true
        } else {
            builder.append(nsid.toString())
        }
        // output values
        if (!values.isEmpty()) {
            val i: Iterator<*> = values.iterator()
            while (i.hasNext()) {
                if (skipValueSpace) skipValueSpace = false else builder.append(" ")
                builder.append(Ki.format(i.next()))
            }
        }

        // output attributes
        if (attributes.isNotEmpty()) {
            val i = attributes.entries.iterator()
            while (i.hasNext()) {
                builder.append(" ")
                val e = i.next()
                // val key = e.key
                // val value = e.value
                builder.append("${Ki.format(e.key)}=${Ki.format(e.value)}")
            }
        }

        // output children
        if (!children.isEmpty()) {
            builder.append(" {$NEW_LINE")
            for (t in children) {
                builder.append(t.toString("$linePrefix    ") + NEW_LINE)
            }
            builder.append("$linePrefix}")
        }
        return builder.toString()
    }

    fun setAttribute(name: String, namespace: String = "", value: Any?) =
        attributes.put(NSID(name, namespace), value)

    fun getAttribute(name: String, namespace: String = "") : Any? =
            attributes[NSID(name, namespace)]

    fun getAttributesInNamespace(namespace:String) : Map<String, Any?> {
        val map = HashMap<String, Any?>()
        for(e in attributes.entries) {
            if(e.key.namespace == namespace)
                map.put(e.key.name, e.value)
        }
        return map
    }

    /**
     * Gets the first child tag with the given name
     */
    fun getChild(name: String, namespace: String = "") : Tag? = children.find {
        it.nsid.name == name && it.nsid.namespace == namespace
    }

    /**
     * Gets the first child tag with the given name, searching all descendants
     */
    fun findChild(name: String, namespace: String = "")  : Tag? {
        val child = getChild(name, namespace)
        if(child==null) {
            for(kid in children) {
                val grandChild = kid.getChild(name, namespace)
                if(grandChild!=null)
                    return grandChild
            }
        }
        return null
    }

    /**
     * Gets all child tags with the given name
     */
    fun getChildren(name: String, namespace: String = "") : List<Tag> = children.filter {
        it.nsid.name == name && it.nsid.namespace == namespace
    }

    /**
     * Gets all child tags with the given name, searching all descendants
     */
    fun findChildren(name: String, namespace: String = "") : List<Tag> {
        val descendents = ArrayList<Tag>()

        descendents.addAll(children.filter {
            it.nsid.name == name && it.nsid.namespace == namespace
        })

        for(descendent in children) {
            descendents.addAll(descendent.children.filter {
                it.nsid.name == name && it.nsid.namespace == namespace
            })
        }

        return descendents
    }

    /**
     * Gets all child tags in the given namespace
     */
    fun getChildrenInNamespace(namespace: String) : List<Tag> = children.filter  {
        it.nsid.namespace == namespace
    }

    /**
     * Finds the first child tag that tests true for the given predicate. The search is
     * recursive.
     *
     * **Example**
     * `
     * val thing = tag.findChild { it.color = "green" && it.size > 5 }
     * `
     */
    inline fun findChild(predicate: (Tag) -> Boolean): Tag? {
        var searchMe = getDescendents()

        for(child in searchMe) {
            if(predicate(child))
                return child
        }

        return null
    }

    /**
     * Finds all tags that tests true for the given predicate. The search is recursive.
     *
     * **Example**
     * `
     * val things = tag.findChildren { it.color = "green" && it.size > 5 }
     * `
     */
    inline fun findChildren(predicate: (Tag) -> Boolean): List<Tag> {
        var searchMe = getDescendents()

        val results = ArrayList<Tag>()
        for(child in searchMe) {
            if(predicate(child))
                results+=child
        }

        return results
    }

    /**
     * Creates a list of all contained tags (recursive).
     */
    fun getDescendents(descendents:MutableList<Tag> = ArrayList()) : List<Tag> {
        descendents.addAll<Tag>(children)
        children.forEach { it.getDescendents(descendents) }
        return descendents
    }

    // TODO: Type parameter
    // TODO: Add a grid datatype to allow get(x,y) style access to grids
    // Q: Should we have number grids with 0 defaults for missing values?

    /**
     * Gets the values of all immediate children as a list of rows.
     */
    fun getChildrenValues() : List<List<Any?>> {
        var rows = ArrayList<List<Any?>>()
        for(child in children) {
            rows.add(child.values)
        }
        return rows
    }

    /**
     * Convenience method that returns the first value in the value list or null if there
     * are no values.
     */
    var value: Any? = null
        get() = if(values.size == 0) null else values[0]

    override fun toString(): String = toString("")

    /**
     * Returns true if this tag (including all of its values, attributes, and
     * children) is equivalent to the given tag.
     *
     * @return true if the tags are equivalent
     */
    override fun equals(other: Any?): Boolean = other is Tag && other.toString() == toString()

    /**
     * @return The hash (based on the output from toString())
     */
    override fun hashCode(): Int = toString().hashCode()
}
