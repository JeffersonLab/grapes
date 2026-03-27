package org.jlab.jnp.grapes.services;

import org.jlab.jnp.hipo4.data.Bank;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.data.SchemaFactory;
import org.jlab.jnp.physics.Particle;
import org.jlab.jnp.physics.ParticleList;
import org.jlab.jnp.utils.json.Json;
import org.jlab.jnp.utils.json.JsonObject;
import org.jlab.jnp.hipo4.io.HipoReader;

/**
 * @author gotra
 *
 * Lambda skim wagon for RG-K. Applies a W > 1.5 GeV cut to remove low-W
 * background below the Lambda production region. Note: the strict Lambda
 * production threshold for ep -> e' K+ Lambda is W ~ 1.61 GeV (m_K + m_Lambda),
 * so this cut removes the resonance region rather than being a threshold cut.
 *
 * W is the invariant mass of the hadronic system:
 * W^2 = M_p^2 + 2*M_p*(E_beam - E'_e) - Q^2
 * where E'_e is the scattered electron energy and Q^2 = -(k - k')^2.
 *
 * Configuration keys passed to init(String jsonString):
 * "id"         : wagon id (1 = skim1 / FD electron, 2 = skim2 / FT electron,
 * 3 = skim3 / expanded skim-3 category)
 * "beamEnergy" : beam energy in GeV (default 6.535 for RG-K)
 * "forward"    : ExtendedEventFilter string for skim1 (default "X+:X-:Xn")
 * "tagger"     : ExtendedEventFilter string for skim2 (default "X+:X-:Xn")
 * "noetrig"    : ExtendedEventFilter string for skim3 (default "X+:X-:Xn")
 *
 * If your workflow originates from YAML, ensure it is converted to the JSON
 * payload passed into init(String jsonString).
 *
 * Trigger/category classification (used consistently in computeW() and getParticleList()):
 *
 * FT trigger: exists pid==11 && status<0 && abs(status)/1000==1 in RECFT::Particle.
 * RECFT::Particle merely having rows is NOT sufficient — the bank
 * shadows REC::Particle and may have rows in non-FT events.
 *
 * FD trigger: pid==11 && status<0 && abs(status)/1000==2 at row 0 of REC::Particle.
 * Only checked if FT trigger is absent.
 *
 * Expanded skim-3 category:
 * any event that is not already skim 1 (FT) and not already
 * skim 2 (FD electron) is eligible for skim 3 if it passes the
 * REC-only electron W cut and the Lambda final-state filter.
 *
 * Skim-specific electron definition for W computation (wagonId-dependent):
 *
 * id=1 (FD trigger):
 * W from FD trigger electron ONLY — row 0 of REC::Particle, pid==11,
 * status < 0, abs(status)/1000 == 2.
 *
 * id=2 (FT trigger):
 * W from FT trigger electron ONLY — loop RECFT::Particle, pid==11,
 * status < 0, abs(status)/1000 == 1.
 *
 * id=3 (expanded skim 3):
 * Any event NOT classified as skim 1 (FT) or skim 2 (FD electron) is
 * eligible for skim 3.
 * Electron is not assumed to be the trigger particle, so status may be positive.
 * W is computed from the highest-momentum inclusive electron found in
 * REC::Particle only: accept pid==11 with abs(status)/1000==1 or 2 and
 * apply the intentional symmetric cut abs(chi2pid) < 5.
 *
 * ParticleList construction:
 * All skims build the ParticleList from REC::Particle (which contains all
 * reconstructed particles including hadrons). RECFT::Particle is used only
 * for trigger classification and W computation, not for particle list building.
 * RECFT shadows REC with the same ordering, so REC is the authoritative source
 * for hadron momenta and vertex information.
 * pid==0 (unidentified) rows are skipped and do not enter the filter.
 *
 * Skim mutual exclusivity:
 * FT trigger present                -> statusWord=TAGGER  -> only LAMBDA2 accepts
 * No FT, FD electron trigger        -> statusWord=FORWARD -> only LAMBDA1 accepts
 * Otherwise                         -> statusWord=ANY     -> expanded LAMBDA3 accepts
 *
 * CENTRAL is currently reserved/unused in this wagon. CD particles are not tagged
 * as CENTRAL; they inherit the skim category status word used for the full event.
 */
