package cluster

class CallPythonToExpandKeywordClusters {

    static final String PYTHON_LOCATION =  /C:\Users\lauri\eSQ\eSQ\.venv\Scripts\python.exe/
    //     /C:\Users\student\IdeaProjects\eSQ\.venv\Scripts\python.exe/

    static void processPythonExpandClusters() throws Exception {

        ProcessBuilder processBuilder = new ProcessBuilder(PYTHON_LOCATION, /src\main\python\expandKeywordClusters.py/)
        processBuilder.redirectErrorStream(true)
        Process process = processBuilder.start()
       // return process.text.split(',')
    }
}
