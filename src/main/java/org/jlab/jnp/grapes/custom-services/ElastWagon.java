package org.jlab.jnp.grapes.services;

import org.jlab.jnp.physics.LorentzVector;
import org.jlab.jnp.hipo4.data.Bank;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.data.SchemaFactory;
import java.util.ArrayList;
import java.util.Random;

/**
 * 
 * Skim for elastic e- events
 * e- events are prescaled by PRESCALE for elastic angle theta_ec<theta_pc
 * corresponding to theta_p cutoff angle THETA_PC 
 *
 * @author lcsmith
 */
public class ElastWagon extends BeamTargetWagon {

    private static final   int PRESCALE = 10;
    private static final double THETA_PC = Math.toRadians(40);
    private static       double THETA_EC = 0;
    private static final double    W_CUT = 1.2;

    private Random random = new Random();

    public ElastWagon(){
        super("ElastWagon","lcsmith","0.3");
    }

    @Override
    public boolean init(String jsonString) {
        boolean status = super.init(jsonString);
        THETA_EC = 2*Math.atan(1/(Math.tan(THETA_PC)*(1+beamEnergy/targetMass))); //electron pre-scale theta        
	return status;
    }

    @Override
    public boolean processDataEvent(Event event, SchemaFactory factory) {

        Bank RecPart = new Bank(factory.getSchema("REC::Particle"));
        event.read(RecPart);
        if (RecPart==null || RecPart.getRows()==0) return false;

        //       if (runno>=19204 && runno<19662) eb = 6.39463; //rgk-f23, rgk-s24
        //       if (runno>=19662 && runno<19894) eb = 8.47757; //rgk-s24

        ArrayList<Integer> eleCandi = new ArrayList<>();

        for (int ipart=0; ipart<RecPart.getRows(); ipart++) {

            final int    pid = RecPart.getInt("pid",ipart);
            final int status = RecPart.getInt("status",ipart);       

            final boolean isFD = (int)(Math.abs(status)/1000) == 2;

            if (pid==11 && isFD ) eleCandi.add(ipart);
        }

        if (eleCandi.isEmpty() || eleCandi.size()>1) return false;

        double epx  = RecPart.getFloat("px",eleCandi.get(0));
        double epy  = RecPart.getFloat("py",eleCandi.get(0));
        double epz  = RecPart.getFloat("pz",eleCandi.get(0));
        double  ee  = Math.sqrt(epx*epx+epy*epy+epz*epz);

        LorentzVector VVE = new LorentzVector(epx,epy,epz,ee);

        double WW  = this.getMissingVector(VVE).mass();

        if (WW > W_CUT) return false;    // W cut must go before theta_c cut!

        if (VVE.theta() < THETA_EC) {  //theta_ec cut
  	     return random.nextInt(PRESCALE)<1;
        }

        return true;
    }
}


