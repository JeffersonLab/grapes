package org.jlab.jnp.grapes.services;

import org.jlab.jnp.hipo4.data.*;
import org.jlab.jnp.hipo.data.HipoNode;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 
 * Skim for 2pion channel with e- in FD
 *
 * @author kneupane
 * @author tylern
 */

public class TwoPionWagon extends Wagon {

	public TwoPionWagon(){
        super("TwoPionWagon","kneupane","0.1");
    }

	@Override
	public boolean init(String jsonString) {
		System.out.println("TwoPionWagon READY.");
		return true;
	}

	@Override
	public boolean processDataEvent(Event event, SchemaFactory factory) {

		Bank bankREC   = new Bank(factory.getSchema("REC::Particle"));
		event.read(bankREC);

        // If the bank is null or empty don't save the event
        if(bankREC == null) return false;

        int     nelec  = 0;
        int     nprot = 0;
        int     npip = 0;
        int     npim = 0;
        int     nother = 0;

        for (int ii = 0; ii < bankREC.getRows(); ii++) {
            int pid    = bankREC.getInt("pid", ii);
            double  px = bankREC.getFloat("px", ii);
            double  py = bankREC.getFloat("py", ii);
            double  pz = bankREC.getFloat("pz", ii);
            double   p = Math.sqrt(px*px+py*py+pz*pz);
            int status = bankREC.getShort("status", ii);

            // Electron in forward detector (status between 2000 and 4000) and not low momentum (p < 1)
            if (pid == 11 && 2000 >= Math.abs(status) && Math.abs(status) < 4000 && p>1) nelec++;

            // Particle in forward or central detector
            if(status>=2000) {
                // Count based on PIDs
                if (pid == 2212)    nprot++;
                else if(pid == 211) npip++;
                else if(pid == -211) npim++;
                else nother++;
            }
        }

        // Checks on the event
        boolean     hasElectron = nelec>=1;

        // TODO: Look into seeing if we need to change this or include nother in count
        boolean     notTooMany = ((nprot+npip+npim)<=7);

        // Setup each topology
        boolean     exclusive = (nprot >=1 && npip >=1 && npim >=1);
        boolean     missingPim = (nprot >=1 && npip >=1);
        boolean     missingPip = (nprot >=1 && npim >=1);
        boolean     missingProt = (npip >=1 && npim >=1);
        

        // If we have an electron and not too many other particles 
        // and the event fits one of the topologies save the event
        boolean goodTwoPion = (hasElectron && notTooMany && (exclusive || missingPim || missingPip || missingProt));  
                
        return goodTwoPion;
	}

}
