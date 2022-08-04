package cluster

import index.IndexEnum
import index.IndexUtils
import index.Indexes
import org.apache.lucene.classification.Classifier
import org.apache.lucene.classification.utils.ConfusionMatrixGenerator
import org.apache.lucene.document.Document
import org.apache.lucene.index.Term
import org.apache.lucene.search.MatchAllDocsQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.ScoreDoc
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.TopDocs
import org.apache.lucene.search.TotalHitCountCollector
import groovy.json.JsonOutput

class Effectiveness {

    static Tuple4<Double, Double, Double, Integer> get_v_measure_h_c_sizOfAllClusters(Classifier classifier, int job, boolean queriesOnly) {

        Query qAll = new MatchAllDocsQuery()
        TopDocs topDocs = Indexes.indexSearcher.search(qAll, Integer.MAX_VALUE)
        ScoreDoc[] allHits = topDocs.scoreDocs

        List<String> classes = []
        List<String> clusters = []

        int unasscount = 0
        int qOnlyCount = 0

        for (ScoreDoc sd : allHits) {
            Document d = Indexes.indexSearcher.doc(sd.doc)
            String category = d.get(Indexes.FIELD_CATEGORY_NAME)
            String contents = d.get(Indexes.FIELD_CONTENTS)
            String queryAssignedCluster = d.get(Indexes.FIELD_QUERY_ASSIGNED_CLUSTER)

            String cluster = queryAssignedCluster

            if (cluster == 'unassigned') {
                cluster = classifier.assignClass(contents).getAssignedClass().utf8ToString()
            }

            if (queryAssignedCluster == 'unassigned') {
                unasscount++;
            }

            if (queriesOnly) {
                if (queryAssignedCluster != 'unassigned') {
                    classes.add(category)
                    clusters.add(cluster)
                    qOnlyCount++
                }
            } else {
                classes.add(category)
                clusters.add(cluster)
            }
        }

        println "qonlycount $qOnlyCount"
        println "in v measure unass count $unasscount"

        println "In v measure classes leng ${classes.size()} clusters len ${clusters.size()}"
        println "in v measure toSet classess ${classes.toSet().size()} clust ${clusters.toSet().size()}"

        String classesFileName = "classes.txt"
        String clustersFileName = "clusters.txt"

        File classesFile = new File(classesFileName)
        File clustersFile = new File(clustersFileName)

        classesFile.write(JsonOutput.toJson(classes))
        clustersFile.write(JsonOutput.toJson(clusters))

        String resultFromPython

        try {
            CallVmeasurePython cp0 = new CallVmeasurePython()
            resultFromPython = cp0.proce()

        } catch (Exception e) {
            print " Exeception  in callVmeasurePython $e"
        }
        List<String> resultsList = resultFromPython.split(',')

        final double vMeasure = resultsList[0].toDouble()
        final double homogeniety = resultsList[1].toDouble()
        final double completness = resultsList[2].toDouble()

        assert classes.size() == clusters.size()

        return new Tuple4(vMeasure, homogeniety, completness, clusters.size())
    }
}
