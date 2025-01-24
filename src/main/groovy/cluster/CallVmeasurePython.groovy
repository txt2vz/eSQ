package cluster

class CallVmeasurePython {

  //  https://www.baeldung.com/java-working-with-python
    static final String PYTHON_LOCATION =      /C:\Users\lauri\IdeaProjects\eSQ\venv\Scripts\python.exe/
        //     /C:\Users\student\IdeaProjects\eSQ\.venv\Scripts\python.exe/


    String processVmeasurePython() throws Exception {

        //ProcessBuilder processBuilder = new ProcessBuilder("python", /C:\Users\lauri\IdeaProjects\eSQ\src\main\python\vMeasure.py/ );
        ProcessBuilder processBuilder = new ProcessBuilder(PYTHON_LOCATION, /src\main\python\vMeasure.py/ );
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        String results =  process.text
        return  results
    }

    static void main(String[] args){
          try {

            CallVmeasurePython callPython = new CallVmeasurePython()
            callPython.processVmeasurePython()
        } catch (Exception e){
            print " Exeception $e"
        }
    }
}
