package index

import java.nio.file.Path
import java.nio.file.Paths

import static java.nio.charset.StandardCharsets.UTF_8;

class CrisisToFileFromCSV {

    Path docsPath = Paths.get(/C:\Users\lauri\OneDrive - Sheffield Hallam University\Research\DataSets\crisisData3/)
    Path filesOut = Paths.get(/C:\Data\crisis3/)

    def catsFreq = [:]

    static main(args) {
        def i = new CrisisToFileFromCSV()
        i.buildIndex()
    }

    def buildIndex() {

        Date start = new Date();
        int categoryNumber = 0
        int id = 0

        docsPath.toFile().eachFileRecurse { file ->

            String catName = file.getName().take(6).replaceAll(/\W/, '').toLowerCase()
            catName = catName.replaceAll('_', '')
            println "File: $file  CatName: $catName"

            int tweetCountPerFile = 0
            int fileID = 0
            file.splitEachLine(',') { fields ->

                if (fileID > 0 && fileID < 500) {

                    String tweetID = fields[0]
                    def textBody = fields[1]

                    String fileName = 'C:\\Data\\crisis3\\' + catName + '\\' + fileID + '\\'
                    println "filename " + fileName

                    Path filesOutP = Paths.get(fileName)

                    if (textBody != " "   ) {// && textBody.size() > 10 ) {

                        byte[] bytes = textBody.getBytes()
                        String utf8String = new String(bytes, UTF_8);

                        utf8String = utf8String.replaceAll("[^\\p{ASCII}]", "")

                        if (utf8String != "" && utf8String.size() > 1) {
                            File f = new File(fileName)
                            f.createNewFile()
                            //f.write(utf8String.toLowerCase())
                            f.write(utf8String)
                        }
                    }
                }
                fileID++
            }
            categoryNumber++

        }
        println 'done...'
    }
}