public class LambdaWagon extends Wagon {

    private String filterStringForward = null;
    private String filterStringTagger  = null;
    private String filterStringAny     = null;

    private ExtendedEventFilter eventFilterForward = new ExtendedEventFilter();
    private ExtendedEventFilter eventFilterTagger  = new ExtendedEventFilter();
    private ExtendedEventFilter eventFilterAny     = new ExtendedEventFilter();

    public static final int TAGGER  = 1;
    public static final int FORWARD = 2;
    public static final int CENTRAL = 3; // reserved / unused in current wagon logic
    public static final int ANY     = 4;

    // Proton mass in GeV
    private static final double M_PROTON = 0.938272;

    // W cut: removes low-W background below Lambda production region
    private static final double W_CUT = 1.5; // GeV

    // Beam energy (GeV), configurable via JSON "beamEnergy"
    private double beamEnergy = 6.535; // RG-K default

    // Wagon id, configurable via JSON "id", determines skim-specific logic
    private int wagonId = 1;

    // Counters for monitoring
    private int nTotal   = 0;
    private int nPassW   = 0;
    private int nPassAll = 0;

    // Banks declared at class level and lazily initialized once per wagon instance.
    // event.read() overwrites existing bank data in place — no per-event allocation,
    // avoiding GC pressure over millions of events.
    private Bank recParticleBank   = null;
    private Bank recFTParticleBank = null;

    public LambdaWagon() {
        super("LambdaWagon", "myname", "0.1");
    }

    /**
     * Classify trigger/category from pre-read banks.
     * Returns TAGGER, FORWARD, or ANY — never any other value.
     *
     * FT trigger: pid==11 && status<0 && abs(status)/1000==1 in RECFT::Particle.
     * FD trigger: pid==11 && status<0 && abs(status)/1000==2 at row 0 of REC::Particle.
     * Only checked if FT trigger is absent.
     * ANY       : expanded skim-3 / non-skim1-non-skim2 fallback category.
     */
    private static int classifyTrigger(Bank recParticleBank, Bank recFTParticleBank) {
        int nrowsFT = recFTParticleBank.getRows();

        // FT trigger: requires actual FT trigger electron, not merely rows in RECFT
        for (int i = 0; i < nrowsFT; i++) {
            int pid    = recFTParticleBank.getInt("pid", i);
            int status = recFTParticleBank.getInt("status", i);
            if (pid == 11 && status < 0 && Math.abs(status) / 1000 == 1) return TAGGER;
        }

        // FD trigger: only checked if FT trigger is absent
        if (recParticleBank.getRows() > 0) {
            int pid    = recParticleBank.getInt("pid", 0);
            int status = recParticleBank.getInt("status", 0);
            if (pid == 11 && status < 0 && Math.abs(status) / 1000 == 2) return FORWARD;
        }

        return ANY; // expanded skim-3 / non-skim1-non-skim2 fallback category
    }

