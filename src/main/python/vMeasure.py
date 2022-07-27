from pathlib import Path
from sklearn.metrics.cluster import v_measure_score
from sklearn.metrics.cluster import homogeneity_score
from sklearn.metrics.cluster import completeness_score

from sklearn.metrics import precision_recall_fscore_support
import numpy as np
from sklearn.metrics import precision_score
from sklearn.metrics import f1_score

import json

data_folder = Path("C:/Users/lauri/IdeaProjects/eSQ/")
classesFile = open( data_folder /"classes.txt", "r")
clustersFile = open( data_folder /"clusters.txt", "r")

classes = classesFile.read()
clusters = clustersFile.read()

classesL = classes.split(",")
clustersL = clusters.split(",")

#print ("classes len: ", len(classesL) )
#print("clusters len: " , len(clustersL))

v = v_measure_score (classesL, clustersL)
h = homogeneity_score (classesL, clustersL)
c = completeness_score (classesL, clustersL)

#print("v measure:    %.4f" % v_measure_score(classesL, clustersL))
#print("homogenity:   %.4f" % homogeneity_score(classesL, clustersL))
#print("completeness: %.4f " % completeness_score(classesL, clustersL))

#resString = str(v) + ", " + str(h) + ", " + str(c) + "\n"
resString = str(v) + ", " + str(h) + ", " + str(c)
#print ("resString " + resString)
print (resString)

resultsFile = open (data_folder /"resultsPython.csv" , "a")
resultsFile.write(resString)
resultsFile.close()