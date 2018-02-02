package org.rsol.solr;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import org.kitesdk.morphline.api.Command;
import org.kitesdk.morphline.api.MorphlineContext;
import org.kitesdk.morphline.api.Record;
import org.kitesdk.morphline.base.Compiler;
import org.kitesdk.morphline.base.Fields;
import org.kitesdk.morphline.base.Notifications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MorphlinProcessor {

  private static final Logger LOGGER = LoggerFactory.getLogger(MorphlineDriver.class);


  public MorphlineProcessor(){

  }

  public void run(File morphlineFile, String morphlineId, String[] files) throws IOException {
    // compile morphline.conf file on the fly

	if(morphlineFile == null)
		throw new IOException("No Morphline file specified...");

    MorphlineContext morphlineContext = new MorphlineContext.Builder().build();
    RecordEmitter re = null;
		re = new RecordEmitter(morphlineContext);
    Command morphline = new Compiler().compile(morphlineFile, morphlineId, morphlineContext, re);
    // process each input data file
    Notifications.notifyBeginTransaction(morphline);
    try {
      for (int i = 0; i < files.length; i++) {
        InputStream in = new BufferedInputStream(new FileInputStream(new File(files[i])));
				Record record = new Record();

        record.put(Fields.ATTACHMENT_BODY, in);
        Notifications.notifyStartSession(morphline);
        boolean success = morphline.process(record);
        if (!success) {
          LOGGER.info("Morphline failed to process record: " + record);
        }

        in.close();
      }
      Notifications.notifyCommitTransaction(morphline);
    } catch (RuntimeException e) {
      Notifications.notifyRollbackTransaction(morphline);
      morphlineContext.getExceptionHandler().handleException(e, null);
    }
    Notifications.notifyShutdown(morphline);
  }
}