    /**
     * Compute W from a reconstructed electron, using skim-specific electron definition.
     * Takes pre-read banks — do NOT call event.read() inside this method.
     *
     * id=1 (FD trigger):
     * FD trigger electron only — row 0 of REC::Particle, pid==11,
     * status < 0, abs(status)/1000 == 2.
     *
     * id=2 (FT trigger):
     * FT trigger electron only — loop RECFT::Particle, pid==11,
     * status < 0, abs(status)/1000 == 1.
     *
     * id=3 (expanded skim 3):
     * Best inclusive electron = highest momentum candidate from REC::Particle only.
     * REC::Particle is the authoritative kinematics source — scanning RECFT too
     * risks double-counting the same electron with slightly different momenta.
     * Electron is NOT the trigger particle so status may be positive.
     * Accept pid==11 with abs(status)/1000==1 or 2.
     * Intentional symmetric PID cut: abs(chi2pid) < 5 keeps both tails equally.
     * Select candidate with maximum |p|.
     *
     * Kinematic guards:
     * Eprime < 1e-6 GeV : zero/near-zero momentum, bad track -> return -1
     * nu < 0            : Eprime > beamEnergy, unphysical    -> return -1
     * W2 < 0            : unphysical kinematics              -> return -1
     *
     * W^2 = M_p^2 + 2*M_p*nu - Q^2
     * nu  = E_beam - E'_e
     * Q^2 = 2*E_beam*E'_e*(1 - cos(theta_e))    [massless electron approximation]
     *
     * Returns -1 if no valid electron found or kinematics are unphysical.
     */
    private double computeW(Bank recParticleBank, Bank recFTParticleBank) {

        double px = 0, py = 0, pz = 0;
        boolean found = false;

        if (wagonId == 1) {
            // Skim 1: FD trigger electron only
            // row 0 of REC::Particle, pid==11, status < 0, abs(status)/1000 == 2
            if (recParticleBank.getRows() > 0) {
                int pid    = recParticleBank.getInt("pid", 0);
                int status = recParticleBank.getInt("status", 0);
                if (pid == 11 && status < 0 && Math.abs(status) / 1000 == 2) {
                    px    = recParticleBank.getFloat("px", 0);
                    py    = recParticleBank.getFloat("py", 0);
                    pz    = recParticleBank.getFloat("pz", 0);
                    found = true;
                }
            }
        } else if (wagonId == 2) {
            // Skim 2: FT trigger electron only
            // loop RECFT::Particle, pid==11, status < 0, abs(status)/1000 == 1
            int nrowsFT = recFTParticleBank.getRows();
            for (int i = 0; i < nrowsFT; i++) {
                int pid    = recFTParticleBank.getInt("pid", i);
                int status = recFTParticleBank.getInt("status", i);
                if (pid == 11 && status < 0 && Math.abs(status) / 1000 == 1) {
                    px    = recFTParticleBank.getFloat("px", i);
                    py    = recFTParticleBank.getFloat("py", i);
                    pz    = recFTParticleBank.getFloat("pz", i);
                    found = true;
                    break;
                }
            }
        } else if (wagonId == 3) {
            // Skim 3: highest-momentum inclusive electron from REC::Particle only.
            // REC::Particle is the authoritative kinematics source — RECFT shadows
            // REC with the same ordering so scanning both would risk double-counting
            // the same electron with slightly different reconstructed momenta.
            double pBest = -1.0;
            int nrows = recParticleBank.getRows();
            for (int i = 0; i < nrows; i++) {
                int pid  = recParticleBank.getInt("pid", i);
                int absS = Math.abs(recParticleBank.getInt("status", i));
                if (pid == 11 && (absS / 1000 == 1 || absS / 1000 == 2)) {
                    float chi2pid = recParticleBank.getFloat("chi2pid", i);
                    // Symmetric PID cut: bound both chi2pid tails equally
                    if (Math.abs(chi2pid) >= 5.0) continue;
                    double tpx = recParticleBank.getFloat("px", i);
                    double tpy = recParticleBank.getFloat("py", i);
                    double tpz = recParticleBank.getFloat("pz", i);
                    double p   = Math.sqrt(tpx * tpx + tpy * tpy + tpz * tpz);
                    if (p > pBest) {
                        pBest = p; px = tpx; py = tpy; pz = tpz; found = true;
                    }
                }
            }
        } else {
            // Invalid wagon id: no electron definition available.
            return -1.0;
        }

        if (!found) return -1.0;

        // Scattered electron energy (massless approximation)
        double Eprime = Math.sqrt(px * px + py * py + pz * pz);

        // Guard: zero or near-zero momentum — bad track
        if (Eprime < 1e-6) return -1.0;

        // Energy transfer
        double nu = beamEnergy - Eprime;

        // Guard: negative energy transfer — Eprime > beamEnergy, unphysical
        if (nu < 0) return -1.0;

        // cos(theta) between beam (along z) and scattered electron.
        // Clamped to [-1, 1] to guard against float rounding (e.g. 1.0000002)
        // which would produce negative Q^2 and artificially inflate W^2.
        double cosTheta = Math.max(-1.0, Math.min(1.0, pz / Eprime));

        // Q^2 = 2 * E_beam * E'_e * (1 - cos(theta_e))
        double Q2 = 2.0 * beamEnergy * Eprime * (1.0 - cosTheta);

        // W^2 = M_p^2 + 2*M_p*nu - Q^2
        double W2 = M_PROTON * M_PROTON + 2.0 * M_PROTON * nu - Q2;

        // Guard: unphysical W^2 — bad track or wrong electron candidate
        return (W2 > 0) ? Math.sqrt(W2) : -1.0;
    }

