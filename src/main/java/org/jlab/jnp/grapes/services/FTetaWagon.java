package org.jlab.jnp.grapes.services;

import org.jlab.jnp.hipo4.data.*;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * Skim for 2pi channel with e- in FT for MesonX
 *
 * @author devita 9/2021 TwoPiFTWagon->> FTetaWagon : modified by C.Salgado
 * adding conditions for : at least two neutrals, neutral energy > 0.5 GeV and
 * two first neutrals in different sectors
 */
public class FTetaWagon extends Wagon {

    public FTetaWagon() {
        super("FTetaWagon", "devita", "0.1");
    }

    @Override
    public boolean init(String jsonString) {
        System.out.println("FTetaWagon READY.");
        return true;
    }

    @Override
    public boolean processDataEvent(Event event, SchemaFactory factory) {

//		Bank bank = new Bank(factory.getSchema("REC::Particle"));
        Bank bankRECFT = new Bank(factory.getSchema("RECFT::Particle"));
        Bank bankREC = new Bank(factory.getSchema("REC::Particle"));
        Bank bankCAL = new Bank(factory.getSchema("REC::Calorimeter"));

//		event.read(bank);
        event.read(bankRECFT);
        event.read(bankREC);
        event.read(bankCAL);

        boolean flag_FTeta = false;
        int nel = 0;
        int npos = 0;
        int nneg = 0;
        Map<Integer,Integer> neus = new HashMap<>(); // map linking neutral indices to their sector
        

        if (bankRECFT != null && bankREC != null && bankCAL!= null) {
            for (int ii = 0; ii < bankRECFT.getRows(); ii++) {
                int pid = bankRECFT.getInt("pid", ii);
                int charge = bankREC.getByte("charge", ii);
                double px = bankREC.getFloat("px", ii);
                double py = bankREC.getFloat("py", ii);
                double pz = bankREC.getFloat("pz", ii);
                double p = Math.sqrt(px * px + py * py + pz * pz);
                int status = bankRECFT.getShort("status", ii);

                if (pid == 11 && status > -2000 && status < -1000 && p > 1) {
                    nel++;
                }
                if (status > 2000) {
                    if (charge > 0) {
                        npos++;
                    } else if (charge < 0) {
                        nneg++;
                    } else if (charge == 0 && p > 0.5) {
                        neus.put(ii, 0); // start by collecting indices of neutrals
                    }
                }
            }
            if(neus.size()>1) {
                for (int jj = 0; jj < bankCAL.getRows(); jj++) {
                    int pindex = bankCAL.getShort("pindex", jj);
                    int sector = bankCAL.getByte("sector", jj);
                    if(neus.containsKey(pindex)) {
                        neus.replace(pindex, sector); // then save the actual sector
                    }
                }
            }
            // finally count the sectors
            int neuSectors = 0;
            for(int sector=1; sector<=6; sector++) {
                if(neus.containsValue(sector)) neuSectors++;
            }
            if (nel >= 1 && (npos + nneg) >= 2 && (npos + nneg) <= 6 && neuSectors>=2) {
                flag_FTeta = true;
            }
        }

        return flag_FTeta;
    }

}
