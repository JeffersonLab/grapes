package org.jlab.jnp.grapes.services;

import org.jlab.jnp.physics.Vector3;
import org.jlab.jnp.physics.LorentzVector;

import org.jlab.jnp.hipo4.data.Bank;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.data.SchemaFactory;

/**
* 
* DVPi0P Skimming
*
* @author fxgirod
*/
 
public class DVPi0PWagon extends BeamTargetWagon {
 
    public LorentzVector VB, VT;

    public DVPi0PWagon() {
        super("DVPi0PWagon","fxgirod","0.0");

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

	    boolean hasDVPi0P = false;
	    if( RecPart!=null && RecPart.getRows()>3 ){

		    for (int ie = 0; ie < RecPart.getRows()-3 && !hasDVPi0P; ie++) {
			    int is_e     = RecPart.getInt("pid", ie);
			    double e_px  = RecPart.getFloat("px", ie);
			    double e_py  = RecPart.getFloat("py", ie);
			    double e_pz  = RecPart.getFloat("pz", ie);
			    int e_stat   = Math.abs(RecPart.getShort("status", ie));

			    double e_mom = Math.sqrt(e_px*e_px+e_py*e_py+e_pz*e_pz);
			    if( is_e==11 && e_stat>2000 && e_stat<4000 && e_mom>0.1*beamEnergy ){
				    LorentzVector VE = new LorentzVector(e_px,e_py,e_pz,e_mom);
				    for (int ip = ie+1; ip < RecPart.getRows()-2 && !hasDVPi0P; ip++) {
					    int is_p     = RecPart.getInt("pid", ip);
					    double p_px  = RecPart.getFloat("px", ip);
					    double p_py  = RecPart.getFloat("py", ip);
					    double p_pz  = RecPart.getFloat("pz", ip);
					    int p_stat   = Math.abs(RecPart.getShort("status", ip));

					    if( is_p==2212 && p_stat>2000 ){
						    double p_ene = Math.sqrt(p_px*p_px+p_py*p_py+p_pz*p_pz+targetMass*targetMass);
						    LorentzVector VP = new LorentzVector(p_px,p_py,p_pz,p_ene);
						    for (int ig1 = ip+1; ig1 < RecPart.getRows()-1 && !hasDVPi0P ; ig1++) {
							    int is_g1     = RecPart.getInt("pid", ig1);
							    double g1_px  = RecPart.getFloat("px", ig1);
							    double g1_py  = RecPart.getFloat("py", ig1);
							    double g1_pz  = RecPart.getFloat("pz", ig1);
							    int g1_stat   = Math.abs(RecPart.getShort("status", ig1));

							    double g1_mom = Math.sqrt(g1_px*g1_px+g1_py*g1_py+g1_pz*g1_pz);
							    if( is_g1==22 && g1_stat<4000 && g1_mom>0.15){
								    LorentzVector VG1 = new LorentzVector(g1_px,g1_py,g1_pz,g1_mom);
								    for (int ig2 = ig1+1; ig2 < RecPart.getRows() && !hasDVPi0P ; ig2++) {
									    int is_g2     = RecPart.getInt("pid", ig2);
									    double g2_px  = RecPart.getFloat("px", ig2);
									    double g2_py  = RecPart.getFloat("py", ig2);
									    double g2_pz  = RecPart.getFloat("pz", ig2);
									    int g2_stat   = Math.abs(RecPart.getShort("status", ig2));

									    double g2_mom = Math.sqrt(g2_px*g2_px+g2_py*g2_py+g2_pz*g2_pz);
									    if( is_g2==22 && g2_stat<4000 && g2_mom>0.15){
										    LorentzVector VG2 = new LorentzVector(g2_px,g2_py,g2_pz,g2_mom);

										    LorentzVector VPI0 = new LorentzVector(0,0,0,0);
										    VPI0.add(VG1);
										    VPI0.add(VG2);

										    LorentzVector Q = new LorentzVector(0,0,0,0);
										    Q.add(VB);
										    Q.sub(VE);
										    LorentzVector W = new LorentzVector(0,0,0,0);
										    W.add(Q);
										    W.add(this.VT);

										    double e_g1_angle = Vangle( VE.vect() , VG1.vect() );
										    double e_g2_angle = Vangle( VE.vect() , VG2.vect() );
										    double g1_g2_angle = Vangle( VG1.vect() , VG2.vect() );

										    if( -Q.mass2()>0.8 && W.mass()>1.8 && e_g1_angle>4 && e_g2_angle>4 && g1_g2_angle>1 ){
											    LorentzVector VmissP = new LorentzVector(0,0,0,0);
											    VmissP.add(W);
											    VmissP.sub(VPI0);
											    LorentzVector VmissPI0 = new LorentzVector(0,0,0,0);
											    VmissPI0.add(W);
											    VmissPI0.sub(VP);
											    LorentzVector VmissAll = new LorentzVector(0,0,0,0);
											    VmissAll.add(VmissPI0);
											    VmissAll.sub(VPI0);

											    hasDVPi0P = true
												    && VmissAll.e() > -1.5 && VmissAll.e() < 2.0
												    && VmissP.mass() > 0 && VmissP.mass() < 2.5
												    && VmissAll.mass2() > -0.1 &&  VmissAll.mass2() < 0.1
												    && VmissAll.px()*VmissAll.px() + VmissAll.py()*VmissAll.py() < 1.0
												    && VPI0.mass()>0.05 && VPI0.mass()<1.0 
												    && Vangle( VPI0.vect() , VmissPI0.vect() ) < 7.5
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
	    return hasDVPi0P;
    }
}


