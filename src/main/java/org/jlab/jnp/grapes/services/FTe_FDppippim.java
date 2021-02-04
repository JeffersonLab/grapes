package org.jlab.jnp.grapes.services;

import org.jlab.jnp.hipo4.data.*;
import org.jlab.jnp.hipo.data.HipoNode;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 
 * Skim for e- in FT, p+pip+pim in FD, FT-based PID
 *
 * @author devita
 * @author celentano
 */

public class FTe_FDppippimWagon extends Wagon {

	public FTe_FDppippimWagon(){
        super("FTe_FDppippimWagon","devita","0.1");
    }

	@Override
	public boolean init(String jsonString) {
		System.out.println("FTe_FDppippimWagon READY.");
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

		boolean flag_saveMe = false;
		int     nel  = 0;
                int     npos = 0;
                int     nneg = 0;

		int nprot=0;
		int npip=0;
		int npim=0;
		
                if(bankRECFT!= null && bankREC!=null) {
                    for (int ii = 0; ii < bankRECFT.getRows(); ii++) {
                            int pid    = bankRECFT.getInt("pid", ii);
                            int charge = bankREC.getByte("charge", ii);
                            double  px = bankREC.getFloat("px", ii);
                            double  py = bankREC.getFloat("py", ii);
                            double  pz = bankREC.getFloat("pz", ii);
                            double   p = Math.sqrt(px*px+py*py+pz*pz);
			    double chi2 = bankREC.getFloat("chi2pid",ii);
			    int status = bankRECFT.getShort("status", ii);

                            if (pid == 11 && status>-2000 && status<-1000 && p>1) nel++; 
                            if ((status>2000)&&(status<4000)) {
                                if (charge >0)    npos++;
                                else if(charge<0) nneg++;
				if (Math.abs(chi2)>5){
				    switch(pid){
				    case 211:
					npip++;
					break;
				    case -211:
					npim++;
					break;
				    case 2212:
					nprot++;
					break;
				    }
				}
			    }
                    }

                    if (nel>=1 && (npos+nneg)>=2 && (npos+nneg)<=6 && npip==1 && nprot==1 && npim==1)  flag_saveMe=true;
                }
                
                return flag_saveMe;
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
