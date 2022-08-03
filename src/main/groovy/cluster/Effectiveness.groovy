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

    static Tuple4 querySetEffectiveness(Set<Query> querySet) {

        List<Double> f1list = [], precisionList = [], recallList = [], fitnessList = []

        Set<String> categoryNames = []
        int duplicateCategory = 0
        int missingCategories = 0

        querySet.each { Query q ->

            Tuple3 tuple3 = IndexUtils.getMostFrequentCategoryForQuery(q)
            String mostFrequentCategoryName = tuple3.first
            if (!categoryNames.add(mostFrequentCategoryName)) {
                duplicateCategory++
            }
            final int mostFrequentCategoryHitsSize = tuple3.second
            final int queryHitsSize = tuple3.third

            double recall = 0
            double precision = 0
            double f1 = 0

            TermQuery mostFrequentCategoryTermQuery = new TermQuery(new Term(Indexes.FIELD_CATEGORY_NAME,
                    mostFrequentCategoryName))

            if (mostFrequentCategoryHitsSize && queryHitsSize && mostFrequentCategoryTermQuery) {
                TotalHitCountCollector totalHitCollector = new TotalHitCountCollector()
                Indexes.indexSearcher.search(mostFrequentCategoryTermQuery, totalHitCollector);
                int categoryTotal = totalHitCollector.getTotalHits()

                assert categoryTotal

                recall = (double) mostFrequentCategoryHitsSize / categoryTotal
                precision = (double) mostFrequentCategoryHitsSize / queryHitsSize
                f1 = (2 * precision * recall) / (precision + recall)
            }

            f1list << f1
            precisionList << precision
            recallList << recall
        }

        Set<String> originalCategoryNames = IndexUtils.categoryFrequencies(Indexes.indexSearcher).keySet().asImmutable()

        categoryNames.each { categoryName ->
            if (!originalCategoryNames.contains(categoryName)) {
                missingCategories++
            }
        }

        final int categoriesPlusPenalty = Indexes.index.numberOfCategories + missingCategories + duplicateCategory

        final double averageF1ForQuerySet = (f1list) ? (double) f1list.sum() / categoriesPlusPenalty : 0
        final double averageRecallForQuerySet = (recallList) ? (double) recallList.sum() / categoriesPlusPenalty : 0
        final double averagePrecisionForQuerySet = (precisionList) ? (double) precisionList.sum() / categoriesPlusPenalty : 0

        assert averageF1ForQuerySet
        assert averageF1ForQuerySet > 0

        return new Tuple4<Double, Double, Double, List<Double>>(averageF1ForQuerySet, averagePrecisionForQuerySet, averageRecallForQuerySet, f1list)
    }

    static Tuple3<Double, Double, Double> classifierEffectiveness(Classifier classifier, IndexEnum testIndex, final int k) {
        index.Indexes.setIndex(testIndex)

        double f1Lucene
        double precisionLucene
        double recallLucene

        ConfusionMatrixGenerator.ConfusionMatrix confusionMatrix =
                ConfusionMatrixGenerator.getConfusionMatrix(
                        Indexes.indexReader,
                        classifier,
                        Indexes.FIELD_CATEGORY_NAME,
                        Indexes.FIELD_CONTENTS,
                        -1)

        // def m = confusionMatrix.getLinearizedMatrix()
        //println "lineraised m  $m"

        if (testIndex.numberOfCategories == k) {

            f1Lucene = confusionMatrix.getF1Measure()
            precisionLucene = confusionMatrix.getPrecision()
            recallLucene = confusionMatrix.getRecall()

            final int numberOfEvaluatedDocs = confusionMatrix.getNumberOfEvaluatedDocs()

            println "Lucene classifier f1: $f1Lucene precisionLucene: $precisionLucene recallLucene: $recallLucene numberOfEvaluatedDocs $numberOfEvaluatedDocs"
            println "linearizedMatrix ${confusionMatrix.getLinearizedMatrix()}"
        }

        //for setk case where number of categories may not match labelled classes
        else {

            List<Double> pList = []
            List<Double> rList = []
            Map categoriesMap = IndexUtils.categoryFrequencies(Indexes.indexSearcher)

            categoriesMap.keySet().each { categoryName ->
                pList << confusionMatrix.getPrecision(categoryName)
                rList << confusionMatrix.getRecall(categoryName)
            }

            final int maxCats = Math.max(k, testIndex.numberOfCategories)
            precisionLucene = pList.sum() / maxCats
            recallLucene = rList.sum() / maxCats
            f1Lucene = 2 * ((precisionLucene * recallLucene) / (precisionLucene + recallLucene))

            println "plist $pList rlist $rList"
            println "avaerage precisionLucene $precisionLucene average recallLucene $recallLucene f1k $f1Lucene k $k"
        }

        //   assert f1Lucene
        //   assert precisionLucene
        //   assert recallLucene

        return new Tuple3(f1Lucene, precisionLucene, recallLucene)
    }

    static Tuple4<Double, Double, Double, Integer> write_classes_clusters_for_v_measure(Classifier classifier, int job, boolean queriesOnly) {

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
