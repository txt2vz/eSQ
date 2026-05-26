package index
class StopSet {

    public static Set<String> getStopSetFromFile() {

        Set<String> stopSet = new File('src/cfg/stop_words_moderate.txt') as Set<String>
        return stopSet.asImmutable()
    }
}