package index

class MergeSets {

    def mergeOverlappingSets(List<Set<String>> sets) {
        def result = []

        sets.each { set ->
            // find all existing sets that overlap with the current one
            def overlapping = result.findAll { !it.intersect(set).isEmpty() }

            if (overlapping) {
                // merge all overlaps + current set
                def merged = overlapping.inject(set as Set) { acc, s -> acc + s }

                // remove old overlapping sets
                result.removeAll(overlapping)

                // add merged set
                result << merged
            } else {
                // no overlap, add as-is
                result << (set as Set)
            }
        }

        result
    }

    static void main(String[] args) {
        def input = [
                ['apple', 'banana'] as Set,
                ['banana', 'pear'] as Set,
                ['carrot'] as Set,
                ['pear', 'orange'] as Set,
                ['lettuce'] as Set
        ]
        def merger = new MergeSets()
        def merged = merger.mergeOverlappingSets(input)
        println merged
    }

}
