package cluster

import index.IndexEnum
import index.IndexUtils
import index.Indexes
import org.apache.lucene.classification.Classifier
import org.apache.lucene.classification.utils.ConfusionMatrixGenerator
import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.TotalHitCountCollector

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

        assert f1Lucene
        assert precisionLucene
        assert recallLucene

        return new Tuple3(f1Lucene, precisionLucene, recallLucene)
    }
}
