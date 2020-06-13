package org.jlab.jnp.grapes.services;

import org.jlab.jnp.physics.Vector3;
import org.jlab.jnp.physics.LorentzVector;

import org.jlab.jnp.hipo4.data.Bank;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.data.SchemaFactory;


//import org.jlab.jnp.grapes.services.*;

/**
* 
* DVPipPimP Skimming
*
* @author fxgirod
*/
 
public class DVPipPimPWagon_10p6 extends Wagon {
 
    static final double BeamEnergy = 10.603f;
    static final double TargetMass = 0.93827f;
    static final double PionMass   = 0.13957f;

    public LorentzVector VB, VT;

    public DVPipPimPWagon_10p6() {
        super("DVPipPimPWagon","fxgirod","0.0");

	VB = new LorentzVector(0,0,BeamEnergy,BeamEnergy);
	VT = new LorentzVector(0,0,0,TargetMass);
    }

    public double Vangle(Vector3 v1, Vector3 v2){ 
	    double res=0;
	    double l1 = v1.mag();
	    double l2 = v2.mag();
	    if( l1*l2 > 0)res = Math.toDegrees( Math.acos( v1.dot(v2)/(l1*l2) ) );
	    return res;
    }

    @Override
    public boolean init(String jsonString) {
        System.out.println("DVPipPimPWagon_10p6 READY.");
        return true;
    }   

    @Override
    public boolean processDataEvent(Event event, SchemaFactory factory) {

	    Bank RecPart = new Bank(factory.getSchema("REC::Particle"));
	    event.read(RecPart);

	    boolean hasDVPipPimP = false;
	    if( RecPart!=null && RecPart.getRows()>3 ){

		    for (int ie = 0; ie < RecPart.getRows()-3 && !hasDVPipPimP; ie++) {
			    int is_e     = RecPart.getInt("pid", ie);
			    double e_px  = RecPart.getFloat("px", ie);
			    double e_py  = RecPart.getFloat("py", ie);
			    double e_pz  = RecPart.getFloat("pz", ie);
			    int e_stat   = Math.abs(RecPart.getShort("status", ie));

			    double e_mom = Math.sqrt(e_px*e_px+e_py*e_py+e_pz*e_pz);
			    if( is_e==11 && e_stat>2000 && e_stat<4000 && e_mom>0.1*BeamEnergy ){
				    LorentzVector VE = new LorentzVector(e_px,e_py,e_pz,e_mom);
				    for (int ip = ie+1; ip < RecPart.getRows()-2 && !hasDVPipPimP; ip++) {
					    int is_p     = RecPart.getInt("pid", ip);
					    double p_px  = RecPart.getFloat("px", ip);
					    double p_py  = RecPart.getFloat("py", ip);
					    double p_pz  = RecPart.getFloat("pz", ip);
					    int p_stat   = Math.abs(RecPart.getShort("status", ip));

					    if( is_p==2212 && p_stat>2000 ){
						    double p_ene = Math.sqrt(p_px*p_px+p_py*p_py+p_pz*p_pz+TargetMass*TargetMass);
						    LorentzVector VP = new LorentzVector(p_px,p_py,p_pz,p_ene);
						    for (int ipip = ip+1; ipip < RecPart.getRows()-1 && !hasDVPipPimP ; ipip++) {
							    int is_pip     = RecPart.getInt("pid", ipip);
							    double pip_px  = RecPart.getFloat("px", ipip);
							    double pip_py  = RecPart.getFloat("py", ipip);
							    double pip_pz  = RecPart.getFloat("pz", ipip);
							    int pip_stat   = Math.abs(RecPart.getShort("status", ipip));

							    double pip_ene = Math.sqrt(pip_px*pip_px+pip_py*pip_py+pip_pz*pip_pz+PionMass*PionMass);
							    if( is_pip==211 && pip_stat>2000 && pip_stat<4000 && pip_ene>0.3){
								    LorentzVector VPIP = new LorentzVector(pip_px,pip_py,pip_pz,pip_ene);
								    for (int ipim = ipip+1; ipim < RecPart.getRows() && !hasDVPipPimP ; ipim++) {
									    int is_pim     = RecPart.getInt("pid", ipim);
									    double pim_px  = RecPart.getFloat("px", ipim);
									    double pim_py  = RecPart.getFloat("py", ipim);
									    double pim_pz  = RecPart.getFloat("pz", ipim);
									    int pim_stat   = Math.abs(RecPart.getShort("status", ipim));

									    double pim_ene = Math.sqrt(pim_px*pim_px+pim_py*pim_py+pim_pz*pim_pz+PionMass*PionMass);
									    if( is_pim==-211 && pim_stat>2000 && pim_stat<4000 && pim_ene>0.3){
										    LorentzVector VPIM = new LorentzVector(pim_px,pim_py,pim_pz,pim_ene);

										    LorentzVector VRHO0 = new LorentzVector(0,0,0,0);
										    VRHO0.add(VPIP);
										    VRHO0.add(VPIM);

										    LorentzVector Q = new LorentzVector(0,0,0,0);
										    Q.add(VB);
										    Q.sub(VE);
										    LorentzVector W = new LorentzVector(0,0,0,0);
										    W.add(Q);
										    W.add(this.VT);

										    if( -Q.mass2()>0.8 && W.mass()>1.8 ){
											    LorentzVector VmissP = new LorentzVector(0,0,0,0);
											    VmissP.add(W);
											    VmissP.sub(VRHO0);
											    LorentzVector VmissPIP = new LorentzVector(0,0,0,0);
											    VmissPIP.add(W);
											    VmissPIP.sub(VP);
											    VmissPIP.sub(VPIM);
											    LorentzVector VmissPIM = new LorentzVector(0,0,0,0);
											    VmissPIM.add(W);
											    VmissPIM.sub(VP);
											    VmissPIM.sub(VPIP);
											    LorentzVector VmissAll = new LorentzVector(0,0,0,0);
											    VmissAll.add(VmissPIM);
											    VmissAll.sub(VPIM);

											    hasDVPipPimP = true
												    && VmissAll.e() > -0.5 && VmissAll.e() < 0.5
												    && VmissAll.mass2() > -0.1 &&  VmissAll.mass2() < 0.1
												    && VmissAll.px()*VmissAll.px() + VmissAll.py()*VmissAll.py() < 0.5
												    && Vangle( VP.vect() , VmissP.vect() ) < 12
												    && Vangle( VPIP.vect() , VmissPIP.vect() ) < 12
												    && Vangle( VPIM.vect() , VmissPIM.vect() ) < 12
												    && VmissP.mass2() > 0.5 && VmissP.mass2() < 1.5
												    && VmissPIP.mass2() > -0.3 && VmissPIP.mass2() < 0.5
												    && VmissPIM.mass2() > -0.3 && VmissPIM.mass2() < 0.5
												    ;
										    }
									    }
								    }
							    }
						    }
					    }
				    }

			    }

		    }
	    }
	    return hasDVPipPimP;
    }
}
