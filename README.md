# eSQ evolved search queries for document clustering
Uses a GA to create Apache Lucene ( https://lucene.apache.org/ ) queries

Install support for Java (17), Groovy and Python ( https://www.jetbrains.com/help/idea/configuring-python-sdk.html )

Install sklearn (https://scikit-learn.org/stable/) for cluster evaluation

## GA Engines 
1. You can run eSQ using ECJ (https://cs.gmu.edu/~eclab/projects/ecj/ ) via https://github.com/txt2vz/eSQ/blob/master/src/main/groovy/cluster/ClusterMainECJ.groovy 
2. Alternatively you can use the more modern Jenetics.IO ( https://jenetics.io/ ) via  https://github.com/txt2vz/eSQ/blob/master/src/main/groovy/cluster/JeneticsMain.java

We have found ECJ to produce slightly better results which we believe may be to do with the use of subpopulations

## Indexes
Lucene is used for indexing and several pre-built indexes are included in the 'indexes' folder.  
You can build your own index by using https://github.com/txt2vz/eSQ/blob/master/src/main/groovy/index/BuildIndex.groovy

## Papers
1. https://shura.shu.ac.uk/28567/ 
2. http://shura.shu.ac.uk/15409/
