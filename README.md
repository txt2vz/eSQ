# eSQ evolved search queries for document clustering
Uses a GA to create [Apache Lucene](https://lucene.apache.org/) queries suitable for document clustering.

Install JDK for Java (21), Groovy (4) and Python (3).

The python sklearn package is used for cluster evaluation.
Install [sklearn](https://scikit-learn.org/stable/) for cluster evaluation.

Change the PYTHON_LOCATION in https://github.com/txt2vz/eSQ/blob/master/src/main/groovy/cluster/CallVmeasurePython.groovy.

## GA Engine 
Uses [Jenetics.IO](https://jenetics.io/) 
To run use: https://github.com/txt2vz/eSQ/blob/master/src/main/groovy/cluster/JeneticsMain.java

## Indexes
Several pre-built Lucene indexes are included in the 'indexes' folder.  
You can build your own index by adapting https://github.com/txt2vz/eSQ/blob/master/src/main/groovy/index/BuildIndex.groovy

## Help
Please let me know if you are interested in helping to improve this project.

## Papers
1. Latest: [Document clustering with evolved multi-word search queries](https://link.springer.com/article/10.1007/s12065-025-01018-w?utm_source=rct_congratemailt&utm_medium=email&utm_campaign=oa_20250224&utm_content=10.1007/s12065-025-01018-w)
2. https://shura.shu.ac.uk/28567/ 
3. http://shura.shu.ac.uk/15409/
