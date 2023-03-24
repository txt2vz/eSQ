# https://scikit-learn.org/stable/auto_examples/text/plot_document_clustering.html

from pathlib import Path
import sklearn
from sklearn.datasets import fetch_20newsgroups
from sklearn.decomposition import TruncatedSVD
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.feature_extraction.text import HashingVectorizer
from sklearn.feature_extraction.text import TfidfTransformer
from sklearn.pipeline import make_pipeline
from sklearn.preprocessing import Normalizer
from sklearn import metrics

from sklearn.cluster import DBSCAN, AgglomerativeClustering, KMeans, MiniBatchKMeans

import logging
from optparse import OptionParser
import sys
from time import time
import numpy as np
import os


data_folder = "C:/Data/DataSetForPaper2023/"

#collection_list = ["crisis3", "NG3", "crisis4", "R4", "NG5", "R5", "NG6", "R6"]
collection_list = ["crisis3", "NG3"]

for collectionName in collection_list:

    container_path = Path(data_folder + collectionName)

    dataset = sklearn.datasets.load_files(container_path,  description=None, categories=None, load_content=True,
                                          shuffle=True, encoding='utf-8', decode_error='ignore', random_state=0, allowed_extensions=None)

    print("%d documents" % len(dataset.data))
    print("%d categories" % len(dataset.target_names))
    print()

    # Feature Extraction
    # ------------------

    labels = dataset.target
    true_k = np.unique(labels).shape[0]

    print("Extracting features from the training dataset using a sparse vectorizer")
    t0 = time()

    useHashing = True
    useIDF = True
    nFeature = 10000

    if useHashing:
        if useIDF:
            # Perform an IDF normalization on the output of HashingVectorizer
            hasher = HashingVectorizer(
                n_features=nFeature,
                stop_words="english",
                alternate_sign=False,
                norm=None,
            )
            vectorizer = make_pipeline(hasher, TfidfTransformer())
        else:
            vectorizer = HashingVectorizer(
                n_features=nFeature,
                stop_words="english",
                alternate_sign=False,
                norm="l2",
            )
    else:
        print("tfidf vectorizer")
        vectorizer = TfidfVectorizer(
            max_df=0.5,
            max_features= 10000, #  opts.n_features,
            min_df=2,
            stop_words="english",
            use_idf= True #opts.use_idf,
        )

    # clustering runs
    for i in range(3):
        X = vectorizer.fit_transform(dataset.data)

        print("done in %fs" % (time() - t0))
        print("n_samples: %d, n_features: %d" % X.shape)
        numDocs = X.shape[0]

        print()

        km = KMeans(
            n_clusters=true_k,
            init="k-means++",
            max_iter=100,
            n_init=1,
            verbose= False
        )

        print("kMeans ++ run number: " + str(i))
        print("Clustering sparse data with %s" % km)
        t0 = time()
        km.fit(X)
        print("done in %0.3fs" % (time() - t0))

        # %%
        # Performance metrics
        # -------------------

        v = metrics.v_measure_score(labels, km.labels_)
        h = metrics.homogeneity_score(labels, km.labels_)
        c = metrics.completeness_score(labels, km.labels_)
        adjustedRand = metrics.adjusted_rand_score(labels, km.labels_)

        print("V-measure: %0.3f" % v)
        print("Homogeneity: %0.3f" % h)
        print("Completeness: %0.3f" % c)

        print("Adjusted Rand-Index: %.3f" %
              metrics.adjusted_rand_score(labels, km.labels_))
        print("Silhouette Coefficient: %0.3f" %
              metrics.silhouette_score(X, km.labels_, sample_size=1000))

        filePath = "resultsKpython.csv"
        resultsFile = open(filePath, "a")

        if os.path.getsize(filePath) == 0:
            resultsFile.write("index, v, h, c, adjustRand, numDocs \n")

        resultsFile.write(collectionName + ", " + str(v) +
                          ", " + str(h) + ", " + str(c) +  ", " + str(adjustedRand) + ", " + str(numDocs) + "\n")

        print()
        resultsFile.close()
