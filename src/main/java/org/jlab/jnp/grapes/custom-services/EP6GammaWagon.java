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
 * EP6Gamma Skimming
 *
 * @author izzy
 */

public class EP6GammaWagon extends BeamTargetWagon {

        private final int    minParticles = 4;
        private final double minProtonEnergy=0.94358;
        private final double minGammaEnergy=0.15;
        private final double minGammaAngle=4;
        private final double minEtaAngle=1;
        private final double minPi0Mass=0;
        private final double maxPi0Mass=0.3;


        private double minElectronEnergy=0;

	public EP6GammaWagon() {
		super("EP6GammaWagon","izzy","0.0");
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

        private boolean isPi0(Particle part) {
            boolean value = false;

            if(part.mass()>this.minPi0Mass && part.mass()<this.maxPi0Mass) {
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

            boolean has3pi = false;

            // event topology
            Particle electron = null;
            ArrayList<Particle> protons = new ArrayList<Particle>();
            ArrayList<Particle> gammas  = new ArrayList<Particle>();
            ArrayList<PiZero> pi0s      = new ArrayList<PiZero>();
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

                boolean is_candidate = electron!=null && protons.size()>0 && gammas.size()>5;
                if(!is_candidate) return false;
                else {

                    Particle q = new Particle();
                    q.copy(beam);
                    q.combine(electron, -1);
                    Particle w = new Particle();
                    w.copy(target);
                    w.combine(q, +1);

                    // build pi0 candidate list
                    for (int ig1 = 0; ig1 < gammas.size()-1; ig1++) {
                        for (int ig2 = ig1+1; ig2 < gammas.size(); ig2++) {
                            PiZero pi0 = new PiZero();
                            pi0.copy(gammas.get(ig1));
                            pi0.combine(gammas.get(ig2), +1);
                            pi0.setId1(ig1);
                            pi0.setId2(ig2);
                            if(this.isPi0(pi0)) pi0s.add(pi0);
                        }
                    }
                    // check for combination of 3 pi0s built from different photons
                    if(pi0s.size()>=3) {
                        for (int ip1 = 0; ip1 < pi0s.size()-2; ip1++) {
                            for (int ip2 = ip1+1; ip2 < pi0s.size()-1; ip2++) {
                                for (int ip3 = ip2+1; ip3 < pi0s.size(); ip3++) {
                                    if(pi0s.get(ip1).diff(pi0s.get(ip2)) &&
                                       pi0s.get(ip2).diff(pi0s.get(ip3)) &&
                                       pi0s.get(ip1).diff(pi0s.get(ip3))) has3pi=true;
                                }
                            }
                        }
                    }

                }
            }
            return has3pi;
	}


        private class PiZero extends Particle {
            private int id1=-1;
            private int id2=-1;

            public int getId1() {
                return id1;
            }

            public void setId1(int id1) {
                this.id1 = id1;
            }

            public int getId2() {
                return id2;
            }

            public void setId2(int id2) {
                this.id2 = id2;
            }

            private boolean diff(PiZero pi0) {

                if(this.getId1() != pi0.getId1() &&
                   this.getId1() != pi0.getId2() &&
                   this.getId2() != pi0.getId1() &&
                   this.getId2() != pi0.getId2()) return true;
                else return false;
            }

        }

}
