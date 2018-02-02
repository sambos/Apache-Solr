package org.rsol.solr;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.hadoop.io.Text;
import org.kitesdk.morphline.api.Command;
import org.kitesdk.morphline.api.MorphlineContext;
import org.kitesdk.morphline.api.Record;
import org.kitesdk.morphline.base.Fields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ListMultimap;
import com.google.gson.Gson;

public class RecordEmitter implements Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecordEmitter.class);
    private final Text line = new Text();
    private final MorphlineContext context;
    private final String newLine = System.getProperty("line.separator");

    public RecordEmitter(MorphlineContext context) {
        this.context = context;
    }

    @Override
    public void notify(Record notification) {
    }

    @Override
    public Command getParent() {
        return null;
    }

    @Override
    public boolean process(Record record) {
        line.set(record.get(Fields.ATTACHMENT_BODY).get(0).toString());

        ListMultimap<String, Object> data = record.getFields();

		Map<String, Object> map = new HashMap<String, Object>();

        for (String key : data.keySet()) {
          if(!key.startsWith("_")) //skip all other
				map.put(key, data.get(key));
	    }
        Gson gson = new Gson();
        String json = gson.toJson(map);

        LOGGER.debug("json output : " + json);

        if(System.getProperty("output") != null)
        	RecordWriter.write(System.getProperty("output"), json + newLine, true);

        if(System.getProperty("post") != null)
          new HttpPostSolrIndexerTool().IndexJsonDoc("[" + json + "]");

        return true;
    }
}
