# eSQ evolved search queries for document clustering
Uses a GA to create Apache Lucene ( https://lucene.apache.org/ ) queries suitable for document clustering

Install JDK for Java (21), Groovy (4) and Python (3) ( https://www.jetbrains.com/help/idea/configuring-python-sdk.html )

The python sklearn package is used for cluster evaluation
Install sklearn (https://scikit-learn.org/stable/) for cluster evaluation

Change the PYTHON_LOCATION in https://github.com/txt2vz/eSQ/blob/esqJenetics/src/main/groovy/cluster/CallVmeasurePython.groovy

## GA Engine 
Uses Jenetics.IO ( https://jenetics.io/ ) via  https://github.com/txt2vz/eSQ/blob/master/src/main/groovy/cluster/JeneticsMain.java

## Indexes
Lucene is used for indexing and several pre-built indexes are included in the 'indexes' folder.  
You can build your own index by adapting https://github.com/txt2vz/eSQ/blob/master/src/main/groovy/index/BuildIndex.groovy

## help
Please let me know if you are interested in helping to improve this project

## Papers
1. https://shura.shu.ac.uk/28567/ 
2. http://shura.shu.ac.uk/15409/
