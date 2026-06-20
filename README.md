# eSQ evolved search queries for document clustering
Uses a GA to create search queries suitable for document clustering.

Install JDK for Java (21), Groovy (4) and Python (3). 

The Java library Apache Lucene is required for query generation:  [Apache Lucene](https://lucene.apache.org/)

The python sklearn package is used for cluster evaluation.
Install [sklearn](https://scikit-learn.org/stable/) for cluster evaluation.

Change the PYTHON_LOCATION in src/main/groovy/cluster/CallPythonToExpandClusters.java

## Overview
The eSQ (evolved Search Queries) is a system for automatic document clustering. Two stages are involved.  

**Stage 1** uses a Genetic Algorithm to build a set of queries. To run stage 1 run JeneticsMain.java
The evolved search queries are stored in JSON format, for example when evolving queries for the NG3 dataset  the following JSON is stored in a file named NG3_keywordSet.json.
[
    [
        "space",
        "nasa",
        "lunar"
    ],
    [
        "god",
        "faith",
        "truth"
    ],
    [
        "team",
        "teams",
        "season"
    ]
]

The keyword sets can be used to build document clusters.  For example, the first cluster would be the set of documents which contain any of the words: ‘space’,  ‘nasa’, ‘lunar’.  The keyword sets are able to produce effective clusters but do not include all documents in the dataset.  

**Stage 2** expands the clusters to include all documents.  To run stage 2 run the python program expandKeywordClusters.py
The keyword sets from stage 1 are used to build base clusters which are used as training documents for various classifiers.  Results are reported.

Note: to call python and run the expansion set expandKeywordClustersWithPython to true in JeneticsMain.java

## GA Engine 
Uses [Jenetics.IO](https://jenetics.io/) 
To run use: https://github.com/txt2vz/eSQ/blob/eSQJ/src/main/java/cluster/JeneticsMain.java

## Indexes
eSQ uses Apache Lucene for indexing and firing queries. Several pre-built Lucene indexes are included in the 'indexes' folder.  
You can build your own index by adapting https://github.com/txt2vz/eSQ/blob/eSQJ/src/main/java/index/BuildIndex.java

## Help
Please let me know if you are interested in helping to improve this project.

## Papers
1. Latest: [Document clustering with evolved multi-word search queries](https://link.springer.com/article/10.1007/s12065-025-01018-w?utm_source=rct_congratemailt&utm_medium=email&utm_campaign=oa_20250224&utm_content=10.1007/s12065-025-01018-w)
2. https://shura.shu.ac.uk/28567/ 
3. http://shura.shu.ac.uk/15409/