    @Override
    public boolean processDataEvent(Event event, SchemaFactory factory) {

        // Lazily initialize banks once — event.read() overwrites in place,
        // avoiding per-event Bank allocation and GC pressure over millions of events.
        if (recParticleBank   == null) recParticleBank   = new Bank(factory.getSchema("REC::Particle"));
        if (recFTParticleBank == null) recFTParticleBank = new Bank(factory.getSchema("RECFT::Particle"));
        event.read(recParticleBank);
        event.read(recFTParticleBank);

        nTotal++;

        // Classify trigger — used consistently for W computation and particle filtering
        int triggerType = classifyTrigger(recParticleBank, recFTParticleBank);

        if (wagonId == 1) {
            // LAMBDA1: accept FD electron trigger only
            if (triggerType != FORWARD) return false;
            double W = computeW(recParticleBank, recFTParticleBank);
            if (W < 0 || W < W_CUT) return false;
        } else if (wagonId == 2) {
            // LAMBDA2: accept FT electron trigger only
            if (triggerType != TAGGER) return false;
            double W = computeW(recParticleBank, recFTParticleBank);
            if (W < 0 || W < W_CUT) return false;
        } else if (wagonId == 3) {
            // LAMBDA3 expanded logic:
            // any event not already accepted by skim 1 (FT) or skim 2 (FD electron)
            // is eligible for skim 3.
            if (triggerType == TAGGER || triggerType == FORWARD) return false;
            double W = computeW(recParticleBank, recFTParticleBank);
            if (W < 0 || W < W_CUT) return false;
        } else {
            // Defensive guard: invalid wagon id must never accept events.
            return false;
        }

        nPassW++;

        // Particle filter — each wagon applies only its own filter.
        // getParticleList() uses classifyTrigger() logic for statusWord assignment.
        // In practice, mutual exclusivity is enforced by the trigger gating above.
        // All skims build ParticleList from REC::Particle — RECFT is used only
        // for trigger classification and W; hadrons are in REC::Particle.
        ParticleList pl = getParticleList(recParticleBank, recFTParticleBank);

        if (wagonId == 1) {
            pl.setStatusWord(FORWARD);
            if (!eventFilterForward.checkFinalState(pl)) return false;
        } else if (wagonId == 2) {
            pl.setStatusWord(TAGGER);
            if (!eventFilterTagger.checkFinalState(pl)) return false;
        } else if (wagonId == 3) {
            pl.setStatusWord(ANY);
            if (!eventFilterAny.checkFinalState(pl)) return false;
        }

        nPassAll++;
        return true;
    }

