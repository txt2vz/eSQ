# https://scikit-learn.org/stable/auto_examples/text/plot_document_clustering.html

from pathlib import Path
import sklearn
from sklearn.datasets import load_files #fetch_20newsgroups
from sklearn.decomposition import TruncatedSVD
from sklearn.feature_extraction.text import TfidfVectorizer
#from sklearn.feature_extraction.text import HashingVectorizer
from sklearn.feature_extraction.text import TfidfTransformer
from sklearn.pipeline import make_pipeline
from sklearn.preprocessing import Normalizer
from sklearn import metrics
from sklearn.cluster import AgglomerativeClustering
from sklearn.cluster import SpectralClustering

from sklearn.cluster import DBSCAN, AgglomerativeClustering, KMeans, MiniBatchKMeans

import logging
from optparse import OptionParser
import sys
from time import time
import numpy as np
import os
from datetime import datetime

data_folder = "C:/Data/DataSetForPaper2023/"
#data_folder = "C:/Users/aceslh/Data/DataSetForPaper2023/"

#collection_list = ["crisis3", "NG3", "crisis4", "R4", "NG5", "R5", "NG6", "R6"]
collection_list = ["NG6"]
#collection_list = ["R6", "NG6"]

for collectionName in collection_list:

    container_path = Path(data_folder + collectionName)

    dataset = sklearn.datasets.load_files(container_path,  description=None, categories=None, load_content=True,
                                          shuffle=True, encoding='utf-8', decode_error='ignore', random_state=0, allowed_extensions=None)

    print(f"Documents: {len(dataset.data)}  Categories: {len(dataset.target_names)}")
    print()

    # Feature Extraction
    labels = dataset.target
    true_k = np.unique(labels).shape[0]
    maxFeature = 200

    print("tfidf vectorizer")
    vectorizer = TfidfVectorizer(
        max_df=0.5,
        max_features= maxFeature,
        min_df=2,
        stop_words="english",
        use_idf= True,
    )

    t0 = time()
    X = vectorizer.fit_transform(dataset.data)
    tvm = vectorizer.fit_transform(dataset.data).toarray()
    print(f"Vectorizing done in {(time() - t0)}")
    numDocs = X.shape[0]
    print(f"numdocs: {numDocs} ")
    print()

    # clustering runs
    for i in range(2):

        km = KMeans(
            n_clusters=true_k,
            init="k-means++",
            max_iter=100,
            n_init= 2,
            verbose= False
        )

        print(f"kMeans ++ run number: {i}")
        print(f"Clustering sparse data with {km}")
        t0 = time()
        km.fit(X)
        print("done in %0.3fs" % (time() - t0))

        # %%
        # Performance metrics
        # -------------------

        km_v = metrics.v_measure_score(labels, km.labels_)
        km_h = metrics.homogeneity_score(labels, km.labels_)
        km_c = metrics.completeness_score(labels, km.labels_)
        km_adjustedRand = metrics.adjusted_rand_score(labels, km.labels_)
        #nmi = metrics.normalized_mutual_info_score(labels, km.labels_)

        print(f"K Means V-measure: {km_v:.5f} Homogeneity: {km_h:.2f} Completeness: {km_c:.2f} Adjusted Rand-Index: {km_adjustedRand:.2f}")
        # print("NMI: %.3f" % nmi)
        #  metrics.adjusted_rand_score(labels, km.labels_))
        #print("Silhouette Coefficient: %0.3f" %
        #      metrics.silhouette_score(X, km.labels_, sample_size=1000))

        ag =  AgglomerativeClustering(n_clusters = None, distance_threshold= 1.70). fit(tvm)

        ag_v = metrics.v_measure_score(labels, ag.labels_)
        ag_h = metrics.homogeneity_score(labels, ag.labels_)
        ag_c = metrics.completeness_score(labels, ag.labels_)
        ag_adjustedRand = metrics.adjusted_rand_score(labels, ag.labels_)

        labelLength = ag.labels_
        uniqueLabel=len(np.unique(labelLength))

        print(f"ag v {ag_v}  ag rand {ag_adjustedRand} ag unique label length {uniqueLabel}")

        sc= SpectralClustering(n_clusters = true_k, affinity ='nearest_neighbors').fit(tvm)

        sc_v = metrics.v_measure_score(labels, sc.labels_)
        sc_h = metrics.homogeneity_score(labels, sc.labels_)
        sc_c = metrics.completeness_score(labels, sc.labels_)
        sc_adjustedRand = metrics.adjusted_rand_score(labels, sc.labels_)

        print(f"spectral v {sc_v} spectral adjustred rand {sc_adjustedRand}")

        filePath = "resultsKpython.csv"
        resultsFile = open(filePath, "a")

        if os.path.getsize(filePath) == 0:
            resultsFile.write("index, km_v, km_h, km_c, km_adjustRand, ag_v, ag_adjustedRand, sc_v, sc_adjustedRand, maxFeature, numDocs, date \n")

        resultsFile.write(f"{collectionName}, {km_v}, {km_h}, {km_c}, {km_adjustedRand}, {ag_v}, {ag_adjustedRand}, {sc_v}, {sc_adjustedRand}, {maxFeature}, {numDocs}, {datetime.now()}  \n")

        print()
        resultsFile.close()
