# eSQ evolved search queries for document clustering
Uses a GA to create Apache Lucene ( https://lucene.apache.org/ ) queries

Install JDK for Java (21), Groovy (4) and Python ( https://www.jetbrains.com/help/idea/configuring-python-sdk.html )

Install sklearn (https://scikit-learn.org/stable/) for cluster evaluation

## GA Engine 
Uses Jenetics.IO ( https://jenetics.io/ ) via  https://github.com/txt2vz/eSQ/blob/master/src/main/groovy/cluster/JeneticsMain.java

## Indexes
Lucene is used for indexing and several pre-built indexes are included in the 'indexes' folder.  
You can build your own index by adapting https://github.com/txt2vz/eSQ/blob/master/src/main/groovy/index/BuildIndex.groovy

## Papers
1. https://shura.shu.ac.uk/28567/ 
2. http://shura.shu.ac.uk/15409/
