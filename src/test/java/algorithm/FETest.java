package algorithm;


import it.unisa.dia.gas.jpbc.Element;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.*;

public class FETest {
    @Test
    public void testFE() throws IOException {
        int lambda = 256;
        int ell = 10;

        FE fe = new FE();

        // setup
        LocalDateTime startSetup = LocalDateTime.now();
        FE.SetupParams setupParams = fe.setup(lambda, ell);
        LocalDateTime endSetup = LocalDateTime.now();
        FE.Params params = setupParams.mpk.params;

        // init x y
        List<Element> x = new ArrayList<>();
        List<Element> y = new ArrayList<>();
        Element xySum = params.pairing.getZr().newZeroElement();
        for (int i = 0; i < ell; i++) {
            x.add(params.pairing.getZr().newRandomElement().getImmutable());
            y.add(params.pairing.getZr().newRandomElement().getImmutable());
            xySum = xySum.add(x.get(i).mul(y.get(i)));
        }

        // keyDer
        LocalDateTime startKeyDer = LocalDateTime.now();
        FE.SKY sky = fe.keyDer(params, setupParams.msk, y);
        LocalDateTime endKeyDer = LocalDateTime.now();

        // encrypt
        LocalDateTime startEncrypt = LocalDateTime.now();
        FE.CT ct = fe.encrypt(setupParams.mpk, x);
        LocalDateTime endEncrypt = LocalDateTime.now();

        // decrypt
        LocalDateTime startDecrypt = LocalDateTime.now();
        FE.DecryptResult decryptResult = fe.decrypt(params, ct, sky, y);
        LocalDateTime endDecrypt = LocalDateTime.now();

        // assert if correct
        assert decryptResult.decryptResult.equals(params.g.powZn(xySum));

        // time
        Duration setupDuration = Duration.between(startSetup, endSetup);
        Duration keyDerDuration = Duration.between(startKeyDer, endKeyDer);
        Duration encryptDuration = Duration.between(startEncrypt, endEncrypt);
        Duration decryptDuration = Duration.between(startDecrypt, endDecrypt);

        System.out.println("setupTime: " + setupDuration.toMillis());
        System.out.println("keyDerTime: " + keyDerDuration.toMillis());
        System.out.println("encryptTime: " + encryptDuration.toMillis());
        System.out.println("decryptTime: " + decryptDuration.toMillis());
    }

    boolean elementEqual(Element e1, Element e2) {
        return e1.equals(e2);
    }

}