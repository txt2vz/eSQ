# eSQ evolved search queries for document clustering
## Uses GA to create queries

Install support for Java (17) and Groovy
Install python

pip3 install sklearn

# Papers
1. https://shura.shu.ac.uk/28567/ 
2.  http://shura.shu.ac.uk/15409/

# GA Engines 
1. You can run eSQ using ECJ (https://cs.gmu.edu/~eclab/projects/ecj/ ) via https://github.com/txt2vz/eSQ/blob/master/src/main/groovy/cluster/ClusterMainECJ.groovy 
2. Alternatively you can use the more modern Jenetics.IO ( https://jenetics.io/ ) via  https://github.com/txt2vz/eSQ/blob/master/src/main/groovy/cluster/JeneticsMain.java

We have found ECJ to produce slightly better results which we believe may be to do with the use of subpopulations
