from pathlib import Path
from sklearn.metrics.cluster import v_measure_score
from sklearn.metrics.cluster import homogeneity_score
from sklearn.metrics.cluster import completeness_score

import numpy as np

import json

classesFile = open( "results/classes.txt", "r")
clustersFile = open( "results/clusters.txt", "r")

classes = classesFile.read()
clusters = clustersFile.read()

classesL = classes.split(",")
clustersL = clusters.split(",")

#print ("classes len: ", len(classesL) )
#print("clusters len: " , len(clustersL))

v = v_measure_score (classesL, clustersL)
h = homogeneity_score (classesL, clustersL)
c = completeness_score (classesL, clustersL)

resString = str(v) + ", " + str(h) + ", " + str(c)
print (resString)

resultsFile = open ("results/resultsPython.csv" , "w")
resultsFile.write(resString)
resultsFile.close()