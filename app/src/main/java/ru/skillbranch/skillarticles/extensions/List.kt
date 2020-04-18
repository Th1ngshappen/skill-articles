package ru.skillbranch.skillarticles.extensions

import kotlin.math.max
import kotlin.math.min

fun List<Pair<Int, Int>>.groupByBounds(bounds: List<Pair<Int, Int>>): List<MutableList<Pair<Int, Int>>> {

    return bounds.map { (leftBound, rightBound) ->
        val range = leftBound..rightBound
        this.filter {
            it.first in range || it.second in range
        }.map {
            max(it.first, leftBound) to min(it.second, rightBound)
        }.toMutableList()
    }
}