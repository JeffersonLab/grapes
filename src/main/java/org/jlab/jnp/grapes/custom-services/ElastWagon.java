package org.jlab.jnp.grapes.services;

import org.jlab.jnp.physics.LorentzVector;
import org.jlab.jnp.hipo4.data.Bank;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.data.SchemaFactory;
import java.util.ArrayList;

/**
 * 
 * Skim for elastic e- events
 * e- events are prescaled by THRESHOLD for elastic angle theta_ec<theta_pc
 * corresponding to theta_p cutoff angle THETA_PC 
 *
 * @author lcsmith
 */
public class ElastWagon extends BeamTargetWagon {

    private static         int callCount = 0;
    private static final   int THRESHOLD = 10;
    private static final double THETA_PC = 40;
    private static final double    W_CUT = 1.2;

    public ElastWagon(){
        super("ElastWagon","lcsmith","0.3");
    }

    @Override
    public boolean init(String jsonString) {
        System.out.println("ElastWagon READY.");
        return true;
    }

    private double getMM(LorentzVector Vb, LorentzVector Vt, LorentzVector Ve, LorentzVector...Vx) {
        LorentzVector VmissN = new LorentzVector(0,0,0,0);
        VmissN.add(Vb);
        VmissN.add(Vt);
        VmissN.sub(Ve);
        for (Object LV : Vx) VmissN.sub((LorentzVector) LV);
        return VmissN.mass2();
    }

    @Override
    public boolean processDataEvent(Event event, SchemaFactory factory) {

        Bank RecPart = new Bank(factory.getSchema("REC::Particle"));
        Bank Config  = new Bank(factory.getSchema("RUN::config"));

        event.read(RecPart);        
        if (RecPart==null || RecPart.getRows()==0) return false;
        event.read(Config);        
        if (Config==null ||  Config.getRows()==0)  return false;

        final int THRESHOLD = 10; //pre-scale threshold

        int runno = Config.getInt("run",0);
        if(runno==0) return false;

        double eb = beamEnergy;        
        //       if (runno>=19204 && runno<19662) eb = 6.39463; //rgk-f23, rgk-s24
        //       if (runno>=19662 && runno<19894) eb = 8.47757; //rgk-s24

        double mp = 0.93828; 
        double theta_pc = Math.toRadians(THETA_PC); //proton theta cutoff       
        double theta_ec = 2*Math.atan(1/(Math.tan(theta_pc)*(1+eb/mp))); //electron pre-scale theta        

        ArrayList<Integer> eleCandi = new ArrayList<>();

        for (int ipart=0; ipart<RecPart.getRows(); ipart++) {

            final int    pid = RecPart.getInt("pid",ipart);
            final int charge = RecPart.getInt("charge",ipart);
            final int status = RecPart.getInt("status",ipart);       

            final boolean isFD = (int)(Math.abs(status)/1000) == 2;

            if (pid==11 && isFD && charge < 0) eleCandi.add(ipart);
        }

        if (eleCandi.isEmpty() || eleCandi.size()>1) return false;

        double epx  = RecPart.getFloat("px",eleCandi.get(0));
        double epy  = RecPart.getFloat("py",eleCandi.get(0));
        double epz  = RecPart.getFloat("pz",eleCandi.get(0));
        double  ee  = Math.sqrt(epx*epx+epy*epy+epz*epz);

        LorentzVector VVB = new LorentzVector(0,0,eb,eb);
        LorentzVector VVT = new LorentzVector(0,0,0,mp);
        LorentzVector VVE = new LorentzVector(epx,epy,epz,ee);

        double WW  = Math.sqrt(getMM(VVB,VVT,VVE));

        if (WW > W_CUT) return false;    // W cut must go before theta_c cut!

        if (VVE.theta() < theta_ec) {  //theta_ec cut
            callCount++;            
            if (callCount <  THRESHOLD) return false;
            if (callCount == THRESHOLD) callCount=0;
        }

        return true;          
    }
}


