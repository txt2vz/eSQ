package cluster

class CallVmeasurePython {

  //  https://www.baeldung.com/java-working-with-python


    public String proce() throws Exception {

        ProcessBuilder processBuilder = new ProcessBuilder("python",/C:\Users\lauri\IdeaProjects\eSQ\src\main\python\vMeasure.py/ );
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        def results =  process.text
        println "results   " + results

        int exitCode = process.waitFor();
        return  results
      //  assertEquals("No errors should be detected", 0, exitCode);
    }


    static void main(String[] args){
        println "hello"


        try {
           // cp0.givenPythonScriptEngineIsAvailable_whenScriptInvoked_thenOutputDisplayed()
            CallVmeasurePython cp0 = new CallVmeasurePython()
            cp0.proce()
        } catch (Exception e){
            print " Exeception $e"
        }
    }
}
