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

    final double vMeasure
    final double homogeneity
    final double completeness
    final double adjusted_rand

    final int numberOfDocumentsInQueryBuiltClusters
    final int clusterCountError
    final int numberOfClusters
    final int numberOfClasses

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

            String clusterAssignedByQuery = d.get(Indexes.FIELD_QUERY_ASSIGNED_CLUSTER)
            String clusterAssignedByQueryThenByClassifier = clusterAssignedByQuery

            if (clusterAssignedByQuery == 'unassigned') {
                unasscount++;

                if (!queriesOnly) {
                    clusterAssignedByQueryThenByClassifier = classifier.assignClass(d.get(Indexes.FIELD_CONTENTS)).getAssignedClass().utf8ToString()
                }
            }

            if (queriesOnly) {
                if (clusterAssignedByQuery != 'unassigned') {
                    classes.add(category)
                    clusters.add(clusterAssignedByQuery)
                    qOnlyCount++
                }
            } else {
                classes.add(category)
                clusters.add(clusterAssignedByQueryThenByClassifier)
            }
        }

        numberOfClusters = clusters.toSet().size()
        numberOfClasses = classes.toSet().size()
        clusterCountError = numberOfClusters - numberOfClasses

        assert classes.size() == clusters.size()
        assert classes.size() > 0
        assert numberOfClasses == Indexes.index.numberOfCategories

        numberOfDocumentsInQueryBuiltClusters =  Indexes.indexReader.maxDoc() - unasscount

        println "In Effectiveness Unassigned: $unasscount Classes: ${classes.toSet().size()} Clusters: ${clusters.toSet().size()}"

        File classesFile = new File(/results\classes.txt/)
        File clustersFile = new File(/results\clusters.txt/)
        classesFile.write(JsonOutput.toJson(classes))
        clustersFile.write(JsonOutput.toJson(clusters))

        List<String> resultsList = resultFromPython().split(',')

        vMeasure = resultsList[0].toDouble()
        homogeneity = resultsList[1].toDouble()
        completeness = resultsList[2].toDouble()
        adjusted_rand = resultsList[3].toDouble()
    }

    //call sklearn to get v measure
    private static String resultFromPython(){

        String resultFromPython = "no result from python"

        try {
            CallVmeasurePython callVmeasurePython = new CallVmeasurePython()
            resultFromPython = callVmeasurePython.processVmeasurePython()

        } catch (Exception e) {
            println "Exeception  in callVmeasurePython $e"
        }
        return  resultFromPython
    }
}
