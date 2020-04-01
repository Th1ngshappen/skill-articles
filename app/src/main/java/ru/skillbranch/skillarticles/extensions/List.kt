package ru.skillbranch.skillarticles.extensions

import kotlin.math.max
import kotlin.math.min

// Необходимо реализовать extension функцию
// для группировки результата поиска по интервалам указанным в коллекции bounds.
// Количество выходных элементов должно быть рано количеству bounds
// Реализуй fun List для группировки результата поиска по интервалам указанным в коллекции bounds.
// Количество выходных элементов должно быть рано количеству bounds
// Пример:
// searchResult = [(2,5), (8,20), (22,30), (45,50), (70,100)]
// bounds = [(0,10), (10,30), (30,50), (50,60), (60,100)]
// result = [[(2, 5), (8, 10)], [(10, 20), (22, 30)], [(45, 50)], [], [(70, 100)]]

fun List<Pair<Int, Int>>.groupByBounds(bounds: List<Pair<Int, Int>>): List<MutableList<Pair<Int, Int>>> {

    return bounds.map { (leftBound, rightBound) ->
        val range = (if (leftBound == 0) 0 else leftBound + 1)..rightBound
        this.filter {
            it.first in range || it.second in range
        }.map {
            max(it.first, leftBound) to min(it.second, rightBound)
        }.toMutableList()
    }
}