    /**
     * Initialize the wagon from a JSON payload. If your workflow starts from YAML,
     * convert it upstream and pass the resulting JSON string here.
     */
    @Override
    public boolean init(String jsonString) {
        JsonObject jsonObj = Json.parse(jsonString).asObject();
        filterStringForward = jsonObj.getString("forward",    "X+:X-:Xn");
        filterStringTagger  = jsonObj.getString("tagger",     "X+:X-:Xn");
        filterStringAny     = jsonObj.getString("noetrig",    "X+:X-:Xn");
        beamEnergy          = jsonObj.getDouble("beamEnergy", 6.535);
        wagonId             = jsonObj.getInt("id",            1);

        if (wagonId < 1 || wagonId > 3) {
            System.err.println("[LambdaWagon] Invalid wagon id: " + wagonId + " (expected 1, 2, or 3)");
            return false;
        }

        eventFilterForward.setFilter(filterStringForward);
        eventFilterTagger.setFilter(filterStringTagger);
        eventFilterAny.setFilter(filterStringAny);

        System.out.println("[LambdaWagon] id: "          + wagonId);
        System.out.println("[LambdaWagon] W cut: W > "   + W_CUT + " GeV");
        System.out.println("[LambdaWagon] Beam energy: " + beamEnergy + " GeV");
        System.out.println("[LambdaWagon] Trigger: " +
                (wagonId == 1 ? "FD electron (row0, pid==11, status -2xxx)"     :
                 wagonId == 2 ? "FT electron (pid==11, status -1xxx)"           :
                                "expanded skim-3 / non-skim1-non-skim2 category"));
        System.out.println("[LambdaWagon] Electron: " +
                (wagonId == 1 ? "FD trigger only (row0, status -2xxx)"                      :
                 wagonId == 2 ? "FT trigger only (status -1xxx)"                             :
                                "highest-p inclusive (abs(chi2pid)<5, abs(status)/1000==1 or 2)"));
        return true;
    }

    /**
     * Build the production ParticleList from pre-read banks.
     * All skims source from REC::Particle — hadrons are not in RECFT::Particle.
     * pid==0 (unidentified) rows are skipped so they do not enter filter checks.
     *
     * statusWord assignment mirrors classifyTrigger():
     * TAGGER  -> FT electron trigger
     * FORWARD -> FD electron trigger
     * ANY     -> expanded skim-3 category
     *
     * CD particles (status -4xxx) are not tagged as CENTRAL — they inherit the
     * event-level skim category status word. CD identification is left to
     * downstream analysis code.
     */
    public static ParticleList getParticleList(Bank recParticleBank, Bank recFTParticleBank) {

        ParticleList pList = new ParticleList();
        int nrows = recParticleBank.getRows();
        if (nrows == 0) return pList;

        int triggerType = classifyTrigger(recParticleBank, recFTParticleBank);
        int detector    = (triggerType == FORWARD) ? FORWARD :
                          (triggerType == TAGGER)  ? TAGGER  : ANY;

        for (int i = 0; i < nrows; i++) {
            int pid = recParticleBank.getInt("pid", i);
            if (pid == 0) continue; // skip unidentified rows

            Particle p = new Particle();
            p.initParticle(pid,
                    recParticleBank.getFloat("px", i),
                    recParticleBank.getFloat("py", i),
                    recParticleBank.getFloat("pz", i),
                    recParticleBank.getFloat("vx", i),
                    recParticleBank.getFloat("vy", i),
                    recParticleBank.getFloat("vz", i));
            p.setStatus(detector);
            pList.add(p);
        }

        pList.setStatusWord(detector);
        return pList;
    }

