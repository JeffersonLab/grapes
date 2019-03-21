/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.jnp.grapes.io;

import java.nio.ByteOrder;
import java.nio.file.Path;
import org.jlab.clara.engine.EngineDataType;
import org.jlab.clara.std.services.AbstractEventReaderService;
import org.jlab.clara.std.services.EventReaderException;
import org.jlab.jnp.hipo4.data.DataFrame;
import org.jlab.jnp.hipo4.data.DataFrameBuilder;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.io.HipoReader;
import org.json.JSONObject;

/**
 *
 * @author gavalian
 */
public class HipoFrameReader extends AbstractEventReaderService<HipoReader> {
    
    private int maxEventsFrame = 10;
    private int maxSizeFrame   = 1024*1024;
    
    
    public void setMaxEvents(int __nevents){ maxEventsFrame = __nevents;}
    public void setMaxSize(int __nsize){ maxSizeFrame = __nsize;}
    
    @Override
    protected HipoReader createReader(Path file, JSONObject opts)
            throws EventReaderException {
        try {
            HipoReader reader = new HipoReader();
            reader.open(file.toString());
            return reader;
        } catch (Exception e) {
            throw new EventReaderException(e);
        }
    }

    @Override
    protected void closeReader() {
        reader.close();
    }

    @Override
    public int readEventCount() throws EventReaderException {
        return reader.getEventCount()/this.maxEventsFrame;
    }

    @Override
    public ByteOrder readByteOrder() throws EventReaderException {
        return ByteOrder.LITTLE_ENDIAN;
    }

    @Override
    public Object readEvent(int eventNumber) throws EventReaderException {
        try {
            int startEvent = eventNumber*this.maxEventsFrame;
            DataFrameBuilder builder = new DataFrameBuilder(this.maxEventsFrame,this.maxSizeFrame);
            Event            event   = new Event();
            for(int i = 0; i < this.maxEventsFrame; i++){
                reader.nextEvent(event);
                event.clearEventBitMask();
                
                builder.addEvent(event.getEventBuffer().array(), 0, 
                        event.getEventBufferSize());
            }
            DataFrame  frame = builder.build();
            //System.out.println("FRAME-READER : count = " + frame.getCount());
            return frame;
            //return reader.readEvent(eventNumber);
        } catch (Exception e) {
            throw new EventReaderException(e);
        }
    }

    @Override
    protected EngineDataType getDataType() {
        return Clas12Types.HIPOFRAME;
    }
}
