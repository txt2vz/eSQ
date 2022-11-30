package index

import java.nio.file.Path
import java.nio.file.Paths

import static java.nio.charset.StandardCharsets.UTF_8;

class CrisisTweetsToFileFromCSV {

    Path docsPath = Paths.get('datasets/SourceData/crisis5Data/')
    String filesOutPath = /C:\Data\crisis5/

    //   Path docsPath = Paths.get('datasets/SourceData/crisis4Data/')
//    String filesOutPath = /C:\Data\crisis4/

    static main(args) {
        new CrisisTweetsToFileFromCSV().writeTweetsToFiles()
    }

    def writeTweetsToFiles() {

        int categoryNumber = 0

        docsPath.toFile().eachFileRecurse { file ->

            String catName = file.getName().take(8).replaceAll(/\W/, '').toLowerCase()
            catName = catName.replaceAll('_', '')
            println "File: $file  CatName: $catName"

            String dirName = filesOutPath + '\\' + catName
            println "dirName $dirName"
            File dirfile = new File(dirName)

            if (!dirfile.exists()) {
                dirfile.mkdir()
                println "made dir $dirfile"
            }

            int fileID = 0
            file.splitEachLine(',') { fields ->

                //line 0 are headings
                if (fileID > 0 && fileID <= 500) {

                    def textBody = fields[1]
                    def ontopic = fields[3]

                    String fileName = dirName + '\\' + fileID + '.txt'
                    println "filename $fileName"

                    if (textBody != " " && ontopic == "on-topic") {

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