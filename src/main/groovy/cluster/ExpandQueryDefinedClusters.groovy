package cluster

import groovy.json.JsonOutput
import index.Indexes
import org.apache.lucene.classification.ClassificationResult
import org.apache.lucene.classification.Classifier
import org.apache.lucene.document.Document
import org.apache.lucene.index.IndexReader
import org.apache.lucene.index.StoredFields
import org.apache.lucene.search.MatchAllDocsQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.ScoreDoc
import org.apache.lucene.search.TopDocs
import org.apache.lucene.util.BytesRef

class ExpandQueryDefinedClusters {

    final double vMeasure
    final double homogeneity
    final double completeness
    final double adjusted_rand

    final int numberOfDocumentsInQueryBuiltClusters
    final int clusterCountError
    final int numberOfClusters
    final int numberOfClasses

    ExpandQueryDefinedClusters(Classifier classifier) {

        List<String> classes = []   //the ground truth labels
        List<String> clusters = []  //clusters - use queries as labels - expand with Lucene Classifier (e.g. KNN)

        Query qAll = new MatchAllDocsQuery()
        TopDocs topDocs = Indexes.indexSearcher.search(qAll, Integer.MAX_VALUE)
        ScoreDoc[] allHits = topDocs.scoreDocs

        int unasscount = 0
        int qOnlyCount = 0
        IndexReader reader = Indexes.indexSearcher.getIndexReader();
        StoredFields storedFields = reader.storedFields();

        int countResults = 0
        int countNullResult = 0

        for (ScoreDoc sd : allHits) {

            Document d = storedFields.document(sd.doc);
            String category = d.get(Indexes.FIELD_CATEGORY_NAME)
            String clusterAssignedByQuery = d.get(Indexes.FIELD_QUERY_ASSIGNED_CLUSTER)
            String clusterAssignedByQueryThenByClassifier = clusterAssignedByQuery

            if (clusterAssignedByQuery == 'unassigned') {
                unasscount++
                ClassificationResult<BytesRef> classificationResult = classifier.assignClass(d.get(Indexes.FIELD_CONTENTS))

                if (classificationResult != null && classificationResult.assignedClass() != null) {
                    clusterAssignedByQueryThenByClassifier = classificationResult.assignedClass().utf8ToString()
                    countResults++
                } else {
                    countNullResult++
                }
            }

            classes.add(category)
            clusters.add(clusterAssignedByQueryThenByClassifier)
        }

        numberOfClusters = clusters.toSet().size()
        numberOfClasses = classes.toSet().size()
        clusterCountError = numberOfClusters - numberOfClasses

        assert classes.size() == clusters.size()
        assert classes.size() > 0
        assert Indexes.index.numberOfClasses == numberOfClasses
        assert numberOfClasses == Indexes.index.numberOfClasses

        numberOfDocumentsInQueryBuiltClusters = Indexes.indexReader.maxDoc() - unasscount

        //println("In ExpandQueryDefinedClusters countResults $countResults countNullResults $countNullResult")
        println "In ExpandQueryDefinedClusters Unassigned: $unasscount Classes: ${classes.toSet().size()} Clusters: ${clusters.toSet().size()} cluserSet ${clusters.toSet()}"

        File classesFile = new File(/results\classes.txt/)
        File clustersFile = new File(/results\clusters.txt/)
        classesFile.write(JsonOutput.toJson(classes))
        clustersFile.write(JsonOutput.toJson(clusters))

        //use sklearn in python to obtain v, h, c, ari
        List<String> resultsList = CallVmeasurePython.processVmeasurePython()

        vMeasure = resultsList[0].toDouble()
        homogeneity = resultsList[1].toDouble()
        completeness = resultsList[2].toDouble()
        adjusted_rand = resultsList[3].toDouble()
    }
}
