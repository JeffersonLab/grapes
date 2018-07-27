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
import org.jlab.jnp.hipo.data.DataFrame;
import org.jlab.jnp.hipo.data.HipoEvent;
import org.jlab.jnp.hipo.io.HipoReader;
import org.json.JSONObject;

/**
 *
 * @author gavalian
 */
public class HipoFrameReader extends AbstractEventReaderService<HipoReader> {
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
        return reader.getEventCount()/50;
    }

    @Override
    public ByteOrder readByteOrder() throws EventReaderException {
        return ByteOrder.LITTLE_ENDIAN;
    }

    @Override
    public Object readEvent(int eventNumber) throws EventReaderException {
        try {
            int startEvent = eventNumber*50;
            DataFrame  frame = new DataFrame(1024*250,300);
            for(int i = 0; i < 50; i++){
                HipoEvent  event = reader.readEvent(startEvent+i);
                frame.add(event.getDataBuffer());
            }
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
