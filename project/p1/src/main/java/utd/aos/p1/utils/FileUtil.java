package utd.aos.p1.utils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

// helps reduce the amount of code needed to read/(and later write) to a file.
public class FileUtil {
    // pull a string at path into a string. Throw an error if there was a problem.
    public static String slurp(String path) throws FileNotFoundException, IOException {
        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(path));) {
            String line;
            while ((line = reader.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        }

        return resultStringBuilder.toString();
    }    
}
