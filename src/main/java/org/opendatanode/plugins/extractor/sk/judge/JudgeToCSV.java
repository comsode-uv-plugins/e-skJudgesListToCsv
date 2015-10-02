package org.opendatanode.plugins.extractor.sk.judge;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class JudgeToCSV {

    private static final String FILE_HEADER = "aktivny;meno;funkcia;sud;poznamka";
    private static final String NEW_LINE = "\n";
    private static final String DELIMITER = ";";

    public static void toCSV(List<Judge> judges, File csvFile) throws IOException {
        FileWriter fw = null;
        try {
            fw = new FileWriter(csvFile);
            fw.append(FILE_HEADER);
            fw.append(NEW_LINE);
            
            for (Judge judge : judges) {
                fw.append(String.valueOf(judge.isActive()));
                fw.append(DELIMITER);
                fw.append(String.valueOf(judge.getName()));
                fw.append(DELIMITER);
                fw.append(String.valueOf(judge.getFunction()));
                fw.append(DELIMITER);
                fw.append(String.valueOf(judge.getLocation()));
                fw.append(DELIMITER);
                fw.append(String.valueOf(judge.getNote()));
                fw.append(NEW_LINE);
            }
        } finally {
            if (fw != null) {
                fw.close();
            }
        }
        
    }
}
