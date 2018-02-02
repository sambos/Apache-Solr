package org.rsol.solr;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public class RecordWriter {
		
	public static void write(String outFile, String data, boolean append) {
        try{
            FileUtils.writeStringToFile(new File(outFile), data, append );
          }catch(IOException e){
          	e.printStackTrace();
          }		
	}

}
