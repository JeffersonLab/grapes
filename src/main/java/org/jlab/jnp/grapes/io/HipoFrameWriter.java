package org.jlab.jnp.grapes.io;

import java.nio.file.Path;
import org.jlab.clara.engine.EngineDataType;
import org.jlab.clara.std.services.AbstractEventWriterService;
import org.jlab.clara.std.services.EventWriterException;
import org.jlab.jnp.hipo4.data.DataFrame;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.io.HipoWriterStream;
import org.jlab.jnp.utils.file.FileUtils;
import org.json.JSONObject;

/**
 *
 * @author gavalian
 */
public class HipoFrameWriter extends AbstractEventWriterService<HipoWriterStream> {
    
    private static final String CONF_COMPRESSION = "compression";
    private static final String CONF_SCHEMA = "schema_dir";
    private static final String CONF_FILTER = "filter";
    
    @Override
    protected HipoWriterStream createWriter(Path file, JSONObject opts) throws EventWriterException {
        try {
            HipoWriterStream writer = new HipoWriterStream();
            int compression = getCompression(opts);
            System.out.printf("%s service: using compression level %d%n", getName(), compression);
            writer.getSchemaFactory().initFromDirectory(getSchemaDirectory(opts));
            String[] filters = getFilterString(opts);
            if(filters.length>0){
                for(int i = 0; i < filters.length; i+=2){
                    int order = Integer.parseInt(filters[i]);
                    writer.addEventFilter(order, filters[i+1]);                    
                }
            }
            writer.setFileName(file.toString());
            writer.open();
            return writer;
        } catch (Exception e) {
            throw new EventWriterException(e);
        }
    }

    private int getCompression(JSONObject opts) {
        return opts.has(CONF_COMPRESSION) ? opts.getInt(CONF_COMPRESSION) : 2;
    }

    private String getSchemaDirectory(JSONObject opts) {
        return opts.has(CONF_SCHEMA)
                ? opts.getString(CONF_SCHEMA)
                : FileUtils.getEnvironmentPath("CLAS12DIR", "etc/bankdefs/hipo4");
    }

    private String[] getFilterString(JSONObject opts){
        if(opts.has(CONF_FILTER)==false) return new String[0];
        String filterString = opts.getString(CONF_FILTER);
        return filterString.split("-");
    }
    
    @Override
    protected void closeWriter() {
        writer.close();
    }

    @Override
    protected void writeEvent(Object event) throws EventWriterException {
        try {
            DataFrame  dataFrame = (DataFrame) event;
            
            int count = dataFrame.getEntries();
            Event  hipoEvent = new Event();
            
            for(int i = 0; i < count; i++){
                
                byte[] buffer = dataFrame.getEventCopy(i);
                hipoEvent.initFrom(buffer);
            
                int eventTag = hipoEvent.getEventTag();
                if(eventTag==1){ 
                    writer.writeEventAll(hipoEvent);
                } else {
                   hipoEvent.setEventTag(0);
                   for(int k = 0; k < 32; k++){
                            int status = hipoEvent.getEventBitMask(k);
                            if(status>0) {                 
                                writer.writeEvent(k,hipoEvent);
                            }
                   }
                }
            }
        } catch (Exception e) {
            throw new EventWriterException(e);
        }
    }

    @Override
    protected EngineDataType getDataType() {
        return Clas12Types.HIPOFRAME;
    }
}
