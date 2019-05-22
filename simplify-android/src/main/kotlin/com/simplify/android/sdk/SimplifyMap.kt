package com.simplify.android.sdk

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.ArrayList


class SimplifyMap : LinkedHashMap<String, Any> {

    companion object {
        private val arrayIndexPattern = "(.*)\\[(.*)\\]".toRegex()

        /**
         * Returns an identical copy of the map
         *
         * @param m The map to copy
         * @return A copy of the original map
         */
        @JvmStatic
        fun normalize(m: MutableMap<String, Any>): MutableMap<String, Any> {
            val pm = SimplifyMap()

            m.keys.forEach { k ->
                when (val v = m[k]) {
                    is List<*> -> pm.set(k, normalize(v as MutableList<Any>))
                    is Map<*, *> -> pm.set(k, normalize(v as MutableMap<String, Any>))
                    is String, is Double, is Float, is Number, is Boolean -> pm.set(k, v)
                    else -> pm.set(k, v.toString())
                }
            }

            return pm
        }

        @JvmStatic
        private fun normalize(l: MutableList<Any>): MutableList<Any> {
            val pl = ArrayList<Any>()

            l.forEach {v ->
                when (v) {
                    is List<*> -> pl.add(normalize(v as MutableList<Any>))
                    is Map<*, *> -> pl.add(normalize(v as MutableMap<String, Any>))
                    is String, is Double, is Float, is Number, is Boolean -> pl.add(v)
                    else -> pl.add(v.toString())
                }
            }

            return pl
        }
    }


    /**
     * Constructs an empty map with the default capacity and load factor.
     */
    constructor() : super()

    /**
     * Constructs an empty map with the specified capacity and default load factor.
     *
     * @param initialCapacity the initial capacity
     */
    constructor(initialCapacity: Int) : super(initialCapacity)

    /**
     * Constructs an empty map with the specified capacity and load factor.
     *
     * @param initialCapacity the initial capacity
     * @param loadFactor      the load factor
     */
    constructor(initialCapacity: Int, loadFactor: Float) : super(initialCapacity, loadFactor)

    /**
     * Constructs a map with the same mappings as in the specifed map.
     *
     * @param map the map whose mappings are to be placed in this map
     */
    constructor(map: Map<String, Any>) : super(map)

    /**
     * Constructs a map based of the speficied JSON string.
     *
     * @param jsonMapString the JSON string used to construct the map
     */
    constructor(jsonMapString: String?) : super() {
        val map = Gson().fromJson<Map<out String, Any>>(jsonMapString, object : TypeToken<Map<out String, Any>>() {}.type)
        putAll(map)
    }

    /**
     * Constructs a map with an initial mapping of keyPath to value.
     *
     * @param keyPath key path with which the specified value is to be associated.
     * @param value   value to be associated with the specified key path.
     */
    constructor(keyPath: String, value: Any) {
        put(keyPath, value)
    }

    /**
     * Associates the specified value to the specified key path.
     *
     * @param key key path to which the specified value is to be associated.
     * @param value   the value which is to be associated with the specified key path.
     * @throws IllegalArgumentException  if part of the key path does not match the expected type.
     * @throws IndexOutOfBoundsException if using an array index in the key path is out of bounds.
     */
    override fun put(key: String, value: Any): Any? {
        val properties = key.split("\\.".toRegex())
        var destinationObject: MutableMap<String, Any> = this

        if (properties.size > 1) {
            for (i in 0 until properties.size - 1) {
                val property = properties[i]
                destinationObject = when {
                    property.contains("[") -> getDestinationMap(property, destinationObject, i == properties.size - 1)
                    else -> getPropertyMapFrom(property, destinationObject)
                }
            }
        } else if (key.contains("[")) {
            destinationObject = getDestinationMap(key, this, true)
        }

        // TODO: need to take care of the case where we are inserting a value into an array rather than
        // map ( eg map.put("a[2]", 123);

        return when {
            destinationObject === this -> super.put(key, value!!)
            value is Map<*, *> -> {     // if putting a map, call put all
                destinationObject.clear()
                val m = SimplifyMap()
                m.putAll(value as Map<out String, Any>)
                destinationObject[properties[properties.size - 1]] = m
                destinationObject
            }
            else -> destinationObject.put(properties[properties.size - 1], value!!)
        }
    }

    /**
     * Associates the specified value to the specified key path and returns a reference to
     * this map.
     *
     * @param keyPath key path to which the specified value is to be associated.
     * @param value   the value which is to be associated with the specified key path.
     * @return this map
     * @throws IllegalArgumentException  if part of the key path does not match the expected type.
     * @throws IndexOutOfBoundsException if using an array index in the key path is out of bounds.
     */
    fun set(keyPath: String, value: Any): SimplifyMap {
        put(keyPath, value)
        return this
    }

    /**
     * Returns the value associated with the specified key path or null if there is no associated value.
     *
     * @param keyPath key path whose associated value is to be returned
     * @return the value to which the specified key is mapped
     * @throws IllegalArgumentException  if part of the key path does not match the expected type.
     * @throws IndexOutOfBoundsException if using an array index in the key path is out of bounds.
     */
    override fun get(key: String): Any? {
        val keys = key.split("\\.".toRegex())

        if (keys.size <= 1) {
            return arrayIndexPattern.find(keys[0])?.groupValues?.let { groups ->
                val k = groups[1]
                val o = super.get(k) as? List<*>
                        ?: throw IllegalArgumentException("Property '$k' is not an array")
                val l = o as List<Map<String, Any>>

                //get last item if none specified
                val index = if (groups[2].isNotEmpty()) {
                    Integer.parseInt(groups[2])
                } else l.size - 1

                return l[index]
            } ?: super.get(keys[0])
        }

        val map = findLastMapInKeyPath(key)     // handles keyPaths beyond 'root' keyPath. i.e. "x.y OR x.y[].z, etc."

        // retrieve the value at the end of the object path i.e. x.y.z, this retrieves whatever is in 'z'
        return map!!.get(keys[keys.size - 1])
    }

