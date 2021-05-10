package com.nex3z.garbageclassification.classifier

class Result(
    probabilities: FloatArray
) {
    val results: List<Pair<Int, Float>> = probabilities
        .mapIndexed { label, p -> Pair(label, p)  }
        .sortedByDescending(Pair<Int, Float>::second)

    override fun toString(): String {
        return "Result(results=$results)"
    }
}
