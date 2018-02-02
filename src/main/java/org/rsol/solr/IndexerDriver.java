package org.rsol.solr;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexerDriver {

	private static final Logger LOGGER = LoggerFactory.getLogger(IndexerDriver.class);
	private static final String DEFAULT_ZK_HOST = "127.0.0.1:9983";
	private static final String[] SOLR_INDEX_COMMANDS = {"loadSolr","sanitizeUnknownSolrFields"};
	private static final String MORPHLINE_COMMAND_PATTERN = "\\{[^\\{\\}]+(%s)[\\s+]\\{[^\\{\\}]+([\\n]*.*:.*[\\n]*)+[^\\{\\}]+\\}[^\\{\\}]+\\}";
	
	public static void main(String[] args) {
		
		if(args.length < 2){
			printUsage();
			System.exit(1);
		}

		String[] dataFiles = applyFilter(Arrays.copyOfRange(args, 1, args.length));
		String morphlineConf = args[0];
		
		printSystemProperties();		
		print(dataFiles);
		
		try{
			
		    File morphlineFile = filterMorphline(new File(morphlineConf));		    
		    processFilesInBatches(morphlineFile, dataFiles);

		}catch (Exception e){
			LOGGER.error("Failed: Error while processing - " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static void processFilesInBatches(File morphlineFile, String[] files) throws IOException {
		 String rangeStr = System.getProperty("range");
		 int range =  rangeStr != null?new Integer(rangeStr).intValue():files.length;
		 List<String> list = Arrays.asList(files);
		 for(int i=0; i<list.size();){
		  	int endIndex = i+range > list.size()?list.size():i+range;
		   	String[] sfiles = list.subList(i, endIndex).toArray(new String[0]);
		   	
			LOGGER.info("Indexing data from files " + i + " to " + endIndex);
		   	processFiles(morphlineFile, sfiles);
		   	i = endIndex;
		 }
	}
	
	public static void processFiles(File morphlineFile, String[] files) throws IOException {
		String morphlineId = null;
		new MorphlineDriver().run(morphlineFile, morphlineId, files);
	   	if(System.getProperty("rm") != null)
			  deleteFiles(files);		
	}
	
	public static String[] applyFilter(String[] files){
		List<String> list = new ArrayList<String>();
		for (String s : files){
			String skip_pattern = System.getProperty("skip");
			if(!s.matches(skip_pattern == null?"none":skip_pattern))
		        list.add(s);
		}
		return list.toArray(new String[list.size()]);
	}
	
	public static void deleteFiles(String[] files){
		for(String f : files){			
			if(f != null && f.matches(System.getProperty("rm"))){
				new File(f).delete();
				LOGGER.info("reclaimed space for source file after indexing " + f);
			}
		}		
	}
	
	public static void print(String[] args) {
		for(String arg: args){
			LOGGER.info(arg);
		}
	}
	
	public static void printUsage(){
		LOGGER.info("Usage: \njava [options] -jar jar-file <morphline.conf> <dataFile1> ... <dataFileN>");
		LOGGER.info("[options] as system properties :");
		LOGGER.info("-Doutput=file-path  - write morphline output to a file");
		LOGGER.info("-Dpost=true - index results using Http POST command");
		LOGGER.info("-Drange=number range of files to process in batch, 0 indicates all at once");
		LOGGER.info("-Drm=regex-pattern - remove matching files after indexing");
		LOGGER.info("-Dskip=regex-pattern remove matching files after indexing");
		LOGGER.info("-DSTEP_LIMIT=number - allowed graph complixity before the g_nodes, g_edges are ignored");
	}
	
	public static void printSystemProperties(){
		String[] props = {"output","post","rm","range","skip","STEP_LIMIT"};
		LOGGER.info("System properties specified :");
		for(String s : props){
			LOGGER.info(String.format("-D%s=%s",s,System.getProperty(s)));
		}	
	}
	
	public static File filterMorphline(File file) throws IOException {		
		String content = FileUtils.readFileToString(file, "UTF-8");
		content = substituteVariables(content);
		
	    if(isDryRun())	    	
		  content = filterCommands(content);
	    
		return writeTempFile(file.getName(), content);
	}
	
	public static File writeTempFile(String filename, String content) throws IOException {
		String tempFileName = "/tmp/" + filename + ".tmp";
		File tempFile = new File(tempFileName);	
		FileUtils.writeStringToFile(tempFile, content, "UTF-8");
		return tempFile;
	}
	
	public static String filterCommands(String content) { 
		String regex = String.format(MORPHLINE_COMMAND_PATTERN, StringUtils.join(SOLR_INDEX_COMMANDS, "|"));
		return content.replaceAll(regex, "");
	}
	
	public static boolean isDryRun(){
		return System.getProperty("output") != null;
	}

	public static String substituteVariables(String content) {
	    String zkHost = System.getProperty("zkHost");
	    if(zkHost == null)
	    	zkHost = DEFAULT_ZK_HOST;
	    
		return content.replaceAll("\\$ZK_HOST", zkHost);
	}
	
}
