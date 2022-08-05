package cluster

class CallVmeasurePython {

  //  https://www.baeldung.com/java-working-with-python


    String proce() throws Exception {

        ProcessBuilder processBuilder = new ProcessBuilder("python", /C:\Users\lauri\IdeaProjects\eSQ\src\main\python\vMeasure.py/ );
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        def results =  process.text
        println "Results in CallVmeasurePython: " + results

        int exitCode = process.waitFor();
        return  results
    }

    static void main(String[] args){
          try {
           // cp0.givenPythonScriptEngineIsAvailable_whenScriptInvoked_thenOutputDisplayed()
            CallVmeasurePython cp0 = new CallVmeasurePython()
            cp0.proce()
        } catch (Exception e){
            print " Exeception $e"
        }
    }
}
