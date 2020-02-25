package org.jlab.jnp.grapes.services;

import org.jlab.jnp.hipo4.data.*;
import org.jlab.jnp.hipo.data.HipoNode;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 
 * Skim for 2pi channel with e- in FT for MesonX
 *
 * @author devita
 */

public class TwoPiFTWagon extends Wagon {

	public TwoPiFTWagon(){
        super("ElecFTkaonWagon","devita","0.1");
    }

	@Override
	public boolean init(String jsonString) {
		System.out.println("TwoPiFTWagon READY.");
		return true;
	}

	@Override
	public boolean processDataEvent(Event event, SchemaFactory factory) {

//		Bank bank = new Bank(factory.getSchema("REC::Particle"));
		Bank bankRECFT = new Bank(factory.getSchema("RECFT::Particle"));
		Bank bankREC   = new Bank(factory.getSchema("REC::Particle"));

//		event.read(bank);

		event.read(bankRECFT);
		event.read(bankREC);

		boolean flag_2piFT = false;
		int     nel  = 0;
                int     npos = 0;
                int     nneg = 0;
                
                if(bankRECFT!= null && bankREC!=null) {
                    for (int ii = 0; ii < bankRECFT.getRows(); ii++) {
                            int pid    = bankRECFT.getInt("pid", ii);
                            int charge = bankREC.getByte("charge", ii);
                            int status = bankRECFT.getShort("status", ii);

                            if (pid == 11 && status>-2000 && status<-1000) nel++; 
                            if(status>2000) {
                                if (charge >0)    npos++;
                                else if(charge<0) nneg++;
                            }
                    }

                    if (nel==1 && npos>=1 && nneg>=1 && (npos+nneg)<=4) flag_2piFT=true;
                }
                
                return flag_2piFT;
	}

	private HashMap<Integer, ArrayList<Integer>> mapByIndex(HipoNode indices) {
		HashMap<Integer, ArrayList<Integer>> map = new HashMap<Integer, ArrayList<Integer>>();
		for (int ii = 0; ii < indices.getDataSize(); ii++) {
			final int index = indices.getInt(ii);
			if (!map.containsKey(index))
				map.put(index, new ArrayList<Integer>());
			map.get(index).add(ii);
		}
		return map;
	}

}