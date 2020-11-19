package org.jlab.jnp.grapes.services;

import java.util.ArrayList;
import org.jlab.jnp.physics.Vector3;
import org.jlab.jnp.physics.LorentzVector;
import org.jlab.jnp.physics.Particle;

import org.jlab.jnp.hipo4.data.Bank;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.data.SchemaFactory;

/**
 *
 * ep2gamma Skimming
 *
 * @author izzy
 */

public class ep2gammaWagon extends BeamTargetWagon {

        private final int    minParticles = 4;
        private final double minProtonEnergy = 0.94358;
        private final double minGammaEnergy = 0.15;
        private final double minGammaAngle = 4;
        private final double minEtaAngle = 1;

        private double minElectronEnergy = 0;

	public ep2gammaWagon() {
		super("ep2gammaWagon","izzy","0.0");
	}

	private double Vangle(Vector3 v1, Vector3 v2){
		double res=0;
		double l1 = v1.mag();
		double l2 = v2.mag();
		if( l1*l2 > 0)res = Math.toDegrees( Math.acos( v1.dot(v2)/(l1*l2) ) );
		return res;
	}

        private boolean isElectron(int pid, int status, Particle part) {
            boolean value = true;

            status = (int) status/1000;
            if(pid==11 && status==2 && part.p()>this.minElectronEnergy) return true;
            else                     return false;
        }

        private boolean isProton(int pid, int status, Particle part) {
            boolean value = false;

            status = (int) status/1000;
            if(pid==2212 && (status==2 || status==4) && part.e()>this.minProtonEnergy) {
                value = true;
            }
            return value;
        }

        private boolean isGamma(int pid, int status, Particle part) {
            boolean value = false;

            status = (int) status/1000;
            if(pid==22 && (status==1 || status==2) && part.e()>this.minGammaEnergy) {
                value = true;
            }
            return value;
        }

	@Override
	public boolean processDataEvent(Event event, SchemaFactory factory) {
            Particle beam   = new Particle(11,0,0,beamEnergy,0,0,0);
            Particle target = Particle.createWithMassCharge(targetMass, +1, 0,0,0, 0,0,0);

            this.minElectronEnergy=0.1*beamEnergy;

            Bank RecPart = new Bank(factory.getSchema("REC::Particle"));
            event.read(RecPart);

            boolean hasEta = false;

            // event topology
            Particle electron = null;
            ArrayList<Particle> protons = new ArrayList<Particle>();
            ArrayList<Particle> gammas  = new ArrayList<Particle>();
            if( RecPart!=null && RecPart.getRows()>=this.minParticles){
                for (int ii = 0; ii < RecPart.getRows() ; ii++) {
                    int pid    = RecPart.getInt("pid", ii);
                    int status = Math.abs(RecPart.getShort("status", ii));
                    if(pid==0) continue;
                    Particle part = new Particle(pid,
                                                     RecPart.getFloat("px",ii),
                                                     RecPart.getFloat("py",ii),
                                                     RecPart.getFloat("pz",ii),
                                                     RecPart.getFloat("vx",ii),
                                                     RecPart.getFloat("vy",ii),
                                                     RecPart.getFloat("vz",ii));
                    if(ii==0 && isElectron(pid,status,part)) electron = part;
                    else if(isProton(pid,status,part))       protons.add(part);
                    else if(isGamma(pid,status,part))        gammas.add(part);
                }

                boolean is_candidate = electron!=null && protons.size()>0 && gammas.size()>1;
                if(!is_candidate) return false;
                else {

                    Particle q = new Particle();
                    q.copy(beam);
                    q.combine(electron, -1);
                    Particle w = new Particle();
                    w.copy(target);
                    w.combine(q, +1);

                    for(int ip=0; ip<protons.size() && !hasEta; ip++) {
                        for (int ig1 = 0; ig1 < gammas.size()-1 && !hasEta ; ig1++) {
                            for (int ig2 = ig1+1; ig2 < gammas.size() && !hasEta ; ig2++) {
                                Particle eta = new Particle();
                                eta.copy(gammas.get(ig1));
                                eta.combine(gammas.get(ig2), +1);

                                double e_g1_angle = Vangle( electron.vector().vect() , gammas.get(ig1).vector().vect() );
                                double e_g2_angle = Vangle( electron.vector().vect() , gammas.get(ig2).vector().vect() );
                                double g1_g2_angle = Vangle( gammas.get(ig1).vector().vect() , gammas.get(ig2).vector().vect());

                                if( e_g1_angle>this.minGammaAngle && e_g2_angle>this.minGammaAngle && g1_g2_angle>this.minEtaAngle ){

                                    Particle missP = new Particle();
                                    missP.copy(w);
                                    missP.combine(eta, -1);
                                    Particle missEta = new Particle();
                                    missEta.copy(w);
                                    missEta.combine(protons.get(ip), -1);
                                    Particle missAll = new Particle();
                                    missAll.copy(missEta);
                                    missAll.combine(eta, -1);

                                    hasEta = true
                                            && missAll.e() > -1.5 && missAll.e() < 2.0
                                            && missP.mass() > 0 && missP.mass() < 2.5
                                            && missAll.mass2() > -0.1 &&  missAll.mass2() < 0.1
                                            && missAll.px()*missAll.px() + missAll.py()*missAll.py() < 1.0
                                            && eta.mass()>0.05 && eta.mass()<1.0
                                            && Vangle( eta.vector().vect() , eta.vector().vect() ) < 7.5
                                            ;
                                }
                            }
                        }
                    }
                }
            }
            return hasEta;
	}
}
