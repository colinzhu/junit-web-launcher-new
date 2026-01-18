package com.junit.launcher;

import io.qameta.allure.Step;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sample test class for testing discovery functionality.
 */
//@ExtendWith(AllureTestExtension.class)
public class SampleTest {
    private static final Logger logger = LoggerFactory.getLogger(SampleTest.class);

    @Test
    void testOne() {
        // Sample test
        System.out.println("Test one executed.");
        runStep();
        logger.info("Test one executed.");
    }

    @Step("Run step")
    void runStep() {
        System.out.println("Step executed.");
    }

    @Test
    void testTwo() {
        // Sample test
        System.out.println("Test two executed.");
        logger.info("Test two executed.");
    }
}
