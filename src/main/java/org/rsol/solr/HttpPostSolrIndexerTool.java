package org.rsol.solr;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

public class HttpPostSolrIndexerTool {
	
	private static String SOLR_URL = "http://localhost:8983/solr/graph_test/update?commit=true&wt=json";
	private HttpClient client = HttpClientBuilder.create().build();
	String charset = "UTF-8"; 
	
	public HttpPostSolrIndexerTool() {
	}

	public static void main(String[] args) throws Exception{
		HttpPostSolrIndexerTool tool = new HttpPostSolrIndexerTool();
		
		String json = "{\"id\":\"100\",\"age\":\"20\"}";
		tool.IndexJsonDoc(json);

	}
	
	public void IndexJsonDocs(File file, int batchSize) throws IOException {		
		IndexJsonDocs(IOUtils.readLines(new FileInputStream(file), charset));
	}
	

	
	public void IndexJsonDocs(List<String> docs) {
		HttpPost request = new HttpPost(SOLR_URL);
		
		try {
			
		    StringEntity entity = new StringEntity(combineJson(docs));
		    request.addHeader("content-type", "application/json");
		    request.addHeader("Accept","application/json");
		    entity.setChunked(true);	
		    request.setEntity(entity);
		    
		    HttpResponse response = client.execute(request);
		    printResponseText(response);
			
		}catch (Exception ex) {
		    ex.printStackTrace();
		} finally {
			request.releaseConnection();		    
		}		
	}
		
	public void IndexJsonDoc(String json) {
		HttpPost request = new HttpPost(SOLR_URL);
		
		try {
		    
		    StringEntity params = new StringEntity(json);
		    request.addHeader("content-type", "application/json");
		    request.addHeader("Accept","application/json");
		    request.setEntity(params);
		    
		    HttpResponse response = client.execute(request);
		    printResponseText(response);
			
		}catch (Exception ex) {
		    ex.printStackTrace();
		} finally {
			request.releaseConnection();		    
		}		
	}
	
	private String printResponseText(HttpResponse response) throws IOException {
		
		int responseCode = response.getStatusLine().getStatusCode();

		System.out.println("\nSending 'POST' request to URL : " + SOLR_URL);
		System.out.println("Response Code : " + responseCode);

		BufferedReader rd = new BufferedReader(
	                new InputStreamReader(response.getEntity().getContent()));

		StringBuffer result = new StringBuffer();
		String line = "";
		while ((line = rd.readLine()) != null) {
			result.append(line);
		}

		rd.close();
		System.out.println(result.toString());
		
		return result.toString();		
	}
	
	private String combineJson(List<String> docs){
		StringBuilder sbuilder = new StringBuilder("[");
		for(String doc: docs){
			sbuilder = sbuilder.append(",").append(doc);
		}
		
		return sbuilder.append("]").toString().replaceFirst(",", "");		
	}
	


}
