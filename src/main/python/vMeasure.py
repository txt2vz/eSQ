from sklearn.metrics.cluster import v_measure_score
from sklearn.metrics.cluster import homogeneity_score
from sklearn.metrics.cluster import completeness_score
from sklearn.metrics.cluster import adjusted_rand_score

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

adjusted_rand_score = adjusted_rand_score(classesL, clustersL)

resultString =  f"{str(v)}, {str(h)}, {str(c)}, {str(adjusted_rand_score)}"
print (resultString)

resultsFile = open ("results/resultsPython.csv" , "w")
resultsFile.write(resultString)
resultsFile.close()