    public static void main(String[] args) {

        if (args.length < 1) {
            System.err.println("Usage: LambdaWagon <input.hipo> [beamEnergyGeV]");
            System.err.println("Example: LambdaWagon rec_clas_005419.evio.hipo 6.535");
            return;
        }

        String hipoFile = args[0];
        double beamE    = args.length > 1 ? Double.parseDouble(args[1]) : 6.535;
        // Set to true to print the full ParticleList for every filter-passing event.
        // Leave false for normal runs — output grows very large on real data files.
        boolean debug = false;

        HipoReader reader = new HipoReader();
        reader.open(hipoFile);
        SchemaFactory factory = reader.getSchemaFactory();

        // Use ExtendedEventFilter to match the production wagon filter path exactly.
        // All three skims share the same filter string so trigger- and
        // W-definition differences can be compared in isolation.
        ExtendedEventFilter tagger  = new ExtendedEventFilter();
        ExtendedEventFilter forward = new ExtendedEventFilter();
        ExtendedEventFilter any     = new ExtendedEventFilter();

        tagger.setFilter("11:2212:-211:X+:X-:Xn");
        System.out.println("ftsummary " + tagger.summary());
        System.out.println(tagger.toString());

        forward.setFilter("11:2212:-211:X+:X-:Xn");
        System.out.println("fdsummary " + forward.summary());
        System.out.println(forward.toString());

        any.setFilter("11:2212:-211:X+:X-:Xn");
        System.out.println("any " + any.summary());
        System.out.println(any.toString());

        // Instantiate wagons for all three skims.
        // Direct field assignment is intentional here; wagonId values 1/2/3
        // are valid and do not require init() validation in this harness context.
        LambdaWagon wagon1 = new LambdaWagon(); wagon1.beamEnergy = beamE; wagon1.wagonId = 1;
        LambdaWagon wagon2 = new LambdaWagon(); wagon2.beamEnergy = beamE; wagon2.wagonId = 2;
        LambdaWagon wagon3 = new LambdaWagon(); wagon3.beamEnergy = beamE; wagon3.wagonId = 3;

        Event event     = new Event();
        int eventNumber = 0;
        int nPassW1     = 0;
        int nPassW2     = 0;
        int nPassW3     = 0;

        // Trigger type counters.
        // nTriggerUnknown should always be zero — classifyTrigger() always returns
        // TAGGER, FORWARD, or ANY. Investigate immediately if nonzero.
        int nTriggerFT      = 0;
        int nTriggerFD      = 0;
        int nTriggerSkim3   = 0;
        int nTriggerUnknown = 0;

        // W distribution histograms: 0-5 GeV in 0.2 GeV bins (25 bins).
        // Events with W > 5 GeV are counted into the last bin (overflow).
        int nBins = 25;
        double binWidth = 0.2;
        int[] wBins1 = new int[nBins];
        int[] wBins2 = new int[nBins];
        int[] wBins3 = new int[nBins];
        int nNoElectron1 = 0;
        int nNoElectron2 = 0;
        int nNoElectron3 = 0;

        // Allocate banks once outside the loop — event.read() overwrites in place,
        // avoiding per-event Bank allocation and GC pressure over millions of events.
        Bank recParticleBank   = new Bank(factory.getSchema("REC::Particle"));
        Bank recFTParticleBank = new Bank(factory.getSchema("RECFT::Particle"));

        while (reader.hasNext()) {
            eventNumber++;
            reader.nextEvent(event);
            event.read(recParticleBank);
            event.read(recFTParticleBank);

            int triggerType = classifyTrigger(recParticleBank, recFTParticleBank);
            if      (triggerType == TAGGER)  nTriggerFT++;
            else if (triggerType == FORWARD) nTriggerFD++;
            else if (triggerType == ANY)     nTriggerSkim3++;
            else                             nTriggerUnknown++; // should never fire

            // Skim 1: FD electron trigger
            if (triggerType == FORWARD) {
                double W1 = wagon1.computeW(recParticleBank, recFTParticleBank);
                if (W1 < 0) {
                    nNoElectron1++;
                } else {
                    int bin = (int)(W1 / binWidth);
                    if (bin < nBins) wBins1[bin]++;
                    else             wBins1[nBins-1]++; // overflow (W > 5 GeV) -> last bin
                    if (W1 >= W_CUT) nPassW1++;
                }
            }

            // Skim 2: FT electron trigger
            if (triggerType == TAGGER) {
                double W2 = wagon2.computeW(recParticleBank, recFTParticleBank);
                if (W2 < 0) {
                    nNoElectron2++;
                } else {
                    int bin = (int)(W2 / binWidth);
                    if (bin < nBins) wBins2[bin]++;
                    else             wBins2[nBins-1]++; // overflow (W > 5 GeV) -> last bin
                    if (W2 >= W_CUT) nPassW2++;
                }
            }

            // Skim 3 expanded logic: any event not already skim 1 or skim 2,
            // using the highest-momentum inclusive electron from REC::Particle.
            if (triggerType != TAGGER && triggerType != FORWARD) {
                double W3 = wagon3.computeW(recParticleBank, recFTParticleBank);
                if (W3 < 0) {
                    nNoElectron3++;
                } else {
                    int bin = (int)(W3 / binWidth);
                    if (bin < nBins) wBins3[bin]++;
                    else             wBins3[nBins-1]++; // overflow (W > 5 GeV) -> last bin
                    if (W3 >= W_CUT) nPassW3++;
                }
            }

            ParticleList pList = LambdaWagon.getParticleList(recParticleBank, recFTParticleBank);

            pList.setStatusWord(LambdaWagon.TAGGER);
            boolean statusT = tagger.checkFinalState(pList);
            pList.setStatusWord(LambdaWagon.FORWARD);
            boolean statusF = forward.checkFinalState(pList);
            pList.setStatusWord(LambdaWagon.ANY);
            boolean statusA = any.checkFinalState(pList);

            if (debug) {
                if (statusF) { System.out.println("FORWARD = "); System.out.println(pList.toString()); }
                if (statusT) { System.out.println("TAGGER = ");  System.out.println(pList.toString()); }
                if (statusA) { System.out.println("ANY = ");     System.out.println(pList.toString()); }
            }
        }

        // Summary
        System.out.println("\n=== SUMMARY (beamEnergy=" + beamE + " GeV, W_CUT=" + W_CUT + " GeV) ===");
        System.out.println("Total events          : " + eventNumber);
        System.out.println("FT trigger events     : " + nTriggerFT);
        System.out.println("FD trigger events     : " + nTriggerFD);
        System.out.println("Expanded skim3 events : " + nTriggerSkim3);
        System.out.println("Unknown trigger       : " + nTriggerUnknown
                + (nTriggerUnknown != 0 ? " *** UNEXPECTED — investigate classifyTrigger() ***" : " (expected zero)"));
        System.out.println("Pass W cut skim1 (FD  electron trigger, FD electron only)                      : " + nPassW1);
        System.out.println("Pass W cut skim2 (FT  electron trigger, FT electron only)                      : " + nPassW2);
        System.out.println("Pass W cut skim3 (expanded skim3 category, highest-p inclusive electron chi2<5): " + nPassW3);

        // W distributions
        System.out.println("\n=== W DISTRIBUTION SKIM1 (FD trigger, FD electron only) ===");
        System.out.println("  No electron found / unphysical : " + nNoElectron1);
        for (int i = 0; i < nBins; i++) {
            System.out.println(String.format("  W=[%.1f-%.1f] GeV : %d",
                i*binWidth, (i+1)*binWidth, wBins1[i]));
        }
        System.out.println(String.format("  (last bin includes overflow W > %.1f GeV)", nBins*binWidth));

        System.out.println("\n=== W DISTRIBUTION SKIM2 (FT tagger trigger, FT electron only) ===");
        System.out.println("  No electron found / unphysical : " + nNoElectron2);
        for (int i = 0; i < nBins; i++) {
            System.out.println(String.format("  W=[%.1f-%.1f] GeV : %d",
                i*binWidth, (i+1)*binWidth, wBins2[i]));
        }
        System.out.println(String.format("  (last bin includes overflow W > %.1f GeV)", nBins*binWidth));

        System.out.println("\n=== W DISTRIBUTION SKIM3 (expanded trigger, highest-p inclusive electron) ===");
        System.out.println("  No electron found / unphysical : " + nNoElectron3);
        for (int i = 0; i < nBins; i++) {
            System.out.println(String.format("  W=[%.1f-%.1f] GeV : %d",
                i*binWidth, (i+1)*binWidth, wBins3[i]));
        }
        System.out.println(String.format("  (last bin includes overflow W > %.1f GeV)", nBins*binWidth));
    }
}

