# https://scikit-learn.org/stable/auto_examples/text/plot_document_clustering.html
# Author: Peter Prettenhofer <peter.prettenhofer@gmail.com>
#         Lars Buitinck
# License: BSD 3 clause

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

# Display progress logs on stdout
logging.basicConfig(level=logging.INFO,
                    format="%(asctime)s %(levelname)s %(message)s")

# parse commandline arguments
op = OptionParser()
op.add_option(
    "--lsa",
    dest="n_components",
    type="int",
    help="Preprocess documents with latent semantic analysis.",
)
op.add_option(
    "--no-minibatch",
    action="store_false",
    dest="minibatch",
    default=True,
    help="Use ordinary k-means algorithm (in batch mode).",
)
op.add_option(
    "--no-idf",
    action="store_false",
    dest="use_idf",
    default=True,
    help="Disable Inverse Document Frequency feature weighting.",
)
op.add_option(
    "--use-hashing",
    action="store_true",
    default=False,
    help="Use a hashing feature vectorizer",
)
op.add_option(
    "--n-features",
    type=int,
    default=10000,
    help="Maximum number of features (dimensions) to extract from text.",
)
op.add_option(
    "--verbose",
    action="store_true",
    dest="verbose",
    default=False,
    help="Print progress reports inside k-means algorithm.",
)

# print(__doc__)


def is_interactive():
    return not hasattr(sys.modules["__main__"], "__file__")


if not is_interactive():
 #   op.print_help()
    print()


# work-around for Jupyter notebook and IPython console
argv = [] if is_interactive() else sys.argv[1:]
(opts, args) = op.parse_args(argv)
if len(args) > 0:
    op.error("this script takes no arguments.")
    sys.exit(1)


# %%
# Load some categories from the training set
# ------------------------------------------

#categories = [
    #  "alt.atheism",
    #  "talk.religion.misc",
    #  "comp.graphics",
#    "sci.space",
#    "rec.sport.hockey",
#    "soc.religion.christian"
#]
# Uncomment the following to do the analysis on all the categories
# categories = None

# print("Loading 20 newsgroups dataset for categories:")
# print(categories)

d_folder = "C:/Data/DataSetForPaper2023/"
# collection_name = "R4"
# collection_name = "NG4"
# collection_name = "crisis3"
# collection_name = "NG5"
# collection_name = "NG6Full"
# collection_name = "NG3N"
#collection_name = "NG4N"

collection_list = ["crisis3", "NG3", "crisis4", "R4", "NG5", "R5", "NG6", "R6"]
#collection_list = ["crisis5"]
#collection_list = ["crisis3"]

# container_path = Path("C:/Data/N3")
# container_path = Path("C:/Data/NG3Full")
# container_path = Path("C:/Data/crisis3")
# container_path = Path(d_folder + collection_name)
# container_path = Path("C:/Data/R6Train")
# container_path = Path("C:/Data/NG4")

# loop here?
for collectionName in collection_list:

    container_path = Path(d_folder + collectionName)

    datan = sklearn.datasets.load_files(container_path,  description=None, categories=None, load_content=True,
                                        shuffle=True, encoding='utf-8', decode_error='ignore', random_state=0, allowed_extensions=None)
# print("%d categories  " % len(datan))

# dataset = fetch_20newsgroups(
#    subset="all", categories=categories, shuffle=True, random_state=42
# )

    dataset = datan

    print("%d documents" % len(dataset.data))
    print("%d categories" % len(dataset.target_names))
    print()


# %%
# Feature Extraction
# ------------------

    labels = dataset.target
    true_k = np.unique(labels).shape[0]

    print("Extracting features from the training dataset using a sparse vectorizer")
    t0 = time()

    if opts.use_hashing:
        if opts.use_idf:
            # Perform an IDF normalization on the output of HashingVectorizer
            hasher = HashingVectorizer(
                n_features=opts.n_features,
                stop_words="english",
                alternate_sign=False,
                norm=None,
            )
            vectorizer = make_pipeline(hasher, TfidfTransformer())
        else:
            vectorizer = HashingVectorizer(
                n_features=opts.n_features,
                stop_words="english",
                alternate_sign=False,
                norm="l2",
            )
    else:
        print("tfidf vectorizer")
        vectorizer = TfidfVectorizer(
            max_df=0.5,
            max_features=opts.n_features,
            min_df=2,
            stop_words="english",
            use_idf=opts.use_idf,

        )

# clustering runs
    for i in range(2):
        X = vectorizer.fit_transform(dataset.data)

        print("done in %fs" % (time() - t0))
        print("n_samples: %d, n_features: %d" % X.shape)
        numDocs = X.shape[0]

        print
        print()

        km = KMeans(
            n_clusters=true_k,
            init="k-means++",
            max_iter=100,
            n_init=1,
            verbose=opts.verbose,
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
# %%

        if not opts.use_hashing:
            print("Top terms per cluster:")

            if opts.n_components:
                original_space_centroids = svd.inverse_transform(
                    km.cluster_centers_)
                order_centroids = original_space_centroids.argsort()[:, ::-1]
            else:
                order_centroids = km.cluster_centers_.argsort()[:, ::-1]

            terms = vectorizer.get_feature_names_out()
            for i in range(true_k):
                print("Cluster %d:" % i, end="")
                for ind in order_centroids[i, :10]:
                    print(" %s" % terms[ind], end="")
                print()
        resultsFile.close()
