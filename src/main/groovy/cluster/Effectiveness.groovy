package cluster

import groovy.json.JsonOutput
import index.Indexes
import org.apache.lucene.classification.Classifier
import org.apache.lucene.document.Document
import org.apache.lucene.search.MatchAllDocsQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.ScoreDoc
import org.apache.lucene.search.TopDocs

class Effectiveness {

    File classesFile = new File(/results\classes.txt/)
    File clustersFile = new File(/results\clusters.txt/)

    double vMeasure
    double homogeniety
    double completness

    int numberOfDocumentsInClusters
    int clusterCountError

    Effectiveness(Classifier classifier, boolean queriesOnly){

        List<String> classes = []
        List<String> clusters = []

        Query qAll = new MatchAllDocsQuery()
        TopDocs topDocs = Indexes.indexSearcher.search(qAll, Integer.MAX_VALUE)
        ScoreDoc[] allHits = topDocs.scoreDocs

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

        clusterCountError = Math.abs(classes.toSet().size() - clusters.toSet().size())

        assert classes.size() == clusters.size()
        numberOfDocumentsInClusters = clusters.size()

        println "qonlycount $qOnlyCount"
        println "In v measure unassigned count $unasscount"
        println "In v measure classes leng ${classes.size()} clusters len ${clusters.size()}"
        println "in v measure toSet classess ${classes.toSet().size()} clust ${clusters.toSet().size()}"

        classesFile.write(JsonOutput.toJson(classes))
        clustersFile.write(JsonOutput.toJson(clusters))

        List<String> resultsList = resultFromPython().split(',')

        vMeasure = resultsList[0].toDouble()
        homogeniety = resultsList[1].toDouble()
        completness = resultsList[2].toDouble()
    }

    private static String resultFromPython(){

        String resultFromPython

        try {
            CallVmeasurePython cp0 = new CallVmeasurePython()
            resultFromPython = cp0.proce()

        } catch (Exception e) {
            println "Exeception  in callVmeasurePython $e"
        }
        return  resultFromPython
    }
}
