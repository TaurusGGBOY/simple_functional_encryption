package algorithm;

import it.unisa.dia.gas.jpbc.*;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import it.unisa.dia.gas.plaf.jpbc.pairing.a.TypeACurveGenerator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

public class FE {

    public static final String paramFilePath = "params.properties";
    public static final String propertyFilePath = "properties.properties";

    class Params{
        Pairing pairing;
        Field G;
        Element g;
        BigInteger p;

        public Params(Pairing pairing, Field G, Element g, BigInteger p) {
            this.pairing = pairing;
            this.G = G;
            this.g = g;
            this.p = p;
        }
    }

    class MPK{
        public MPK(Params params, List<Element> pks) {
            this.params = params;
            this.pks = pks;
        }

        Params params;
        List<Element> pks;
    }

    class MSK{
        public MSK(List<Element> sks) {
            this.sks = sks;
        }

        List<Element> sks;
    }

    class SKY{
        public SKY(Element sky) {
            this.sky = sky;
        }

        Element sky;
    }

    class SetupParams{
        public SetupParams(MPK mpk, MSK msk) {
            this.mpk = mpk;
            this.msk = msk;
        }

        MPK mpk;
        MSK msk;
    }

    class CT0{
        Element ct0;

        public CT0(Element ct0) {
            this.ct0 = ct0;
        }
    }

    class CTI{
        List<Element> cti;

        public CTI(List<Element> cti) {
            this.cti = cti;
        }
    }

    class CT{
        CT0 ct0;
        CTI cti;

        public CT(CT0 ct0, CTI cti) {
            this.ct0 = ct0;
            this.cti = cti;
        }
    }

    class DecryptResult{
        public DecryptResult(Element decryptResult) {
            this.decryptResult = decryptResult;
        }

        Element decryptResult;
    }

    public void curveInit(int lambda) throws IOException {
        // curve 256bit, limit field 512bit
        // A E A1 F four curves
        PairingParametersGenerator generator = new TypeACurveGenerator(lambda, lambda * 2);
        PairingParameters parameters = generator.generate();
        BufferedWriter writer = new BufferedWriter(new FileWriter(paramFilePath));
        writer.write(parameters.toString());
        writer.close();
    }

    public SetupParams setup(int lambda, int ell) throws IOException {
        // init curve params
        File paramFile = new File(paramFilePath);
        if (!paramFile.exists() || paramFile.length() == 0) {
            curveInit(lambda);
        }

        // get params
        Pairing pairing = PairingFactory.getPairing(paramFilePath);
        Field G = pairing.getG1();
        BigInteger p = G.getOrder();
        Element g = G.newRandomElement().getImmutable();

        // init pks and sks
        List<Element> pks = new ArrayList<>();
        List<Element> sks = new ArrayList<>();
        for (int i = 0; i < ell; i++) {
            sks.add(pairing.getZr().newRandomElement().getImmutable());
            pks.add(g.powZn(sks.get(i)).getImmutable());
        }

        // init params
        Params params = new Params(pairing, G, g, p);

        // init mpk and msk
        MPK mpk = new MPK(params, pks);
        MSK msk = new MSK(sks);

        // init setup params
        SetupParams setupParams = new SetupParams(mpk, msk);

        return setupParams;
    }

    public SKY keyDer(Params params, MSK msk, List<Element> y){
        Element sky = params.pairing.getZr().newZeroElement();
        for (int i = 0; i < y.size(); i++) {
            sky = sky.add(y.get(i).mul(msk.sks.get(i)));
        }
        return new SKY(sky.getImmutable());
    }

    public CT encrypt(MPK mpk, List<Element> x) {
        Params params = mpk.params;
        Element r = params.pairing.getZr().newRandomElement().getImmutable();
        Element ct0 = params.g.powZn(r).getImmutable();
        List<Element> cti = new ArrayList<>();
        for (int i = 0; i < x.size(); i++) {
            Element gx = params.g.powZn(x.get(i)).getImmutable();
            Element pkr = mpk.pks.get(i).powZn(r).getImmutable();
            cti.add(pkr.mul(gx).getImmutable());
        }
        return new CT(new CT0(ct0), new CTI(cti));
    }

    public DecryptResult decrypt(Params params, CT ct, SKY sky, List<Element> y) {
        Element decryptResult = params.pairing.getG1().newOneElement();
        for (int i = 0; i < y.size(); i++) {
            Element cty = ct.cti.cti.get(i).powZn(y.get(i));
            decryptResult = decryptResult.mul(cty);
        }
        Element ctskf = ct.ct0.ct0.powZn(sky.sky).getImmutable();
        decryptResult = decryptResult.div(ctskf);
        return new DecryptResult(decryptResult);
    }
}