    /**
     * Returns true if there is a value associated with the specified key path.
     *
     * @param key key path whose associated value is to be tested
     * @return true if this map contains an value associated with the specified key path
     * @throws IllegalArgumentException  if part of the key path does not match the expected type.
     * @throws IndexOutOfBoundsException if using an array index in the key path is out of bounds.
     */
    override fun containsKey(key: String): Boolean {
        val keys = key.split("\\.".toRegex())

        if (keys.size <= 1) {
            return arrayIndexPattern.find(keys[0])?.groupValues?.let { groups ->
                val k = groups[1]
                val o = super.get(k) as? MutableList<*>
                        ?: throw IllegalArgumentException("Property '$k' is not an array")  // get the list from the map
                val l = o as MutableList<MutableMap<String, Any>>  // get the list from the map

                val index: Int = if (groups[2].isNotEmpty()) Integer.parseInt(groups[2]) else l.size - 1

                index >= 0 && index < l.size
            } ?: super.containsKey(keys[0])
        }

        val map = findLastMapInKeyPath(key) ?: return false
        return map.containsKey(keys[keys.size - 1])
    }

    /**
     * Removes the value associated with the specified key path from the map.
     *
     * @param key key path whose associated value is to be removed
     * @throws IllegalArgumentException  if part of the key path does not match the expected type.
     * @throws IndexOutOfBoundsException if using an array index in the key path is out of bounds.
     */
    override fun remove(key: String): Any? {
        val keys = key.split("\\.".toRegex())

        if (keys.size <= 1) {
            return arrayIndexPattern.find(keys[0])?.groupValues?.let { groups ->
                val k = groups[1]
                val o = super.get(k) as? MutableList<*>
                        ?: throw IllegalArgumentException("Property '$k' is not an array")  // get the list from the map
                val l = o as MutableList<MutableMap<String, Any>>  // get the list from the map

                val index: Int = if (groups[2].isNotEmpty()) Integer.parseInt(groups[2]) else l.size - 1

                l.removeAt(index)
            } ?: super.remove(keys[0])
        }

        val map = findLastMapInKeyPath(key)

        return map!!.remove(keys[keys.size - 1])
    }

    private fun findLastMapInKeyPath(keyPath: String): MutableMap<String, Any>? {
        val keys = keyPath.split("\\.".toRegex())

        var map: MutableMap<String, Any>? = null

        for (i in 0..keys.size - 2) {
            var thisKey = keys[i]
            map = arrayIndexPattern.find(keys[i])?.groupValues?.let { groups ->
                thisKey = groups[1]

                val o: Any? = (if (null == map) super.get(thisKey) else map!![thisKey]) as? List<*>
                        ?: throw IllegalArgumentException("Property '$thisKey' is not an array")

                val l = o as MutableList<MutableMap<String, Any>>?

                var index: Int? = l!!.size - 1                                        //get last item if none specified

                if ("" != groups[2]) {
                    index = Integer.parseInt(groups[2])
                }

                l[index!!]
            } ?: if (map == null) {
                super.get(thisKey) as MutableMap<String, Any>?
            } else {
                map[thisKey] as MutableMap<String, Any>?
            }
        }

        return map
    }

    private fun getDestinationMap(property: String, destinationObject: MutableMap<String, Any>, createMap: Boolean): MutableMap<String, Any> {
        return arrayIndexPattern.find(property)?.groupValues?.let { groups ->
            val propName = groups[1]
            val index: Int? = if (groups[2].isNotEmpty()) {
                Integer.parseInt(groups[2])
            } else null

            findOrAddToList(destinationObject, propName, index, createMap)
        } ?: destinationObject
    }

    private fun findOrAddToList(destinationObject: MutableMap<String, Any>, propName: String, index: Int?, createMap: Boolean): MutableMap<String, Any> {
        var destObject = destinationObject

        var list = ArrayList<MutableMap<String, Any>>()
        // find existing list or put the new list
        if (destObject.containsKey(propName)) {
            val o = destObject[propName] as? List<*>
                    ?: throw IllegalArgumentException("Property '$propName' is not an array")
            list = o as ArrayList<MutableMap<String, Any>>
        } else {
            destObject[propName] = list
        }

        // get the existing object in the list at the index
        var propertyValue: MutableMap<String, Any>? = null
        if (index != null && list.size > index) {
            propertyValue = list[index]
        }

        // no object at the index, create a new map and add it
        if (null == propertyValue) {
            propertyValue = java.util.LinkedHashMap()
            if (null == index) {
                list.add(propertyValue)
            } else {
                list.add(index, propertyValue)
            }
        }

        // return the map retrieved from or added to the list
        destObject = propertyValue

        return destObject
    }

    private fun getPropertyMapFrom(property: String, obj: MutableMap<String, Any>): MutableMap<String, Any> {
        // create a new map at the key specified if it doesn't already exist
        if (!obj.containsKey(property)) {
            val value = LinkedHashMap<String, Any>()
            obj[property] = value
        }

        val o = obj[property]
        return if (o is Map<*, *>) {
            o as MutableMap<String, Any>
        } else {
            throw IllegalArgumentException("cannot change nested property to map")
        }
    }
}