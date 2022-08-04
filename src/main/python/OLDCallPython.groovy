import javax.script.ScriptContext
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import javax.script.SimpleScriptContext

class CallPython {

  //  https://www.baeldung.com/java-working-with-python

    public void givenPythonScriptEngineIsAvailable_whenScriptInvoked_thenOutputDisplayed() throws Exception {
        StringWriter writer = new StringWriter();
        ScriptContext context = new SimpleScriptContext();
        context.setWriter(writer);

        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("python");
      //  def f = new FileReader(/C:\Users\lauri\IdeaProjects\eSQ\src\main\groovy\hello.py/)
     //   println f.text
     //   println f.toString()
        println "z " + engine.eval('print ("tat")')
     //   engine.eval(f, context);
     //   println writer.toString()
        engine.eval(new FileReader(/C:\Users\lauri\IdeaProjects\eSQ\src\main\groovy\hello.py/), context);
//        assertEquals("Should contain script output: ", "Hello Baeldung Readers!!", writer.toString().trim());
        println "wrt string " + writer.toString()
    }

    public void proce() throws Exception {
        //ProcessBuilder processBuilder = new ProcessBuilder("python", /C:\Users\lauri\IdeaProjects\eSQ\src\main\groovy\hello.py/);
        ProcessBuilder processBuilder = new ProcessBuilder("python",/C:\Users\lauri\IdeaProjects\eSQ\src\main\vMeasure.py/ );
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
       // List<String> results = readProcessOutput(process.getInputStream());
      // def results =  process.getInputStream().text
        def results =  process.text


        println "resutls  " + results

       // assertThat("Results should not be empty", results, is(not(empty())));
       // assertThat("Results should contain output of script: ", results, hasItem(
        //        containsString("Hello Baeldung Readers!!")));

        int exitCode = process.waitFor();
      //  assertEquals("No errors should be detected", 0, exitCode);
    }



    static void main(String[] args){
        println "hello"

        CallPython cp0 = new CallPython()
        try {
           // cp0.givenPythonScriptEngineIsAvailable_whenScriptInvoked_thenOutputDisplayed()
            cp0.proce()
        } catch (Exception e){
            print " Exeception $e"
        }
    }

}
