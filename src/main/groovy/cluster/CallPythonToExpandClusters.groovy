package cluster

class CallPythonToExpandKeywordClusters {

    static final String PYTHON_LOCATION =  /C:\Users\lauri\eSQ\eSQ\.venv\Scripts\python.exe/
    //     /C:\Users\student\IdeaProjects\eSQ\.venv\Scripts\python.exe/

    static int  processPythonExpandClusters() throws Exception {
        println "Calling Python script to expand keyword clusters..."

        ProcessBuilder processBuilder = new ProcessBuilder(PYTHON_LOCATION, /src\main\python\expandKeywordClusters.py/)
        processBuilder.redirectErrorStream(true)
        Process process = processBuilder.start()
        String output = process.inputStream.text     
        println "********************************************************   "
        println "Output from Python script:\n$output"
        int exitCode = process.waitFor()  // Groovy waits here until Python is done

    if (exitCode == 0) {
        println "Python finished successfully"
    } else {
        println "Python failed with exit code: ${exitCode}"
    }
    return exitCode
    }    
}
