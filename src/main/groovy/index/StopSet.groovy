package index

import groovy.transform.CompileStatic

@CompileStatic
class StopSet {

    public static Set<String> getStopSetFromFile() {

        Set<String> stopSet = new File('src/cfg/stop_words_moderate.txt') as Set<String>
        return stopSet.asImmutable()
    }

    static Set<String> smallStopSet = ["a", "an", "and", "are", "as", "at", "be", "but", "by", "for", "if", "in", "into", "is", "it", "no", "not", "of", "on", "or", "such", "that", "the", "their", "then", "there", "these", "they", "this", "to", "was", "will", "with", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0",
                                     "td", "dd", "dt", "tr", "h4", "em", "icons", "ps", "blockquote", "postscript", "valign", "vlink", "ffffff", "cellpadding"] as Set<String>

}