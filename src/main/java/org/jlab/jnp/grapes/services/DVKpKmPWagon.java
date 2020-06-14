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
 
public class DVKpKmPWagon extends BeamTargetWagon {
 
    static final double KaonMass   = 0.493677f;

    public LorentzVector VB, VT;

    public DVKpKmPWagon() {
        super("DVKpKmPWagon","fxgirod","0.0");

	VB = new LorentzVector(0,0,beamEnergy,beamEnergy);
	VT = new LorentzVector(0,0,0,targetMass);
    }

    public double Vangle(Vector3 v1, Vector3 v2){ 
	    double res=0;
	    double l1 = v1.mag();
	    double l2 = v2.mag();
	    if( l1*l2 > 0)res = Math.toDegrees( Math.acos( v1.dot(v2)/(l1*l2) ) );
	    return res;
    }

    @Override
    public boolean processDataEvent(Event event, SchemaFactory factory) {

	    Bank RecPart = new Bank(factory.getSchema("REC::Particle"));
	    event.read(RecPart);

	    boolean hasDVKapKamP = false;
	    if( RecPart!=null && RecPart.getRows()>3 ){

		    for (int ie = 0; ie < RecPart.getRows()-3 && !hasDVKapKamP; ie++) {
			    int is_e     = RecPart.getInt("pid", ie);
			    double e_px  = RecPart.getFloat("px", ie);
			    double e_py  = RecPart.getFloat("py", ie);
			    double e_pz  = RecPart.getFloat("pz", ie);
			    int e_stat   = Math.abs(RecPart.getShort("status", ie));

			    double e_mom = Math.sqrt(e_px*e_px+e_py*e_py+e_pz*e_pz);
			    if( is_e==11 && e_stat>2000 && e_stat<4000 && e_mom>0.1*beamEnergy ){
				    LorentzVector VE = new LorentzVector(e_px,e_py,e_pz,e_mom);
				    for (int ip = ie+1; ip < RecPart.getRows()-2 && !hasDVKapKamP; ip++) {
					    int is_p     = RecPart.getInt("pid", ip);
					    double p_px  = RecPart.getFloat("px", ip);
					    double p_py  = RecPart.getFloat("py", ip);
					    double p_pz  = RecPart.getFloat("pz", ip);
					    int p_stat   = Math.abs(RecPart.getShort("status", ip));

					    if( is_p==2212 && p_stat>2000 ){
						    double p_ene = Math.sqrt(p_px*p_px+p_py*p_py+p_pz*p_pz+targetMass*targetMass);
						    LorentzVector VP = new LorentzVector(p_px,p_py,p_pz,p_ene);
						    for (int ikap = ip+1; ikap < RecPart.getRows()-1 && !hasDVKapKamP ; ikap++) {
							    int is_kap     = RecPart.getInt("pid", ikap);
							    double kap_px  = RecPart.getFloat("px", ikap);
							    double kap_py  = RecPart.getFloat("py", ikap);
							    double kap_pz  = RecPart.getFloat("pz", ikap);
							    int kap_stat   = Math.abs(RecPart.getShort("status", ikap));

							    double kap_ene = Math.sqrt(kap_px*kap_px+kap_py*kap_py+kap_pz*kap_pz+KaonMass*KaonMass);
							    if( is_kap==321 && kap_stat>2000 && kap_stat<4000 && kap_ene>0.523){
								    LorentzVector VKAP = new LorentzVector(kap_px,kap_py,kap_pz,kap_ene);
								    for (int ikam = ikap+1; ikam < RecPart.getRows() && !hasDVKapKamP ; ikam++) {
									    int is_kam     = RecPart.getInt("pid", ikam);
									    double kam_px  = RecPart.getFloat("px", ikam);
									    double kam_py  = RecPart.getFloat("py", ikam);
									    double kam_pz  = RecPart.getFloat("pz", ikam);
									    int kam_stat   = Math.abs(RecPart.getShort("status", ikam));

									    double kam_ene = Math.sqrt(kam_px*kam_px+kam_py*kam_py+kam_pz*kam_pz+KaonMass*KaonMass);
									    if( is_kam==-211 && kam_stat>2000 && kam_stat<4000 && kam_ene>0.523){
										    LorentzVector VKAM = new LorentzVector(kam_px,kam_py,kam_pz,kam_ene);

										    LorentzVector VPHI = new LorentzVector(0,0,0,0);
										    VPHI.add(VKAP);
										    VPHI.add(VKAM);

										    LorentzVector Q = new LorentzVector(0,0,0,0);
										    Q.add(VB);
										    Q.sub(VE);
										    LorentzVector W = new LorentzVector(0,0,0,0);
										    W.add(Q);
										    W.add(this.VT);

										    if( -Q.mass2()>0.8 && W.mass()>1.8 ){
											    LorentzVector VmissP = new LorentzVector(0,0,0,0);
											    VmissP.add(W);
											    VmissP.sub(VPHI);
											    LorentzVector VmissKAP = new LorentzVector(0,0,0,0);
											    VmissKAP.add(W);
											    VmissKAP.sub(VP);
											    VmissKAP.sub(VKAM);
											    LorentzVector VmissKAM = new LorentzVector(0,0,0,0);
											    VmissKAM.add(W);
											    VmissKAM.sub(VP);
											    VmissKAM.sub(VKAP);
											    LorentzVector VmissAll = new LorentzVector(0,0,0,0);
											    VmissAll.add(VmissP);
											    VmissAll.sub(VP);

											    hasDVKapKamP = true
												    && VmissAll.e() > -0.5 && VmissAll.e() < 0.5
												    && VmissAll.mass2() > -0.05 &&  VmissAll.mass2() < 0.05
												    && VmissAll.px()*VmissAll.px() + VmissAll.py()*VmissAll.py() < 0.25
												    && Vangle( VP.vect() , VmissP.vect() ) < 12
												    && Vangle( VKAP.vect() , VmissKAP.vect() ) < 12
												    && Vangle( VKAM.vect() , VmissKAM.vect() ) < 12
												    && VmissP.mass2() > 0.5 && VmissP.mass2() < 1.5
												    && VmissKAP.mass2() > 0.035 && VmissKAP.mass2() < 0.6
												    && VmissKAM.mass2() > 0.035 && VmissKAM.mass2() < 0.6
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
	    return hasDVKapKamP;
    }
}
