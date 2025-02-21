package cluster

class CallVmeasurePython {

    static final String PYTHON_LOCATION =  /C:\Users\lauri\IdeaProjects\eSQ\venv\Scripts\python.exe/
    //     /C:\Users\student\IdeaProjects\eSQn\.venv\Scripts\python.exe/

    static List<String> processVmeasurePython() throws Exception {

        ProcessBuilder processBuilder = new ProcessBuilder(PYTHON_LOCATION, /src\main\python\vMeasure.py/)
        processBuilder.redirectErrorStream(true)
        Process process = processBuilder.start()
        return process.text.split(',')
    }
}
