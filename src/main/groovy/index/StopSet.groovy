package index
class StopSet {

    public static Set<String> getStopSetFromFile() {

      Set<String> stopSet = new File('src/stop_words/stop_words_moderate.txt') as Set<String>
     //  Set<String> stopSet = new File('src/stop_words/nltkStopWords.txt') as Set<String>
       return stopSet.asImmutable()
    }
}